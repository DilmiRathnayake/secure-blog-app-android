package com.blogspace.app.ui;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.databinding.BottomSheetShareBinding;
import com.blogspace.app.databinding.ItemShareTargetBinding;
import com.blogspace.app.util.AppExecutors;
import com.blogspace.app.util.ImageStorage;
import com.blogspace.app.util.ShareHelper;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;

/**
 * Share sheet styled like Android's: Email, WhatsApp, Facebook, Messenger, Copy and More.
 * Every target uses the standard ACTION_SEND intent (see {@link ShareHelper}). App shortcuts are
 * only shown when that app is installed, using the app's real icon.
 */
public final class ShareSheet {

    private static final String WHATSAPP = "com.whatsapp";
    private static final String FACEBOOK = "com.facebook.katana";
    private static final String MESSENGER = "com.facebook.orca";

    private ShareSheet() {
    }

    public static void show(@NonNull Activity activity, @NonNull Post post) {
        BottomSheetDialog dialog = new BottomSheetDialog(activity);
        BottomSheetShareBinding b = BottomSheetShareBinding.inflate(LayoutInflater.from(activity));
        dialog.setContentView(b.getRoot());

        b.previewTitle.setText(post.getTitle());
        b.previewBody.setText(post.getContent());
        if (post.hasImage()) {
            int size = activity.getResources().getDimensionPixelSize(R.dimen.thumb_size);
            AppExecutors.disk().execute(() -> {
                File file = ImageStorage.getImageFile(activity, post.getImageFileName());
                Bitmap bmp = file == null ? null : ImageStorage.decodeSampled(file, size, size);
                AppExecutors.main(() -> {
                    if (bmp != null) {
                        b.previewImage.setImageBitmap(bmp);
                        b.previewImage.setVisibility(View.VISIBLE);
                    }
                });
            });
        }

        addTarget(activity, b.targets, R.drawable.ic_email, null, R.color.status_error_bg, R.color.status_error,
                activity.getString(R.string.share_target_email), () -> ShareHelper.shareByEmail(activity, post), dialog);
        addAppTarget(activity, b.targets, WHATSAPP, "WhatsApp", post, dialog);
        addAppTarget(activity, b.targets, FACEBOOK, "Facebook", post, dialog);
        addAppTarget(activity, b.targets, MESSENGER, "Messenger", post, dialog);
        addTarget(activity, b.targets, R.drawable.ic_copy, null, R.color.bs_primary_container, R.color.brand_indigo,
                activity.getString(post.getRemoteUrl() != null ? R.string.share_target_copy_link : R.string.share_target_copy),
                () -> ShareHelper.copyToClipboard(activity, post), dialog);
        addTarget(activity, b.targets, R.drawable.ic_more_horiz, null, R.color.bs_surface_container_high, R.color.bs_on_surface_variant,
                activity.getString(R.string.share_target_more), () -> ShareHelper.shareWithChooser(activity, post), dialog);
        dialog.show();
    }

    private static void addAppTarget(Activity activity, GridLayout grid, String pkg, String label,
                                     Post post, BottomSheetDialog dialog) {
        Drawable icon;
        try {
            icon = activity.getPackageManager().getApplicationIcon(pkg);
        } catch (PackageManager.NameNotFoundException e) {
            return; // not installed: no fake shortcut
        }
        addTarget(activity, grid, 0, icon, 0, 0, label, () -> ShareHelper.shareToPackage(activity, post, pkg), dialog);
    }

    private static void addTarget(Activity activity, GridLayout grid, @DrawableRes int iconRes,
                                  @Nullable Drawable appIcon, @ColorRes int bg, @ColorRes int tint,
                                  String label, Runnable action, BottomSheetDialog dialog) {
        ItemShareTargetBinding t = ItemShareTargetBinding.inflate(LayoutInflater.from(activity), grid, false);
        if (appIcon != null) {
            t.icon.setImageDrawable(appIcon);
            t.iconBg.setBackground(null);
            t.icon.getLayoutParams().width = t.iconBg.getLayoutParams().width;
            t.icon.getLayoutParams().height = t.iconBg.getLayoutParams().height;
        } else {
            t.icon.setImageResource(iconRes);
            t.icon.setImageTintList(ContextCompat.getColorStateList(activity, tint));
            t.iconBg.setBackgroundTintList(ContextCompat.getColorStateList(activity, bg));
        }
        t.label.setText(label);
        t.getRoot().setContentDescription(label);
        t.getRoot().setOnClickListener(v -> {
            dialog.dismiss();
            action.run();
        });
        GridLayout.LayoutParams lp = new GridLayout.LayoutParams(
                GridLayout.spec(GridLayout.UNDEFINED), GridLayout.spec(GridLayout.UNDEFINED, 1f));
        lp.width = 0;
        grid.addView(t.getRoot(), lp);
    }
}
