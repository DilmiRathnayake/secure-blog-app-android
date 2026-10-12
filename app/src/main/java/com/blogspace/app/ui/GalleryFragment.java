package com.blogspace.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.FragmentGalleryBinding;
import com.blogspace.app.databinding.ItemGalleryBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ThumbnailLoader;
import com.blogspace.app.util.UiUtils;

import java.util.List;

/**
 * Gallery tab: a grid of every photo attached to a post. Tap a photo to preview it full screen
 * (open the post or remove the photo). The two cards start a new post straight from the camera
 * or the photo picker.
 */
public class GalleryFragment extends Fragment implements MainActivity.Refreshable {

    private FragmentGalleryBinding binding;
    private GalleryAdapter adapter;
    private boolean firstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentGalleryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        UiUtils.applyStatusBarPadding(binding.header);
        // 3 columns on phones, more on wider screens.
        float widthDp = getResources().getConfiguration().screenWidthDp;
        int columns = Math.max(3, (int) (widthDp / 130));
        binding.grid.setLayoutManager(new GridLayoutManager(requireContext(), columns));
        adapter = new GalleryAdapter(post -> {
            Intent i = new Intent(requireContext(), ImagePreviewActivity.class)
                    .putExtra(Navigation.EXTRA_POST_ID, post.getId());
            startActivity(i);
        });
        binding.grid.setAdapter(adapter);

        binding.cardCamera.setOnClickListener(v -> Navigation.newPostWithPhoto(requireContext(), Navigation.START_CAMERA));
        binding.cardGallery.setOnClickListener(v -> Navigation.newPostWithPhoto(requireContext(), Navigation.START_GALLERY));
        binding.empty.emptyIcon.setImageResource(R.drawable.ill_no_images);
        binding.empty.emptyTitle.setText(R.string.gallery_empty_title);
        binding.empty.emptyBody.setText(R.string.gallery_empty_body);
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
            List<Post> posts = PostRepository.getInstance(context).getWithImages();
            AppExecutors.main(() -> {
                if (binding == null) {
                    return;
                }
                adapter.submitList(posts);
                if (firstLoad) {
                    binding.grid.scheduleLayoutAnimation();
                    firstLoad = false;
                }
                binding.empty.getRoot().setVisibility(posts.isEmpty() ? View.VISIBLE : View.GONE);
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    // ---------------------------------------------------------------- grid adapter

    interface OnPhotoClick {
        void onClick(@NonNull Post post);
    }

    private static class GalleryAdapter extends ListAdapter<Post, GalleryAdapter.Holder> {
        private final OnPhotoClick listener;

        GalleryAdapter(OnPhotoClick listener) {
            super(new DiffUtil.ItemCallback<Post>() {
                @Override
                public boolean areItemsTheSame(@NonNull Post a, @NonNull Post b) {
                    return a.getId() == b.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Post a, @NonNull Post b) {
                    return a.hasSameContentAs(b);
                }
            });
            this.listener = listener;
        }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new Holder(ItemGalleryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull Holder holder, int position) {
            Post post = getItem(position);
            holder.b.caption.setText(post.getTitle());
            holder.b.card.setContentDescription(post.getTitle());
            int size = holder.b.getRoot().getResources().getDisplayMetrics().widthPixels / 3;
            ThumbnailLoader.load(holder.b.image, post.getImageFileName(), size);
            holder.b.card.setOnClickListener(v -> listener.onClick(post));
        }

        static class Holder extends RecyclerView.ViewHolder {
            final ItemGalleryBinding b;

            Holder(ItemGalleryBinding b) {
                super(b.getRoot());
                this.b = b;
            }
        }
    }
}
