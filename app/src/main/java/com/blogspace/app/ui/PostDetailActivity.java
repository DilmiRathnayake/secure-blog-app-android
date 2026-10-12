package com.blogspace.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.ActivityPostDetailBinding;
import com.blogspace.app.publish.PublishController;
import com.blogspace.app.ui.widget.StatusBadge;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Collections;

/**
 * Shows one post in full: collapsing photo header (or gradient when there is no photo), status
 * badge, title, dates and text, with Edit, Delete, Share and Upload actions.
 */
public class PostDetailActivity extends AppCompatActivity implements PublishController.Host {

    private ActivityPostDetailBinding binding;
    private PublishController publishController;
    private long postId;
    @Nullable
    private Post post;
    @Nullable
    private String pendingAutoAction;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Header draws behind the status bar; only the bottom action bar needs padding.
        ViewCompat.setOnApplyWindowInsetsListener(binding.actionBar, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    getResources().getDimensionPixelSize(R.dimen.space_s) + bars.bottom);
            return insets;
        });
        setSupportActionBar(binding.toolbar);
        setTitle("");
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        postId = getIntent().getLongExtra(Navigation.EXTRA_POST_ID, Post.NO_ID);
        if (savedInstanceState == null) {
            pendingAutoAction = getIntent().getStringExtra(Navigation.EXTRA_AUTO_ACTION);
        }
        publishController = new PublishController(this, this);

        binding.btnEdit.setOnClickListener(v -> Navigation.editPost(this, postId));
        binding.btnDelete.setOnClickListener(v -> confirmDelete());
        binding.btnShare.setOnClickListener(v -> share());
        binding.btnPublish.setOnClickListener(v -> publish());
        binding.btnViewOnline.setOnClickListener(v -> openRemoteUrl());
    }

    @Override
    protected void onResume() {
        super.onResume();
        load(); // reload every time, so changes made in the editor appear immediately
    }

    private void load() {
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            Post loaded = PostRepository.getInstance(context).getById(postId);
            AppExecutors.main(() -> {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                if (loaded == null) {
                    Toast.makeText(this, R.string.error_post_missing, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                post = loaded;
                bind(loaded);
                runAutoAction();
            });
        });
    }

    /** "Save & publish" / "Save & share" from the editor continue here once the post is visible. */
    private void runAutoAction() {
        if (pendingAutoAction == null) {
            return;
        }
        String action = pendingAutoAction;
        pendingAutoAction = null;
        if (Navigation.ACTION_PUBLISH.equals(action)) {
            publish();
        } else if (Navigation.ACTION_SHARE.equals(action)) {
            share();
        }
    }

    private void bind(@NonNull Post p) {
        binding.title.setText(p.getTitle().isEmpty() ? getString(R.string.untitled_draft) : p.getTitle());
        binding.content.setText(p.getContent());
        StatusBadge.apply(binding.statusBadge, p.getStatus());
        String dates = getString(R.string.created_on, UiUtils.fullDateTime(p.getCreatedAt()));
        if (p.getUpdatedAt() != p.getCreatedAt()) {
            dates += "  ·  " + getString(R.string.updated_on, UiUtils.fullDateTime(p.getUpdatedAt()));
        }
        binding.dates.setText(dates);

        binding.publishedRow.setVisibility(p.isPublished() && !p.isDraft() ? View.VISIBLE : View.GONE);
        if (p.isPublished()) {
            binding.publishedText.setText(getString(R.string.published_on, UiUtils.fullDateTime(p.getPublishedAt())));
            binding.btnViewOnline.setVisibility(p.getRemoteUrl() == null ? View.GONE : View.VISIBLE);
        }
        bindImage(p);
    }

    private void bindImage(@NonNull Post p) {
        binding.imageMissing.setVisibility(View.GONE);
        if (!p.hasImage()) {
            binding.image.setVisibility(View.GONE);
            binding.headerIcon.setVisibility(View.VISIBLE);
            return;
        }
        int width = getResources().getDisplayMetrics().widthPixels;
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            File file = ImageStorage.getImageFile(context, p.getImageFileName());
            Bitmap bitmap = file == null ? null : ImageStorage.decodeSampled(file, width, width);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (bitmap == null) {
                    binding.image.setVisibility(View.GONE);
                    binding.headerIcon.setVisibility(View.VISIBLE);
                    binding.imageMissing.setVisibility(View.VISIBLE);
                } else {
                    binding.image.setImageBitmap(bitmap);
                    binding.headerIcon.setVisibility(View.GONE);
                    if (binding.image.getVisibility() != View.VISIBLE) {
                        binding.image.setAlpha(0f);
                        binding.image.setVisibility(View.VISIBLE);
                        binding.image.animate().alpha(1f).setDuration(300).start();
                    }
                }
            });
        });
    }

    // ---------------------------------------------------------------- actions

    private void share() {
        if (post != null) {
            ShareSheet.show(this, post);
        }
    }

    private void publish() {
        if (post == null) {
            return;
        }
        if (post.isDraft()) {
            new MaterialAlertDialogBuilder(this)
                    .setIcon(R.drawable.ic_draft)
                    .setTitle(R.string.draft_cannot_publish_title)
                    .setMessage(R.string.draft_cannot_publish_body)
                    .setPositiveButton(R.string.action_edit, (d, w) -> Navigation.editPost(this, postId))
                    .setNegativeButton(R.string.action_cancel, null)
                    .show();
            return;
        }
        publishController.publish(Collections.singletonList(postId));
    }

    private void confirmDelete() {
        if (post == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_one_title)
                .setMessage(getString(post.hasImage() ? R.string.delete_one_message_photo : R.string.delete_one_message, post.getTitle()))
                .setPositiveButton(R.string.action_delete, (d, w) -> delete())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void delete() {
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            boolean ok;
            try {
                ok = PostRepository.getInstance(context).delete(postId);
            } catch (RuntimeException e) {
                ok = false;
            }
            final boolean deleted = ok;
            AppExecutors.main(() -> {
                Toast.makeText(context, deleted ? R.string.deleted_one : R.string.error_delete,
                        Toast.LENGTH_SHORT).show();
                if (deleted) {
                    finish();
                }
            });
        });
    }

    private void openRemoteUrl() {
        if (post == null || post.getRemoteUrl() == null) {
            return;
        }
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(post.getRemoteUrl())));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, post.getRemoteUrl(), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------------------------------------------------------- PublishController.Host

    @Override
    public void onPublishFinished() {
        load();
    }

    @Override
    public void openPublishSettings() {
        startActivity(new Intent(this, PublishSettingsActivity.class));
    }
}
