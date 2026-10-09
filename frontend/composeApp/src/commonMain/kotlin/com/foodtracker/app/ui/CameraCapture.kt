package com.foodtracker.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Full-screen in-app camera. Owns everything platform-specific about taking a photo: asking for
 * the camera permission, the live preview and the shutter.
 *
 * @param onCaptured the captured photo as JPEG bytes.
 * @param onCancel the user backed out without a photo.
 * @param onUseGallery the user chose the gallery instead (e.g. after denying the permission).
 */
@Composable
expect fun CameraCapture(
    onCaptured: (ByteArray) -> Unit,
    onCancel: () -> Unit,
    onUseGallery: () -> Unit,
    modifier: Modifier = Modifier,
)
