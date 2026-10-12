package com.blogspace.app.util;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Pure list logic for recent searches (unit-tested). */
public final class SearchHistory {

    private SearchHistory() {
    }

    /**
     * Returns a new list with {@code query} first, any earlier copy (ignoring case) removed, and
     * at most {@code max} entries.
     */
    @NonNull
    public static List<String> add(@NonNull List<String> current, @NonNull String query, int max) {
        String q = query.trim();
        List<String> result = new ArrayList<>();
        if (q.isEmpty()) {
            result.addAll(current);
            return result;
        }
        result.add(q);
        for (String s : current) {
            if (result.size() >= max) {
                break;
            }
            if (!s.toLowerCase(Locale.ROOT).equals(q.toLowerCase(Locale.ROOT))) {
                result.add(s);
            }
        }
        return result;
    }
}
