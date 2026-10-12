package com.blogspace.app.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.ActivityImagePreviewBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;

/** Full-screen photo from the Gallery with "Open post" and "Remove image". */
public class ImagePreviewActivity extends AppCompatActivity {

    private ActivityImagePreviewBinding binding;
    private long postId;
    @Nullable
    private Post post;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityImagePreviewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), true);
        postId = getIntent().getLongExtra(Navigation.EXTRA_POST_ID, Post.NO_ID);

        binding.btnClose.setOnClickListener(v -> finish());
        binding.btnOpenPost.setOnClickListener(v -> Navigation.openPost(this, postId));
        binding.btnRemove.setOnClickListener(v -> confirmRemove());
        // Tap the photo to hide/show the controls.
        binding.image.setOnClickListener(v -> {
            float target = binding.bottom.getAlpha() > 0.5f ? 0f : 1f;
            binding.bottom.animate().alpha(target).setDuration(200).start();
            binding.btnClose.animate().alpha(target).setDuration(200).start();
        });
        load();
    }

    private void load() {
        Context context = getApplicationContext();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        AppExecutors.disk().execute(() -> {
            Post loaded = PostRepository.getInstance(context).getById(postId);
            File file = loaded == null ? null : ImageStorage.getImageFile(context, loaded.getImageFileName());
            Bitmap bitmap = file == null ? null : ImageStorage.decodeSampled(file, width, height);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (loaded == null || bitmap == null) {
                    Toast.makeText(this, R.string.photo_missing, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                post = loaded;
                binding.caption.setText(loaded.getTitle());
                binding.image.setImageBitmap(bitmap);
                binding.image.setScaleX(0.96f);
                binding.image.setScaleY(0.96f);
                binding.image.animate().scaleX(1f).scaleY(1f).setDuration(250).start();
            });
        });
    }

    private void confirmRemove() {
        if (post == null) {
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_delete)
                .setTitle(R.string.remove_image_title)
                .setMessage(getString(R.string.remove_image_body, post.getTitle()))
                .setPositiveButton(R.string.action_remove_image, (d, w) -> remove())
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void remove() {
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            boolean ok = PostRepository.getInstance(context).removeImage(postId);
            AppExecutors.main(() -> {
                Toast.makeText(context, ok ? R.string.image_removed : R.string.error_save, Toast.LENGTH_SHORT).show();
                if (ok) {
                    finish();
                }
            });
        });
    }
}
