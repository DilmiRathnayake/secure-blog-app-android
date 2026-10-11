package com.blogspace.app.publish;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Non-secret publishing preferences (destination, chosen blog id/name, options).
 * <p>
 * No passwords or OAuth tokens are ever stored here: Google Play services keeps the user's
 * authorisation and gives us a short-lived access token each time we publish.
 */
public class PublishSettings {

    public enum Mode { BLOGGER, TEST }

    private static final String PREFS = "publish_settings";
    private static final String KEY_MODE = "mode";
    private static final String KEY_BLOG_ID = "blog_id";
    private static final String KEY_BLOG_NAME = "blog_name";
    private static final String KEY_BLOG_URL = "blog_url";
    private static final String KEY_INCLUDE_IMAGES = "include_images";
    private static final String KEY_SIMULATE_FAILURE = "simulate_failure";

    private final SharedPreferences prefs;

    public PublishSettings(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @NonNull
    public Mode getMode() {
        String value = prefs.getString(KEY_MODE, Mode.BLOGGER.name());
        try {
            return Mode.valueOf(value);
        } catch (IllegalArgumentException e) {
            return Mode.BLOGGER;
        }
    }

    public void setMode(@NonNull Mode mode) {
        prefs.edit().putString(KEY_MODE, mode.name()).apply();
    }

    public boolean isBloggerConfigured() {
        return getBlogId() != null;
    }

    @Nullable
    public String getBlogId() {
        return prefs.getString(KEY_BLOG_ID, null);
    }

    @Nullable
    public String getBlogName() {
        return prefs.getString(KEY_BLOG_NAME, null);
    }

    @Nullable
    public String getBlogUrl() {
        return prefs.getString(KEY_BLOG_URL, null);
    }

    public void setBlog(@NonNull BloggerApi.BlogInfo blog) {
        prefs.edit()
                .putString(KEY_BLOG_ID, blog.id)
                .putString(KEY_BLOG_NAME, blog.name)
                .putString(KEY_BLOG_URL, blog.url)
                .apply();
    }

    public void clearBlog() {
        prefs.edit().remove(KEY_BLOG_ID).remove(KEY_BLOG_NAME).remove(KEY_BLOG_URL).apply();
    }

    public boolean isIncludeImages() {
        return prefs.getBoolean(KEY_INCLUDE_IMAGES, true);
    }

    public void setIncludeImages(boolean include) {
        prefs.edit().putBoolean(KEY_INCLUDE_IMAGES, include).apply();
    }

    public boolean isSimulateFailure() {
        return prefs.getBoolean(KEY_SIMULATE_FAILURE, false);
    }

    public void setSimulateFailure(boolean simulate) {
        prefs.edit().putBoolean(KEY_SIMULATE_FAILURE, simulate).apply();
    }
}
