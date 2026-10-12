package com.blogspace.app.ui;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.data.PostValidator;
import com.blogspace.app.databinding.ActivityEditPostBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;

/**
 * Creates a new post or edits an existing one (same screen; editing keeps the same post id, so no
 * duplicate row is created). Posts can be saved as a finished post ("Save offline") or as a
 * Draft. The menu also offers "Save &amp; publish online" and "Save &amp; share".
 * <p>
 * Photo handling:
 * <ul>
 *   <li><b>Gallery</b> – Android Photo Picker (no storage permission needed).</li>
 *   <li><b>Camera</b> – the device camera app writes to a temporary file we expose through
 *       FileProvider (no CAMERA permission needed because we do not use the camera directly).</li>
 * </ul>
 * Either way the image is copied into app storage. Files created during this editing session are
 * tracked so that photos which end up unused (replaced, removed, or the edit is discarded) are
 * deleted instead of being left behind. The post's original photo is only deleted after a
 * successful save that replaced or removed it.
 */
public class EditPostActivity extends AppCompatActivity {

    private static final String STATE_ORIGINAL_IMAGE = "original_image";
    private static final String STATE_CURRENT_IMAGE = "current_image";
    private static final String STATE_SESSION_IMAGES = "session_images";
    private static final String STATE_CAMERA_PATH = "camera_path";
    private static final String STATE_INITIAL_TITLE = "initial_title";
    private static final String STATE_INITIAL_CONTENT = "initial_content";
    private static final String STATE_CREATED_AT = "created_at";
    private static final String STATE_WAS_DRAFT = "was_draft";

    private ActivityEditPostBinding binding;
    private long postId = Post.NO_ID;
    private long createdAt;
    private boolean wasDraft;

    /** Photo stored in the database when editing began (may be null). */
    @Nullable
    private String originalImage;
    /** Photo currently shown / to be saved (may be null). */
    @Nullable
    private String currentImage;
    /** Photos imported during this session; unused ones are cleaned up. */
    private ArrayList<String> sessionImages = new ArrayList<>();
    /** Temporary file the camera app is writing to, if a capture is in progress. */
    @Nullable
    private String cameraTempPath;

    private String initialTitle = "";
    private String initialContent = "";
    private boolean saving;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickImage =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onImagePicked);
    private final ActivityResultLauncher<Uri> takePicture =
            registerForActivityResult(new ActivityResultContracts.TakePicture(), this::onPictureTaken);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), true);
        setSupportActionBar(binding.toolbar);

        postId = getIntent().getLongExtra(Navigation.EXTRA_POST_ID, Post.NO_ID);
        boolean isNew = postId == Post.NO_ID;
        setTitle(isNew ? R.string.title_new_post : R.string.title_edit_post);
        binding.btnSave.setText(isNew ? R.string.action_save_offline : R.string.action_save_changes);
        binding.titleLayout.setCounterMaxLength(PostValidator.MAX_TITLE_LENGTH);

        binding.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                attemptClose();
            }
        });

        binding.btnGallery.setOnClickListener(v -> launchGallery());
        binding.btnCamera.setOnClickListener(v -> launchCamera());
        binding.btnReplacePhoto.setOnClickListener(v -> chooseReplacementSource());
        binding.btnRemovePhoto.setOnClickListener(v -> {
            currentImage = null;
            renderImage();
            Toast.makeText(this, R.string.photo_removed, Toast.LENGTH_SHORT).show();
        });
        binding.btnSave.setOnClickListener(v -> save(false, null));
        binding.btnDraft.setOnClickListener(v -> save(true, null));
        clearErrorOnEdit(binding.titleLayout);
        clearErrorOnEdit(binding.contentLayout);
        binding.contentInput.addTextChangedListener(new SimpleWatcher(this::updateCounter));
        updateCounter();

        if (savedInstanceState != null) {
            restoreState(savedInstanceState);
            renderImage();
        } else if (!isNew) {
            loadPost();
        } else {
            renderImage();
            // Shortcut from the Gallery tab: jump straight into the camera or photo picker.
            String startWith = getIntent().getStringExtra(Navigation.EXTRA_START_WITH);
            if (Navigation.START_CAMERA.equals(startWith)) {
                launchCamera();
            } else if (Navigation.START_GALLERY.equals(startWith)) {
                launchGallery();
            }
        }
    }

    private void loadPost() {
        setFormEnabled(false);
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            Post post = PostRepository.getInstance(context).getById(postId);
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (post == null) {
                    Toast.makeText(this, R.string.error_post_missing, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                // Prepopulate the form with the saved values.
                initialTitle = post.getTitle();
                initialContent = post.getContent();
                createdAt = post.getCreatedAt();
                wasDraft = post.isDraft();
                originalImage = post.getImageFileName();
                currentImage = originalImage;
                binding.titleInput.setText(initialTitle);
                binding.contentInput.setText(initialContent);
                setFormEnabled(true);
                renderImage();
            });
        });
    }

    private void updateCounter() {
        int length = contentText().length();
        binding.charCounter.setText(getString(R.string.char_counter, length, PostValidator.MAX_CONTENT_LENGTH));
        binding.charCounter.setTextColor(ContextCompat.getColor(this,
                length > PostValidator.MAX_CONTENT_LENGTH ? R.color.status_error : R.color.bs_outline));
    }

    // ---------------------------------------------------------------- photo: gallery & camera

    private void launchGallery() {
        pickImage.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                .build());
    }

    private void chooseReplacementSource() {
        CharSequence[] options = {getString(R.string.action_camera), getString(R.string.action_gallery)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.action_replace)
                .setItems(options, (d, which) -> {
                    if (which == 0) {
                        launchCamera();
                    } else {
                        launchGallery();
                    }
                })
                .show();
    }

    private void onImagePicked(@Nullable Uri uri) {
        if (uri == null) {
            Toast.makeText(this, R.string.photo_pick_cancelled, Toast.LENGTH_SHORT).show();
            return;
        }
        importImage(uri, null);
    }

    private void launchCamera() {
        File temp;
        try {
            temp = ImageStorage.createCameraTempFile(this);
        } catch (IOException e) {
            Toast.makeText(this, R.string.error_camera_file, Toast.LENGTH_LONG).show();
            return;
        }
        cameraTempPath = temp.getAbsolutePath();
        try {
            takePicture.launch(ImageStorage.getShareUri(this, temp));
        } catch (ActivityNotFoundException e) {
            deleteCameraTemp();
            Toast.makeText(this, R.string.error_no_camera, Toast.LENGTH_LONG).show();
        }
    }

    private void onPictureTaken(Boolean success) {
        if (cameraTempPath == null) {
            return;
        }
        File temp = new File(cameraTempPath);
        if (Boolean.TRUE.equals(success) && temp.length() > 0) {
            importImage(Uri.fromFile(temp), temp);
        } else {
            deleteCameraTemp();
            Toast.makeText(this, R.string.photo_capture_cancelled, Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteCameraTemp() {
        if (cameraTempPath != null) {
            //noinspection ResultOfMethodCallIgnored
            new File(cameraTempPath).delete();
            cameraTempPath = null;
        }
    }

    /** Copies the image into app storage on a background thread, then shows it. */
    private void importImage(@NonNull Uri source, @Nullable File tempToDelete) {
        binding.previewCard.setVisibility(View.VISIBLE);
        binding.previewProgress.setVisibility(View.VISIBLE);
        setPhotoButtonsEnabled(false);
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            String name = null;
            try {
                name = ImageStorage.importImage(context, source);
            } catch (IOException | SecurityException | OutOfMemoryError e) {
                // Unreadable/unsupported file or permission revoked: handled below.
            }
            if (tempToDelete != null) {
                //noinspection ResultOfMethodCallIgnored
                tempToDelete.delete();
            }
            final String imported = name;
            AppExecutors.main(() -> {
                if (tempToDelete != null) {
                    cameraTempPath = null;
                }
                if (isDestroyed()) {
                    ImageStorage.deleteImage(context, imported); // nobody will use it
                    return;
                }
                binding.previewProgress.setVisibility(View.GONE);
                setPhotoButtonsEnabled(true);
                if (imported == null) {
                    Toast.makeText(this, R.string.error_photo_import, Toast.LENGTH_LONG).show();
                } else {
                    sessionImages.add(imported);
                    currentImage = imported;
                    Toast.makeText(this, R.string.photo_attached, Toast.LENGTH_SHORT).show();
                }
                renderImage();
            });
        });
    }

    private void renderImage() {
        binding.previewProgress.setVisibility(View.GONE);
        if (currentImage == null) {
            binding.previewCard.setVisibility(View.GONE);
            binding.noPhoto.setText(R.string.photo_none);
            return;
        }
        final String name = currentImage;
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDimensionPixelSize(R.dimen.preview_height);
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            File file = ImageStorage.getImageFile(context, name);
            Bitmap bitmap = file == null ? null : ImageStorage.decodeSampled(file, width, height);
            AppExecutors.main(() -> {
                if (isDestroyed() || !Objects.equals(name, currentImage)) {
                    return;
                }
                boolean wasHidden = binding.previewCard.getVisibility() != View.VISIBLE;
                binding.previewCard.setVisibility(View.VISIBLE);
                if (bitmap == null) {
                    // Attached file is missing: say so, and let the user remove or replace it.
                    binding.preview.setImageResource(R.drawable.ill_no_images);
                    binding.preview.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
                    binding.noPhoto.setText(R.string.photo_missing);
                } else {
                    binding.preview.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                    binding.preview.setImageBitmap(bitmap);
                    binding.noPhoto.setText(R.string.photo_attached);
                }
                if (wasHidden) {
                    // "Beautiful attachment preview": card scales in.
                    binding.previewCard.setScaleX(0.94f);
                    binding.previewCard.setScaleY(0.94f);
                    binding.previewCard.setAlpha(0f);
                    binding.previewCard.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(260).start();
                }
            });
        });
    }

    private void setPhotoButtonsEnabled(boolean enabled) {
        binding.btnGallery.setEnabled(enabled);
        binding.btnCamera.setEnabled(enabled);
        binding.btnRemovePhoto.setEnabled(enabled);
        binding.btnReplacePhoto.setEnabled(enabled);
        binding.btnSave.setEnabled(enabled && !saving);
        binding.btnDraft.setEnabled(enabled && !saving);
    }

    // ---------------------------------------------------------------- save / validation

    private String titleText() {
        return binding.titleInput.getText() == null ? "" : binding.titleInput.getText().toString();
    }

    private String contentText() {
        return binding.contentInput.getText() == null ? "" : binding.contentInput.getText().toString();
    }

    /** Validates a finished post, showing errors inline. Returns true if the form is valid. */
    private boolean validate() {
        boolean valid = true;
        switch (PostValidator.validateTitle(titleText())) {
            case TITLE_EMPTY:
                binding.titleLayout.setError(getString(R.string.error_title_required));
                valid = false;
                break;
            case TITLE_TOO_LONG:
                binding.titleLayout.setError(getString(R.string.error_title_too_long, PostValidator.MAX_TITLE_LENGTH));
                valid = false;
                break;
            default:
                binding.titleLayout.setError(null);
        }
        switch (PostValidator.validateContent(contentText())) {
            case CONTENT_EMPTY:
                binding.contentLayout.setError(getString(R.string.error_content_required));
                valid = false;
                break;
            case CONTENT_TOO_LONG:
                binding.contentLayout.setError(getString(R.string.error_content_too_long, PostValidator.MAX_CONTENT_LENGTH));
                valid = false;
                break;
            default:
                binding.contentLayout.setError(null);
        }
        if (!valid) {
            (binding.titleLayout.getError() != null ? binding.titleInput : binding.contentInput).requestFocus();
        }
        return valid;
    }

    /** Drafts only need some text, and must still respect the length limits. */
    private boolean validateDraft() {
        if (!PostValidator.isValidDraft(titleText(), contentText())) {
            Toast.makeText(this, R.string.error_draft_empty, Toast.LENGTH_LONG).show();
            return false;
        }
        boolean ok = true;
        if (PostValidator.validateTitle(titleText()) == PostValidator.Result.TITLE_TOO_LONG) {
            binding.titleLayout.setError(getString(R.string.error_title_too_long, PostValidator.MAX_TITLE_LENGTH));
            ok = false;
        }
        if (PostValidator.validateContent(contentText()) == PostValidator.Result.CONTENT_TOO_LONG) {
            binding.contentLayout.setError(getString(R.string.error_content_too_long, PostValidator.MAX_CONTENT_LENGTH));
            ok = false;
        }
        return ok;
    }

    /**
     * Saves the post.
     *
     * @param asDraft     true for the Draft button (lenient validation, never published)
     * @param afterAction optional {@link Navigation#ACTION_PUBLISH} / {@link Navigation#ACTION_SHARE}
     *                    to run on the detail screen once saved
     */
    private void save(boolean asDraft, @Nullable String afterAction) {
        if (saving || !(asDraft ? validateDraft() : validate())) {
            return;
        }
        saving = true;
        binding.btnSave.setEnabled(false);
        binding.btnDraft.setEnabled(false);

        Post post = new Post(titleText().trim(), contentText().trim(), currentImage);
        post.setId(postId);
        post.setCreatedAt(createdAt);
        post.setDraft(asDraft);
        final boolean isNew = postId == Post.NO_ID;
        Context context = getApplicationContext();
        AppExecutors.disk().execute(() -> {
            boolean ok;
            try {
                PostRepository repo = PostRepository.getInstance(context);
                ok = isNew ? repo.insert(post) != -1 : repo.update(post);
            } catch (RuntimeException e) {
                ok = false;
            }
            final boolean success = ok;
            AppExecutors.main(() -> {
                saving = false;
                if (!success) {
                    if (!isDestroyed()) {
                        binding.btnSave.setEnabled(true);
                        binding.btnDraft.setEnabled(true);
                    }
                    Toast.makeText(context, R.string.error_save, Toast.LENGTH_LONG).show();
                    return;
                }
                cleanUpImagesAfterSave();
                int message = asDraft ? R.string.draft_saved : (isNew ? R.string.saved_new : R.string.saved_update);
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                if (afterAction != null) {
                    Navigation.openPostThen(this, post.getId(), afterAction);
                }
                finish();
            });
        });
    }

    /** Deletes photos that are no longer referenced by the saved post. */
    private void cleanUpImagesAfterSave() {
        for (String name : sessionImages) {
            if (!name.equals(currentImage)) {
                ImageStorage.deleteImage(this, name);
            }
        }
        if (originalImage != null && !originalImage.equals(currentImage)) {
            ImageStorage.deleteImage(this, originalImage);
        }
        sessionImages.clear();
    }

    // ---------------------------------------------------------------- leaving the screen

    private boolean hasUnsavedChanges() {
        return !titleText().equals(initialTitle)
                || !contentText().equals(initialContent)
                || !Objects.equals(currentImage, originalImage);
    }

    private void attemptClose() {
        if (!hasUnsavedChanges()) {
            discardAndFinish();
            return;
        }
        new MaterialAlertDialogBuilder(this)
                .setIcon(R.drawable.ic_draft)
                .setTitle(R.string.discard_title)
                .setMessage(R.string.discard_message)
                .setPositiveButton(R.string.action_discard, (d, w) -> discardAndFinish())
                .setNeutralButton(R.string.action_save_draft, (d, w) -> save(true, null))
                .setNegativeButton(R.string.action_keep_editing, null)
                .show();
    }

    /** Leaves without saving; photos imported in this session were never saved, so delete them. */
    private void discardAndFinish() {
        for (String name : sessionImages) {
            ImageStorage.deleteImage(this, name);
        }
        sessionImages.clear();
        deleteCameraTemp();
        finish();
    }

    // ---------------------------------------------------------------- misc

    private void setFormEnabled(boolean enabled) {
        binding.titleInput.setEnabled(enabled);
        binding.contentInput.setEnabled(enabled);
        setPhotoButtonsEnabled(enabled);
    }

    private static void clearErrorOnEdit(@NonNull TextInputLayout layout) {
        if (layout.getEditText() != null) {
            layout.getEditText().addTextChangedListener(new SimpleWatcher(() -> layout.setError(null)));
        }
    }

    /** TextWatcher that only cares that the text changed. */
    private static class SimpleWatcher implements TextWatcher {
        private final Runnable onChange;

        SimpleWatcher(Runnable onChange) {
            this.onChange = onChange;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            onChange.run();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_save_publish) {
            save(false, Navigation.ACTION_PUBLISH);
            return true;
        } else if (item.getItemId() == R.id.action_save_share) {
            save(false, Navigation.ACTION_SHARE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // EditText contents are saved automatically by Android; save everything else.
        outState.putString(STATE_ORIGINAL_IMAGE, originalImage);
        outState.putString(STATE_CURRENT_IMAGE, currentImage);
        outState.putStringArrayList(STATE_SESSION_IMAGES, sessionImages);
        outState.putString(STATE_CAMERA_PATH, cameraTempPath);
        outState.putString(STATE_INITIAL_TITLE, initialTitle);
        outState.putString(STATE_INITIAL_CONTENT, initialContent);
        outState.putLong(STATE_CREATED_AT, createdAt);
        outState.putBoolean(STATE_WAS_DRAFT, wasDraft);
    }

    private void restoreState(@NonNull Bundle state) {
        originalImage = state.getString(STATE_ORIGINAL_IMAGE);
        currentImage = state.getString(STATE_CURRENT_IMAGE);
        ArrayList<String> session = state.getStringArrayList(STATE_SESSION_IMAGES);
        sessionImages = session == null ? new ArrayList<>() : session;
        cameraTempPath = state.getString(STATE_CAMERA_PATH);
        initialTitle = state.getString(STATE_INITIAL_TITLE, "");
        initialContent = state.getString(STATE_INITIAL_CONTENT, "");
        createdAt = state.getLong(STATE_CREATED_AT);
        wasDraft = state.getBoolean(STATE_WAS_DRAFT);
    }
}
