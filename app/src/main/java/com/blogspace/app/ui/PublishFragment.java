package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.blogspace.app.R;
import com.blogspace.app.databinding.FragmentPublishBinding;
import com.blogspace.app.publish.BloggerApi;
import com.blogspace.app.publish.GoogleAuthHelper;
import com.blogspace.app.publish.PublishException;
import com.blogspace.app.publish.PublishSettings;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

/**
 * Publishing settings. The user picks Blogger (real) or Test mode (simulated), connects a Google
 * account and chooses which of their Blogger blogs receives the posts.
 */
public class PublishFragment extends Fragment {

    /** Implemented by the hosting Activity, which owns the Google consent-screen launcher. */
    public interface AuthHost {
        @NonNull
        GoogleAuthHelper getAuthHelper();
    }

    private FragmentPublishBinding binding;
    private PublishSettings settings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPublishBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        settings = new PublishSettings(requireContext());

        binding.modeGroup.check(settings.getMode() == PublishSettings.Mode.TEST ? R.id.mode_test : R.id.mode_blogger);
        binding.modeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            settings.setMode(checkedId == R.id.mode_test ? PublishSettings.Mode.TEST : PublishSettings.Mode.BLOGGER);
            render();
        });

        binding.switchImages.setChecked(settings.isIncludeImages());
        binding.switchImages.setOnCheckedChangeListener((b, checked) -> settings.setIncludeImages(checked));
        binding.switchSimulateFailure.setChecked(settings.isSimulateFailure());
        binding.switchSimulateFailure.setOnCheckedChangeListener((b, checked) -> settings.setSimulateFailure(checked));

        binding.btnConnect.setOnClickListener(v -> connect());
        binding.btnDisconnect.setOnClickListener(v -> {
            settings.clearBlog();
            Toast.makeText(requireContext(), R.string.blogger_disconnected, Toast.LENGTH_SHORT).show();
            render();
        });
        render();
    }

    private void render() {
        boolean blogger = settings.getMode() == PublishSettings.Mode.BLOGGER;
        // Dim the section that is not in use, so the active destination is obvious.
        binding.bloggerCard.setAlpha(blogger ? 1f : 0.6f);
        binding.testCard.setAlpha(blogger ? 0.6f : 1f);

        if (settings.isBloggerConfigured()) {
            binding.bloggerStatus.setText(getString(R.string.blogger_connected, settings.getBlogName()));
            binding.bloggerUrl.setText(settings.getBlogUrl());
            binding.bloggerUrl.setVisibility(settings.getBlogUrl() == null ? View.GONE : View.VISIBLE);
            binding.btnConnect.setText(R.string.blogger_change_blog);
            binding.btnDisconnect.setVisibility(View.VISIBLE);
        } else {
            binding.bloggerStatus.setText(R.string.blogger_not_connected);
            binding.bloggerUrl.setVisibility(View.GONE);
            binding.btnConnect.setText(R.string.blogger_connect);
            binding.btnDisconnect.setVisibility(View.GONE);
        }
    }

    /** Google consent → list the user's blogs → let them choose one. */
    private void connect() {
        if (!UiUtils.isOnline(requireContext())) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.publish_offline_title)
                    .setMessage(R.string.publish_offline_body)
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }
        androidx.fragment.app.FragmentActivity activity = requireActivity();
        GoogleAuthHelper auth = ((AuthHost) activity).getAuthHelper();
        setBusy(true);
        auth.requestToken(activity, new GoogleAuthHelper.Callback() {
            @Override
            public void onToken(@NonNull String accessToken) {
                if (isAdded()) {
                    loadBlogs(auth, accessToken);
                }
            }

            @Override
            public void onError(@NonNull String message) {
                if (!isAdded()) {
                    return; // user left this tab while the Google dialog was open
                }
                setBusy(false);
                showError(getString(R.string.publish_auth_failed, message));
            }
        });
    }

    private void loadBlogs(@NonNull GoogleAuthHelper auth, @NonNull String token) {
        Context context = requireContext().getApplicationContext();
        AppExecutors.network().execute(() -> {
            List<BloggerApi.BlogInfo> blogs = null;
            String error = null;
            try {
                blogs = new BloggerApi().listMyBlogs(token);
            } catch (PublishException e) {
                if (e.getKind() == PublishException.Kind.AUTH) {
                    auth.invalidate(token);
                }
                error = e.describe(context);
            }
            final List<BloggerApi.BlogInfo> result = blogs;
            final String message = error;
            AppExecutors.main(() -> {
                if (binding == null) {
                    return;
                }
                setBusy(false);
                if (message != null) {
                    showError(message);
                } else if (result.isEmpty()) {
                    showError(getString(R.string.blogger_no_blogs));
                } else {
                    chooseBlog(result);
                }
            });
        });
    }

    private void chooseBlog(@NonNull List<BloggerApi.BlogInfo> blogs) {
        String[] names = new String[blogs.size()];
        for (int i = 0; i < blogs.size(); i++) {
            BloggerApi.BlogInfo b = blogs.get(i);
            names[i] = b.url == null ? b.name : b.name + "\n" + b.url;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.blogger_choose_blog)
                .setItems(names, (d, which) -> {
                    settings.setBlog(blogs.get(which));
                    settings.setMode(PublishSettings.Mode.BLOGGER);
                    binding.modeGroup.check(R.id.mode_blogger);
                    render();
                })
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void setBusy(boolean busy) {
        if (binding == null) {
            return;
        }
        binding.bloggerProgress.setVisibility(busy ? View.VISIBLE : View.GONE);
        binding.btnConnect.setEnabled(!busy);
    }

    private void showError(@NonNull String message) {
        if (!isAdded()) {
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setMessage(message)
                .setPositiveButton(R.string.action_ok, null)
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
