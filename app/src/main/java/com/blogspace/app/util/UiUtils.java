package com.blogspace.app.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.blogspace.app.R;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

/** Small helpers shared by several screens. */
public final class UiUtils {

    private UiUtils() {
    }

    /**
     * Android 15+ draws apps edge-to-edge (behind the status and navigation bars). This pads
     * {@code root} by the system bar and keyboard sizes so no content is hidden behind them.
     */
    public static void applySystemBarInsets(@NonNull View root, boolean includeBottom) {
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            int bottomInset = includeBottom ? Math.max(bars.bottom, ime.bottom) : 0;
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bottomInset);
            return includeBottom ? WindowInsetsCompat.CONSUMED : windowInsets;
        });
    }

    /**
     * Adds the status-bar height to the top padding of a header that is drawn edge-to-edge
     * (e.g. a gradient hero), so its content is not hidden under the status bar.
     */
    public static void applyStatusBarPadding(@NonNull View header) {
        final int top = header.getPaddingTop();
        ViewCompat.setOnApplyWindowInsetsListener(header, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            v.setPadding(v.getPaddingLeft(), top + bars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });
        ViewCompat.requestApplyInsets(header);
    }

    /** Pads only the bottom of {@code view} by the navigation bar / keyboard height. */
    public static void applyBottomInsets(@NonNull View view) {
        final int bottom = view.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets ime = insets.getInsets(WindowInsetsCompat.Type.ime());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), bottom + Math.max(bars.bottom, ime.bottom));
            return insets;
        });
    }

    /** Time-of-day greeting, e.g. "Good morning,". */
    @NonNull
    public static String greeting(@NonNull Context context) {
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour < 12) {
            return context.getString(R.string.greeting_morning);
        }
        return context.getString(hour < 18 ? R.string.greeting_afternoon : R.string.greeting_evening);
    }

    /** True when the device currently has a validated internet connection. */
    public static boolean isOnline(@NonNull Context context) {
        ConnectivityManager cm = ContextCompat.getSystemService(context, ConnectivityManager.class);
        if (cm == null) {
            return false;
        }
        Network network = cm.getActiveNetwork();
        NetworkCapabilities caps = network == null ? null : cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
    }

    /** "5 minutes ago", "Yesterday", etc. */
    @NonNull
    public static CharSequence relativeTime(long timeMillis) {
        return DateUtils.getRelativeTimeSpanString(timeMillis, System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE);
    }

    /** Full localised date and time, e.g. "12 Oct 2026, 14:05". */
    @NonNull
    public static String fullDateTime(long timeMillis) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.getDefault())
                .format(new Date(timeMillis));
    }

    /** Highlights every case-insensitive occurrence of {@code query} in {@code text}. */
    @NonNull
    public static CharSequence highlight(@NonNull Context context, @NonNull String text, @Nullable String query) {
        if (query == null || query.trim().isEmpty()) {
            return text;
        }
        String needle = query.trim().toLowerCase(Locale.ROOT);
        String haystack = text.toLowerCase(Locale.ROOT);
        // Lower-casing can change length for some scripts; only highlight when indexes still line up.
        if (haystack.length() != text.length()) {
            return text;
        }
        SpannableString spannable = new SpannableString(text);
        int colour = ContextCompat.getColor(context, R.color.bs_highlight);
        int index = haystack.indexOf(needle);
        while (index >= 0 && !needle.isEmpty()) {
            spannable.setSpan(new BackgroundColorSpan(colour), index, index + needle.length(),
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            index = haystack.indexOf(needle, index + needle.length());
        }
        return spannable;
    }

    /**
     * Returns a snippet of {@code content} that starts near the first match, so search results
     * show why a long post matched.
     */
    @NonNull
    public static String snippetAround(@NonNull String content, @Nullable String query, int lead) {
        String flat = content.replace('\n', ' ').trim();
        if (query == null || query.trim().isEmpty()) {
            return flat;
        }
        int index = flat.toLowerCase(Locale.ROOT).indexOf(query.trim().toLowerCase(Locale.ROOT));
        if (index <= lead || flat.length() != flat.toLowerCase(Locale.ROOT).length()) {
            return flat;
        }
        int start = flat.lastIndexOf(' ', index - lead);
        start = start < 0 ? index - lead : start + 1;
        return "…" + flat.substring(start);
    }
}
