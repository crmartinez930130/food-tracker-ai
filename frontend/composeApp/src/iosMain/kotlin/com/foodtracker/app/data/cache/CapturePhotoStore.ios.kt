package com.foodtracker.app.data.cache

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** Kept capture photos in the documents directory. Not built or tested yet (no Mac). */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
class IosCapturePhotoStore : CapturePhotoStore {
    private val dir: String = run {
        val documents = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
        requireNotNull(documents?.path) + "/pending-captures"
    }

    override suspend fun write(name: String, bytes: ByteArray) {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, true, null, null)
        val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
        data.writeToFile("$dir/$name", true)
    }

    override suspend fun read(name: String): ByteArray? {
        val data = NSData.dataWithContentsOfFile("$dir/$name") ?: return null
        return ByteArray(data.length.toInt()).also { bytes ->
            if (bytes.isNotEmpty()) bytes.usePinned { memcpy(it.addressOf(0), data.bytes, data.length) }
        }
    }

    override suspend fun delete(name: String) {
        NSFileManager.defaultManager.removeItemAtPath("$dir/$name", null)
    }
}
