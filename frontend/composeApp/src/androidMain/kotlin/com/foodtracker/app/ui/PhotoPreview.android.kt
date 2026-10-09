package com.foodtracker.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import kotlin.math.max

actual fun ByteArray.decodePreviewBitmap(maxDimension: Int): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxDimension)
    }
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size, options) ?: return null

    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(this)).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val degrees = exifRotationDegrees(orientation)
    if (degrees == 0) return bitmap.asImageBitmap()

    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).asImageBitmap()
}

/** Largest power-of-two subsample that keeps the long edge at or above [maxDimension]. */
internal fun sampleSizeFor(width: Int, height: Int, maxDimension: Int): Int {
    val longEdge = max(width, height)
    var sampleSize = 1
    while (longEdge / (sampleSize * 2) >= maxDimension) sampleSize *= 2
    return sampleSize
}

/**
 * Clockwise rotation needed to display an image upright for an EXIF orientation value. Mirrored
 * orientations map to their rotation only - cameras don't produce them for normal shots.
 */
internal fun exifRotationDegrees(orientation: Int): Int = when (orientation) {
    ExifInterface.ORIENTATION_ROTATE_90, ExifInterface.ORIENTATION_TRANSPOSE -> 90
    ExifInterface.ORIENTATION_ROTATE_180, ExifInterface.ORIENTATION_FLIP_VERTICAL -> 180
    ExifInterface.ORIENTATION_ROTATE_270, ExifInterface.ORIENTATION_TRANSVERSE -> 270
    else -> 0
}
