package com.foodtracker.wear

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foodtracker.app.data.DEFAULT_GOAL
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.cache.SaveOutcome
import com.foodtracker.app.data.cache.SendReport
import com.foodtracker.app.data.cache.storedSettingsOutbox
import com.foodtracker.app.util.currentInstantIso
import com.foodtracker.app.util.todayDateString
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Logs one glass of today's goal size from the watch (wear-app), through the watch's own offline queue as the
 * phone does: sent at once when the backend answers, queued (and a background send asked for) when it doesn't.
 * A queued glass counts at once in [todayStates]; a sent one is followed by a refresh of today, since that path
 * doesn't touch the cache. A refusal (4xx) is dropped, as on the phone widget. False without settings yet.
 */
suspend fun logWatchGlass(context: Context): Boolean {
    val settings = WatchSettingsStore.load(context) ?: return false
    val database = watchDatabase(context)
    val glassMl = runCatching { todayStates(context, settings).first().glassMl }.getOrDefault(DEFAULT_GOAL.waterGlassMl)
    val today = todayDateString()
    val (outbox, outboxClient) = storedSettingsOutbox(
        settings.serverUrl,
        settings.accessToken,
        database.outboxDao(),
        database.dayCacheDao(),
        requestSync = { WatchOutboxSync.request(context) },
    )
    try {
        val outcome = outbox.logWater(LogWaterRequest(glassMl, loggedAt = currentInstantIso()), today)
        if (outcome is SaveOutcome.Saved) refreshToday(context) else requestTileUpdate(context)
    } finally {
        outboxClient.close()
    }
    return true
}

/** Sends the watch's queued glasses in the background once the backend answers (offline-queue, on the watch). */
object WatchOutboxSync {
    private const val WORK_NAME = "watch-outbox-sync"
    private const val BACKOFF_SECONDS = 30L

    fun request(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            WORK_NAME,
            ExistingWorkPolicy.KEEP,
            OneTimeWorkRequestBuilder<WatchOutboxWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
                .build(),
        )
    }
}

/** Retry later while the backend can't be reached; done otherwise (a refused row is the phone's Home to handle). */
fun watchSyncResult(report: SendReport): ListenableWorker.Result =
    if (report.offline) ListenableWorker.Result.retry() else ListenableWorker.Result.success()

class WatchOutboxWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val settings = WatchSettingsStore.load(applicationContext) ?: return Result.success()
        val database = watchDatabase(applicationContext)
        val (outbox, httpClient) = storedSettingsOutbox(settings.serverUrl, settings.accessToken, database.outboxDao(), database.dayCacheDao())
        val report = try {
            outbox.sendAll()
        } finally {
            httpClient.close()
        }
        // Sent rows were merged into the cached day; a refresh also brings what the phone logged meanwhile.
        if (report.sent > 0) refreshToday(applicationContext)
        return watchSyncResult(report)
    }
}

/** The prefix of the tile's "+ glass" button id; each drawn tile gives its button a fresh id (see [glassClickId]). */
const val ADD_GLASS_CLICK = "add_glass"

/** A new id for the tile's "+ glass" button, unique per drawn tile: a tap then names exactly one drawing. */
fun glassClickId(now: Long): String = "$ADD_GLASS_CLICK:$now"

/**
 * Whether a tile request is a tap on "+ glass" that wasn't handled yet. Each drawn tile gives its button a fresh
 * id, so a request that carries an id already handled (were the system ever to repeat it, e.g. on a later
 * refresh) never logs a second glass.
 */
fun isNewTileClick(lastClickableId: String, lastHandledId: String?): Boolean =
    lastClickableId.startsWith("$ADD_GLASS_CLICK:") && lastClickableId != lastHandledId
