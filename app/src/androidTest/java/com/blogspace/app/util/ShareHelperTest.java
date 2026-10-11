package com.blogspace.app.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.blogspace.app.data.Post;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** TC-17 / TC-18: contents of the ACTION_SEND intent for text-only and photo posts. */
@RunWith(AndroidJUnit4.class)
public class ShareHelperTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    @Test
    public void textOnlyPost() {
        Post post = new Post("Title", "Body text", null);
        Intent i = ShareHelper.buildSendIntent(context, post);
        assertEquals(Intent.ACTION_SEND, i.getAction());
        assertEquals("text/plain", i.getType());
        assertEquals("Title", i.getStringExtra(Intent.EXTRA_SUBJECT));
        assertTrue(i.getStringExtra(Intent.EXTRA_TEXT).startsWith("Title\n\nBody text"));
        assertNull(i.getParcelableExtra(Intent.EXTRA_STREAM));
    }

    @Test
    public void photoPostAttachesStreamWithReadGrant() throws IOException {
        Bitmap bmp = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);
        File src = new File(context.getCacheDir(), "share_src.png");
        try (FileOutputStream os = new FileOutputStream(src)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        }
        String name = ImageStorage.importImage(context, Uri.fromFile(src));
        try {
            Post post = new Post("Photo", "With picture", name);
            Intent i = ShareHelper.buildSendIntent(context, post);
            assertEquals("image/jpeg", i.getType());
            Uri stream = i.getParcelableExtra(Intent.EXTRA_STREAM);
            assertNotNull(stream);
            assertEquals("content", stream.getScheme());
            assertNotNull(i.getClipData());
            assertEquals(stream, i.getClipData().getItemAt(0).getUri());
            assertTrue((i.getFlags() & Intent.FLAG_GRANT_READ_URI_PERMISSION) != 0);
            assertTrue(i.getStringExtra(Intent.EXTRA_TEXT).contains("With picture"));
        } finally {
            ImageStorage.deleteImage(context, name);
        }
    }
}
