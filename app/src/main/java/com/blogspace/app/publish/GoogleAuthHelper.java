package com.blogspace.app.publish;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.util.Log;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.identity.AuthorizationRequest;
import com.google.android.gms.auth.api.identity.AuthorizationResult;
import com.google.android.gms.auth.api.identity.Identity;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Tasks;

import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Gets OAuth 2.0 access tokens for the Blogger API using Google Play services'
 * AuthorizationClient (the current Google-recommended API for requesting scopes on Android).
 * <p>
 * Security: the user signs in and consents on Google's own screens. The app never sees the
 * password, there is no client secret in the app (Android OAuth clients are identified by package
 * name + signing-certificate SHA-1), and tokens are only held in memory for the current request.
 */
public class GoogleAuthHelper implements TokenProvider {

    private static final String TAG = "GoogleAuthHelper";
    private static final long TOKEN_TIMEOUT_SECONDS = 30;

    public interface Callback {
        void onToken(@NonNull String accessToken);

        void onError(@NonNull String message);
    }

    private final Context appContext;
    private final ActivityResultLauncher<IntentSenderRequest> consentLauncher;
    @Nullable
    private Callback pendingCallback;

    /**
     * Must be constructed while the Activity/Fragment is being created, because it registers an
     * Activity Result launcher for Google's consent screen.
     */
    public GoogleAuthHelper(@NonNull Context context, @NonNull ActivityResultCaller caller) {
        this.appContext = context.getApplicationContext();
        this.consentLauncher = caller.registerForActivityResult(
                new ActivityResultContracts.StartIntentSenderForResult(), this::onConsentResult);
    }

    private static AuthorizationRequest bloggerRequest() {
        return AuthorizationRequest.builder()
                .setRequestedScopes(Collections.singletonList(new Scope(BloggerApi.SCOPE)))
                .build();
    }

    /**
     * Interactive: shows Google account selection / consent if needed, then returns a token.
     * Call on the main thread.
     */
    public void requestToken(@NonNull Activity activity, @NonNull Callback callback) {
        Identity.getAuthorizationClient(activity)
                .authorize(bloggerRequest())
                .addOnSuccessListener(result -> {
                    if (result.hasResolution()) {
                        PendingIntent pendingIntent = result.getPendingIntent();
                        if (pendingIntent == null) {
                            callback.onError("No consent screen available");
                            return;
                        }
                        pendingCallback = callback;
                        consentLauncher.launch(
                                new IntentSenderRequest.Builder(pendingIntent.getIntentSender()).build());
                    } else {
                        deliver(result, callback);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "authorize failed", e);
                    callback.onError(describe(e));
                });
    }

    private void onConsentResult(@NonNull ActivityResult activityResult) {
        Callback callback = pendingCallback;
        pendingCallback = null;
        if (callback == null) {
            return; // e.g. Activity was recreated while the consent screen was open
        }
        if (activityResult.getResultCode() != Activity.RESULT_OK) {
            callback.onError(appContext.getString(com.blogspace.app.R.string.publish_signin_cancelled));
            return;
        }
        try {
            AuthorizationResult result = Identity.getAuthorizationClient(appContext)
                    .getAuthorizationResultFromIntent(activityResult.getData());
            deliver(result, callback);
        } catch (ApiException e) {
            callback.onError(describe(e));
        }
    }

    private static void deliver(@NonNull AuthorizationResult result, @NonNull Callback callback) {
        String token = result.getAccessToken();
        if (token == null) {
            callback.onError("Google did not return an access token");
        } else {
            callback.onToken(token);
        }
    }

    // ---------------------------------------------------------------- TokenProvider (background)

    @WorkerThread
    @NonNull
    @Override
    public String getToken() throws PublishException {
        try {
            AuthorizationResult result = Tasks.await(
                    Identity.getAuthorizationClient(appContext).authorize(bloggerRequest()),
                    TOKEN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (result.hasResolution() || result.getAccessToken() == null) {
                // Consent was revoked; the user has to reconnect interactively.
                throw new PublishException(PublishException.Kind.AUTH, "Re-authorisation required");
            }
            return result.getAccessToken();
        } catch (ExecutionException | TimeoutException e) {
            throw new PublishException(PublishException.Kind.AUTH, describe(e), 0, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishException(PublishException.Kind.AUTH, "Interrupted", 0, e);
        }
    }

    @WorkerThread
    @Override
    public void invalidate(@NonNull String token) {
        try {
            GoogleAuthUtil.clearToken(appContext, token);
        } catch (Exception e) {
            Log.w(TAG, "clearToken failed", e);
        }
    }

    @NonNull
    private static String describe(@NonNull Exception e) {
        Throwable cause = e.getCause() != null ? e.getCause() : e;
        if (cause instanceof ApiException) {
            ApiException api = (ApiException) cause;
            // Status 10 (DEVELOPER_ERROR) almost always means the OAuth client's package name or
            // SHA-1 does not match this build; see docs/PUBLISHING_SETUP.md.
            if (api.getStatusCode() == 10) {
                return "OAuth client not configured for this app signature (DEVELOPER_ERROR 10). "
                        + "Check the package name and SHA-1 in Google Cloud Console.";
            }
            return "Google error " + api.getStatusCode() + ": " + api.getMessage();
        }
        return cause.getMessage() == null ? cause.getClass().getSimpleName() : cause.getMessage();
    }
}
