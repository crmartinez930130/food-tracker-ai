package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** Launches the platform's photo picker; [onImagePicked] receives the raw image bytes. */
interface ImagePickerLauncher {
    fun launch()
}

@Composable
expect fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher
