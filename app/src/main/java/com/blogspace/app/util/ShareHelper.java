package com.blogspace.app.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.blogspace.app.R;
import com.blogspace.app.data.Post;

import java.io.File;

/**
 * Shares a single post (text + photo) using Android's standard ACTION_SEND intent.
 * <p>
 * How the photo is shared securely: the image lives in app-private storage, so we hand out a
 * {@code content://} URI from FileProvider and add {@link Intent#FLAG_GRANT_READ_URI_PERMISSION}.
 * The receiving app (e.g. Gmail) gets temporary read access to that one file only. The URI is put
 * in both {@link Intent#EXTRA_STREAM} and {@link ClipData}; ClipData is what actually carries the
 * permission grant through the chooser on modern Android versions.
 */
public final class ShareHelper {

    private ShareHelper() {
    }

    /** Opens the system share sheet ("Share with any app"). */
    public static void shareWithChooser(@NonNull Activity activity, @NonNull Post post) {
        Intent send = buildSendIntent(activity, post);
        Intent chooser = Intent.createChooser(send,
                activity.getString(R.string.share_chooser_title, post.getTitle()));
        try {
            activity.startActivity(chooser);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.error_no_share_app, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Sends the post by email. The selector limits the choice to apps that handle "mailto:",
     * while the main intent is still ACTION_SEND so the photo can be attached.
     * Falls back to the normal share sheet when no email app is installed.
     */
    public static void shareByEmail(@NonNull Activity activity, @NonNull Post post) {
        Intent send = buildSendIntent(activity, post);
        send.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
        try {
            activity.startActivity(send);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(activity, R.string.error_no_email_app, Toast.LENGTH_LONG).show();
            shareWithChooser(activity, post);
        }
    }

    /** Builds the ACTION_SEND intent with subject, text and (if available) the photo. */
    @NonNull
    static Intent buildSendIntent(@NonNull Context context, @NonNull Post post) {
        String text = ShareText.build(post.getTitle(), post.getContent(),
                context.getString(R.string.share_footer, context.getString(R.string.app_name)));

        Intent send = new Intent(Intent.ACTION_SEND);
        send.putExtra(Intent.EXTRA_SUBJECT, post.getTitle());
        send.putExtra(Intent.EXTRA_TITLE, post.getTitle());
        send.putExtra(Intent.EXTRA_TEXT, text);

        Uri imageUri = imageUriFor(context, post);
        if (imageUri != null) {
            send.setType("image/jpeg");
            send.putExtra(Intent.EXTRA_STREAM, imageUri);
            send.setClipData(ClipData.newRawUri(post.getTitle(), imageUri));
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } else {
            send.setType("text/plain");
        }
        return send;
    }

    @Nullable
    private static Uri imageUriFor(@NonNull Context context, @NonNull Post post) {
        if (!post.hasImage()) {
            return null;
        }
        File file = ImageStorage.getImageFile(context, post.getImageFileName());
        if (file == null) {
            // The photo was removed from storage; share the text rather than failing.
            Toast.makeText(context, R.string.share_image_missing, Toast.LENGTH_LONG).show();
            return null;
        }
        return ImageStorage.getShareUri(context, file);
    }
}
