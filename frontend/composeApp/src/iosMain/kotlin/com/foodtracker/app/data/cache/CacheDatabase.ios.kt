package com.foodtracker.app.data.cache

import androidx.room3.Room
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** The cache database in the app's documents directory. Not built or tested yet (no Mac). */
@OptIn(ExperimentalForeignApi::class)
fun cacheDatabase(): CacheDatabase {
    val documents = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)
    return Room.databaseBuilder<CacheDatabase>(name = requireNotNull(documents?.path) + "/$CACHE_DATABASE_NAME")
        .buildCacheDatabase()
}
