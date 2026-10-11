package com.blogspace.app.publish;

import org.json.JSONException;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** TC-19/TC-21 support: parsing Blogger responses and classifying HTTP errors. */
public class BloggerApiParsingTest {

    @Test
    public void parseBlogList_readsItems() throws JSONException {
        List<BloggerApi.BlogInfo> blogs = BloggerApi.parseBlogList(
                "{\"items\":[{\"id\":\"123\",\"name\":\"My Blog\",\"url\":\"http://my.blogspot.com/\"}]}");
        assertEquals(1, blogs.size());
        assertEquals("123", blogs.get(0).id);
        assertEquals("My Blog", blogs.get(0).name);
        assertEquals("http://my.blogspot.com/", blogs.get(0).url);
    }

    @Test
    public void parseBlogList_noBlogs() throws JSONException {
        assertTrue(BloggerApi.parseBlogList("{}").isEmpty());
    }

    @Test
    public void parseError_readsMessageAndReason() {
        BloggerApi.ApiError e = BloggerApi.parseError(
                "{\"error\":{\"code\":403,\"message\":\"Blogger API has not been used\","
                        + "\"errors\":[{\"reason\":\"accessNotConfigured\"}]}}");
        assertEquals("Blogger API has not been used", e.message);
        assertEquals("accessNotConfigured", e.reason);
    }

    @Test
    public void parseError_nonJsonBody() {
        BloggerApi.ApiError e = BloggerApi.parseError("Bad Gateway");
        assertEquals("Bad Gateway", e.message);
        assertNull(e.reason);
    }

    @Test
    public void classify_mapsStatusCodes() {
        assertEquals(PublishException.Kind.AUTH, BloggerApi.classify(401, null));
        assertEquals(PublishException.Kind.FORBIDDEN, BloggerApi.classify(403, "accessNotConfigured"));
        assertEquals(PublishException.Kind.RATE_LIMIT, BloggerApi.classify(403, "userRateLimitExceeded"));
        assertEquals(PublishException.Kind.RATE_LIMIT, BloggerApi.classify(429, null));
        assertEquals(PublishException.Kind.NOT_FOUND, BloggerApi.classify(404, null));
        assertEquals(PublishException.Kind.REJECTED, BloggerApi.classify(400, "invalid"));
        assertEquals(PublishException.Kind.SERVER, BloggerApi.classify(503, null));
    }
}
