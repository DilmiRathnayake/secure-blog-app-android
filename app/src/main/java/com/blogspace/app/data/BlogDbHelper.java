package com.blogspace.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Creates and upgrades the on-device SQLite database that stores every post.
 * <p>
 * Schema (table {@value #TABLE_POSTS}):
 * <pre>
 *  _id            INTEGER PRIMARY KEY AUTOINCREMENT
 *  title          TEXT    NOT NULL
 *  content        TEXT    NOT NULL
 *  image_file     TEXT    NULL      -- file name in filesDir/images, or NULL
 *  created_at     INTEGER NOT NULL  -- epoch millis
 *  updated_at     INTEGER NOT NULL  -- epoch millis
 *  remote_post_id TEXT    NULL      -- Blogger post id after a confirmed publish
 *  remote_url     TEXT    NULL      -- public Blogger URL after a confirmed publish
 *  published_at   INTEGER NOT NULL DEFAULT 0
 *  is_draft       INTEGER NOT NULL DEFAULT 0  -- added in version 2
 * </pre>
 */
public class BlogDbHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "blogspace.db";
    /** Increase this and add a step in {@link #onUpgrade} whenever the schema changes. */
    public static final int DATABASE_VERSION = 2;

    public static final String TABLE_POSTS = "posts";
    public static final String COL_ID = "_id";
    public static final String COL_TITLE = "title";
    public static final String COL_CONTENT = "content";
    public static final String COL_IMAGE_FILE = "image_file";
    public static final String COL_CREATED_AT = "created_at";
    public static final String COL_UPDATED_AT = "updated_at";
    public static final String COL_REMOTE_POST_ID = "remote_post_id";
    public static final String COL_REMOTE_URL = "remote_url";
    public static final String COL_PUBLISHED_AT = "published_at";
    public static final String COL_IS_DRAFT = "is_draft";

    private static final String SQL_CREATE_POSTS =
            "CREATE TABLE " + TABLE_POSTS + " ("
                    + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + COL_TITLE + " TEXT NOT NULL, "
                    + COL_CONTENT + " TEXT NOT NULL, "
                    + COL_IMAGE_FILE + " TEXT, "
                    + COL_CREATED_AT + " INTEGER NOT NULL, "
                    + COL_UPDATED_AT + " INTEGER NOT NULL, "
                    + COL_REMOTE_POST_ID + " TEXT, "
                    + COL_REMOTE_URL + " TEXT, "
                    + COL_PUBLISHED_AT + " INTEGER NOT NULL DEFAULT 0, "
                    + COL_IS_DRAFT + " INTEGER NOT NULL DEFAULT 0)";

    /** Lists are always sorted newest-edited first, so index that column. */
    private static final String SQL_CREATE_UPDATED_INDEX =
            "CREATE INDEX idx_posts_updated_at ON " + TABLE_POSTS + " (" + COL_UPDATED_AT + " DESC)";

    public BlogDbHelper(@NonNull Context context) {
        this(context, DATABASE_NAME);
    }

    /** @param databaseName file name, or null for a temporary in-memory database (tests). */
    public BlogDbHelper(@NonNull Context context, @Nullable String databaseName) {
        // Application context avoids leaking an Activity through the singleton repository.
        super(context.getApplicationContext(), databaseName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_POSTS);
        db.execSQL(SQL_CREATE_UPDATED_INDEX);
    }

    /**
     * Migrates step by step from {@code oldVersion} so that users never lose posts.
     * Each schema version adds one step that ALTERs the table; existing rows are never dropped.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        int version = oldVersion;
        if (version < 2) {
            // v2: drafts. Existing posts become normal (non-draft) posts.
            db.execSQL("ALTER TABLE " + TABLE_POSTS + " ADD COLUMN " + COL_IS_DRAFT
                    + " INTEGER NOT NULL DEFAULT 0");
            version = 2;
        }
        if (version != newVersion) {
            throw new IllegalStateException(
                    "No migration path from database version " + oldVersion + " to " + newVersion);
        }
    }
}
