package com.foodtracker.app.data.cache

import android.content.Context
import androidx.room3.Room
import com.foodtracker.app.widget.WidgetUpdates

private val lock = Any()
private var database: CacheDatabase? = null

/**
 * The process's single cache database, in the app's databases directory: a domain the backup
 * rules exclude, so the cache never leaves the phone (offline-cache).
 */
fun cacheDatabase(context: Context): CacheDatabase = synchronized(lock) {
    database ?: context.applicationContext.let { app ->
        Room.databaseBuilder<CacheDatabase>(app, app.getDatabasePath(CACHE_DATABASE_NAME).absolutePath)
            .buildCacheDatabase()
            // The widgets show this database's day, goal and queue: redraw them whenever those change.
            .also { WidgetUpdates.watch(app, it) }
    }.also { database = it }
}
