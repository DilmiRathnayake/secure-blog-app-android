package com.blogspace.app.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/** TC-10 / TC-08 support: search input handling and SQL LIKE escaping. */
public class SearchQueryTest {

    @Test
    public void normalise_blankInputIsNull() {
        assertNull(SearchQuery.normalise(null));
        assertNull(SearchQuery.normalise(""));
        assertNull(SearchQuery.normalise("   \n\t"));
    }

    @Test
    public void normalise_trimsWhitespace() {
        assertEquals("hello world", SearchQuery.normalise("  hello world  "));
    }

    @Test
    public void containsPattern_wrapsInWildcards() {
        assertEquals("%travel%", SearchQuery.toContainsPattern("travel"));
    }

    @Test
    public void containsPattern_escapesLikeWildcardsAndEscapeChar() {
        assertEquals("%50\\%%", SearchQuery.toContainsPattern("50%"));
        assertEquals("%to\\_do%", SearchQuery.toContainsPattern("to_do"));
        assertEquals("%a\\\\b%", SearchQuery.toContainsPattern("a\\b"));
    }

    @Test
    public void containsPattern_keepsQuotesLiteral() {
        // Quotes are harmless because the pattern is a bound argument, never concatenated SQL.
        assertEquals("%' OR 1=1 --%", SearchQuery.toContainsPattern("' OR 1=1 --"));
    }
}
