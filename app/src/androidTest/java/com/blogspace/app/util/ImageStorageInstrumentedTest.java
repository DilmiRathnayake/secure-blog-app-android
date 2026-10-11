package com.blogspace.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** TC-13/14 support: importing JPEG, PNG and WebP images into app storage. */
@RunWith(AndroidJUnit4.class)
public class ImageStorageInstrumentedTest {

    private final Context context = ApplicationProvider.getApplicationContext();

    private File writeSample(String name, Bitmap.CompressFormat format, int w, int h) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bmp.eraseColor(Color.MAGENTA);
        File f = new File(context.getCacheDir(), name);
        try (FileOutputStream os = new FileOutputStream(f)) {
            bmp.compress(format, 90, os);
        }
        return f;
    }

    private void assertImports(Bitmap.CompressFormat format, String ext) throws IOException {
        File src = writeSample("sample." + ext, format, 640, 480);
        String name = ImageStorage.importImage(context, Uri.fromFile(src));
        File stored = ImageStorage.getImageFile(context, name);
        assertNotNull(stored);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(stored.getAbsolutePath(), o);
        assertEquals(640, o.outWidth);
        assertEquals("image/jpeg", o.outMimeType);
        ImageStorage.deleteImage(context, name);
        assertNull(ImageStorage.getImageFile(context, name));
    }

    @Test
    public void importsJpeg() throws IOException {
        assertImports(Bitmap.CompressFormat.JPEG, "jpg");
    }

    @Test
    public void importsPng() throws IOException {
        assertImports(Bitmap.CompressFormat.PNG, "png");
    }

    @SuppressWarnings("deprecation")
    @Test
    public void importsWebp() throws IOException {
        assertImports(Bitmap.CompressFormat.WEBP, "webp");
    }

    @Test
    public void largeImageIsScaledDown() throws IOException {
        File src = writeSample("big.jpg", Bitmap.CompressFormat.JPEG, 4000, 3000);
        String name = ImageStorage.importImage(context, Uri.fromFile(src));
        File stored = ImageStorage.getImageFile(context, name);
        assertNotNull(stored);
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(stored.getAbsolutePath(), o);
        assertTrue(Math.max(o.outWidth, o.outHeight) <= 2048);
        ImageStorage.deleteImage(context, name);
    }

    @Test
    public void nonImageIsRejected() throws IOException {
        File f = new File(context.getCacheDir(), "not_an_image.jpg");
        try (FileOutputStream os = new FileOutputStream(f)) {
            os.write("hello".getBytes());
        }
        try {
            ImageStorage.importImage(context, Uri.fromFile(f));
            fail("Expected IOException");
        } catch (IOException expected) {
            // handled gracefully by the editor with an error message
        }
    }

    @Test
    public void missingOrUnsafeNamesReturnNull() {
        assertNull(ImageStorage.getImageFile(context, null));
        assertNull(ImageStorage.getImageFile(context, "does_not_exist.jpg"));
        assertNull(ImageStorage.getImageFile(context, "../databases/blogspace.db"));
    }

    @Test
    public void shareUriIsContentUriFromFileProvider() throws IOException {
        File src = writeSample("share.png", Bitmap.CompressFormat.PNG, 10, 10);
        String name = ImageStorage.importImage(context, Uri.fromFile(src));
        File stored = ImageStorage.getImageFile(context, name);
        assertNotNull(stored);
        Uri uri = ImageStorage.getShareUri(context, stored);
        assertEquals("content", uri.getScheme());
        assertEquals(context.getPackageName() + ".fileprovider", uri.getAuthority());
        ImageStorage.deleteImage(context, name);
    }
}
