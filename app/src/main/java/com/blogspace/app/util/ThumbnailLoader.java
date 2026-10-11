package com.blogspace.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.LruCache;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blogspace.app.R;

import java.io.File;

/**
 * Loads small, cached thumbnails off the UI thread for RecyclerView rows.
 * <p>
 * Each ImageView is tagged with the file it should show; when a background decode finishes we
 * only set the bitmap if the (recycled) view still wants that file. This avoids rows briefly
 * showing another post's photo while scrolling.
 */
public final class ThumbnailLoader {

    /** Cache about 1/16 of the app's memory, measured in kilobytes. */
    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) (Runtime.getRuntime().maxMemory() / 1024 / 16)) {
                @Override
                protected int sizeOf(String key, Bitmap value) {
                    return value.getByteCount() / 1024;
                }
            };

    private ThumbnailLoader() {
    }

    /**
     * Shows {@code fileName} in {@code target}, scaled to roughly {@code sizePx}.
     * Shows a placeholder icon if the image file is missing.
     */
    public static void load(@NonNull ImageView target, @Nullable String fileName, int sizePx) {
        Context context = target.getContext().getApplicationContext();
        String key = fileName + "@" + sizePx;
        target.setTag(R.id.tag_thumbnail_key, key);

        Bitmap cached = CACHE.get(key);
        if (cached != null) {
            target.setImageBitmap(cached);
            return;
        }
        target.setImageResource(R.drawable.ic_image);
        AppExecutors.disk().execute(() -> {
            File file = ImageStorage.getImageFile(context, fileName);
            Bitmap bitmap = file == null ? null : ImageStorage.decodeSampled(file, sizePx, sizePx);
            if (bitmap != null) {
                CACHE.put(key, bitmap);
            }
            AppExecutors.main(() -> {
                if (key.equals(target.getTag(R.id.tag_thumbnail_key)) && bitmap != null) {
                    target.setImageBitmap(bitmap);
                }
            });
        });
    }
}
