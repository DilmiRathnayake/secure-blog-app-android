package com.blogspace.app.data;

import androidx.annotation.Nullable;

/**
 * Validation rules for a post, kept separate from the UI so they can be unit-tested.
 * Rules: title and content are required (whitespace-only counts as empty) and have maximum lengths.
 */
public final class PostValidator {

    public static final int MAX_TITLE_LENGTH = 120;
    public static final int MAX_CONTENT_LENGTH = 20000;

    public enum Result {
        OK,
        TITLE_EMPTY,
        TITLE_TOO_LONG,
        CONTENT_EMPTY,
        CONTENT_TOO_LONG
    }

    private PostValidator() {
    }

    public static Result validateTitle(@Nullable String title) {
        String t = title == null ? "" : title.trim();
        if (t.isEmpty()) {
            return Result.TITLE_EMPTY;
        }
        return t.length() > MAX_TITLE_LENGTH ? Result.TITLE_TOO_LONG : Result.OK;
    }

    public static Result validateContent(@Nullable String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) {
            return Result.CONTENT_EMPTY;
        }
        return c.length() > MAX_CONTENT_LENGTH ? Result.CONTENT_TOO_LONG : Result.OK;
    }
}
