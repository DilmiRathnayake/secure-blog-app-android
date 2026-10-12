package com.blogspace.app.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.blogspace.app.R;
import com.blogspace.app.data.PostFilter;
import com.blogspace.app.databinding.ActivityMainBinding;
import com.blogspace.app.publish.PublishController;

/**
 * Main screen: hosts the five tabs (Home, Posts, Upload, Gallery, Profile) with a bottom
 * navigation bar, and owns the shared {@link PublishController}.
 */
public class MainActivity extends AppCompatActivity implements PublishController.Host {

    /** Optional extra: which bottom-nav tab to open (a menu item id). */
    public static final String EXTRA_OPEN_TAB = "open_tab";

    private ActivityMainBinding binding;
    private PublishController publishController;
    private OnBackPressedCallback backToHome;
    /** Filter to apply when the Posts tab is next created (from a Home statistic card). */
    @Nullable
    private PostFilter pendingPostsFilter;

    /** Tabs implement this so they can be refreshed after publishing. */
    public interface Refreshable {
        void refresh();
    }

    /** Intent that brings MainActivity to the front on a given tab. */
    @NonNull
    public static Intent tabIntent(@NonNull Context context, @IdRes int tab) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_OPEN_TAB, tab)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Must be created before the Activity is STARTED (it registers an activity-result launcher).
        publishController = new PublishController(this, this);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        // Re-selecting the current tab scrolls/refreshes rather than recreating it.
        binding.bottomNav.setOnItemReselectedListener(item -> onPublishFinished());

        backToHome = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                binding.bottomNav.setSelectedItemId(R.id.nav_home);
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backToHome);

        if (savedInstanceState == null) {
            int tab = getIntent().getIntExtra(EXTRA_OPEN_TAB, R.id.nav_home);
            if (tab == binding.bottomNav.getSelectedItemId()) {
                showTab(tab); // listener does not fire for the already-selected item
            } else {
                binding.bottomNav.setSelectedItemId(tab);
            }
        }
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        backToHome.setEnabled(binding.bottomNav.getSelectedItemId() != R.id.nav_home);
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        int tab = intent.getIntExtra(EXTRA_OPEN_TAB, 0);
        if (tab != 0) {
            binding.bottomNav.setSelectedItemId(tab);
        }
    }

    private void showTab(@IdRes int itemId) {
        getSupportFragmentManager().beginTransaction()
                .setReorderingAllowed(true)
                .setCustomAnimations(R.anim.fade_in, R.anim.fade_out)
                .replace(R.id.fragment_container, createFragment(itemId))
                .commit();
        backToHome.setEnabled(itemId != R.id.nav_home);
    }

    @NonNull
    private Fragment createFragment(@IdRes int itemId) {
        if (itemId == R.id.nav_posts) {
            PostsFragment posts = new PostsFragment();
            if (pendingPostsFilter != null) {
                Bundle args = new Bundle();
                args.putString(PostsFragment.ARG_FILTER, pendingPostsFilter.name());
                posts.setArguments(args);
                pendingPostsFilter = null;
            }
            return posts;
        } else if (itemId == R.id.nav_upload) {
            return new UploadFragment();
        } else if (itemId == R.id.nav_gallery) {
            return new GalleryFragment();
        } else if (itemId == R.id.nav_profile) {
            return new ProfileFragment();
        }
        return new HomeFragment();
    }

    /** Opens the Posts tab with a filter chip pre-selected (Home statistic cards). */
    public void openPosts(@NonNull PostFilter filter) {
        pendingPostsFilter = filter;
        if (binding.bottomNav.getSelectedItemId() == R.id.nav_posts) {
            showTab(R.id.nav_posts);
        } else {
            binding.bottomNav.setSelectedItemId(R.id.nav_posts);
        }
    }

    /** Switches tab programmatically (e.g. Home's "View all" button). */
    public void selectTab(@IdRes int itemId) {
        binding.bottomNav.setSelectedItemId(itemId);
    }

    @NonNull
    public PublishController getPublishController() {
        return publishController;
    }

    // ---------------------------------------------------------------- PublishController.Host

    @Override
    public void onPublishFinished() {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current instanceof Refreshable) {
            ((Refreshable) current).refresh();
        }
    }

    @Override
    public void openPublishSettings() {
        startActivity(new Intent(this, PublishSettingsActivity.class));
    }
}
