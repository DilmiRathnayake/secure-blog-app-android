package com.blogspace.app.ui.widget;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Pulsing "loading skeleton" effect: fades a placeholder view in and out until stopped. */
public final class Shimmer {

    @Nullable
    private ObjectAnimator animator;
    private final View view;

    public Shimmer(@NonNull View view) {
        this.view = view;
    }

    public void start() {
        view.setVisibility(View.VISIBLE);
        if (animator == null) {
            animator = ObjectAnimator.ofFloat(view, View.ALPHA, 1f, 0.45f);
            animator.setDuration(750);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(ValueAnimator.INFINITE);
        }
        animator.start();
    }

    public void stop() {
        if (animator != null) {
            animator.cancel();
        }
        view.setAlpha(1f);
        view.setVisibility(View.GONE);
    }
}
