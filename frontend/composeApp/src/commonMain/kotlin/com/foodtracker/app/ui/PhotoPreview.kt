package com.foodtracker.app.ui

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Decodes a JPEG/PNG for on-screen preview: upright (the EXIF orientation applied - camera photos
 * store sideways pixels plus a tag) and downsampled so its long edge is about [maxDimension] px.
 * Returns null if the bytes can't be decoded.
 */
expect fun ByteArray.decodePreviewBitmap(maxDimension: Int = 1080): ImageBitmap?

/** The long edge photos are reduced to before sending: the size the backend analyzes anyway. */
const val UPLOAD_LONG_EDGE_PX = 1568

/**
 * The photo upright and with its long edge at most [maxLongEdge], as JPEG, so sending several of them
 * (and keeping them while offline) stays small. Returns the photo unchanged if it can't be decoded.
 */
expect fun ByteArray.downscaledJpeg(maxLongEdge: Int = UPLOAD_LONG_EDGE_PX): ByteArray
