package com.blogspace.app.publish;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

import com.blogspace.app.data.Post;

/**
 * Something that can publish one post. Two implementations:
 * {@link BloggerPublishService} (real) and {@link MockPublishService} (Test mode).
 */
public interface PublishService {

    /** Publishes the post. Blocks, so call on a background thread. */
    @WorkerThread
    @NonNull
    PublishResult publish(@NonNull Post post) throws PublishException;
}
