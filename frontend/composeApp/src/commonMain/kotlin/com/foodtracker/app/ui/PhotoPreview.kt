package com.foodtracker.app.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a JPEG/PNG for on-screen preview: upright (the EXIF orientation applied - camera photos
 * store sideways pixels plus a tag) and downsampled so its long edge is about [maxDimension] px.
 * Returns null if the bytes can't be decoded.
 */
expect fun ByteArray.decodePreviewBitmap(maxDimension: Int = 1080): ImageBitmap?
