package com.foodtracker.wear

import android.content.Context
import androidx.room3.Room
import androidx.wear.tiles.TileService
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foodtracker.app.data.cache.CACHE_DATABASE_NAME
import com.foodtracker.app.data.cache.CacheDatabase
import com.foodtracker.app.data.cache.RefreshResult
import com.foodtracker.app.data.cache.buildCacheDatabase
import com.foodtracker.app.data.cache.epochMillisNow
import com.foodtracker.app.data.cache.storedSettingsDayRepository
import com.foodtracker.app.util.todayDateString
import com.foodtracker.app.wear.WatchSettings
import com.foodtracker.app.widget.TodayWidgetState
import com.foodtracker.app.widget.todayWidgetStates
import kotlinx.coroutines.flow.Flow

private val lock = Any()
private var database: CacheDatabase? = null

/** The watch's own copy of today and the goal (the same cache as the phone's, in the watch's storage). */
fun watchDatabase(context: Context): CacheDatabase = synchronized(lock) {
    database ?: context.applicationContext.let { app ->
        Room.databaseBuilder<CacheDatabase>(app, app.getDatabasePath(CACHE_DATABASE_NAME).absolutePath).buildCacheDatabase()
    }.also { database = it }
}

/** Today as the phone's widget shows it, from the watch's copy, again whenever it changes. */
fun todayStates(context: Context, settings: WatchSettings): Flow<TodayWidgetState> {
    val db = watchDatabase(context)
    return todayWidgetStates(db.dayCacheDao(), db.outboxDao(), settings.serverUrl, todayDateString(), ::epochMillisNow)
}

/** Fetches today and the goal into the watch's copy, then redraws the tile. Null without settings yet. */
suspend fun refreshToday(context: Context): RefreshResult? {
    val settings = WatchSettingsStore.load(context) ?: return null
    val (days, httpClient) = storedSettingsDayRepository(settings.serverUrl, settings.accessToken, watchDatabase(context).dayCacheDao())
    val result = try {
        days.refresh(todayDateString())
    } finally {
        httpClient.close()
    }
    requestTileUpdate(context)
    return result
}

/**
 * Asks the system to redraw the "Today" tile. With the application context: the updater binds to a service,
 * which a BroadcastReceiver's context isn't allowed to do.
 */
fun requestTileUpdate(context: Context) {
    TileService.getUpdater(context.applicationContext).requestUpdate(TodayTileService::class.java)
}

/** Background refreshes for the tile: one at a time, with a network, at most every 15 minutes. */
object WatchRefresh {
    private const val WORK_NAME = "watch-refresh"
    private const val PREFS = "watch_refresh"
    private const val KEY_LAST = "last_requested"
    private const val MIN_INTERVAL_MILLIS = 15 * 60 * 1000L

    /** A refresh now, e.g. right after new settings arrive. */
    fun request(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_LAST, System.currentTimeMillis()).apply()
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WatchRefreshWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build(),
        )
    }

    /**
     * A refresh unless one was asked for in the last 15 minutes. The tile asks on every draw, and a refresh
     * redraws the tile, so without this they would keep triggering each other.
     */
    fun requestIfDue(context: Context) {
        val last = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong(KEY_LAST, 0L)
        if (isRefreshDue(last, System.currentTimeMillis(), MIN_INTERVAL_MILLIS)) request(context)
    }
}

/** Whether a refresh last asked for at [lastRequested] can be asked for again at [now]. */
fun isRefreshDue(lastRequested: Long, now: Long, minIntervalMillis: Long): Boolean =
    lastRequested <= 0L || now - lastRequested >= minIntervalMillis || now < lastRequested

class WatchRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // A failed refresh keeps the copy; the screen and the tile then say how old it is.
        refreshToday(applicationContext)
        return Result.success()
    }
}
