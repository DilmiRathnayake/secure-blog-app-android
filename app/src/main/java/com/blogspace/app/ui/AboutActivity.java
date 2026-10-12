package com.blogspace.app.ui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.R;
import com.blogspace.app.databinding.ActivityHostBinding;
import com.blogspace.app.util.UiUtils;

/** Hosts the About / Help page. */
public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        ActivityHostBinding b = ActivityHostBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        UiUtils.applySystemBarInsets(b.getRoot(), true);
        setSupportActionBar(b.toolbar);
        setTitle(R.string.setting_about);
        b.toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.host_container, new AboutFragment())
                    .commit();
        }
    }
}
