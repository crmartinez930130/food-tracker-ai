package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

// iOS photo picker wiring (PHPickerViewController) is deferred - iOS is a secondary
// target for this change (see design.md Non-Goals). This stub keeps commonMain
// portable without blocking Android, which is what setup-project-foundation verifies.
@Composable
actual fun rememberImagePickerLauncher(onImagePicked: (ByteArray) -> Unit): ImagePickerLauncher {
    return object : ImagePickerLauncher {
        override fun launch() {
            throw NotImplementedError("iOS photo picker is not implemented yet")
        }
    }
}
