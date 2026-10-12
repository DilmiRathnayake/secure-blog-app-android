package com.blogspace.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/** Filter chips on the Posts screen. Each maps to a WHERE clause and sort order. */
public enum PostFilter {
    NEWEST(null, BlogDbHelper.COL_UPDATED_AT + " DESC, " + BlogDbHelper.COL_ID + " DESC"),
    OLDEST(null, BlogDbHelper.COL_UPDATED_AT + " ASC, " + BlogDbHelper.COL_ID + " ASC"),
    UPLOADED(BlogDbHelper.COL_PUBLISHED_AT + " > 0 AND " + BlogDbHelper.COL_IS_DRAFT + " = 0", BlogDbHelper.COL_UPDATED_AT + " DESC, " + BlogDbHelper.COL_ID + " DESC"),
    OFFLINE(BlogDbHelper.COL_PUBLISHED_AT + " = 0 AND " + BlogDbHelper.COL_IS_DRAFT + " = 0", BlogDbHelper.COL_UPDATED_AT + " DESC, " + BlogDbHelper.COL_ID + " DESC"),
    DRAFT(BlogDbHelper.COL_IS_DRAFT + " = 1", BlogDbHelper.COL_UPDATED_AT + " DESC, " + BlogDbHelper.COL_ID + " DESC");

    /** Fixed SQL fragment (contains no user input). Null means "all posts". */
    @Nullable
    final String selection;
    @NonNull
    final String orderBy;

    PostFilter(@Nullable String selection, @NonNull String orderBy) {
        this.selection = selection;
        this.orderBy = orderBy;
    }
}
