package com.foodtracker.app.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min

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

actual fun ByteArray.downscaledJpeg(maxLongEdge: Int): ByteArray {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(this, 0, size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return this

    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSizeFor(bounds.outWidth, bounds.outHeight, maxLongEdge)
    }
    val bitmap = BitmapFactory.decodeByteArray(this, 0, size, options) ?: return this
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(this)).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    // The re-encoded JPEG carries no EXIF, so the rotation is applied to the pixels.
    val scale = min(1f, maxLongEdge.toFloat() / max(bitmap.width, bitmap.height))
    val matrix = Matrix().apply {
        postScale(scale, scale)
        postRotate(exifRotationDegrees(orientation).toFloat())
    }
    val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    return ByteArrayOutputStream().use { out ->
        upright.compress(Bitmap.CompressFormat.JPEG, UPLOAD_JPEG_QUALITY, out)
        out.toByteArray()
    }
}

private const val UPLOAD_JPEG_QUALITY = 85

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
