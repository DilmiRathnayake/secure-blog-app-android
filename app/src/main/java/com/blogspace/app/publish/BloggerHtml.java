package com.blogspace.app.publish;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Converts a plain-text post into safe HTML for Blogger. Pure Java, so it is unit-tested.
 * <p>
 * All user text is HTML-escaped, so characters like {@code <} or {@code &} appear literally and
 * cannot inject markup. Blank lines become paragraphs, single line breaks become {@code <br />}.
 */
public final class BloggerHtml {

    private BloggerHtml() {
    }

    @NonNull
    public static String build(@NonNull String content, @Nullable String imageDataUri) {
        StringBuilder html = new StringBuilder();
        if (imageDataUri != null) {
            html.append("<p><img src=\"").append(escape(imageDataUri))
                    .append("\" alt=\"\" style=\"max-width:100%;height:auto;\" /></p>\n");
        }
        String normalised = content.replace("\r\n", "\n").trim();
        for (String paragraph : normalised.split("\n\\s*\n")) {
            String p = paragraph.trim();
            if (p.isEmpty()) {
                continue;
            }
            html.append("<p>").append(escape(p).replace("\n", "<br />")).append("</p>\n");
        }
        return html.toString().trim();
    }

    @NonNull
    public static String escape(@NonNull String text) {
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            switch (c) {
                case '&':
                    sb.append("&amp;");
                    break;
                case '<':
                    sb.append("&lt;");
                    break;
                case '>':
                    sb.append("&gt;");
                    break;
                case '"':
                    sb.append("&quot;");
                    break;
                case '\'':
                    sb.append("&#39;");
                    break;
                default:
                    sb.append(c);
            }
        }
        return sb.toString();
    }
}
