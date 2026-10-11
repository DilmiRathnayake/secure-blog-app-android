package com.blogspace.app.util;

import androidx.annotation.NonNull;

/**
 * Builds the plain-text body used when a post is shared. Kept free of Android classes
 * so it can be unit-tested on the JVM.
 */
public final class ShareText {

    private ShareText() {
    }

    /**
     * Formats a post for sharing:
     * <pre>
     * Title
     *
     * Body text
     *
     * — Shared from BlogSpace
     * </pre>
     */
    @NonNull
    public static String build(@NonNull String title, @NonNull String content, @NonNull String footer) {
        StringBuilder sb = new StringBuilder();
        sb.append(title.trim()).append("\n\n").append(content.trim());
        if (!footer.isEmpty()) {
            sb.append("\n\n— ").append(footer);
        }
        return sb.toString();
    }
}
