package com.blogspace.app.ui.widget;

import android.content.res.ColorStateList;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;

/** Colours a pill TextView as a Draft / Offline / Uploaded badge. */
public final class StatusBadge {

    private StatusBadge() {
    }

    public static void apply(@NonNull TextView badge, @NonNull Post.Status status) {
        int text;
        int fg;
        int bg;
        switch (status) {
            case DRAFT:
                text = R.string.status_draft;
                fg = R.color.status_warning;
                bg = R.color.status_warning_bg;
                break;
            case UPLOADED:
                text = R.string.status_uploaded;
                fg = R.color.status_success;
                bg = R.color.status_success_bg;
                break;
            default:
                text = R.string.status_offline;
                fg = R.color.status_offline;
                bg = R.color.status_offline_bg;
        }
        badge.setText(text);
        badge.setTextColor(color(badge, fg));
        badge.setBackgroundTintList(ColorStateList.valueOf(color(badge, bg)));
        TextViewCompat.setCompoundDrawableTintList(badge, ColorStateList.valueOf(color(badge, fg)));
    }

    private static int color(TextView v, @ColorRes int res) {
        return ContextCompat.getColor(v.getContext(), res);
    }
}
