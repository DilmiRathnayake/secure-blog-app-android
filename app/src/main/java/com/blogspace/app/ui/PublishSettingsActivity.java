package com.blogspace.app.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.databinding.ActivityHostBinding;
import com.blogspace.app.publish.GoogleAuthHelper;
import com.blogspace.app.util.UiUtils;

/** Hosts the publishing settings (Blogger account / Test mode). */
public class PublishSettingsActivity extends AppCompatActivity implements PublishFragment.AuthHost {

    private GoogleAuthHelper authHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        ActivityHostBinding b = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.applySystemBarInsets(b.getRoot(), true);
        setSupportActionBar(b.toolbar);
        setTitle(R.string.publish_title);
        b.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        // Registered before STARTED (consent screen launcher).
        authHelper = new GoogleAuthHelper(this, this);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.host_container, new PublishFragment())
                    .commit();
        }
    }

    @NonNull
    @Override
    public GoogleAuthHelper getAuthHelper() {
        return authHelper;
    }
}
