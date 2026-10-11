package com.blogspace.app.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background threads for database, image and network work, so the UI thread never blocks.
 * <ul>
 *   <li>{@link #disk()} – SQLite and file work (2 threads).</li>
 *   <li>{@link #network()} – HTTP calls to Blogger (1 thread, so uploads happen one at a time).</li>
 *   <li>{@link #main(Runnable)} – post a result back to the UI thread.</li>
 * </ul>
 */
public final class AppExecutors {

    private static final ExecutorService DISK = Executors.newFixedThreadPool(2);
    private static final ExecutorService NETWORK = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    @NonNull
    public static ExecutorService disk() {
        return DISK;
    }

    @NonNull
    public static ExecutorService network() {
        return NETWORK;
    }

    public static void main(@NonNull Runnable task) {
        MAIN.post(task);
    }
}
