package com.blogspace.app.ui;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/** Builds Intents for opening screens, so extras are defined in one place. */
public final class Navigation {

    public static final String EXTRA_POST_ID = "post_id";

    private Navigation() {
    }

    public static void openPost(@NonNull Context context, long postId) {
        context.startActivity(new Intent(context, PostDetailActivity.class).putExtra(EXTRA_POST_ID, postId));
    }

    public static void newPost(@NonNull Context context) {
        context.startActivity(new Intent(context, EditPostActivity.class));
    }

    public static void editPost(@NonNull Context context, long postId) {
        context.startActivity(new Intent(context, EditPostActivity.class).putExtra(EXTRA_POST_ID, postId));
    }
}
