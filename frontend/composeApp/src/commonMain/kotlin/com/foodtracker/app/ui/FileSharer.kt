package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** Hands a generated file to the platform's share sheet, so the user picks where it goes. */
interface FileSharer {
    suspend fun share(fileName: String, mimeType: String, content: String)
}

@Composable
expect fun rememberFileSharer(): FileSharer
