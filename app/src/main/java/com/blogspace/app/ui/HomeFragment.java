package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostFilter;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentHomeBinding;
import com.blogspace.app.databinding.ItemStatCardBinding;
import com.blogspace.app.ui.widget.Shimmer;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.AppPrefs;
import com.blogspace.app.util.AvatarHelper;
import com.blogspace.app.util.UiUtils;

import java.util.List;

/**
 * Dashboard: gradient hero (avatar, greeting, online/offline chip, search), four statistic cards
 * (Total, Offline, Uploaded, Drafts), the five most recent posts and a "New post" FAB.
 */
public class HomeFragment extends Fragment implements MainActivity.Refreshable {

    private static final int RECENT_LIMIT = 5;

    private FragmentHomeBinding binding;
    private PostAdapter adapter;
    private Shimmer shimmer;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        UiUtils.applyStatusBarPadding(binding.hero);
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClicked(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onPostLongPressed(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        shimmer = new Shimmer(binding.skeleton.getRoot());

        setupStat(binding.statTotal, R.drawable.ic_article, R.color.status_offline, R.color.status_offline_bg, R.string.stat_total, PostFilter.NEWEST);
        setupStat(binding.statOffline, R.drawable.ic_cloud_off, R.color.bs_secondary, R.color.bs_secondary_container, R.string.stat_offline, PostFilter.OFFLINE);
        setupStat(binding.statUploaded, R.drawable.ic_cloud_upload, R.color.status_success, R.color.status_success_bg, R.string.stat_uploaded, PostFilter.UPLOADED);
        setupStat(binding.statDrafts, R.drawable.ic_draft, R.color.status_warning, R.color.status_warning_bg, R.string.stat_drafts, PostFilter.DRAFT);

        binding.fabNew.setOnClickListener(v -> Navigation.newPost(requireContext()));
        binding.searchPill.setOnClickListener(v -> Navigation.openSearch(requireContext()));
        binding.avatar.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_profile));
        binding.btnViewAll.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_posts));
        binding.empty.emptyTitle.setText(R.string.posts_empty_title);
        binding.empty.emptyBody.setText(R.string.posts_empty_body);
        binding.empty.emptyAction.setVisibility(View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.action_write_post);
        binding.empty.emptyAction.setOnClickListener(v -> Navigation.newPost(requireContext()));

        // FAB collapses to an icon while scrolling down, expands when scrolling up.
        binding.scroll.setOnScrollChangeListener((View.OnScrollChangeListener) (v, x, y, oldX, oldY) -> {
            if (y > oldY + 8 && binding.fabNew.isExtended()) {
                binding.fabNew.shrink();
            } else if (y < oldY - 8 && !binding.fabNew.isExtended()) {
                binding.fabNew.extend();
            }
        });
    }

    private void setupStat(ItemStatCardBinding stat, @DrawableRes int icon, @ColorRes int tint,
                           @ColorRes int tileBg, @StringRes int label, @NonNull PostFilter filter) {
        Context c = requireContext();
        stat.statIcon.setImageResource(icon);
        stat.statIcon.setImageTintList(ContextCompat.getColorStateList(c, tint));
        stat.statIcon.setBackgroundTintList(ContextCompat.getColorStateList(c, tileBg));
        stat.statLabel.setText(label);
        stat.getRoot().setOnClickListener(v -> ((MainActivity) requireActivity()).openPosts(filter));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        AppPrefs prefs = new AppPrefs(context);
        String name = prefs.getName();
        binding.greeting.setText(UiUtils.greeting(context));
        binding.userName.setText(name == null ? getString(R.string.default_user_name) : name);
        AvatarHelper.show(binding.avatar, prefs.getAvatar());

        boolean online = UiUtils.isOnline(context);
        binding.statusChip.setText(online ? R.string.chip_online : R.string.chip_offline);
        binding.statusChip.setCompoundDrawablesRelativeWithIntrinsicBounds(
                online ? R.drawable.ic_cloud_upload : R.drawable.ic_wifi_off, 0, 0, 0);

        if (firstLoad) {
            shimmer.start();
        }
        AppExecutors.disk().execute(() -> {
            PostRepository repo = PostRepository.getInstance(context);
            PostRepository.Stats stats = repo.getStats();
            List<Post> recent = repo.getRecent(RECENT_LIMIT);
            AppExecutors.main(() -> {
                if (binding == null) {
                    return; // view destroyed while loading
                }
                shimmer.stop();
                animateCount(binding.statTotal.statValue, stats.total);
                animateCount(binding.statOffline.statValue, stats.offline);
                animateCount(binding.statUploaded.statValue, stats.published);
                animateCount(binding.statDrafts.statValue, stats.drafts);
                adapter.submitList(recent);
                if (firstLoad) {
                    binding.recycler.scheduleLayoutAnimation();
                    firstLoad = false;
                }
                binding.empty.getRoot().setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
                binding.btnViewAll.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    /** Counts a statistic up from its current value (small "alive" detail). */
    private static void animateCount(TextView view, int target) {
        int start;
        try {
            start = Integer.parseInt(view.getText().toString());
        } catch (NumberFormatException e) {
            start = 0;
        }
        if (start == target) {
            view.setText(String.valueOf(target));
            return;
        }
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofInt(start, target);
        anim.setDuration(600);
        anim.addUpdateListener(a -> view.setText(String.valueOf(a.getAnimatedValue())));
        anim.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
