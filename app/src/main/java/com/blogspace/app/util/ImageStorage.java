package com.blogspace.app.util;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.core.content.FileProvider;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Stores post photos in the app's private storage ({@code filesDir/images}).
 * <p>
 * Design decision: gallery and camera images are <b>copied</b> into app storage rather than
 * keeping the original content:// URI. Picker URIs and temporary camera files can stop working
 * after a restart or if the user deletes the original, but our copy stays valid as long as the post
 * exists. Every image is decoded, rotated upright using its EXIF data, scaled down to at most
 * {@value #MAX_STORED_DIMENSION}px and re-saved as JPEG. That means any format Android can decode
 * (JPEG, PNG, WebP, GIF, BMP, and HEIF on Android 9+) ends up as a JPEG that every email app and
 * share target understands.
 */
public final class ImageStorage {

    private static final String TAG = "ImageStorage";
    private static final String IMAGES_DIR = "images";
    private static final String CAMERA_DIR = "camera";
    private static final int MAX_STORED_DIMENSION = 2048;
    private static final int JPEG_QUALITY = 88;

    private ImageStorage() {
    }

    /** Authority declared for androidx FileProvider in AndroidManifest.xml. */
    @NonNull
    public static String authority(@NonNull Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    @NonNull
    private static File imagesDir(@NonNull Context context) {
        File dir = new File(context.getFilesDir(), IMAGES_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            Log.w(TAG, "Could not create images directory");
        }
        return dir;
    }

    /** Returns the stored file for a post's image name, or null if it is missing on disk. */
    @Nullable
    public static File getImageFile(@NonNull Context context, @Nullable String fileName) {
        if (fileName == null || fileName.isEmpty() || fileName.contains("/")) {
            return null; // the "/" check stops a corrupted name from escaping the images folder
        }
        File file = new File(imagesDir(context), fileName);
        return file.isFile() ? file : null;
    }

    /** A content:// URI other apps can read (when granted), used for sharing. */
    @NonNull
    public static Uri getShareUri(@NonNull Context context, @NonNull File file) {
        return FileProvider.getUriForFile(context, authority(context), file);
    }

    /**
     * Creates an empty file in cache/camera for the camera app to write into.
     * It is temporary: {@link #importImage} copies it to permanent storage, then it is deleted.
     */
    @NonNull
    public static File createCameraTempFile(@NonNull Context context) throws IOException {
        File dir = new File(context.getCacheDir(), CAMERA_DIR);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new IOException("Cannot create camera cache directory");
        }
        return File.createTempFile("capture_", ".jpg", dir);
    }

    /**
     * Decodes the image at {@code source} (gallery content:// URI or camera file), fixes its
     * orientation, scales it down and saves it as a new JPEG in app storage.
     *
     * @return the new file name to store in the database
     * @throws IOException if the image cannot be read or decoded
     */
    @WorkerThread
    @NonNull
    public static String importImage(@NonNull Context context, @NonNull Uri source) throws IOException {
        ContentResolver resolver = context.getContentResolver();

        // 1. Read only the dimensions, to choose a memory-friendly sample size.
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream in = open(resolver, source)) {
            BitmapFactory.decodeStream(in, null, bounds);
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new IOException("Unsupported or corrupt image");
        }

        // 2. Decode a down-sampled bitmap (never the full-resolution photo).
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inSampleSize = calculateInSampleSize(bounds.outWidth, bounds.outHeight,
                MAX_STORED_DIMENSION, MAX_STORED_DIMENSION);
        Bitmap bitmap;
        try (InputStream in = open(resolver, source)) {
            bitmap = BitmapFactory.decodeStream(in, null, opts);
        }
        if (bitmap == null) {
            throw new IOException("Image could not be decoded");
        }

        // 3. Rotate upright (camera photos are often stored sideways with an EXIF flag).
        int rotation = 0;
        try (InputStream in = open(resolver, source)) {
            rotation = new ExifInterface(in).getRotationDegrees();
        } catch (IOException | RuntimeException e) {
            Log.i(TAG, "No EXIF orientation available: " + e.getMessage());
        }
        bitmap = scaleAndRotate(bitmap, MAX_STORED_DIMENSION, rotation);

        // 4. Save as JPEG with a unique name.
        String name = "img_" + UUID.randomUUID() + ".jpg";
        File out = new File(imagesDir(context), name);
        try (OutputStream os = new FileOutputStream(out)) {
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os)) {
                throw new IOException("Could not encode JPEG");
            }
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            throw e;
        } finally {
            bitmap.recycle();
        }
        return name;
    }

    private static InputStream open(ContentResolver resolver, Uri uri) throws IOException {
        InputStream in = resolver.openInputStream(uri);
        if (in == null) {
            throw new IOException("Cannot open " + uri);
        }
        return in;
    }

    /** Deletes a stored image. Safe to call with null or an already-deleted name. */
    public static void deleteImage(@NonNull Context context, @Nullable String fileName) {
        File file = getImageFile(context, fileName);
        if (file != null && !file.delete()) {
            Log.w(TAG, "Failed to delete " + fileName);
        }
    }

    /**
     * Loads a stored image no larger than {@code reqWidth} x {@code reqHeight} (approximately),
     * for previews and list thumbnails.
     */
    @WorkerThread
    @Nullable
    public static Bitmap decodeSampled(@NonNull File file, int reqWidth, int reqHeight) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
        if (opts.outWidth <= 0) {
            return null;
        }
        opts.inSampleSize = calculateInSampleSize(opts.outWidth, opts.outHeight, reqWidth, reqHeight);
        opts.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), opts);
    }

    /**
     * Returns a reduced JPEG as a base64 "data:" URI, used to embed the photo in a Blogger post
     * (the Blogger API has no image-upload endpoint).
     */
    @WorkerThread
    @Nullable
    public static String toJpegDataUri(@NonNull File file, int maxDimension, int quality) {
        Bitmap bitmap = decodeSampled(file, maxDimension, maxDimension);
        if (bitmap == null) {
            return null;
        }
        bitmap = scaleAndRotate(bitmap, maxDimension, 0);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, bos);
        bitmap.recycle();
        return "data:image/jpeg;base64," + Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
    }

    /**
     * Largest power-of-two sample size that keeps both sides at least the requested size.
     * Public for unit testing.
     */
    public static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    /** Scales so the longest side is at most {@code maxDimension}, and rotates if needed. */
    private static Bitmap scaleAndRotate(@NonNull Bitmap src, int maxDimension, int rotationDegrees) {
        int longest = Math.max(src.getWidth(), src.getHeight());
        float scale = longest > maxDimension ? (float) maxDimension / longest : 1f;
        if (scale == 1f && rotationDegrees == 0) {
            return src;
        }
        Matrix m = new Matrix();
        m.postScale(scale, scale);
        m.postRotate(rotationDegrees);
        Bitmap result = Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), m, true);
        if (result != src) {
            src.recycle();
        }
        return result;
    }
}
