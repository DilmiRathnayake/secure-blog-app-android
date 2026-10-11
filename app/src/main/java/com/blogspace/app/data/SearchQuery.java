package com.blogspace.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Turns the user's search text into a safe SQL LIKE pattern.
 * <p>
 * The text is passed to SQLite as a bound argument (never concatenated into SQL), so it cannot
 * inject SQL. We also escape LIKE's own wildcards ({@code %} and {@code _}) so that searching for
 * "50%" finds the literal text "50%" instead of matching everything.
 */
public final class SearchQuery {

    /** Escape character used in the SQL: {@code ... LIKE ? ESCAPE '\'}. */
    public static final char ESCAPE_CHAR = '\\';

    private SearchQuery() {
    }

    /** Returns the trimmed query, or null if there is nothing to search for. */
    @Nullable
    public static String normalise(@Nullable String raw) {
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** Builds a "contains" LIKE pattern, e.g. {@code to_do} becomes {@code %to\_do%}. */
    @NonNull
    public static String toContainsPattern(@NonNull String query) {
        StringBuilder sb = new StringBuilder(query.length() + 2);
        sb.append('%');
        for (int i = 0; i < query.length(); i++) {
            char c = query.charAt(i);
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                sb.append(ESCAPE_CHAR);
            }
            sb.append(c);
        }
        sb.append('%');
        return sb.toString();
    }
}
