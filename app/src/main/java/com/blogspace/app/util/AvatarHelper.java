package com.blogspace.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.blogspace.app.R;
import com.google.android.material.color.MaterialColors;

import java.io.File;

/** Shows the local profile photo, or a tinted person icon when there is none. */
public final class AvatarHelper {

    private AvatarHelper() {
    }

    public static void show(ImageView view, @Nullable String fileName) {
        Context context = view.getContext().getApplicationContext();
        showPlaceholder(view);
        if (fileName == null) {
            return;
        }
        int size = Math.max(view.getLayoutParams() != null ? view.getLayoutParams().width : 0, 256);
        AppExecutors.disk().execute(() -> {
            File file = ImageStorage.getImageFile(context, fileName);
            Bitmap bitmap = file == null ? null : ImageStorage.decodeSampled(file, size, size);
            if (bitmap != null) {
                AppExecutors.main(() -> {
                    view.setImageTintList(null);
                    view.setPadding(0, 0, 0, 0);
                    view.setImageBitmap(bitmap);
                });
            }
        });
    }

    private static void showPlaceholder(ImageView view) {
        int pad = view.getResources().getDimensionPixelSize(R.dimen.space_s);
        view.setPadding(pad, pad, pad, pad);
        view.setImageDrawable(ContextCompat.getDrawable(view.getContext(), R.drawable.ic_person));
        view.setImageTintList(android.content.res.ColorStateList.valueOf(
                MaterialColors.getColor(view, com.google.android.material.R.attr.colorPrimary)));
    }
}
