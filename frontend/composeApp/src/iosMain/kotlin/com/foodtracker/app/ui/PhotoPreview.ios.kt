package com.foodtracker.app.ui

import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap

// iOS photo input is still stubbed (see ImagePicker.ios.kt); plain decoding is enough until then.
actual fun ByteArray.decodePreviewBitmap(maxDimension: Int): ImageBitmap? =
    runCatching { decodeToImageBitmap() }.getOrNull()
