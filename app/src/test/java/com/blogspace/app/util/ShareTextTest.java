package com.blogspace.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** TC-17 support: text placed in EXTRA_TEXT when sharing. */
public class ShareTextTest {

    @Test
    public void build_includesTitleBodyAndFooter() {
        assertEquals("My trip\n\nIt was great.\n\n— Shared from BlogSpace",
                ShareText.build("  My trip ", "It was great.\n", "Shared from BlogSpace"));
    }

    @Test
    public void build_withoutFooter() {
        assertEquals("T\n\nB", ShareText.build("T", "B", ""));
    }
}
