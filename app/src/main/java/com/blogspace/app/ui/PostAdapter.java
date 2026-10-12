package com.blogspace.app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.databinding.ItemPostBinding;
import com.blogspace.app.ui.widget.StatusBadge;
import com.blogspace.app.util.ThumbnailLoader;
import com.blogspace.app.util.UiUtils;
import com.google.android.material.color.MaterialColors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shows posts in a RecyclerView and keeps track of which posts are selected.
 * <p>
 * Selection is stored as a set of post <b>ids</b> (not row positions), so it stays correct when the
 * list is reloaded or re-sorted. Used by the Home, Posts and Search screens.
 */
public class PostAdapter extends ListAdapter<Post, PostAdapter.PostViewHolder> {

    public interface Listener {
        /** Row tapped while not selecting: open the post. */
        void onPostClicked(@NonNull Post post);

        /** Row long-pressed: usually starts selection mode. */
        void onPostLongPressed(@NonNull Post post);

        /** Selected set changed (for updating the "N selected" title). */
        void onSelectionChanged(int selectedCount);
    }

    /** Payload used to redraw only the checkbox/stroke, avoiding a full rebind flicker. */
    private static final Object PAYLOAD_SELECTION = new Object();

    private final Listener listener;
    private final Set<Long> selectedIds = new LinkedHashSet<>();
    private boolean selectionMode;
    @Nullable
    private String highlightQuery;

    public PostAdapter(@NonNull Listener listener) {
        super(DIFF);
        this.listener = listener;
        setHasStableIds(true);
    }

    private static final DiffUtil.ItemCallback<Post> DIFF = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.getId() == b.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Post a, @NonNull Post b) {
            return a.hasSameContentAs(b);
        }
    };

    /** Post shown at an adapter position (used by swipe actions). */
    @NonNull
    public Post getPostAt(int position) {
        return getItem(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    // ---------------------------------------------------------------- selection API

    public boolean isSelectionMode() {
        return selectionMode;
    }

    public void setSelectionMode(boolean enabled) {
        if (selectionMode == enabled) {
            return;
        }
        selectionMode = enabled;
        if (!enabled) {
            selectedIds.clear();
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        listener.onSelectionChanged(selectedIds.size());
    }

    public void toggle(long id) {
        if (!selectedIds.remove(id)) {
            selectedIds.add(id);
        }
        int position = positionOf(id);
        if (position != RecyclerView.NO_POSITION) {
            notifyItemChanged(position, PAYLOAD_SELECTION);
        }
        listener.onSelectionChanged(selectedIds.size());
    }

    public void selectAll() {
        for (Post p : getCurrentList()) {
            selectedIds.add(p.getId());
        }
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        listener.onSelectionChanged(selectedIds.size());
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyItemRangeChanged(0, getItemCount(), PAYLOAD_SELECTION);
        listener.onSelectionChanged(0);
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    /** Selected ids in the order they appear on screen. */
    @NonNull
    public ArrayList<Long> getSelectedIds() {
        ArrayList<Long> ordered = new ArrayList<>();
        for (Post p : getCurrentList()) {
            if (selectedIds.contains(p.getId())) {
                ordered.add(p.getId());
            }
        }
        return ordered;
    }

    /** Restores selection after rotation. */
    public void restoreSelection(@NonNull List<Long> ids) {
        selectedIds.clear();
        selectedIds.addAll(ids);
    }

    /** Drops ids that are no longer in the list (e.g. deleted elsewhere). Call after submitList. */
    public void pruneSelection() {
        Set<Long> present = new HashSet<>();
        for (Post p : getCurrentList()) {
            present.add(p.getId());
        }
        if (selectedIds.retainAll(present)) {
            listener.onSelectionChanged(selectedIds.size());
        }
    }

    public void setHighlightQuery(@Nullable String query) {
        highlightQuery = query;
    }

    private int positionOf(long id) {
        List<Post> list = getCurrentList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId() == id) {
                return i;
            }
        }
        return RecyclerView.NO_POSITION;
    }

    // ---------------------------------------------------------------- RecyclerView

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PostViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (payloads.contains(PAYLOAD_SELECTION)) {
            holder.bindSelection(getItem(position));
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding b;
        private final int thumbPx;

        PostViewHolder(@NonNull ItemPostBinding binding) {
            super(binding.getRoot());
            b = binding;
            thumbPx = binding.getRoot().getResources().getDimensionPixelSize(R.dimen.thumb_size);
            b.card.setOnClickListener(v -> {
                Post post = currentPost();
                if (post == null) {
                    return;
                }
                if (selectionMode) {
                    toggle(post.getId());
                } else {
                    listener.onPostClicked(post);
                }
            });
            b.card.setOnLongClickListener(v -> {
                Post post = currentPost();
                if (post != null) {
                    listener.onPostLongPressed(post);
                }
                return post != null;
            });
        }

        @Nullable
        private Post currentPost() {
            int pos = getBindingAdapterPosition();
            return pos == RecyclerView.NO_POSITION ? null : getItem(pos);
        }

        void bind(@NonNull Post post) {
            Context ctx = b.getRoot().getContext();
            String title = post.getTitle().isEmpty() ? ctx.getString(R.string.untitled_draft) : post.getTitle();
            b.title.setText(UiUtils.highlight(ctx, title, highlightQuery));
            String snippet = UiUtils.snippetAround(post.getContent(), highlightQuery, 30);
            b.preview.setText(UiUtils.highlight(ctx, snippet, highlightQuery));
            b.date.setText(ctx.getString(R.string.updated_relative, UiUtils.relativeTime(post.getUpdatedAt())));
            StatusBadge.apply(b.statusBadge, post.getStatus());
            if (post.hasImage()) {
                b.thumbnail.setVisibility(View.VISIBLE);
                ThumbnailLoader.load(b.thumbnail, post.getImageFileName(), thumbPx);
            } else {
                b.thumbnail.setVisibility(View.GONE);
                b.thumbnail.setImageDrawable(null);
            }
            bindSelection(post);
        }

        void bindSelection(@NonNull Post post) {
            boolean selected = selectedIds.contains(post.getId());
            b.checkbox.setVisibility(selectionMode ? View.VISIBLE : View.GONE);
            b.checkbox.setChecked(selected);
            b.checkbox.setContentDescription(
                    b.getRoot().getContext().getString(R.string.cd_select_post, post.getTitle()));
            int stroke = MaterialColors.getColor(b.card,
                    selected ? androidx.appcompat.R.attr.colorPrimary : com.google.android.material.R.attr.colorOutlineVariant);
            b.card.setStrokeColor(ColorStateList.valueOf(stroke));
            b.card.setStrokeWidth(b.getRoot().getResources().getDimensionPixelSize(
                    selected ? R.dimen.selected_stroke : R.dimen.default_stroke));
            b.card.setSelected(selected);
        }
    }
}
