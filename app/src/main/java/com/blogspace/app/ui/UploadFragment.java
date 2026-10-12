package com.blogspace.app.ui;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
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
import com.blogspace.app.databinding.FragmentUploadBinding;
import com.blogspace.app.publish.PublishSettings;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.UiUtils;

import java.util.List;

/**
 * Upload tab: shows the publishing destination and a queue of finished posts that are not yet
 * uploaded. Select some (or all) and tap Upload; progress appears in the upload sheet
 * (see PublishController).
 */
public class UploadFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentUploadBinding binding;
    private PostAdapter adapter;
    private ObjectAnimator floatAnim;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        UiUtils.applyStatusBarPadding(binding.hero);
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClicked(@NonNull Post post) {
                // In the queue a tap toggles selection (handled by the adapter in selection mode).
            }

            @Override
            public void onPostLongPressed(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                updateButton();
            }
        });
        adapter.setSelectionMode(true); // queue rows always show checkboxes
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);

        binding.btnSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), PublishSettingsActivity.class)));
        binding.btnSelectAll.setOnClickListener(v -> {
            if (adapter.getSelectedCount() == adapter.getItemCount()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });
        binding.btnUpload.setOnClickListener(v -> {
            List<Long> ids = adapter.getSelectedIds();
            if (!ids.isEmpty()) {
                ((MainActivity) requireActivity()).getPublishController().publish(ids);
            }
        });
        binding.empty.emptyIcon.setImageResource(R.drawable.ill_all_done);
        binding.empty.emptyTitle.setText(R.string.upload_empty_title);
        binding.empty.emptyBody.setText(R.string.upload_empty_body);

        // Floating cloud animation in the header.
        floatAnim = ObjectAnimator.ofFloat(binding.cloudBadge, View.TRANSLATION_Y, -6f, 6f);
        floatAnim.setDuration(1600);
        floatAnim.setRepeatMode(ValueAnimator.REVERSE);
        floatAnim.setRepeatCount(ValueAnimator.INFINITE);
        floatAnim.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        PublishSettings settings = new PublishSettings(context);
        if (settings.getMode() == PublishSettings.Mode.TEST) {
            binding.destValue.setText(R.string.upload_dest_test);
        } else if (settings.isBloggerConfigured()) {
            binding.destValue.setText(getString(R.string.upload_dest_blogger, settings.getBlogName()));
        } else {
            binding.destValue.setText(R.string.upload_dest_not_set);
        }

        AppExecutors.disk().execute(() -> {
            List<Post> queue = PostRepository.getInstance(context).getUploadQueue();
            AppExecutors.main(() -> {
                if (binding == null) {
                    return;
                }
                adapter.submitList(queue, () -> {
                    if (binding != null) {
                        adapter.pruneSelection();
                        updateButton();
                    }
                });
                if (firstLoad) {
                    binding.recycler.scheduleLayoutAnimation();
                    firstLoad = false;
                }
                binding.queueTitle.setText(getString(R.string.upload_queue_count, queue.size()));
                binding.empty.getRoot().setVisibility(queue.isEmpty() ? View.VISIBLE : View.GONE);
                binding.btnSelectAll.setVisibility(queue.isEmpty() ? View.GONE : View.VISIBLE);
                binding.bottomBar.setVisibility(queue.isEmpty() ? View.GONE : View.VISIBLE);
            });
        });
    }

    private void updateButton() {
        if (binding == null) {
            return;
        }
        int count = adapter.getSelectedCount();
        binding.btnUpload.setEnabled(count > 0);
        binding.btnUpload.setText(count > 0
                ? getResources().getQuantityString(R.plurals.upload_button, count, count)
                : getString(R.string.upload_select_some));
    }

    @Override
    public void onDestroyView() {
        if (floatAnim != null) {
            floatAnim.cancel();
        }
        super.onDestroyView();
        binding = null;
    }
}
