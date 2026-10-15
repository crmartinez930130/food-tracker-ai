package com.foodtracker.app.data.cache

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.effectiveServerUrl
import java.util.concurrent.TimeUnit

/**
 * The offline work in the background (offline-queue): sends the queued meals and water, then analyzes
 * the kept captures, when the phone has a network, and again with growing waits while the backend
 * can't be reached. WorkManager keeps it across process death
 * and reboots; a force-stop suspends it until the app is opened again.
 */
object OutboxSync {
    private const val WORK_NAME = "outbox-sync"

    /** Schedules a send unless one is already waiting (its backoff is kept). */
    fun request(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.KEEP, syncRequest())
    }

    /** A send that waits for a network and, while the backend can't be reached, retries with growing waits. */
    internal fun syncRequest(): OneTimeWorkRequest =
        OneTimeWorkRequestBuilder<OutboxSyncWorker>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, BACKOFF_SECONDS, TimeUnit.SECONDS)
            .build()

    /** Retry later while anything waits for the backend; done otherwise (refused rows are the user's to handle). */
    internal fun resultFor(sent: SendReport, analyzed: AnalyzeReport): ListenableWorker.Result =
        if (sent.offline || analyzed.offline) ListenableWorker.Result.retry() else ListenableWorker.Result.success()

    internal const val BACKOFF_SECONDS = 30L
}

/** Sends the queue and analyzes the kept captures with the stored server and token; see [OutboxSync.resultFor]. */
class OutboxSyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = PreferencesStore.load(applicationContext)
        val serverUrl = effectiveServerUrl(preferences)
        val database = cacheDatabase(applicationContext)
        val (outbox, outboxClient) = storedSettingsOutbox(serverUrl, preferences.accessToken, database.outboxDao(), database.dayCacheDao())
        val (captures, captureClient) = storedSettingsCaptureQueue(
            serverUrl,
            preferences.accessToken,
            database.captureDao(),
            AndroidCapturePhotoStore(applicationContext),
        )
        return try {
            val sent = outbox.sendAll()
            val analyzed = captures.analyzeWaiting()
            if (CaptureNotifications.shouldNotify(analyzed)) CaptureNotifications.showReady(applicationContext)
            OutboxSync.resultFor(sent, analyzed)
        } finally {
            outboxClient.close()
            captureClient.close()
        }
    }
}
