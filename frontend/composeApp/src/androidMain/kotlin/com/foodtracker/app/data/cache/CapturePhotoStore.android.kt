package com.foodtracker.app.data.cache

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Kept capture photos under `files/pending-captures/`: the `file` domain, excluded from backups. */
class AndroidCapturePhotoStore(context: Context) : CapturePhotoStore {
    private val dir = File(context.applicationContext.filesDir, "pending-captures")

    override suspend fun write(name: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
        dir.mkdirs()
        File(dir, name).writeBytes(bytes)
    }

    override suspend fun read(name: String): ByteArray? = withContext(Dispatchers.IO) {
        File(dir, name).takeIf { it.isFile }?.readBytes()
    }

    override suspend fun delete(name: String) {
        withContext(Dispatchers.IO) { File(dir, name).delete() }
    }
}
