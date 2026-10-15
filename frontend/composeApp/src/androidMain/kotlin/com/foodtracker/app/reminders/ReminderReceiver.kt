package com.foodtracker.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.cache.OutboxKind
import com.foodtracker.app.data.cache.cacheDatabase
import com.foodtracker.app.data.cache.decodeOrNull
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.ui.reminders.shouldShowReminder
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.applyAppLocale
import com.foodtracker.app.ui.settings.effectiveServerUrl
import com.foodtracker.app.util.todayDateString
import io.ktor.client.engine.android.Android
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Fires reminders and keeps them scheduled: on a reminder alarm it shows the notification (if that
 * reminder is still on and the meal isn't logged yet today) and schedules the next day; on boot,
 * clock, timezone or app-update broadcasts it re-registers every alarm from the stored settings.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = ReminderStore.load(context)
        val meal = intent.takeIf { it.action == ReminderScheduler.ACTION_REMINDER }
            ?.getStringExtra(ReminderScheduler.EXTRA_MEAL)
            ?.let { name -> MealType.entries.firstOrNull { it.name == name } }
            ?.takeIf { fired -> settings.reminders.any { it.meal == fired && it.enabled } }

        if (meal == null) {
            // System broadcasts (or a stale alarm): just make the alarms match the settings again.
            ReminderScheduler.applyAll(context, settings)
            return
        }

        // Checking today's meals needs the network, so finish asynchronously (goAsync allows ~10 s).
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // A fresh receiver process has the phone's locale; texts must use the app's language.
                applyAppLocale(context)
                val preferences = PreferencesStore.load(context)
                val serverUrl = effectiveServerUrl(preferences)
                if (shouldShowReminder(meal, mealsTodayOrNull(serverUrl, preferences.accessToken), queuedMealTypesToday(context, serverUrl))) {
                    ReminderNotifications.show(context, meal)
                }
            } finally {
                // Reschedules the fired reminder for tomorrow (nextTriggerAt skips "now").
                ReminderScheduler.applyAll(context, settings)
                pending.finish()
            }
        }
    }

    /**
     * Today's meals, or null if the backend can't answer quickly - then the reminder is shown anyway.
     * A fresh receiver process never set BackendConfig, so the saved address and token are passed in explicitly.
     */
    private suspend fun mealsTodayOrNull(serverUrl: String, accessToken: String?): List<MealEntry>? {
        // Engine timeouts too: a blocking connect to a sleeping laptop may not react to cancellation.
        val httpClient = createHttpClient(
            Android.create {
                connectTimeout = MEALS_TIMEOUT_MS.toInt()
                socketTimeout = MEALS_TIMEOUT_MS.toInt()
            },
            tokenProvider = { accessToken },
        )
        return try {
            withTimeoutOrNull(MEALS_TIMEOUT_MS) { FoodTrackerApiClient(httpClient) { serverUrl }.listMeals(todayDateString()) }
        } catch (e: Exception) {
            null
        } finally {
            httpClient.close()
        }
    }

    /**
     * The types of today's meals waiting in the offline queue for [serverUrl]: they count as logged.
     * Empty if the queue can't be read - then the reminder is decided as before.
     */
    private suspend fun queuedMealTypesToday(context: Context, serverUrl: String): List<MealType?> =
        try {
            cacheDatabase(context).outboxDao().pendingOfDay(todayDateString(), serverUrl)
                .filter { it.kind == OutboxKind.MEAL }
                .mapNotNull { row -> decodeOrNull<SaveMealRequest>(row.payloadJson) }
                .map { it.mealType }
        } catch (e: Exception) {
            emptyList()
        }

    private companion object {
        const val MEALS_TIMEOUT_MS = 4_000L
    }
}
