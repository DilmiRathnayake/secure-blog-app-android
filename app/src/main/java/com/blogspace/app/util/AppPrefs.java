package com.blogspace.app.util;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import java.util.ArrayList;
import java.util.List;

/**
 * Small, non-secret app preferences: onboarding seen, local profile (name + avatar file),
 * theme choice and recent searches. Stored in private SharedPreferences.
 */
public class AppPrefs {

    private static final String PREFS = "app_prefs";
    private static final String KEY_ONBOARDING_DONE = "onboarding_done";
    private static final String KEY_PROFILE_DONE = "profile_done";
    private static final String KEY_NAME = "profile_name";
    private static final String KEY_AVATAR = "profile_avatar";
    private static final String KEY_THEME = "theme_mode";
    private static final String KEY_SEARCH_HISTORY = "search_history";
    /** Unit separator: cannot be typed in a search box, so it is a safe list delimiter. */
    private static final String SEPARATOR = "\u001F";
    public static final int MAX_HISTORY = 8;

    private final SharedPreferences prefs;

    public AppPrefs(@NonNull Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------------------------------------------------------------- first run

    public boolean isOnboardingDone() {
        return prefs.getBoolean(KEY_ONBOARDING_DONE, false);
    }

    public void setOnboardingDone(boolean done) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply();
    }

    public boolean isProfileDone() {
        return prefs.getBoolean(KEY_PROFILE_DONE, false);
    }

    public void setProfileDone() {
        prefs.edit().putBoolean(KEY_PROFILE_DONE, true).apply();
    }

    // ---------------------------------------------------------------- profile

    @Nullable
    public String getName() {
        return prefs.getString(KEY_NAME, null);
    }

    public void setName(@Nullable String name) {
        prefs.edit().putString(KEY_NAME, name).apply();
    }

    /** File name of the avatar in the app's images folder (see ImageStorage), or null. */
    @Nullable
    public String getAvatar() {
        return prefs.getString(KEY_AVATAR, null);
    }

    public void setAvatar(@Nullable String fileName) {
        prefs.edit().putString(KEY_AVATAR, fileName).apply();
    }

    // ---------------------------------------------------------------- theme

    /** One of AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM / MODE_NIGHT_NO / MODE_NIGHT_YES. */
    public int getThemeMode() {
        return prefs.getInt(KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    public void setThemeMode(int mode) {
        prefs.edit().putInt(KEY_THEME, mode).apply();
        applyTheme(mode);
    }

    public static void applyTheme(int mode) {
        AppCompatDelegate.setDefaultNightMode(mode);
    }

    // ---------------------------------------------------------------- search history

    @NonNull
    public List<String> getSearchHistory() {
        String raw = prefs.getString(KEY_SEARCH_HISTORY, "");
        List<String> list = new ArrayList<>();
        if (raw != null && !raw.isEmpty()) {
            for (String s : raw.split(SEPARATOR)) {
                if (!s.isEmpty()) {
                    list.add(s);
                }
            }
        }
        return list;
    }

    /** Adds a query to the front of the history (case-insensitive de-duplication). */
    public void addSearch(@NonNull String query) {
        List<String> list = SearchHistory.add(getSearchHistory(), query, MAX_HISTORY);
        prefs.edit().putString(KEY_SEARCH_HISTORY, android.text.TextUtils.join(SEPARATOR, list)).apply();
    }

    public void clearSearchHistory() {
        prefs.edit().remove(KEY_SEARCH_HISTORY).apply();
    }
}
