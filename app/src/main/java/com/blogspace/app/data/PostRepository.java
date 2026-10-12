package com.blogspace.app.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.blogspace.app.util.ImageStorage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.blogspace.app.data.BlogDbHelper.COL_CONTENT;
import static com.blogspace.app.data.BlogDbHelper.COL_CREATED_AT;
import static com.blogspace.app.data.BlogDbHelper.COL_ID;
import static com.blogspace.app.data.BlogDbHelper.COL_IMAGE_FILE;
import static com.blogspace.app.data.BlogDbHelper.COL_IS_DRAFT;
import static com.blogspace.app.data.BlogDbHelper.COL_PUBLISHED_AT;
import static com.blogspace.app.data.BlogDbHelper.COL_REMOTE_POST_ID;
import static com.blogspace.app.data.BlogDbHelper.COL_REMOTE_URL;
import static com.blogspace.app.data.BlogDbHelper.COL_TITLE;
import static com.blogspace.app.data.BlogDbHelper.COL_UPDATED_AT;
import static com.blogspace.app.data.BlogDbHelper.TABLE_POSTS;

/**
 * The only class that reads or writes the posts table.
 * <p>
 * Every query uses bound "?" arguments (never string concatenation of user input), every
 * Cursor is closed with try-with-resources, and multi-row deletes run in a transaction.
 * Methods are blocking, so call them from a background thread (see {@code AppExecutors}).
 */
public class PostRepository {

    private static final String[] ALL_COLUMNS = {
            COL_ID, COL_TITLE, COL_CONTENT, COL_IMAGE_FILE, COL_CREATED_AT,
            COL_UPDATED_AT, COL_REMOTE_POST_ID, COL_REMOTE_URL, COL_PUBLISHED_AT, COL_IS_DRAFT
    };
    /** Newest edits first; id breaks ties so the order is always stable. */
    private static final String DEFAULT_ORDER = COL_UPDATED_AT + " DESC, " + COL_ID + " DESC";

    private static volatile PostRepository instance;

    private final Context appContext;
    private final BlogDbHelper dbHelper;

    /** Counts shown on the Home and Profile screens. */
    public static class Stats {
        public final int total;
        public final int withImages;
        public final int published;
        public final int offline;
        public final int drafts;

        Stats(int total, int withImages, int published, int offline, int drafts) {
            this.total = total;
            this.withImages = withImages;
            this.published = published;
            this.offline = offline;
            this.drafts = drafts;
        }
    }

    private PostRepository(@NonNull Context context, @Nullable String databaseName) {
        appContext = context.getApplicationContext();
        dbHelper = new BlogDbHelper(appContext, databaseName);
    }

    /** Creates a repository on a separate database so tests never touch the user's real posts. */
    @VisibleForTesting
    @NonNull
    public static PostRepository createForTesting(@NonNull Context context, @Nullable String databaseName) {
        return new PostRepository(context, databaseName);
    }

    /** Closes the database connection (used by tests). */
    @VisibleForTesting
    public void close() {
        dbHelper.close();
    }

    /** One shared instance, so the whole app uses one SQLiteOpenHelper (SQLite's recommendation). */
    @NonNull
    public static PostRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (PostRepository.class) {
                if (instance == null) {
                    instance = new PostRepository(context, BlogDbHelper.DATABASE_NAME);
                }
            }
        }
        return instance;
    }

    // ---------------------------------------------------------------- create / update

    /**
     * Inserts a new post and sets its id and timestamps.
     *
     * @return the new row id, or -1 if the insert failed
     */
    @WorkerThread
    public long insert(@NonNull Post post) {
        long now = System.currentTimeMillis();
        post.setCreatedAt(now);
        post.setUpdatedAt(now);
        ContentValues values = editableValues(post);
        values.put(COL_CREATED_AT, now);
        long id = dbHelper.getWritableDatabase().insert(TABLE_POSTS, null, values);
        if (id != -1) {
            post.setId(id);
        }
        return id;
    }

    /**
     * Updates title, content and photo of an existing post (same id, so no duplicate is created).
     *
     * @return true if exactly one row was updated
     */
    @WorkerThread
    public boolean update(@NonNull Post post) {
        if (post.getId() == Post.NO_ID) {
            throw new IllegalArgumentException("Cannot update a post that has no id");
        }
        post.setUpdatedAt(System.currentTimeMillis());
        int rows = dbHelper.getWritableDatabase().update(
                TABLE_POSTS, editableValues(post), COL_ID + " = ?", idArg(post.getId()));
        return rows == 1;
    }

    /** Records a publish confirmed by the online platform. */
    @WorkerThread
    public boolean markPublished(long id, @Nullable String remoteId, @Nullable String remoteUrl) {
        ContentValues values = new ContentValues();
        values.put(COL_REMOTE_POST_ID, remoteId);
        values.put(COL_REMOTE_URL, remoteUrl);
        values.put(COL_PUBLISHED_AT, System.currentTimeMillis());
        return dbHelper.getWritableDatabase()
                .update(TABLE_POSTS, values, COL_ID + " = ?", idArg(id)) == 1;
    }

    private static ContentValues editableValues(@NonNull Post post) {
        ContentValues values = new ContentValues();
        values.put(COL_TITLE, post.getTitle());
        values.put(COL_CONTENT, post.getContent());
        if (post.hasImage()) {
            values.put(COL_IMAGE_FILE, post.getImageFileName());
        } else {
            values.putNull(COL_IMAGE_FILE);
        }
        values.put(COL_UPDATED_AT, post.getUpdatedAt());
        values.put(COL_IS_DRAFT, post.isDraft() ? 1 : 0);
        return values;
    }

    // ---------------------------------------------------------------- read

    @WorkerThread
    @Nullable
    public Post getById(long id) {
        try (Cursor c = dbHelper.getReadableDatabase().query(TABLE_POSTS, ALL_COLUMNS,
                COL_ID + " = ?", idArg(id), null, null, null)) {
            return c.moveToFirst() ? fromCursor(c) : null;
        }
    }

    @WorkerThread
    @NonNull
    public List<Post> getAll() {
        return query(null, null, null);
    }

    /** Posts matching a filter chip (Newest, Oldest, Uploaded, Offline, Draft). */
    @WorkerThread
    @NonNull
    public List<Post> getAll(@NonNull PostFilter filter) {
        return query(filter.selection, null, filter.orderBy, null);
    }

    /** Posts that have a photo, newest first (Gallery screen). */
    @WorkerThread
    @NonNull
    public List<Post> getWithImages() {
        return query(COL_IMAGE_FILE + " IS NOT NULL", null, null);
    }

    /** Finished posts that have not been uploaded yet (Upload screen queue). */
    @WorkerThread
    @NonNull
    public List<Post> getUploadQueue() {
        return getAll(PostFilter.OFFLINE);
    }

    /** Detaches the photo from a post and deletes the file. */
    @WorkerThread
    public boolean removeImage(long id) {
        Post post = getById(id);
        if (post == null || !post.hasImage()) {
            return false;
        }
        String name = post.getImageFileName();
        post.setImageFileName(null);
        if (!update(post)) {
            return false;
        }
        ImageStorage.deleteImage(appContext, name);
        return true;
    }

    @WorkerThread
    @NonNull
    public List<Post> getRecent(int limit) {
        return query(null, null, String.valueOf(limit));
    }

    /** Loads the given posts (in list order). Ids that no longer exist are skipped. */
    @WorkerThread
    @NonNull
    public List<Post> getByIds(@NonNull Collection<Long> ids) {
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return query(COL_ID + " IN (" + placeholders(ids.size()) + ")", toArgs(ids), null);
    }

    /**
     * Finds posts whose title OR content contains {@code rawQuery}.
     * Matching is partial and case-insensitive (SQLite LIKE ignores case for A-Z).
     * Title matches are listed first, then newest first.
     *
     * @return matching posts, or an empty list for blank input
     */
    @WorkerThread
    @NonNull
    public List<Post> search(@Nullable String rawQuery) {
        String query = SearchQuery.normalise(rawQuery);
        if (query == null) {
            return new ArrayList<>();
        }
        String pattern = SearchQuery.toContainsPattern(query);
        String escape = " ESCAPE '" + SearchQuery.ESCAPE_CHAR + "'";
        String selection = COL_TITLE + " LIKE ?" + escape + " OR " + COL_CONTENT + " LIKE ?" + escape;
        // CASE expression ranks title hits above body-only hits; the pattern is bound, not concatenated.
        String order = "CASE WHEN " + COL_TITLE + " LIKE ?" + escape + " THEN 0 ELSE 1 END, " + DEFAULT_ORDER;
        String sql = "SELECT " + TextUtils.join(", ", ALL_COLUMNS) + " FROM " + TABLE_POSTS
                + " WHERE " + selection + " ORDER BY " + order;
        List<Post> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().rawQuery(sql,
                new String[]{pattern, pattern, pattern})) {
            while (c.moveToNext()) {
                result.add(fromCursor(c));
            }
        }
        return result;
    }

    @WorkerThread
    @NonNull
    public Stats getStats() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*), "
                + "SUM(CASE WHEN " + COL_IMAGE_FILE + " IS NOT NULL THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + COL_PUBLISHED_AT + " > 0 AND " + COL_IS_DRAFT + " = 0 THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + COL_PUBLISHED_AT + " = 0 AND " + COL_IS_DRAFT + " = 0 THEN 1 ELSE 0 END), "
                + "SUM(CASE WHEN " + COL_IS_DRAFT + " = 1 THEN 1 ELSE 0 END) FROM " + TABLE_POSTS;
        try (Cursor c = db.rawQuery(sql, null)) {
            if (c.moveToFirst()) {
                return new Stats(c.getInt(0), c.getInt(1), c.getInt(2), c.getInt(3), c.getInt(4));
            }
        }
        return new Stats(0, 0, 0, 0, 0);
    }

    private List<Post> query(@Nullable String selection, @Nullable String[] args, @Nullable String limit) {
        return query(selection, args, DEFAULT_ORDER, limit);
    }

    private List<Post> query(@Nullable String selection, @Nullable String[] args,
                             @NonNull String orderBy, @Nullable String limit) {
        List<Post> result = new ArrayList<>();
        try (Cursor c = dbHelper.getReadableDatabase().query(TABLE_POSTS, ALL_COLUMNS,
                selection, args, null, null, orderBy, limit)) {
            while (c.moveToNext()) {
                result.add(fromCursor(c));
            }
        }
        return result;
    }

    private static Post fromCursor(@NonNull Cursor c) {
        Post p = new Post();
        p.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        p.setTitle(c.getString(c.getColumnIndexOrThrow(COL_TITLE)));
        p.setContent(c.getString(c.getColumnIndexOrThrow(COL_CONTENT)));
        p.setImageFileName(c.getString(c.getColumnIndexOrThrow(COL_IMAGE_FILE)));
        p.setCreatedAt(c.getLong(c.getColumnIndexOrThrow(COL_CREATED_AT)));
        p.setUpdatedAt(c.getLong(c.getColumnIndexOrThrow(COL_UPDATED_AT)));
        p.setRemotePostId(c.getString(c.getColumnIndexOrThrow(COL_REMOTE_POST_ID)));
        p.setRemoteUrl(c.getString(c.getColumnIndexOrThrow(COL_REMOTE_URL)));
        p.setPublishedAt(c.getLong(c.getColumnIndexOrThrow(COL_PUBLISHED_AT)));
        p.setDraft(c.getInt(c.getColumnIndexOrThrow(COL_IS_DRAFT)) == 1);
        return p;
    }

    // ---------------------------------------------------------------- delete

    /** Deletes one post and its photo file. */
    @WorkerThread
    public boolean delete(long id) {
        List<Long> ids = new ArrayList<>();
        ids.add(id);
        return deleteMany(ids) == 1;
    }

    /**
     * Deletes all the given posts in a single transaction (all or nothing), then removes their
     * photo files. Files are only removed after the transaction commits, so a failed delete never
     * leaves a post pointing at a missing photo.
     *
     * @return number of posts deleted
     */
    @WorkerThread
    public int deleteMany(@NonNull Collection<Long> ids) {
        if (ids.isEmpty()) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        List<String> imagesToRemove = new ArrayList<>();
        int deleted = 0;
        db.beginTransaction();
        try {
            for (Long id : ids) {
                String[] arg = idArg(id);
                try (Cursor c = db.query(TABLE_POSTS, new String[]{COL_IMAGE_FILE},
                        COL_ID + " = ?", arg, null, null, null)) {
                    if (c.moveToFirst() && !c.isNull(0)) {
                        imagesToRemove.add(c.getString(0));
                    }
                }
                deleted += db.delete(TABLE_POSTS, COL_ID + " = ?", arg);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        for (String name : imagesToRemove) {
            ImageStorage.deleteImage(appContext, name);
        }
        return deleted;
    }

    // ---------------------------------------------------------------- helpers

    private static String[] idArg(long id) {
        return new String[]{String.valueOf(id)};
    }

    private static String[] toArgs(Collection<Long> ids) {
        String[] args = new String[ids.size()];
        int i = 0;
        for (Long id : ids) {
            args[i++] = String.valueOf(id);
        }
        return args;
    }

    private static String placeholders(int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(i == 0 ? "?" : ",?");
        }
        return sb.toString();
    }
}
