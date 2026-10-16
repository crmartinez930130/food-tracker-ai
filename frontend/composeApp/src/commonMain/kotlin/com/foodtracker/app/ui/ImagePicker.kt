package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** Launches the platform's photo picker; [onImagePicked] receives the raw image bytes. */
interface ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher

/**
 * Launches the platform's photo picker for up to [maxItems] photos at once (at least 1);
 * [onImagesPicked] receives their raw bytes, in the order chosen.
 */
@Composable
expect fun rememberMultiImagePickerLauncher(maxItems: Int, onImagesPicked: (List<ByteArray>) -> Unit): ImagePickerLauncher
