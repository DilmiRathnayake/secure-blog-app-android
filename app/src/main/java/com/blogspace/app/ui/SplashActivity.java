package com.blogspace.app.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.OvershootInterpolator;

import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.blogspace.app.data.PostRepository;
import com.blogspace.app.databinding.ActivitySplashBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.AppPrefs;

/**
 * Animated gradient splash. While the logo animates, the database is opened on a background
 * thread (so the first screen loads instantly), then the user is routed to onboarding, profile
 * setup or the main screen.
 */
@SuppressLint("CustomSplashScreen") // our own branded splash follows the system one on Android 12+
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_DISPLAY_MS = 1400;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable proceed = this::routeNext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // Light status-bar icons on the dark gradient.
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        super.onCreate(savedInstanceState);
        ActivitySplashBinding b = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        // Logo pops in, text fades up, loader appears.
        b.logo.setScaleX(0.6f);
        b.logo.setScaleY(0.6f);
        b.logo.setAlpha(0f);
        b.logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.6f)).start();
        fadeUp(b.appName, 250);
        fadeUp(b.tagline, 400);
        fadeUp(b.loaderGroup, 600);
        b.glowTop.animate().scaleX(1.15f).scaleY(1.15f).setDuration(1400).start();

        // Warm up SQLite (creates/upgrades the database) off the main thread.
        AppExecutors.disk().execute(() -> PostRepository.getInstance(this).getStats());
        handler.postDelayed(proceed, MIN_DISPLAY_MS);
    }

    private static void fadeUp(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(24f);
        v.animate().alpha(1f).translationY(0f).setStartDelay(delay).setDuration(450).start();
    }

    private void routeNext() {
        AppPrefs prefs = new AppPrefs(this);
        Class<?> next;
        if (!prefs.isOnboardingDone()) {
            next = OnboardingActivity.class;
        } else if (!prefs.isProfileDone()) {
            next = ProfileSetupActivity.class;
        } else {
            next = MainActivity.class;
        }
        startActivity(new Intent(this, next));
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(proceed);
        super.onDestroy();
    }
}
