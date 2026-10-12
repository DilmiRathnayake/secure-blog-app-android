package com.blogspace.app.ui.widget;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;

import com.blogspace.app.R;
import com.blogspace.app.databinding.ViewSettingRowBinding;

/**
 * Reusable settings row component: tinted icon tile, title, optional subtitle and either a chevron
 * or a switch. Declared in XML with app:rowIcon / rowTitle / rowSubtitle / rowTint.
 */
public class SettingRowView extends LinearLayout {

    private final ViewSettingRowBinding b;

    public SettingRowView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        int padH = getResources().getDimensionPixelSize(R.dimen.space_l);
        int padV = getResources().getDimensionPixelSize(R.dimen.space_m);
        setPadding(padH, padV, padH, padV);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.search_pill_height) + padV);
        setClickable(true);
        setFocusable(true);
        TypedValue ripple = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, ripple, true);
        setBackgroundResource(ripple.resourceId);

        b = ViewSettingRowBinding.inflate(LayoutInflater.from(context), this);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SettingRowView);
        try {
            b.rowIcon.setImageDrawable(a.getDrawable(R.styleable.SettingRowView_rowIcon));
            b.rowTitle.setText(a.getString(R.styleable.SettingRowView_rowTitle));
            setSubtitle(a.getString(R.styleable.SettingRowView_rowSubtitle));
            int tint = a.getColor(R.styleable.SettingRowView_rowTint, Color.GRAY);
            b.rowIcon.setImageTintList(ColorStateList.valueOf(tint));
            // Icon tile = same colour at low opacity.
            b.rowIcon.setBackgroundTintList(ColorStateList.valueOf(ColorUtils.setAlphaComponent(tint, 0x24)));
        } finally {
            a.recycle();
        }
    }

    public void setSubtitle(@Nullable CharSequence subtitle) {
        b.rowSubtitle.setText(subtitle);
        b.rowSubtitle.setVisibility(subtitle == null || subtitle.length() == 0 ? View.GONE : View.VISIBLE);
    }

    /** Turns the row into a toggle: the whole row flips the switch. */
    public void setSwitch(boolean checked, @NonNull OnToggle listener) {
        b.rowChevron.setVisibility(View.GONE);
        b.rowSwitch.setVisibility(View.VISIBLE);
        // State always comes from the caller, not from the restored view state after recreation.
        b.rowSwitch.setSaveEnabled(false);
        b.rowSwitch.setChecked(checked);
        setOnClickListener(v -> {
            boolean now = !b.rowSwitch.isChecked();
            b.rowSwitch.setChecked(now);
            listener.onToggle(now);
        });
    }

    public interface OnToggle {
        void onToggle(boolean checked);
    }
}
