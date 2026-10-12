package com.blogspace.app.data;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Exercises the real SQLite database on a device/emulator, using a separate test database file
 * so the user's posts are never touched. Covers TC-01, 03, 04, 05, 06, 07, 08, 09, 10, 11.
 */
@RunWith(AndroidJUnit4.class)
public class PostRepositoryTest {

    private static final String TEST_DB = "blogspace_test.db";
    private Context context;
    private PostRepository repo;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        context.deleteDatabase(TEST_DB);
        repo = PostRepository.createForTesting(context, TEST_DB);
    }

    @After
    public void tearDown() {
        repo.close();
        context.deleteDatabase(TEST_DB);
    }

    private Post insert(String title, String content) {
        Post p = new Post(title, content, null);
        assertNotEquals(-1, repo.insert(p));
        return p;
    }

    @Test
    public void insertAndReadBack() {
        Post p = insert("First post", "Hello SQLite");
        Post loaded = repo.getById(p.getId());
        assertNotNull(loaded);
        assertEquals("First post", loaded.getTitle());
        assertEquals("Hello SQLite", loaded.getContent());
        assertTrue(loaded.getCreatedAt() > 0);
        assertFalse(loaded.hasImage());
        assertFalse(loaded.isPublished());
    }

    @Test
    public void updateKeepsIdAndDoesNotDuplicate() throws InterruptedException {
        Post p = insert("Draft", "v1");
        long created = p.getCreatedAt();
        Thread.sleep(5);
        p.setTitle("Final");
        p.setContent("v2");
        assertTrue(repo.update(p));

        List<Post> all = repo.getAll();
        assertEquals(1, all.size());
        assertEquals(p.getId(), all.get(0).getId());
        assertEquals("Final", all.get(0).getTitle());
        assertEquals("v2", all.get(0).getContent());
        assertEquals(created, all.get(0).getCreatedAt());
        assertTrue(all.get(0).getUpdatedAt() > created);
    }

    @Test
    public void listIsNewestFirst() throws InterruptedException {
        Post a = insert("A", "a");
        Thread.sleep(5);
        Post b = insert("B", "b");
        List<Post> all = repo.getAll();
        assertEquals(b.getId(), all.get(0).getId());
        assertEquals(a.getId(), all.get(1).getId());
    }

    @Test
    public void deleteSingle() {
        Post a = insert("A", "a");
        Post b = insert("B", "b");
        assertTrue(repo.delete(a.getId()));
        assertNull(repo.getById(a.getId()));
        assertNotNull(repo.getById(b.getId()));
        assertFalse(repo.delete(a.getId())); // already gone
    }

    @Test
    public void deleteManyRemovesOnlySelected() {
        Post a = insert("A", "a");
        Post b = insert("B", "b");
        Post c = insert("C", "c");
        assertEquals(2, repo.deleteMany(Arrays.asList(a.getId(), c.getId())));
        List<Post> all = repo.getAll();
        assertEquals(1, all.size());
        assertEquals(b.getId(), all.get(0).getId());
    }

    @Test
    public void searchMatchesTitleAndBodyPartialCaseInsensitive() {
        Post title = insert("Holiday in Kandy", "Temple visit");
        Post body = insert("Weekend", "We went on a HOLIDAY to the beach");
        insert("Unrelated", "Nothing here");

        List<Post> results = repo.search("holi");
        assertEquals(2, results.size());
        // Title matches are ranked first.
        assertEquals(title.getId(), results.get(0).getId());
        assertEquals(body.getId(), results.get(1).getId());

        assertEquals(1, repo.search("BEACH").size());
        assertEquals(0, repo.search("zebra").size());
        assertEquals(0, repo.search("   ").size());
    }

    @Test
    public void searchTreatsWildcardsAndQuotesLiterally() {
        insert("Sale", "Everything 50% off");
        insert("Other", "Just 50 items");
        assertEquals(1, repo.search("50%").size());
        assertEquals(0, repo.search("' OR '1'='1").size());
        assertEquals(2, repo.getAll().size()); // table intact
    }

    @Test
    public void markPublishedAndStats() {
        Post a = insert("A", "a");
        Post b = new Post("B", "b", "img_test.jpg");
        repo.insert(b);
        assertTrue(repo.markPublished(a.getId(), "999", "https://example.blogspot.com/a.html"));

        Post loaded = repo.getById(a.getId());
        assertNotNull(loaded);
        assertTrue(loaded.isPublished());
        assertEquals("999", loaded.getRemotePostId());

        PostRepository.Stats stats = repo.getStats();
        assertEquals(2, stats.total);
        assertEquals(1, stats.withImages);
        assertEquals(1, stats.published);
    }

    @Test
    public void draftsAndStatusFilters() throws InterruptedException {
        Post offline = insert("Offline post", "saved");
        Thread.sleep(5);
        Post uploaded = insert("Uploaded post", "online");
        repo.markPublished(uploaded.getId(), "1", "https://example.blogspot.com/u.html");
        Thread.sleep(5);
        Post draft = new Post("", "half an idea", null);
        draft.setDraft(true);
        repo.insert(draft);

        assertEquals(1, repo.getAll(PostFilter.DRAFT).size());
        assertTrue(repo.getAll(PostFilter.DRAFT).get(0).isDraft());
        assertEquals(Post.Status.DRAFT, repo.getById(draft.getId()).getStatus());
        assertEquals(offline.getId(), repo.getAll(PostFilter.OFFLINE).get(0).getId());
        assertEquals(uploaded.getId(), repo.getAll(PostFilter.UPLOADED).get(0).getId());
        assertEquals(draft.getId(), repo.getAll(PostFilter.NEWEST).get(0).getId());
        assertEquals(offline.getId(), repo.getAll(PostFilter.OLDEST).get(0).getId());
        // Upload queue = finished, not-yet-uploaded posts only (drafts excluded).
        assertEquals(1, repo.getUploadQueue().size());
        assertEquals(offline.getId(), repo.getUploadQueue().get(0).getId());

        PostRepository.Stats stats = repo.getStats();
        assertEquals(3, stats.total);
        assertEquals(1, stats.offline);
        assertEquals(1, stats.published);
        assertEquals(1, stats.drafts);

        // Finishing a draft turns it into a normal offline post with the same id.
        Post loaded = repo.getById(draft.getId());
        loaded.setTitle("Finished");
        loaded.setDraft(false);
        assertTrue(repo.update(loaded));
        assertEquals(0, repo.getAll(PostFilter.DRAFT).size());
        assertEquals(2, repo.getUploadQueue().size());
    }

    @Test
    public void upgradeFromVersion1KeepsExistingPosts() {
        repo.close();
        context.deleteDatabase(TEST_DB);
        // Build a database exactly as version 1 of the app created it.
        android.database.sqlite.SQLiteDatabase v1 = context.openOrCreateDatabase(TEST_DB, Context.MODE_PRIVATE, null);
        v1.execSQL("CREATE TABLE posts (_id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, "
                + "content TEXT NOT NULL, image_file TEXT, created_at INTEGER NOT NULL, updated_at INTEGER NOT NULL, "
                + "remote_post_id TEXT, remote_url TEXT, published_at INTEGER NOT NULL DEFAULT 0)");
        v1.execSQL("INSERT INTO posts (title, content, created_at, updated_at) VALUES ('Old post', 'from v1', 1, 1)");
        v1.setVersion(1);
        v1.close();

        // Opening with the current helper runs onUpgrade(1 -> 2).
        repo = PostRepository.createForTesting(context, TEST_DB);
        List<Post> all = repo.getAll();
        assertEquals(1, all.size());
        assertEquals("Old post", all.get(0).getTitle());
        assertFalse(all.get(0).isDraft());
        assertEquals(1, repo.getUploadQueue().size());
    }

    @Test
    public void removeImageDetachesPhoto() {
        Post p = new Post("With photo", "text", "img_missing_file.jpg");
        repo.insert(p);
        assertTrue(repo.removeImage(p.getId()));
        assertFalse(repo.getById(p.getId()).hasImage());
        assertEquals(0, repo.getWithImages().size());
    }

    @Test
    public void dataSurvivesReopeningDatabase() {
        Post p = insert("Persistent", "Still here after restart");
        repo.close();
        // A brand-new repository/helper simulates the app being closed and reopened.
        repo = PostRepository.createForTesting(context, TEST_DB);
        Post loaded = repo.getById(p.getId());
        assertNotNull(loaded);
        assertEquals("Persistent", loaded.getTitle());
    }
}
