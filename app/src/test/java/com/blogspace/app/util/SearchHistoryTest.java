package com.blogspace.app.util;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;

/** Recent searches: newest first, no case-insensitive duplicates, capped length. */
public class SearchHistoryTest {

    @Test
    public void addsToFrontAndDeduplicatesIgnoringCase() {
        List<String> result = SearchHistory.add(Arrays.asList("kandy", "Beach"), "beach", 8);
        assertEquals(Arrays.asList("beach", "kandy"), result);
    }

    @Test
    public void capsLength() {
        List<String> result = SearchHistory.add(Arrays.asList("a", "b", "c"), "d", 3);
        assertEquals(Arrays.asList("d", "a", "b"), result);
    }

    @Test
    public void ignoresBlank() {
        assertEquals(Collections.singletonList("a"), SearchHistory.add(Collections.singletonList("a"), "   ", 8));
    }
}
