package com.blogspace.app;

import android.app.Application;

import com.blogspace.app.util.AppPrefs;

/** Application entry point: applies the user's saved Light/Dark/System theme before any screen opens. */
public class ConnectHubApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        AppPrefs.applyTheme(new AppPrefs(this).getThemeMode());
    }
}
