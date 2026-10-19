package com.foodtracker.app.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.foodtracker.app.data.cache.CacheDatabase
import com.foodtracker.app.data.cache.cacheDatabase
import com.foodtracker.app.data.cache.storedSettingsDayRepository
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.effectiveServerUrl
import com.foodtracker.app.util.todayDateString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * Keeps the home-screen widgets current (home-widgets): redraws them when the phone's copy of today, the
 * goal or the offline queue changes, refreshes today every 30 minutes while a widget is placed, and
 * switches them to the new day at midnight.
 */
object WidgetUpdates {
    private const val REFRESH_WORK = "widget-refresh"
    private const val REFRESH_NOW_WORK = "widget-refresh-now"
    private const val REFRESH_MINUTES = 30L

    /** Lives as long as the process: the database watcher and the redraws it asks for run here. */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Redraws the widgets whenever a table they show changes, whoever wrote it: the app, the background
     * sender or a widget action. Called once per process, with its single database.
     */
    @OptIn(FlowPreview::class)
    fun watch(context: Context, database: CacheDatabase) {
        val app = context.applicationContext
        scope.launch {
            database.invalidationTracker.createFlow("day_cache", "goal_cache", "outbox", emitInitialState = false)
                // A refresh writes the day and the goal together; one redraw is enough.
                .debounce(300)
                .collect { updateAll(app) }
        }
    }

    /**
     * Counts redraw requests. A Glance session that is still open only recomposes on update, so the widgets'
     * content restarts from this: each request re-reads the server, today's date and the clock.
     */
    internal val redraws = MutableStateFlow(0L)

    /** Redraws every placed widget; nothing to do when none is placed. */
    suspend fun updateAll(context: Context) {
        if (!hasWidgets(context)) return
        redraws.update { it + 1 }
        TodayCaloriesWidget().updateAll(context)
        QuickActionsWidget().updateAll(context)
    }

    /** [updateAll] from code that can't suspend, e.g. after the settings change the language. */
    fun requestUpdate(context: Context) {
        val app = context.applicationContext
        scope.launch { updateAll(app) }
    }

    fun hasWidgets(context: Context): Boolean {
        val manager = AppWidgetManager.getInstance(context)
        return listOf(TodayCaloriesWidgetReceiver::class.java, QuickActionsWidgetReceiver::class.java).any {
            manager.getAppWidgetIds(ComponentName(context, it)).isNotEmpty()
        }
    }

    /**
     * Called when the first widget of a kind is placed or the last one removed: the background refresh and
     * the midnight alarm only exist while some widget is on the home screen.
     */
    fun onWidgetsChanged(context: Context) {
        val work = WorkManager.getInstance(context)
        if (hasWidgets(context)) {
            work.enqueueUniquePeriodicWork(
                REFRESH_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<WidgetRefreshWorker>(REFRESH_MINUTES, TimeUnit.MINUTES)
                    .setConstraints(networkConstraint())
                    .build(),
            )
        } else {
            work.cancelUniqueWork(REFRESH_WORK)
            work.cancelUniqueWork(REFRESH_NOW_WORK)
            context.getSystemService(AlarmManager::class.java).cancel(midnightIntent(context))
        }
    }

    /** A refresh as soon as there is a network, e.g. right after midnight. */
    fun refreshNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            REFRESH_NOW_WORK,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<WidgetRefreshWorker>().setConstraints(networkConstraint()).build(),
        )
    }

    /**
     * Wakes [MidnightReceiver] a minute after the next local midnight. Inexact, so it needs no permission and
     * may come a few minutes late while the phone dozes. Each widget draw re-arms it (same intent, replaced).
     */
    fun scheduleMidnight(context: Context) {
        val zone = ZoneId.systemDefault()
        val nextMidnight = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC, nextMidnight + 60_000, midnightIntent(context))
    }

    private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, MidnightReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun networkConstraint() = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    internal fun launch(block: suspend () -> Unit) = scope.launch { block() }
}

/** Refreshes today's copy and the goal from the backend; the database watcher then redraws the widgets. */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val preferences = PreferencesStore.load(applicationContext)
        val (days, httpClient) = storedSettingsDayRepository(
            effectiveServerUrl(preferences),
            preferences.accessToken,
            cacheDatabase(applicationContext).dayCacheDao(),
        )
        try {
            // A failed refresh leaves the copy as it was; the widget then says how old it is.
            days.refresh(todayDateString())
        } finally {
            httpClient.close()
        }
        // Drawn here too: the watcher's debounced redraw might not run before the process goes away.
        WidgetUpdates.updateAll(applicationContext)
        return Result.success()
    }
}

/**
 * Midnight, and clock or time-zone changes: the widgets switch to the (new) current day at once, then a
 * refresh fetches it. Not exported: the alarm uses an explicit intent, and the system still delivers its own broadcasts.
 */
class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        if (!WidgetUpdates.hasWidgets(app)) return
        val pending = goAsync()
        WidgetUpdates.launch {
            try {
                WidgetUpdates.updateAll(app)
                WidgetUpdates.refreshNow(app)
            } finally {
                pending.finish()
            }
        }
    }
}
