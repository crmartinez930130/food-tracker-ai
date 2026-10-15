package com.foodtracker.app.data.cache

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.effectiveServerUrl
import java.util.concurrent.TimeUnit

/**
 * Sends the offline queue in the background (offline-queue): when the phone has a network, and again
 * with growing waits while the backend can't be reached. WorkManager keeps it across process death
 * and reboots; a force-stop suspends it until the app is opened again.
 */
object OutboxSync {
    private const val WORK_NAME = "outbox-sync"

    /** Schedules a send unless one is already waiting (its backoff is kept). */
    fun request(context: Context) {
        val work = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, work)
    }
}

class OutboxSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // A fresh process never set BackendConfig: the stored address and token are used explicitly.
        val preferences = PreferencesStore.load(applicationContext)
        val serverUrl = effectiveServerUrl(preferences)
        val database = cacheDatabase(applicationContext)
        val httpClient = createHttpClient(tokenProvider = { preferences.accessToken })
        return try {
            val outbox = OutboxRepository(
                api = FoodTrackerApiClient(httpClient) { serverUrl },
                outbox = database.outboxDao(),
                dayCache = database.dayCacheDao(),
                requestSync = {},
                serverUrl = { serverUrl },
            )
            if (outbox.sendAll().offline) Result.retry() else Result.success()
        } finally {
            httpClient.close()
        }
    }
}
