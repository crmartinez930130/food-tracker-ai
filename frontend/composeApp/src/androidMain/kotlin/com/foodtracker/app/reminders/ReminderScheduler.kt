package com.foodtracker.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.foodtracker.app.data.MealType
import com.foodtracker.app.ui.reminders.ReminderSettings
import com.foodtracker.app.ui.reminders.nextTriggerAt
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Keeps the registered alarms equal to the settings: one one-shot alarm per enabled meal, none for
 * disabled ones. Called after every settings change, on app start, and on boot/clock/timezone/update
 * broadcasts (see ReminderReceiver), so no alarm is ever orphaned or lost.
 */
object ReminderScheduler {
    const val ACTION_REMINDER = "com.foodtracker.app.REMINDER"
    const val EXTRA_MEAL = "meal"

    /** setWindow needs no special permission; Android 14+ stretches shorter windows to 10 minutes anyway. */
    private const val WINDOW_MS = 10 * 60 * 1000L

    @OptIn(ExperimentalTime::class)
    fun applyAll(context: Context, settings: ReminderSettings) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val now = Clock.System.now()
        for (reminder in settings.reminders) {
            val pendingIntent = alarmIntent(context, reminder.meal)
            if (reminder.enabled) {
                val trigger = nextTriggerAt(now, reminder.hour, reminder.minute, TimeZone.currentSystemDefault())
                alarmManager.setWindow(AlarmManager.RTC_WAKEUP, trigger.toEpochMilliseconds(), WINDOW_MS, pendingIntent)
            } else {
                alarmManager.cancel(pendingIntent)
            }
        }
    }

    /** Same request code and intent for a meal every time, so scheduling replaces and cancel matches it. */
    private fun alarmIntent(context: Context, meal: MealType): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            meal.ordinal,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMINDER).putExtra(EXTRA_MEAL, meal.name),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
