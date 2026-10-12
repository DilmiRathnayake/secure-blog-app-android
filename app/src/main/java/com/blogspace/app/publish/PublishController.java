package com.blogspace.app.publish;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.drawable.Animatable;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.BottomSheetUploadBinding;
import com.blogspace.app.databinding.ItemUploadRowBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Runs the whole "Upload" flow for one or many posts, from any screen:
 * <ol>
 *   <li>Check setup (Blogger blog chosen, or Test mode) and connectivity.</li>
 *   <li>Ask the user to confirm.</li>
 *   <li>Get a Google access token (shows Google's consent screen the first time).</li>
 *   <li>Upload posts one at a time on a background thread, showing an animated progress sheet
 *       with a row per post (waiting → uploading → done / failed).</li>
 *   <li>Record each <i>confirmed</i> success in SQLite; offer Retry for failures.</li>
 * </ol>
 * A post is only marked "Uploaded" after Blogger returns its id; failures and Test-mode runs never
 * change the database. Drafts are skipped and never uploaded.
 */
public class PublishController {

    public interface Host {
        /** Called after publishing so the screen can refresh its status badges. */
        void onPublishFinished();

        /** Navigate to the publishing settings screen. */
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
                        .setIcon(R.drawable.ic_settings)
                        .setTitle(R.string.publish_need_setup_title)
                        .setMessage(R.string.publish_need_setup_body)
                        .setPositiveButton(R.string.action_open_settings, (d, w) -> host.openPublishSettings())
                        .setNegativeButton(R.string.action_cancel, null)
                        .show();
                return;
            }
            if (!UiUtils.isOnline(activity)) {
                showConnectionLost();
                return;
            }
        }

        String message = mode == PublishSettings.Mode.TEST
                ? activity.getString(R.string.publish_confirm_test)
                : activity.getString(R.string.publish_confirm_blogger, settings.getBlogName());
        new MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.ic_cloud_upload)
                .setTitle(activity.getResources().getQuantityString(R.plurals.publish_confirm_title, ids.size(), ids.size()))
                .setMessage(message)
                .setPositiveButton(R.string.action_publish, (d, w) -> startAfterConfirm(ids, mode))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void showConnectionLost() {
        new MaterialAlertDialogBuilder(activity)
                .setIcon(R.drawable.ic_wifi_off)
                .setTitle(R.string.connection_lost_title)
                .setMessage(R.string.publish_offline_body)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    private void startAfterConfirm(List<Long> ids, PublishSettings.Mode mode) {
        if (mode == PublishSettings.Mode.TEST) {
            new UploadRun(ids, new MockPublishService(settings.isSimulateFailure()), true).start();
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
                new UploadRun(ids, new BloggerPublishService(activity, new BloggerApi(), authHelper, blogId,
                        settings.isIncludeImages()), false).start();
            }

            @Override
            public void onError(@NonNull String message) {
                showMessage(activity.getString(R.string.publish_auth_failed, message));
            }
        });
    }

    /** One upload session shown in a bottom sheet; can be retried for the posts that failed. */
    private final class UploadRun {
        private final List<Long> ids;
        private final PublishService service;
        private final boolean testMode;
        private BottomSheetDialog sheet;
        private BottomSheetUploadBinding b;
        private ObjectAnimator pulse;
        private final List<ItemUploadRowBinding> rows = new ArrayList<>();

        UploadRun(List<Long> ids, PublishService service, boolean testMode) {
            this.ids = ids;
            this.service = service;
            this.testMode = testMode;
        }

        void start() {
            b = BottomSheetUploadBinding.inflate(LayoutInflater.from(activity));
            sheet = new BottomSheetDialog(activity);
            sheet.setContentView(b.getRoot());
            sheet.setCancelable(false); // stays until the run finishes
            sheet.show();
            begin(ids);
        }

        private void begin(List<Long> runIds) {
            rows.clear();
            b.rows.removeAllViews();
            b.buttons.setVisibility(View.GONE);
            b.note.setVisibility(View.GONE);
            b.resultIcon.setVisibility(View.GONE);
            b.centerGroup.setVisibility(View.VISIBLE);
            b.ring.setVisibility(View.VISIBLE);
            b.ring.setProgressCompat(0, false);
            b.percent.setText(activity.getString(R.string.upload_percent, 0));
            b.title.setText(R.string.upload_in_progress);
            b.subtitle.setText(testMode ? activity.getString(R.string.upload_dest_test)
                    : activity.getString(R.string.upload_dest_blogger, settings.getBlogName()));
            // Cloud icon gently bobs while uploading.
            pulse = ObjectAnimator.ofFloat(b.cloud, View.TRANSLATION_Y, 0f, -10f);
            pulse.setDuration(650);
            pulse.setRepeatMode(ValueAnimator.REVERSE);
            pulse.setRepeatCount(ValueAnimator.INFINITE);
            pulse.start();

            PostRepository repo = PostRepository.getInstance(activity);
            AppExecutors.network().execute(() -> {
                List<Post> loaded = repo.getByIds(runIds);
                List<Post> posts = new ArrayList<>();
                int skippedDrafts = 0;
                for (Post p : loaded) {
                    if (p.isDraft()) {
                        skippedDrafts++;
                    } else {
                        posts.add(p);
                    }
                }
                final int drafts = skippedDrafts;
                AppExecutors.main(() -> buildRows(posts, drafts));

                List<Long> failed = new ArrayList<>();
                int ok = 0;
                for (int i = 0; i < posts.size(); i++) {
                    Post post = posts.get(i);
                    final int index = i;
                    AppExecutors.main(() -> setRowUploading(index));
                    try {
                        PublishResult result = service.publish(post);
                        if (!result.simulated) {
                            // Only a confirmed real upload is recorded as published.
                            repo.markPublished(post.getId(), result.remoteId, result.url);
                        }
                        ok++;
                        AppExecutors.main(() -> setRowDone(index, result));
                    } catch (PublishException e) {
                        String reason = e.describe(activity);
                        failed.add(post.getId());
                        AppExecutors.main(() -> setRowFailed(index, reason));
                        if (e.getKind() == PublishException.Kind.AUTH || e.getKind() == PublishException.Kind.NETWORK
                                || e.getKind() == PublishException.Kind.RATE_LIMIT) {
                            // The remaining posts would fail for the same reason; stop early.
                            for (int j = i + 1; j < posts.size(); j++) {
                                failed.add(posts.get(j).getId());
                                final int jj = j;
                                AppExecutors.main(() -> setRowFailed(jj, reason));
                            }
                            break;
                        }
                    }
                    final int progress = (int) ((i + 1) * 100f / Math.max(1, posts.size()));
                    AppExecutors.main(() -> setProgress(progress));
                    if (i < posts.size() - 1) {
                        sleepQuietly(DELAY_BETWEEN_POSTS_MS);
                    }
                }
                final int successes = ok;
                AppExecutors.main(() -> finish(successes, failed, posts.size()));
            });
        }

        private void buildRows(List<Post> posts, int skippedDrafts) {
            if (activity.isDestroyed()) {
                return;
            }
            for (Post p : posts) {
                ItemUploadRowBinding row = ItemUploadRowBinding.inflate(LayoutInflater.from(activity), b.rows, true);
                row.rowTitle.setText(p.getTitle());
                row.stateIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.bs_outline));
                rows.add(row);
            }
            if (skippedDrafts > 0) {
                b.note.setVisibility(View.VISIBLE);
                b.note.setText(activity.getResources().getQuantityString(R.plurals.drafts_skipped, skippedDrafts, skippedDrafts));
            }
        }

        private void setRowUploading(int index) {
            if (index < rows.size()) {
                rows.get(index).spinner.setVisibility(View.VISIBLE);
                rows.get(index).stateIcon.setVisibility(View.INVISIBLE);
            }
        }

        private void setRowDone(int index, PublishResult result) {
            if (index >= rows.size()) {
                return;
            }
            ItemUploadRowBinding row = rows.get(index);
            row.spinner.setVisibility(View.GONE);
            row.stateIcon.setVisibility(View.VISIBLE);
            row.stateIcon.setImageResource(R.drawable.ic_check_circle);
            row.stateIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_success));
            row.stateIcon.setScaleX(0f);
            row.stateIcon.setScaleY(0f);
            row.stateIcon.animate().scaleX(1f).scaleY(1f).setDuration(220).start();
            String detail;
            if (result.simulated) {
                detail = activity.getString(R.string.upload_simulated_row);
            } else {
                detail = result.url != null ? result.url : "";
                if (result.imageOmitted) {
                    detail += "\n" + activity.getString(R.string.publish_result_image_omitted).trim();
                }
            }
            if (!detail.isEmpty()) {
                row.rowDetail.setVisibility(View.VISIBLE);
                row.rowDetail.setText(detail);
            }
        }

        private void setRowFailed(int index, String reason) {
            if (index >= rows.size()) {
                return;
            }
            ItemUploadRowBinding row = rows.get(index);
            row.spinner.setVisibility(View.GONE);
            row.stateIcon.setVisibility(View.VISIBLE);
            row.stateIcon.setImageResource(R.drawable.ic_error);
            row.stateIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_error));
            row.rowDetail.setVisibility(View.VISIBLE);
            row.rowDetail.setText(reason);
            row.rowDetail.setTextColor(ContextCompat.getColor(activity, R.color.status_error));
        }

        private void setProgress(int percent) {
            if (activity.isDestroyed()) {
                return;
            }
            b.ring.setProgressCompat(percent, true);
            b.percent.setText(activity.getString(R.string.upload_percent, percent));
        }

        private void finish(int successes, List<Long> failed, int total) {
            if (pulse != null) {
                pulse.cancel();
            }
            host.onPublishFinished();
            if (activity.isFinishing() || activity.isDestroyed()) {
                return;
            }
            b.centerGroup.setVisibility(View.GONE);
            b.ring.setVisibility(View.INVISIBLE);
            b.resultIcon.setVisibility(View.VISIBLE);
            if (failed.isEmpty() && total > 0) {
                b.resultIcon.setImageResource(R.drawable.avd_success_check);
                if (b.resultIcon.getDrawable() instanceof Animatable) {
                    ((Animatable) b.resultIcon.getDrawable()).start();
                }
                b.title.setText(testMode ? R.string.upload_simulated_title : R.string.upload_done_title);
            } else {
                b.resultIcon.setImageResource(R.drawable.ic_error);
                b.resultIcon.setImageTintList(ContextCompat.getColorStateList(activity, R.color.status_error));
                b.title.setText(successes > 0 ? R.string.upload_partial_title : R.string.upload_failed_title);
            }
            b.buttons.setVisibility(View.VISIBLE);
            b.btnRetry.setVisibility(failed.isEmpty() ? View.GONE : View.VISIBLE);
            b.btnRetry.setOnClickListener(v -> {
                if (!testMode && !UiUtils.isOnline(activity)) {
                    showConnectionLost();
                    return;
                }
                b.resultIcon.setImageTintList(null);
                begin(failed);
            });
            b.btnDone.setOnClickListener(v -> sheet.dismiss());
            sheet.setCancelable(true);
        }
    }

    private void showMessage(@Nullable String message) {
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
