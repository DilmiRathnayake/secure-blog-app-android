package com.blogspace.app.publish;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/** Supplies OAuth access tokens to the publishing code (implemented by {@link GoogleAuthHelper}). */
public interface TokenProvider {

    /** Returns a valid access token without showing UI, or throws AUTH if the user must sign in again. */
    @WorkerThread
    @NonNull
    String getToken() throws PublishException;

    /** Tells Google Play services a token was rejected, so the next call fetches a fresh one. */
    @WorkerThread
    void invalidate(@NonNull String token);
}
