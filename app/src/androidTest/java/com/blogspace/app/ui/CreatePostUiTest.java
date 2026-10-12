package com.blogspace.app.ui;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;
import com.blogspace.app.data.PostRepository;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** TC-01 / TC-02 through the real UI: validation, then saving a post. */
@RunWith(AndroidJUnit4.class)
public class CreatePostUiTest {

    @Test
    public void emptyFormShowsErrorsThenValidPostIsSaved() throws InterruptedException {
        Context context = ApplicationProvider.getApplicationContext();
        String title = "UI test post " + System.currentTimeMillis();
        try (ActivityScenario<EditPostActivity> ignored = ActivityScenario.launch(EditPostActivity.class)) {
            onView(withId(R.id.btn_save)).perform(click());
            onView(withId(R.id.title_layout))
                    .check(matches(hasDescendant(withText(R.string.error_title_required))));
            onView(withId(R.id.content_layout))
                    .check(matches(hasDescendant(withText(R.string.error_content_required))));

            onView(withId(R.id.title_input)).perform(typeText(title), closeSoftKeyboard());
            onView(withId(R.id.content_input)).perform(typeText("Written by Espresso"), closeSoftKeyboard());
            onView(withId(R.id.btn_save)).perform(click());
        }
        Thread.sleep(500); // save runs on a background thread
        PostRepository repo = PostRepository.getInstance(context);
        List<Post> found = repo.search(title);
        assertEquals(1, found.size());
        assertEquals("Written by Espresso", found.get(0).getContent());
        assertTrue(repo.delete(found.get(0).getId())); // clean up
    }

    @Test
    public void mainScreenTabsOpen() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_posts)).perform(click());
            onView(withId(R.id.filter_group)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_upload)).perform(click());
            onView(withId(R.id.btn_settings)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_gallery)).perform(click());
            onView(withId(R.id.card_camera)).check(matches(isDisplayed()));
            onView(withId(R.id.nav_profile)).perform(click());
            onView(withId(R.id.row_theme)).check(matches(isDisplayed()));
        }
    }
}
