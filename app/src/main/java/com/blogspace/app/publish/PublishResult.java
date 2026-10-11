package com.blogspace.app.publish;

import androidx.annotation.Nullable;

/** What the platform returned for one successfully published post. */
public class PublishResult {

    @Nullable
    public final String remoteId;
    @Nullable
    public final String url;
    /** True only in Test mode: nothing was really uploaded. */
    public final boolean simulated;
    /** True if the photo had to be left out because the platform rejected it. */
    public final boolean imageOmitted;

    public PublishResult(@Nullable String remoteId, @Nullable String url, boolean simulated, boolean imageOmitted) {
        this.remoteId = remoteId;
        this.url = url;
        this.simulated = simulated;
        this.imageOmitted = imageOmitted;
    }
}
