package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

// iOS share sheet (UIActivityViewController) is deferred, like the other iOS actuals - Android is
// the target the export is verified on.
@Composable
actual fun rememberFileSharer(): FileSharer {
    return object : FileSharer {
        override suspend fun share(fileName: String, mimeType: String, content: String) {
            throw NotImplementedError("iOS file sharing is not implemented yet")
        }
    }
}
