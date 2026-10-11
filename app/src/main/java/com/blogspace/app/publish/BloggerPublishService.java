package com.blogspace.app.publish;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blogspace.app.data.Post;
import com.blogspace.app.util.ImageStorage;

import java.io.File;

/**
 * Publishes a post to the user's chosen Blogger blog.
 * <ul>
 *   <li>If the access token has expired (HTTP 401) it is invalidated, refreshed and retried once.</li>
 *   <li>The Blogger API has no media-upload endpoint, so the photo is embedded as a reduced
 *       JPEG data URI. If Blogger rejects the request because of it (HTTP 400/413), the post is
 *       retried without the photo and the result says so.</li>
 * </ul>
 */
public class BloggerPublishService implements PublishService {

    /** Longest side of an embedded photo; keeps the request small. */
    private static final int EMBED_MAX_DIMENSION = 1000;
    private static final int EMBED_JPEG_QUALITY = 75;

    private final Context appContext;
    private final BloggerApi api;
    private final TokenProvider tokens;
    private final String blogId;
    private final boolean includeImages;

    public BloggerPublishService(@NonNull Context context, @NonNull BloggerApi api,
                                 @NonNull TokenProvider tokens, @NonNull String blogId, boolean includeImages) {
        this.appContext = context.getApplicationContext();
        this.api = api;
        this.tokens = tokens;
        this.blogId = blogId;
        this.includeImages = includeImages;
    }

    @NonNull
    @Override
    public PublishResult publish(@NonNull Post post) throws PublishException {
        String imageDataUri = includeImages ? embeddedImage(post) : null;
        try {
            return createWithTokenRetry(post, BloggerHtml.build(post.getContent(), imageDataUri), false);
        } catch (PublishException e) {
            if (imageDataUri != null && e.getKind() == PublishException.Kind.REJECTED) {
                return createWithTokenRetry(post, BloggerHtml.build(post.getContent(), null), true);
            }
            throw e;
        }
    }

    private PublishResult createWithTokenRetry(Post post, String html, boolean imageOmitted) throws PublishException {
        String token = tokens.getToken();
        try {
            return api.createPost(token, blogId, post.getTitle(), html, imageOmitted);
        } catch (PublishException e) {
            if (e.getKind() != PublishException.Kind.AUTH) {
                throw e;
            }
            tokens.invalidate(token);
            return api.createPost(tokens.getToken(), blogId, post.getTitle(), html, imageOmitted);
        }
    }

    @Nullable
    private String embeddedImage(@NonNull Post post) {
        if (!post.hasImage()) {
            return null;
        }
        File file = ImageStorage.getImageFile(appContext, post.getImageFileName());
        return file == null ? null : ImageStorage.toJpegDataUri(file, EMBED_MAX_DIMENSION, EMBED_JPEG_QUALITY);
    }
}
