package com.blogspace.app.publish;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal client for the official Blogger API v3 (https://developers.google.com/blogger/docs/3.0).
 * Uses only HttpURLConnection and org.json from the Android SDK, so no extra networking library.
 * <p>
 * Every request is HTTPS and authenticated with a short-lived OAuth 2.0 access token in the
 * {@code Authorization: Bearer} header. The token is never logged or stored.
 */
public class BloggerApi {

    /** OAuth scope that allows managing the user's Blogger posts. */
    public static final String SCOPE = "https://www.googleapis.com/auth/blogger";

    private static final String BASE_URL = "https://www.googleapis.com/blogger/v3";
    private static final int CONNECT_TIMEOUT_MS = 15_000;
    private static final int READ_TIMEOUT_MS = 30_000;

    /** A blog the signed-in user can post to. */
    public static class BlogInfo {
        public final String id;
        public final String name;
        @Nullable
        public final String url;

        public BlogInfo(@NonNull String id, @NonNull String name, @Nullable String url) {
            this.id = id;
            this.name = name;
            this.url = url;
        }
    }

    /** Lists the blogs owned by / authored by the signed-in user. */
    @WorkerThread
    @NonNull
    public List<BlogInfo> listMyBlogs(@NonNull String accessToken) throws PublishException {
        String body = request("GET", BASE_URL + "/users/self/blogs?fields=items(id,name,url)", accessToken, null);
        try {
            return parseBlogList(body);
        } catch (JSONException e) {
            throw new PublishException(PublishException.Kind.UNKNOWN, "Unexpected response from Blogger", 0, e);
        }
    }

    /**
     * Creates and immediately publishes a post (Blogger publishes by default unless isDraft=true).
     *
     * @return result containing the Blogger post id and public URL
     */
    @WorkerThread
    @NonNull
    public PublishResult createPost(@NonNull String accessToken, @NonNull String blogId,
                                    @NonNull String title, @NonNull String html, boolean imageOmitted)
            throws PublishException {
        String body;
        try {
            JSONObject json = new JSONObject();
            json.put("kind", "blogger#post");
            json.put("title", title);
            json.put("content", html);
            body = json.toString();
        } catch (JSONException e) {
            throw new PublishException(PublishException.Kind.UNKNOWN, e.getMessage(), 0, e);
        }
        String url = BASE_URL + "/blogs/" + encode(blogId) + "/posts/?fields=id,url";
        String response = request("POST", url, accessToken, body);
        try {
            JSONObject json = new JSONObject(response);
            String id = json.optString("id", null);
            if (id == null) {
                throw new PublishException(PublishException.Kind.UNKNOWN, "Blogger did not return a post id");
            }
            return new PublishResult(id, json.optString("url", null), false, imageOmitted);
        } catch (JSONException e) {
            throw new PublishException(PublishException.Kind.UNKNOWN, "Unexpected response from Blogger", 0, e);
        }
    }

    // ---------------------------------------------------------------- HTTP

    @NonNull
    private String request(@NonNull String method, @NonNull String url, @NonNull String token,
                           @Nullable String jsonBody) throws PublishException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod(method);
            conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
            conn.setReadTimeout(READ_TIMEOUT_MS);
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Accept", "application/json");
            if (jsonBody != null) {
                byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                conn.setDoOutput(true);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setFixedLengthStreamingMode(bytes.length);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(bytes);
                }
            }
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                return readFully(conn.getInputStream());
            }
            String errorBody = conn.getErrorStream() == null ? "" : readFully(conn.getErrorStream());
            ApiError error = parseError(errorBody);
            throw new PublishException(classify(code, error.reason), error.message, code, null);
        } catch (IOException e) {
            throw new PublishException(PublishException.Kind.NETWORK, e.getMessage(), 0, e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static String readFully(InputStream in) throws IOException {
        try (InputStream input = in; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int n;
            while ((n = input.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static String encode(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- parsing (unit-tested)

    /** Message and machine-readable reason from a Google API error body. */
    static class ApiError {
        final String message;
        @Nullable
        final String reason;

        ApiError(String message, @Nullable String reason) {
            this.message = message;
            this.reason = reason;
        }
    }

    @NonNull
    static List<BlogInfo> parseBlogList(@NonNull String body) throws JSONException {
        List<BlogInfo> blogs = new ArrayList<>();
        JSONArray items = new JSONObject(body).optJSONArray("items");
        if (items == null) {
            return blogs; // user has no blogs
        }
        for (int i = 0; i < items.length(); i++) {
            JSONObject b = items.getJSONObject(i);
            blogs.add(new BlogInfo(b.getString("id"), b.optString("name", b.getString("id")),
                    b.optString("url", null)));
        }
        return blogs;
    }

    /** Google error format: {"error":{"code":403,"message":"...","errors":[{"reason":"..."}]}}. */
    @NonNull
    static ApiError parseError(@Nullable String body) {
        if (body == null || body.isEmpty()) {
            return new ApiError("No details", null);
        }
        try {
            JSONObject error = new JSONObject(body).getJSONObject("error");
            String message = error.optString("message", "No details");
            String reason = null;
            JSONArray errors = error.optJSONArray("errors");
            if (errors != null && errors.length() > 0) {
                reason = errors.getJSONObject(0).optString("reason", null);
            }
            if (reason == null) {
                reason = error.optString("status", null);
            }
            return new ApiError(message, reason);
        } catch (JSONException e) {
            return new ApiError(body.length() > 200 ? body.substring(0, 200) : body, null);
        }
    }

    /** Maps an HTTP status (+ Google error reason) to a failure category. */
    @NonNull
    static PublishException.Kind classify(int httpCode, @Nullable String reason) {
        if (httpCode == 401) {
            return PublishException.Kind.AUTH;
        }
        if (httpCode == 429 || (reason != null && (reason.contains("RateLimit")
                || reason.contains("rateLimit") || reason.contains("quota") || reason.equals("RESOURCE_EXHAUSTED")))) {
            return PublishException.Kind.RATE_LIMIT;
        }
        if (httpCode == 403) {
            return PublishException.Kind.FORBIDDEN;
        }
        if (httpCode == 404) {
            return PublishException.Kind.NOT_FOUND;
        }
        if (httpCode == 400 || httpCode == 413 || httpCode == 422) {
            return PublishException.Kind.REJECTED;
        }
        if (httpCode >= 500) {
            return PublishException.Kind.SERVER;
        }
        return PublishException.Kind.UNKNOWN;
    }
}
