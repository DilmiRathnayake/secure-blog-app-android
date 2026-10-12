package com.blogspace.app.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.databinding.ActivityProfileSetupBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.AppPrefs;
import com.blogspace.app.util.AvatarHelper;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.UiUtils;

import java.io.IOException;

/**
 * Local profile instead of a fake login: a display name and an optional photo, stored only on
 * this device. Also used later from Profile → Edit profile.
 */
public class ProfileSetupActivity extends AppCompatActivity {

    /** Opened from the Profile tab: just save and close instead of continuing to Main. */
    public static final String EXTRA_EDIT = "edit";

    private ActivityProfileSetupBinding binding;
    private AppPrefs prefs;
    @Nullable
    private String avatar;
    private boolean editMode;

    private final ActivityResultLauncher<PickVisualMediaRequest> pickAvatar =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), this::onAvatarPicked);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityProfileSetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applyStatusBarPadding(binding.hero);
        UiUtils.applyBottomInsets(binding.getRoot());

        prefs = new AppPrefs(this);
        editMode = getIntent().getBooleanExtra(EXTRA_EDIT, false);
        avatar = savedInstanceState != null ? savedInstanceState.getString("avatar") : prefs.getAvatar();
        if (savedInstanceState == null && prefs.getName() != null) {
            binding.nameInput.setText(prefs.getName());
        }
        AvatarHelper.show(binding.avatar, avatar);
        binding.btnSkip.setText(editMode ? R.string.action_cancel : R.string.action_skip_for_now);

        binding.btnAvatar.setOnClickListener(v -> launchPicker());
        binding.btnContinue.setOnClickListener(v -> save());
        binding.btnSkip.setOnClickListener(v -> {
            if (editMode) {
                finish();
            } else {
                prefs.setProfileDone();
                openMain();
            }
        });
    }

    private void launchPicker() {
        pickAvatar.launch(new PickVisualMediaRequest.Builder()
                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE).build());
    }

    private void onAvatarPicked(@Nullable Uri uri) {
        if (uri == null) {
            return;
        }
        AppExecutors.disk().execute(() -> {
            String name = null;
            try {
                name = ImageStorage.importImage(getApplicationContext(), uri);
            } catch (IOException | SecurityException e) {
                // shown below
            }
            final String imported = name;
            AppExecutors.main(() -> {
                if (isDestroyed()) {
                    return;
                }
                if (imported == null) {
                    Toast.makeText(this, R.string.error_photo_import, Toast.LENGTH_LONG).show();
                    return;
                }
                avatar = imported;
                AvatarHelper.show(binding.avatar, avatar);
                binding.avatar.animate().scaleX(1.08f).scaleY(1.08f).setDuration(120)
                        .withEndAction(() -> binding.avatar.animate().scaleX(1f).scaleY(1f).setDuration(160).start())
                        .start();
            });
        });
    }

    private void save() {
        String name = binding.nameInput.getText() == null ? "" : binding.nameInput.getText().toString().trim();
        String old = prefs.getAvatar();
        if (old != null && !old.equals(avatar)) {
            ImageStorage.deleteImage(this, old); // replaced photo is no longer needed
        }
        prefs.setName(name.isEmpty() ? null : name);
        prefs.setAvatar(avatar);
        prefs.setProfileDone();
        if (editMode) {
            Toast.makeText(this, R.string.profile_saved, Toast.LENGTH_SHORT).show();
            finish();
        } else {
            openMain();
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("avatar", avatar);
    }
}
