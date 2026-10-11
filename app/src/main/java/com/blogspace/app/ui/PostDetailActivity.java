package com.blogspace.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.ActivityPostDetailBinding;
import com.blogspace.app.publish.PublishController;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.ShareHelper;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.util.Collections;

/** Shows one post in full, with Edit, Delete, Share (any app / email) and Publish actions. */
public class PostDetailActivity extends AppCompatActivity implements PublishController.Host {

    private ActivityPostDetailBinding binding;
    private PublishController publishController;
    private long postId;
    @Nullable
    private Post post;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityPostDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), true);
        setSupportActionBar(binding.toolbar);
        setTitle("");
        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        postId = getIntent().getLongExtra(Navigation.EXTRA_POST_ID, Post.NO_ID);
        publishController = new PublishController(this, this);

        binding.btnShare.setOnClickListener(v -> chooseShareMethod());
        binding.btnPublish.setOnClickListener(v -> publishController.publish(Collections.singletonList(postId)));
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
            });
        });
    }

    private void bind(@NonNull Post p) {
        binding.title.setText(p.getTitle());
        binding.content.setText(p.getContent());
        String dates = getString(R.string.created_on, UiUtils.fullDateTime(p.getCreatedAt()));
        if (p.getUpdatedAt() != p.getCreatedAt()) {
            dates += "\n" + getString(R.string.updated_on, UiUtils.fullDateTime(p.getUpdatedAt()));
        }
        binding.dates.setText(dates);

        binding.publishedRow.setVisibility(p.isPublished() ? View.VISIBLE : View.GONE);
        if (p.isPublished()) {
            binding.publishedText.setText(getString(R.string.published_on, UiUtils.fullDateTime(p.getPublishedAt())));
            binding.btnViewOnline.setVisibility(p.getRemoteUrl() == null ? View.GONE : View.VISIBLE);
        }
        bindImage(p);
    }

    private void bindImage(@NonNull Post p) {
        binding.imageMissing.setVisibility(View.GONE);
        if (!p.hasImage()) {
            binding.imageCard.setVisibility(View.GONE);
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
                    binding.imageCard.setVisibility(View.GONE);
                    binding.imageMissing.setVisibility(View.VISIBLE);
                } else {
                    binding.image.setImageBitmap(bitmap);
                    binding.imageCard.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    // ---------------------------------------------------------------- actions

    private void chooseShareMethod() {
        if (post == null) {
            return;
        }
        final Post current = post;
        CharSequence[] options = {getString(R.string.share_any_app), getString(R.string.share_email)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.share_choose_title)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        ShareHelper.shareWithChooser(this, current);
                    } else {
                        ShareHelper.shareByEmail(this, current);
                    }
                })
                .show();
    }

    private void confirmDelete() {
        if (post == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_one_title)
                .setMessage(getString(R.string.delete_one_message, post.getTitle()))
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

    // ---------------------------------------------------------------- menu

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_edit) {
            Navigation.editPost(this, postId);
            return true;
        } else if (item.getItemId() == R.id.action_delete) {
            confirmDelete();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ---------------------------------------------------------------- PublishController.Host

    @Override
    public void onPublishFinished() {
        load();
    }

    @Override
    public void openPublishSettings() {
        startActivity(MainActivity.publishSettingsIntent(this));
    }
}
