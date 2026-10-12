package com.blogspace.app.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * A single blog post (the "message" in the assessment brief).
 * <p>
 * This is a plain data object; it is read from and written to SQLite by {@link PostRepository}.
 */
public class Post {

    /** Id used for a post that has not been inserted into the database yet. */
    public static final long NO_ID = -1;

    private long id = NO_ID;
    private String title = "";
    private String content = "";
    /**
     * File name (not full path) of the attached photo inside the app's private images folder,
     * or null when the post has no photo. Storing just the name keeps records valid even if the
     * absolute app storage path changes (e.g. after a backup is restored).
     */
    @Nullable
    private String imageFileName;
    private long createdAt;
    private long updatedAt;
    /** Blogger post id, set only after Blogger confirms a successful publish. */
    @Nullable
    private String remotePostId;
    /** Public URL of the post on Blogger, when known. */
    @Nullable
    private String remoteUrl;
    /** Time of the last confirmed publish, or 0 if never published. */
    private long publishedAt;
    /** A draft is saved on the device but not finished; drafts are never published. */
    private boolean draft;

    public Post() {
    }

    public Post(@NonNull String title, @NonNull String content, @Nullable String imageFileName) {
        this.title = title;
        this.content = content;
        this.imageFileName = imageFileName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @NonNull
    public String getTitle() {
        return title;
    }

    public void setTitle(@NonNull String title) {
        this.title = title;
    }

    @NonNull
    public String getContent() {
        return content;
    }

    public void setContent(@NonNull String content) {
        this.content = content;
    }

    @Nullable
    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(@Nullable String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public boolean hasImage() {
        return imageFileName != null && !imageFileName.isEmpty();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Nullable
    public String getRemotePostId() {
        return remotePostId;
    }

    public void setRemotePostId(@Nullable String remotePostId) {
        this.remotePostId = remotePostId;
    }

    @Nullable
    public String getRemoteUrl() {
        return remoteUrl;
    }

    public void setRemoteUrl(@Nullable String remoteUrl) {
        this.remoteUrl = remoteUrl;
    }

    public long getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(long publishedAt) {
        this.publishedAt = publishedAt;
    }

    public boolean isPublished() {
        return publishedAt > 0;
    }

    public boolean isDraft() {
        return draft;
    }

    public void setDraft(boolean draft) {
        this.draft = draft;
    }

    /** Status badge shown in lists: Draft, Uploaded or Offline (saved only on the device). */
    @NonNull
    public Status getStatus() {
        if (draft) {
            return Status.DRAFT;
        }
        return isPublished() ? Status.UPLOADED : Status.OFFLINE;
    }

    public enum Status { DRAFT, OFFLINE, UPLOADED }

    /** Used by the RecyclerView DiffUtil to decide whether a row must be redrawn. */
    public boolean hasSameContentAs(@NonNull Post other) {
        return id == other.id
                && title.equals(other.title)
                && content.equals(other.content)
                && Objects.equals(imageFileName, other.imageFileName)
                && updatedAt == other.updatedAt
                && publishedAt == other.publishedAt
                && draft == other.draft;
    }
}
