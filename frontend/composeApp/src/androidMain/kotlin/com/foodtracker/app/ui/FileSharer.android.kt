package com.foodtracker.app.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Writes the file to the app's private cache (`exports/`, matching res/xml/file_paths.xml) and
 * offers it through an ACTION_SEND chooser with a temporary read grant - never public storage.
 */
@Composable
actual fun rememberFileSharer(): FileSharer {
    val context = LocalContext.current
    return remember(context) {
        object : FileSharer {
            override suspend fun share(fileName: String, mimeType: String, content: String) {
                val file = withContext(Dispatchers.IO) {
                    File(context.cacheDir, "exports").let { dir ->
                        dir.mkdirs()
                        File(dir, fileName).apply { writeText(content, Charsets.UTF_8) }
                    }
                }
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = mimeType
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, fileName)
                    // The read grant applies to the ClipData URI, so set it explicitly for the chooser.
                    clipData = ClipData.newRawUri(fileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(send, null))
            }
        }
    }
}
