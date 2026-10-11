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
import com.blogspace.app.databinding.ActivityMainBinding;
import com.blogspace.app.publish.PublishController;
import com.blogspace.app.util.UiUtils;

/**
 * Main screen: hosts the five tabs (Home, Posts, Search, Publish, About) with a bottom
 * navigation bar, and owns the shared {@link PublishController}.
 */
public class MainActivity extends AppCompatActivity implements PublishController.Host {

    /** Optional extra: which bottom-nav tab to open (a menu item id). */
    public static final String EXTRA_OPEN_TAB = "open_tab";

    private ActivityMainBinding binding;
    private PublishController publishController;
    private OnBackPressedCallback backToHome;

    /** Fragments call this when something can't be refreshed by them alone. */
    public interface Refreshable {
        void refresh();
    }

    /** Intent that brings MainActivity to the front on the Publish tab. */
    @NonNull
    public static Intent publishSettingsIntent(@NonNull Context context) {
        return new Intent(context, MainActivity.class)
                .putExtra(EXTRA_OPEN_TAB, R.id.nav_publish)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.applySystemBarInsets(binding.getRoot(), false);
        setSupportActionBar(binding.toolbar);

        // Must be created before the Activity is STARTED (it registers an activity-result launcher).
        publishController = new PublishController(this, this);

        binding.bottomNav.setOnItemSelectedListener(item -> {
            showTab(item.getItemId());
            return true;
        });
        // Re-selecting the current tab does nothing (avoids recreating the fragment).
        binding.bottomNav.setOnItemReselectedListener(item -> { });

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
        // After rotation the FragmentManager restores the fragment; just restore title/back state.
        updateChrome(binding.bottomNav.getSelectedItemId());
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
                .replace(R.id.fragment_container, createFragment(itemId))
                .commit();
        updateChrome(itemId);
    }

    private void updateChrome(@IdRes int itemId) {
        setTitle(titleFor(itemId));
        backToHome.setEnabled(itemId != R.id.nav_home);
    }

    @NonNull
    private static Fragment createFragment(@IdRes int itemId) {
        if (itemId == R.id.nav_posts) {
            return new PostsFragment();
        } else if (itemId == R.id.nav_search) {
            return new SearchFragment();
        } else if (itemId == R.id.nav_publish) {
            return new PublishFragment();
        } else if (itemId == R.id.nav_about) {
            return new AboutFragment();
        }
        return new HomeFragment();
    }

    @NonNull
    private CharSequence titleFor(@IdRes int itemId) {
        if (itemId == R.id.nav_posts) {
            return getString(R.string.nav_posts);
        } else if (itemId == R.id.nav_search) {
            return getString(R.string.nav_search);
        } else if (itemId == R.id.nav_publish) {
            return getString(R.string.publish_title);
        } else if (itemId == R.id.nav_about) {
            return getString(R.string.nav_about);
        }
        return getString(R.string.app_name);
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
        selectTab(R.id.nav_publish);
    }
}
