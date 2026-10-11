package com.blogspace.app.publish;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** TC-19 support: HTML sent to Blogger is escaped and keeps paragraphs. */
public class BloggerHtmlTest {

    @Test
    public void escapesHtmlSpecialCharacters() {
        assertEquals("<p>a &lt;b&gt; &amp; &quot;c&quot; &#39;d&#39;</p>",
                BloggerHtml.build("a <b> & \"c\" 'd'", null));
    }

    @Test
    public void paragraphsAndLineBreaks() {
        assertEquals("<p>Line 1<br />Line 2</p>\n<p>Para 2</p>",
                BloggerHtml.build("Line 1\nLine 2\n\nPara 2", null));
    }

    @Test
    public void imageIsPlacedFirst() {
        String html = BloggerHtml.build("Text", "data:image/jpeg;base64,AAAA");
        assertTrue(html.startsWith("<p><img src=\"data:image/jpeg;base64,AAAA\""));
        assertTrue(html.endsWith("<p>Text</p>"));
    }
}
