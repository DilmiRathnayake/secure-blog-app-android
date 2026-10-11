package com.blogspace.app.publish;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the whole "Publish" flow for one or many posts, from any screen:
 * <ol>
 *   <li>Check setup (Blogger blog chosen, or Test mode) and connectivity.</li>
 *   <li>Ask the user to confirm.</li>
 *   <li>Get a Google access token (shows Google's consent screen the first time).</li>
 *   <li>Upload posts one at a time on a background thread, showing progress.</li>
 *   <li>Record each <i>confirmed</i> success in SQLite and show a per-post result summary.</li>
 * </ol>
 * A post is only marked "Published" after Blogger returns its id; failures and Test-mode runs never
 * change the database.
 */
public class PublishController {

    public interface Host {
        /** Called after publishing so the screen can refresh its "Published" badges. */
        void onPublishFinished();

        /** Navigate to the Publish settings screen. */
        void openPublishSettings();
    }

    /** Small pause between uploads to stay well within Blogger's rate limits. */
    private static final long DELAY_BETWEEN_POSTS_MS = 1000;

    private final AppCompatActivity activity;
    private final Host host;
    private final GoogleAuthHelper authHelper;
    private final PublishSettings settings;

    /** Construct in the Activity's onCreate (it registers an Activity Result launcher). */
    public PublishController(@NonNull AppCompatActivity activity, @NonNull Host host) {
        this.activity = activity;
        this.host = host;
        this.authHelper = new GoogleAuthHelper(activity, activity);
        this.settings = new PublishSettings(activity);
    }

    @NonNull
    public GoogleAuthHelper getAuthHelper() {
        return authHelper;
    }

    /** Starts publishing the given post ids (in the given order). */
    public void publish(@NonNull List<Long> postIds) {
        if (postIds.isEmpty()) {
            return;
        }
        final List<Long> ids = new ArrayList<>(postIds);
        PublishSettings.Mode mode = settings.getMode();

        if (mode == PublishSettings.Mode.BLOGGER) {
            if (!settings.isBloggerConfigured()) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.publish_need_setup_title)
                        .setMessage(R.string.publish_need_setup_body)
                        .setPositiveButton(R.string.action_open_settings, (d, w) -> host.openPublishSettings())
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
                return;
            }
            if (!UiUtils.isOnline(activity)) {
                new MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.publish_offline_title)
                        .setMessage(R.string.publish_offline_body)
                        .setPositiveButton(R.string.action_ok, null)
                        .show();
                return;
            }
        }

        String message = mode == PublishSettings.Mode.TEST
                ? activity.getString(R.string.publish_confirm_test)
                : activity.getString(R.string.publish_confirm_blogger, settings.getBlogName());
        new MaterialAlertDialogBuilder(activity)
                .setTitle(activity.getResources().getQuantityString(R.plurals.publish_confirm_title, ids.size(), ids.size()))
                .setMessage(message)
                .setPositiveButton(R.string.action_publish, (d, w) -> startAfterConfirm(ids, mode))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void startAfterConfirm(List<Long> ids, PublishSettings.Mode mode) {
        if (mode == PublishSettings.Mode.TEST) {
            run(ids, new MockPublishService(settings.isSimulateFailure()), true);
            return;
        }
        // Interactive authorisation first (consent screen if needed); after that the background
        // uploads can fetch tokens silently through the TokenProvider.
        authHelper.requestToken(activity, new GoogleAuthHelper.Callback() {
            @Override
            public void onToken(@NonNull String accessToken) {
                String blogId = settings.getBlogId();
                if (blogId == null) {
                    return;
                }
                run(ids, new BloggerPublishService(activity, new BloggerApi(), authHelper, blogId,
                        settings.isIncludeImages()), false);
            }

            @Override
            public void onError(@NonNull String message) {
                showMessage(activity.getString(R.string.publish_auth_failed, message));
            }
        });
    }

    /** One line of the final summary. */
    private static class Outcome {
        final String title;
        @Nullable
        final PublishResult result;
        @Nullable
        final String error;

        Outcome(String title, @Nullable PublishResult result, @Nullable String error) {
            this.title = title;
            this.result = result;
            this.error = error;
        }
    }

    private void run(List<Long> ids, PublishService service, boolean testMode) {
        View view = LayoutInflater.from(activity).inflate(R.layout.dialog_progress, null);
        LinearProgressIndicator bar = view.findViewById(R.id.progress_bar);
        TextView label = view.findViewById(R.id.progress_label);
        bar.setMax(ids.size());
        label.setText(activity.getString(R.string.publish_progress, 1, ids.size()));
        AlertDialog progress = new MaterialAlertDialogBuilder(activity)
                .setTitle(R.string.publish_progress_title)
                .setView(view)
                .setCancelable(false)
                .show();

        PostRepository repo = PostRepository.getInstance(activity);
        AppExecutors.network().execute(() -> {
            List<Outcome> outcomes = new ArrayList<>();
            List<Post> posts = repo.getByIds(ids);
            for (int i = 0; i < posts.size(); i++) {
                Post post = posts.get(i);
                final int number = i + 1;
                AppExecutors.main(() -> {
                    bar.setProgressCompat(number - 1, true);
                    label.setText(activity.getString(R.string.publish_progress, number, posts.size()));
                });
                try {
                    PublishResult result = service.publish(post);
                    if (!result.simulated) {
                        // Only a confirmed real upload is recorded as published.
                        repo.markPublished(post.getId(), result.remoteId, result.url);
                    }
                    outcomes.add(new Outcome(post.getTitle(), result, null));
                } catch (PublishException e) {
                    outcomes.add(new Outcome(post.getTitle(), null, e.describe(activity)));
                    if (e.getKind() == PublishException.Kind.AUTH || e.getKind() == PublishException.Kind.NETWORK
                            || e.getKind() == PublishException.Kind.RATE_LIMIT) {
                        // The remaining posts would fail for the same reason; stop early.
                        for (int j = i + 1; j < posts.size(); j++) {
                            outcomes.add(new Outcome(posts.get(j).getTitle(), null, e.describe(activity)));
                        }
                        break;
                    }
                }
                if (i < posts.size() - 1) {
                    sleepQuietly(DELAY_BETWEEN_POSTS_MS);
                }
            }
            AppExecutors.main(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) {
                    return;
                }
                progress.dismiss();
                showSummary(outcomes, testMode);
                host.onPublishFinished();
            });
        });
    }

    private void showSummary(List<Outcome> outcomes, boolean testMode) {
        StringBuilder sb = new StringBuilder();
        String singleUrl = null;
        int successes = 0;
        for (Outcome o : outcomes) {
            if (sb.length() > 0) {
                sb.append("\n\n");
            }
            if (o.result == null) {
                sb.append(activity.getString(R.string.publish_result_fail, o.title, o.error));
                continue;
            }
            successes++;
            if (o.result.simulated) {
                sb.append(activity.getString(R.string.publish_result_simulated, o.title));
            } else if (o.result.url != null) {
                sb.append(activity.getString(R.string.publish_result_ok, o.title, o.result.url));
                singleUrl = o.result.url;
            } else {
                sb.append(activity.getString(R.string.publish_result_ok_no_url, o.title));
            }
            if (o.result.imageOmitted) {
                sb.append('\n').append(activity.getString(R.string.publish_result_image_omitted));
            }
        }
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity)
                .setTitle(testMode ? R.string.publish_result_test_title : R.string.publish_result_title)
                .setMessage(sb.toString())
                .setPositiveButton(R.string.action_ok, null);
        if (successes == 1 && singleUrl != null) {
            final String url = singleUrl;
            builder.setNeutralButton(R.string.action_view_online, (d, w) -> openUrl(url));
        }
        builder.show();
    }

    private void openUrl(String url) {
        try {
            activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (android.content.ActivityNotFoundException e) {
            showMessage(url);
        }
    }

    private void showMessage(String message) {
        if (activity.isFinishing()) {
            return;
        }
        new MaterialAlertDialogBuilder(activity)
                .setMessage(message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private static void sleepQuietly(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
