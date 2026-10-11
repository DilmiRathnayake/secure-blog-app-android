package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ActionMode;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentPostsBinding;
import com.blogspace.app.util.AppExecutors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * The full list of saved posts.
 * <ul>
 *   <li>Tap a post to open it; long-press (or the toolbar "Select" button) to start selection mode.</li>
 *   <li>Selection mode shows checkboxes and a contextual bar with: N selected, Select all,
 *       Publish, Delete and Clear selection.</li>
 * </ul>
 */
public class PostsFragment extends Fragment implements MainActivity.Refreshable {

    private static final String STATE_SELECTING = "selecting";
    private static final String STATE_SELECTED_IDS = "selected_ids";

    private FragmentPostsBinding binding;
    private PostAdapter adapter;
    @Nullable
    private ActionMode actionMode;
    /** Selection restored after rotation, applied once the list has loaded. */
    @Nullable
    private ArrayList<Long> pendingRestoreIds;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPostsBinding.inflate(inflater, container, false);
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
                if (!adapter.isSelectionMode()) {
                    startSelection();
                }
                adapter.toggle(post.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                updateActionModeTitle();
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        binding.fabNew.setOnClickListener(v -> Navigation.newPost(requireContext()));
        binding.empty.emptyTitle.setText(R.string.posts_empty_title);
        binding.empty.emptyBody.setText(R.string.posts_empty_body);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.menu_posts, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_select) {
                    if (adapter.getItemCount() > 0) {
                        startSelection();
                    }
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);

        if (savedInstanceState != null && savedInstanceState.getBoolean(STATE_SELECTING)) {
            long[] ids = savedInstanceState.getLongArray(STATE_SELECTED_IDS);
            pendingRestoreIds = new ArrayList<>();
            if (ids != null) {
                for (long id : ids) {
                    pendingRestoreIds.add(id);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        AppExecutors.disk().execute(() -> {
            List<Post> posts = PostRepository.getInstance(context).getAll();
            AppExecutors.main(() -> {
                if (binding == null) {
                    return;
                }
                adapter.submitList(posts, () -> {
                    if (binding == null) {
                        return;
                    }
                    if (pendingRestoreIds != null) {
                        adapter.restoreSelection(pendingRestoreIds);
                        pendingRestoreIds = null;
                        startSelection();
                    }
                    adapter.pruneSelection();
                    if (posts.isEmpty() && actionMode != null) {
                        actionMode.finish();
                    }
                });
                binding.empty.getRoot().setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    // ---------------------------------------------------------------- selection mode

    private void startSelection() {
        if (actionMode != null) {
            return;
        }
        actionMode = ((AppCompatActivity) requireActivity()).startSupportActionMode(selectionCallback);
        adapter.setSelectionMode(true);
        binding.fabNew.hide();
        updateActionModeTitle();
    }

    private void updateActionModeTitle() {
        if (actionMode != null) {
            int count = adapter.getSelectedCount();
            actionMode.setTitle(getResources().getQuantityString(R.plurals.selected_count, count, count));
        }
    }

    private final ActionMode.Callback selectionCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_selection, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int id = item.getItemId();
            if (id == R.id.action_select_all) {
                adapter.selectAll();
            } else if (id == R.id.action_clear_selection) {
                adapter.clearSelection();
            } else if (id == R.id.action_delete_selected) {
                confirmDeleteSelected();
            } else if (id == R.id.action_publish_selected) {
                publishSelected();
            } else {
                return false;
            }
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            adapter.setSelectionMode(false);
            if (binding != null) {
                binding.fabNew.show();
            }
        }
    };

    private void confirmDeleteSelected() {
        ArrayList<Long> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(getResources().getQuantityString(R.plurals.delete_many_title, ids.size(), ids.size()))
                .setMessage(R.string.delete_many_message)
                .setPositiveButton(R.string.action_delete, (d, w) -> deleteSelected(ids))
                .setNegativeButton(R.string.action_cancel, null)
                .show();
    }

    private void deleteSelected(@NonNull List<Long> ids) {
        Context context = requireContext().getApplicationContext();
        AppExecutors.disk().execute(() -> {
            int deleted;
            try {
                deleted = PostRepository.getInstance(context).deleteMany(ids);
            } catch (RuntimeException e) {
                deleted = -1;
            }
            final int count = deleted;
            AppExecutors.main(() -> {
                if (count < 0) {
                    Toast.makeText(context, R.string.error_delete, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(context, context.getResources()
                            .getQuantityString(R.plurals.deleted_many, count, count), Toast.LENGTH_SHORT).show();
                }
                if (actionMode != null) {
                    actionMode.finish();
                }
                if (binding != null) {
                    refresh();
                }
            });
        });
    }

    private void publishSelected() {
        ArrayList<Long> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        ((MainActivity) requireActivity()).getPublishController().publish(ids);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (adapter != null && adapter.isSelectionMode()) {
            outState.putBoolean(STATE_SELECTING, true);
            List<Long> ids = adapter.getSelectedIds();
            long[] array = new long[ids.size()];
            for (int i = 0; i < array.length; i++) {
                array[i] = ids.get(i);
            }
            outState.putLongArray(STATE_SELECTED_IDS, array);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (actionMode != null) {
            actionMode.finish();
        }
        binding = null;
    }
}
