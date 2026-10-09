package com.foodtracker.app.ui

import androidx.exifinterface.media.ExifInterface
import kotlin.test.Test
import kotlin.test.assertEquals

class PhotoPreviewTest {

    @Test
    fun `camera orientations map to clockwise rotations`() {
        assertEquals(0, exifRotationDegrees(ExifInterface.ORIENTATION_NORMAL))
        assertEquals(0, exifRotationDegrees(ExifInterface.ORIENTATION_UNDEFINED))
        assertEquals(90, exifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_90))
        assertEquals(180, exifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_180))
        assertEquals(270, exifRotationDegrees(ExifInterface.ORIENTATION_ROTATE_270))
    }

    @Test
    fun `sample size halves until the long edge would drop below the target`() {
        assertEquals(1, sampleSizeFor(1080, 810, maxDimension = 1080))
        assertEquals(1, sampleSizeFor(2000, 1500, maxDimension = 1080))
        assertEquals(2, sampleSizeFor(2160, 1620, maxDimension = 1080))
        assertEquals(4, sampleSizeFor(4032, 3024, maxDimension = 1000))
    }

    @Test
    fun `sample size uses the long edge for portrait images`() {
        assertEquals(2, sampleSizeFor(1620, 2160, maxDimension = 1080))
    }
}
