package com.blogspace.app.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.blogspace.app.R;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentProfileBinding;
import com.blogspace.app.publish.PublishSettings;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.AppPrefs;
import com.blogspace.app.util.AvatarHelper;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/** Profile: avatar, name, statistics and settings (theme, publishing, privacy, about). */
public class ProfileFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentProfileBinding binding;
    private AppPrefs prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        UiUtils.applyStatusBarPadding(binding.hero);
        prefs = new AppPrefs(requireContext());

        View.OnClickListener editProfile = v -> startActivity(new Intent(requireContext(), ProfileSetupActivity.class)
                .putExtra(ProfileSetupActivity.EXTRA_EDIT, true));
        binding.btnEditProfile.setOnClickListener(editProfile);
        binding.avatar.setOnClickListener(editProfile);

        // Quick dark-mode switch (Light <-> Dark); "Theme" offers System as well.
        binding.rowDarkMode.setSwitch(isDarkNow(), checked -> prefs.setThemeMode(
                checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO));
        binding.rowTheme.setOnClickListener(v -> chooseTheme());
        binding.rowPublishing.setOnClickListener(v -> startActivity(new Intent(requireContext(), PublishSettingsActivity.class)));
        binding.rowPrivacy.setOnClickListener(v -> new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_shield)
                .setTitle(R.string.setting_privacy)
                .setMessage(R.string.about_privacy)
                .setPositiveButton(R.string.action_ok, null)
                .show());
        binding.rowOnboarding.setOnClickListener(v -> startActivity(new Intent(requireContext(), OnboardingActivity.class)
                .putExtra(OnboardingActivity.EXTRA_REPLAY, true)));
        binding.rowAbout.setOnClickListener(v -> startActivity(new Intent(requireContext(), AboutActivity.class)));

        String version = "?";
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            // Cannot happen for our own package.
        }
        binding.version.setText(getString(R.string.profile_version, getString(R.string.app_name), version));
    }

    private boolean isDarkNow() {
        int mode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void chooseTheme() {
        int[] modes = {AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
        CharSequence[] labels = {getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark)};
        int current = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == prefs.getThemeMode()) {
                current = i;
            }
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_palette)
                .setTitle(R.string.setting_theme)
                .setSingleChoiceItems(labels, current, (d, which) -> {
                    d.dismiss();
                    prefs.setThemeMode(modes[which]); // recreates activities with the new theme
                })
                .show();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        String name = prefs.getName();
        binding.name.setText(name == null ? getString(R.string.default_user_name) : name);
        AvatarHelper.show(binding.avatar, prefs.getAvatar());
        binding.rowTheme.setSubtitle(themeLabel(prefs.getThemeMode()));

        PublishSettings ps = new PublishSettings(context);
        binding.rowPublishing.setSubtitle(ps.getMode() == PublishSettings.Mode.TEST ? getString(R.string.upload_dest_test)
                : ps.isBloggerConfigured() ? getString(R.string.upload_dest_blogger, ps.getBlogName())
                : getString(R.string.upload_dest_not_set));

        AppExecutors.disk().execute(() -> {
            PostRepository.Stats stats = PostRepository.getInstance(context).getStats();
            AppExecutors.main(() -> {
                if (binding == null) {
                    return;
                }
                binding.statTotal.setText(String.valueOf(stats.total));
                binding.statUploaded.setText(String.valueOf(stats.published));
                binding.statOffline.setText(String.valueOf(stats.offline));
                binding.statDrafts.setText(String.valueOf(stats.drafts));
            });
        });
    }

    private String themeLabel(int mode) {
        if (mode == AppCompatDelegate.MODE_NIGHT_NO) {
            return getString(R.string.theme_light);
        } else if (mode == AppCompatDelegate.MODE_NIGHT_YES) {
            return getString(R.string.theme_dark);
        }
        return getString(R.string.theme_system);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
