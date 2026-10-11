package com.blogspace.app.ui;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blogspace.app.R;
import com.blogspace.app.databinding.FragmentAboutBinding;

/** About / Help: what the app does, how to use it, privacy notes and third-party libraries. */
public class AboutFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.aboutTitle.setText(getString(R.string.about_title, getString(R.string.app_name)));
        String version = "?";
        try {
            version = requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException ignored) {
            // Cannot happen for our own package; keep "?".
        }
        binding.aboutVersion.setText(getString(R.string.about_version, version));
        return binding.getRoot();
    }
}
