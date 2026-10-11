package com.blogspace.app.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/** Memory-safe decoding: the sample size never decodes more pixels than needed. */
public class ImageStorageTest {

    @Test
    public void sampleSize_smallImageNotScaled() {
        assertEquals(1, ImageStorage.calculateInSampleSize(800, 600, 2048, 2048));
    }

    @Test
    public void sampleSize_largeCameraPhotoDownsampled() {
        // 12 MP photo (4000x3000) requested at 1000px -> sample size 2 keeps both sides >= 1000.
        assertEquals(2, ImageStorage.calculateInSampleSize(4000, 3000, 1000, 1000));
        assertEquals(4, ImageStorage.calculateInSampleSize(8000, 6000, 1000, 1000));
    }
}
