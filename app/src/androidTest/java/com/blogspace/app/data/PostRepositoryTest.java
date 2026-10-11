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
