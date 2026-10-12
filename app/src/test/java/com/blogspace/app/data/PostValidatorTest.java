package com.blogspace.app.data;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** TC-02: validation rules for title and body. */
public class PostValidatorTest {

    @Test
    public void title_requiredAndWhitespaceOnlyRejected() {
        assertEquals(PostValidator.Result.TITLE_EMPTY, PostValidator.validateTitle(null));
        assertEquals(PostValidator.Result.TITLE_EMPTY, PostValidator.validateTitle(""));
        assertEquals(PostValidator.Result.TITLE_EMPTY, PostValidator.validateTitle("   "));
    }

    @Test
    public void title_lengthLimit() {
        assertEquals(PostValidator.Result.OK, PostValidator.validateTitle(repeat('a', PostValidator.MAX_TITLE_LENGTH)));
        assertEquals(PostValidator.Result.TITLE_TOO_LONG,
                PostValidator.validateTitle(repeat('a', PostValidator.MAX_TITLE_LENGTH + 1)));
    }

    @Test
    public void content_requiredAndLengthLimit() {
        assertEquals(PostValidator.Result.CONTENT_EMPTY, PostValidator.validateContent(" \n "));
        assertEquals(PostValidator.Result.OK, PostValidator.validateContent("Hello"));
        assertEquals(PostValidator.Result.CONTENT_TOO_LONG,
                PostValidator.validateContent(repeat('b', PostValidator.MAX_CONTENT_LENGTH + 1)));
    }

    @Test
    public void draft_needsTitleOrContent() {
        org.junit.Assert.assertFalse(PostValidator.isValidDraft("  ", null));
        org.junit.Assert.assertTrue(PostValidator.isValidDraft("Idea", ""));
        org.junit.Assert.assertTrue(PostValidator.isValidDraft("", "Some text"));
    }

    private static String repeat(char c, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(c);
        }
        return sb.toString();
    }
}
