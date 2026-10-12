package com.blogspace.app.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostFilter;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentPostsBinding;
import com.blogspace.app.ui.widget.Shimmer;
import com.blogspace.app.ui.widget.SwipeActions;
import com.blogspace.app.util.AppExecutors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The full list of saved posts.
 * <ul>
 *   <li>Filter chips: Newest, Oldest, Uploaded, Offline, Draft.</li>
 *   <li>Tap to open; swipe left to delete (with Undo) or right to edit.</li>
 *   <li>Long-press or the checklist button starts selection mode: a gradient bar shows
 *       "N selected", Select all, Upload, Delete and X (clear / exit).</li>
 * </ul>
 */
public class PostsFragment extends Fragment implements MainActivity.Refreshable {

    /** Argument: which filter chip to start on (a {@link PostFilter} name). */
    public static final String ARG_FILTER = "filter";
    private static final String STATE_SELECTING = "selecting";
    private static final String STATE_SELECTED_IDS = "selected_ids";

    private FragmentPostsBinding binding;
    private PostAdapter adapter;
    private Shimmer shimmer;
    private PostFilter filter = PostFilter.NEWEST;
    /** Posts swiped away but not yet deleted (Undo is still possible). */
    private final Set<Long> pendingDelete = new HashSet<>();
    @Nullable
    private ArrayList<Long> pendingRestoreIds;
    private boolean firstLoad = true;

    private final OnBackPressedCallback exitSelectionOnBack = new OnBackPressedCallback(false) {
        @Override
        public void handleOnBackPressed() {
            setSelectionMode(false);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentPostsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        com.blogspace.app.util.UiUtils.applyStatusBarPadding(binding.headerContainer);
        adapter = new PostAdapter(new PostAdapter.Listener() {
            @Override
            public void onPostClicked(@NonNull Post post) {
                Navigation.openPost(requireContext(), post.getId());
            }

            @Override
            public void onPostLongPressed(@NonNull Post post) {
                if (!adapter.isSelectionMode()) {
                    setSelectionMode(true);
                }
                adapter.toggle(post.getId());
            }

            @Override
            public void onSelectionChanged(int selectedCount) {
                updateSelectionTitle();
            }
        });
        binding.recycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recycler.setAdapter(adapter);
        shimmer = new Shimmer(binding.skeleton.getRoot());

        new ItemTouchHelper(new SwipeActions(requireContext(), new SwipeActions.Listener() {
            @Override
            public void onSwipeDelete(int position) {
                swipeDelete(adapter.getPostAt(position));
            }

            @Override
            public void onSwipeEdit(int position) {
                Post post = adapter.getPostAt(position);
                adapter.notifyItemChanged(position); // slide the card back
                Navigation.editPost(requireContext(), post.getId());
            }

            @Override
            public boolean isSwipeEnabled() {
                return !adapter.isSelectionMode();
            }
        })).attachToRecyclerView(binding.recycler);

        // FAB shrinks while scrolling down.
        binding.recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView rv, int dx, int dy) {
                if (dy > 6 && binding.fabNew.isExtended()) {
                    binding.fabNew.shrink();
                } else if (dy < -6 && !binding.fabNew.isExtended()) {
                    binding.fabNew.extend();
                }
            }
        });

        binding.fabNew.setOnClickListener(v -> Navigation.newPost(requireContext()));
        binding.searchPill.setOnClickListener(v -> Navigation.openSearch(requireContext()));
        binding.btnSelect.setOnClickListener(v -> {
            if (adapter.getItemCount() > 0) {
                setSelectionMode(true);
            }
        });
        binding.btnCloseSelection.setOnClickListener(v -> setSelectionMode(false));
        binding.btnSelectAll.setOnClickListener(v -> {
            if (adapter.getSelectedCount() == adapter.getItemCount()) {
                adapter.clearSelection();
            } else {
                adapter.selectAll();
            }
        });
        binding.btnDeleteSelected.setOnClickListener(v -> confirmDeleteSelected());
        binding.btnUploadSelected.setOnClickListener(v -> publishSelected());
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), exitSelectionOnBack);

        // Filter chips
        String initial = getArguments() == null ? null : getArguments().getString(ARG_FILTER);
        if (initial != null) {
            filter = PostFilter.valueOf(initial);
        }
        binding.filterGroup.check(chipFor(filter));
        binding.filterGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            filter = filterFor(checkedIds.get(0));
            firstLoad = true;
            refresh();
        });

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

    private static int chipFor(PostFilter f) {
        switch (f) {
            case OLDEST:
                return R.id.chip_oldest;
            case UPLOADED:
                return R.id.chip_uploaded;
            case OFFLINE:
                return R.id.chip_offline;
            case DRAFT:
                return R.id.chip_draft;
            default:
                return R.id.chip_newest;
        }
    }

    private static PostFilter filterFor(int chipId) {
        if (chipId == R.id.chip_oldest) {
            return PostFilter.OLDEST;
        } else if (chipId == R.id.chip_uploaded) {
            return PostFilter.UPLOADED;
        } else if (chipId == R.id.chip_offline) {
            return PostFilter.OFFLINE;
        } else if (chipId == R.id.chip_draft) {
            return PostFilter.DRAFT;
        }
        return PostFilter.NEWEST;
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    public void refresh() {
        Context context = requireContext().getApplicationContext();
        final PostFilter current = filter;
        if (firstLoad) {
            shimmer.start();
            binding.recycler.setVisibility(View.INVISIBLE);
        }
        AppExecutors.disk().execute(() -> {
            List<Post> all = PostRepository.getInstance(context).getAll(current);
            List<Post> posts = new ArrayList<>();
            for (Post p : all) {
                if (!pendingDelete.contains(p.getId())) {
                    posts.add(p);
                }
            }
            AppExecutors.main(() -> {
                if (binding == null || current != filter) {
                    return;
                }
                shimmer.stop();
                binding.recycler.setVisibility(View.VISIBLE);
                adapter.submitList(posts, () -> {
                    if (binding == null) {
                        return;
                    }
                    if (pendingRestoreIds != null) {
                        adapter.restoreSelection(pendingRestoreIds);
                        pendingRestoreIds = null;
                        setSelectionMode(true);
                    }
                    adapter.pruneSelection();
                    if (posts.isEmpty() && adapter.isSelectionMode()) {
                        setSelectionMode(false);
                    }
                });
                if (firstLoad) {
                    binding.recycler.scheduleLayoutAnimation();
                    firstLoad = false;
                }
                binding.count.setText(getResources().getQuantityString(R.plurals.post_count_label, posts.size(), posts.size()));
                showEmpty(posts.isEmpty());
            });
        });
    }

    private void showEmpty(boolean empty) {
        binding.emptyScroll.setVisibility(empty ? View.VISIBLE : View.GONE);
        if (!empty) {
            return;
        }
        boolean filtered = filter != PostFilter.NEWEST && filter != PostFilter.OLDEST;
        binding.empty.emptyIcon.setImageResource(filtered ? R.drawable.ill_no_results : R.drawable.ill_empty_posts);
        binding.empty.emptyTitle.setText(filtered ? R.string.posts_filter_empty_title : R.string.posts_empty_title);
        binding.empty.emptyBody.setText(filtered ? R.string.posts_filter_empty_body : R.string.posts_empty_body);
        binding.empty.emptyAction.setVisibility(filtered ? View.GONE : View.VISIBLE);
        binding.empty.emptyAction.setText(R.string.action_write_post);
        binding.empty.emptyAction.setOnClickListener(v -> Navigation.newPost(requireContext()));
    }

    // ---------------------------------------------------------------- swipe to delete with Undo

    /**
     * Hides the post immediately and shows an Undo snackbar. The row is only deleted from SQLite
     * (with its photo) once the snackbar goes away without Undo being pressed.
     */
    private void swipeDelete(@NonNull Post post) {
        pendingDelete.add(post.getId());
        refresh();
        Context context = requireContext().getApplicationContext();
        Snackbar.make(binding.coordinator, R.string.swipe_deleted, Snackbar.LENGTH_LONG)
                .setAnchorView(binding.fabNew)
                .setAction(R.string.action_undo, v -> {
                    pendingDelete.remove(post.getId());
                    if (binding != null) {
                        refresh();
                    }
                })
                .addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
                    @Override
                    public void onDismissed(Snackbar bar, int event) {
                        if (event == DISMISS_EVENT_ACTION || !pendingDelete.contains(post.getId())) {
                            return;
                        }
                        AppExecutors.disk().execute(() -> {
                            PostRepository.getInstance(context).delete(post.getId());
                            pendingDelete.remove(post.getId());
                        });
                    }
                })
                .show();
    }

    // ---------------------------------------------------------------- selection mode

    private void setSelectionMode(boolean enabled) {
        if (binding == null) {
            return;
        }
        adapter.setSelectionMode(enabled);
        exitSelectionOnBack.setEnabled(enabled);
        crossFade(enabled ? binding.header : binding.selectionBar, enabled ? binding.selectionBar : binding.header);
        // Gradient continues behind the status bar while selecting.
        binding.headerContainer.setBackgroundResource(enabled ? R.drawable.bg_gradient_primary : 0);
        if (enabled) {
            binding.fabNew.hide();
        } else {
            binding.fabNew.show();
        }
        updateSelectionTitle();
    }

    private static void crossFade(View out, View in) {
        if (in.getVisibility() == View.VISIBLE) {
            return;
        }
        in.setAlpha(0f);
        in.setVisibility(View.VISIBLE);
        in.animate().alpha(1f).setDuration(180).start();
        out.animate().alpha(0f).setDuration(180).withEndAction(() -> {
            out.setVisibility(View.GONE);
            out.setAlpha(1f);
        }).start();
    }

    private void updateSelectionTitle() {
        if (binding != null) {
            int count = adapter.getSelectedCount();
            binding.selectionCount.setText(getResources().getQuantityString(R.plurals.selected_count, count, count));
        }
    }

    private void confirmDeleteSelected() {
        ArrayList<Long> ids = adapter.getSelectedIds();
        if (ids.isEmpty()) {
            Toast.makeText(requireContext(), R.string.nothing_selected, Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setIcon(R.drawable.ic_delete)
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
                if (binding == null) {
                    return;
                }
                if (count < 0) {
                    Snackbar.make(binding.coordinator, R.string.error_delete, Snackbar.LENGTH_LONG).show();
                } else {
                    Snackbar.make(binding.coordinator, context.getResources()
                            .getQuantityString(R.plurals.deleted_many, count, count), Snackbar.LENGTH_SHORT).show();
                }
                setSelectionMode(false);
                refresh();
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
        setSelectionMode(false);
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
        binding = null;
    }
}
