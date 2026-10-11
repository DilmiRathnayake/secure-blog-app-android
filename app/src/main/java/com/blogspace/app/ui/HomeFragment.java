package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentHomeBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.UiUtils;

import java.util.List;

/** Dashboard: welcome banner, online/offline status, post counts and the 5 most recent posts. */
public class HomeFragment extends Fragment implements MainActivity.Refreshable {

    private static final int RECENT_LIMIT = 5;

    private FragmentHomeBinding binding;
    private PostAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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
        binding.btnNewPost.setOnClickListener(v -> Navigation.newPost(requireContext()));
        binding.btnViewAll.setOnClickListener(v -> ((MainActivity) requireActivity()).selectTab(R.id.nav_posts));
        binding.empty.emptyTitle.setText(R.string.posts_empty_title);
        binding.empty.emptyBody.setText(R.string.posts_empty_body);
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        boolean online = UiUtils.isOnline(context);
        binding.statusText.setText(online ? R.string.status_online : R.string.status_offline);
        binding.statusIcon.setImageResource(online ? R.drawable.ic_cloud_upload : R.drawable.ic_cloud_off);

        AppExecutors.disk().execute(() -> {
            PostRepository repo = PostRepository.getInstance(context);
            PostRepository.Stats stats = repo.getStats();
            List<Post> recent = repo.getRecent(RECENT_LIMIT);
            AppExecutors.main(() -> {
                if (binding == null) {
                    return; // view destroyed while loading
                }
                binding.statTotal.setText(String.valueOf(stats.total));
                binding.statImages.setText(String.valueOf(stats.withImages));
                binding.statPublished.setText(String.valueOf(stats.published));
                adapter.submitList(recent);
                binding.empty.getRoot().setVisibility(recent.isEmpty() ? View.VISIBLE : View.GONE);
                binding.btnViewAll.setVisibility(recent.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
