package com.blogspace.app.publish;

import androidx.annotation.NonNull;

import com.blogspace.app.data.Post;

/**
 * Test-mode publisher: sends nothing over the network. Results are flagged as simulated, so the UI
 * never labels the post "Published" and nothing is written to the database. It can also simulate a
 * failure, which is used to test the app's error handling.
 */
public class MockPublishService implements PublishService {

    private static final long FAKE_DELAY_MS = 700;

    private final boolean simulateFailure;

    public MockPublishService(boolean simulateFailure) {
        this.simulateFailure = simulateFailure;
    }

    @NonNull
    @Override
    public PublishResult publish(@NonNull Post post) throws PublishException {
        try {
            Thread.sleep(FAKE_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (simulateFailure) {
            throw new PublishException(PublishException.Kind.SIMULATED, null);
        }
        return new PublishResult("test-" + post.getId(), null, true, false);
    }
}
