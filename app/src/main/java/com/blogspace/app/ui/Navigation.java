package com.blogspace.app.ui;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

/** Builds Intents for opening screens, so extras are defined in one place. */
public final class Navigation {

    public static final String EXTRA_POST_ID = "post_id";
    /** EditPostActivity: immediately open the camera / photo picker (Gallery tab shortcuts). */
    public static final String EXTRA_START_WITH = "start_with";
    public static final String START_CAMERA = "camera";
    public static final String START_GALLERY = "gallery";
    /** PostDetailActivity: run an action once the post is shown (after "Save & publish/share"). */
    public static final String EXTRA_AUTO_ACTION = "auto_action";
    public static final String ACTION_PUBLISH = "publish";
    public static final String ACTION_SHARE = "share";

    private Navigation() {
    }

    public static void openPost(@NonNull Context context, long postId) {
        context.startActivity(new Intent(context, PostDetailActivity.class).putExtra(EXTRA_POST_ID, postId));
    }

    public static void openPostThen(@NonNull Context context, long postId, @NonNull String action) {
        context.startActivity(new Intent(context, PostDetailActivity.class)
                .putExtra(EXTRA_POST_ID, postId)
                .putExtra(EXTRA_AUTO_ACTION, action));
    }

    public static void newPost(@NonNull Context context) {
        context.startActivity(new Intent(context, EditPostActivity.class));
    }

    /** New post that immediately opens the camera or gallery picker. */
    public static void newPostWithPhoto(@NonNull Context context, @NonNull String source) {
        context.startActivity(new Intent(context, EditPostActivity.class).putExtra(EXTRA_START_WITH, source));
    }

    public static void editPost(@NonNull Context context, long postId) {
        context.startActivity(new Intent(context, EditPostActivity.class).putExtra(EXTRA_POST_ID, postId));
    }

    public static void openSearch(@NonNull Context context) {
        context.startActivity(new Intent(context, SearchActivity.class));
    }
}
