package com.foodtracker.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foodtracker.app.data.MealType

/**
 * Fires reminders and keeps them scheduled: on a reminder alarm it shows the notification (if that
 * reminder is still on) and schedules the next day; on boot, clock, timezone or app-update
 * broadcasts it re-registers every alarm from the stored settings.
 */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val settings = ReminderStore.load(context)
        if (intent.action == ReminderScheduler.ACTION_REMINDER) {
            val meal = intent.getStringExtra(ReminderScheduler.EXTRA_MEAL)
                ?.let { name -> MealType.entries.firstOrNull { it.name == name } }
            if (meal != null && settings.reminders.any { it.meal == meal && it.enabled }) {
                ReminderNotifications.show(context, meal)
            }
        }
        // Reschedules the fired reminder for tomorrow (nextTriggerAt skips "now"), and restores
        // everything after boot/time changes.
        ReminderScheduler.applyAll(context, settings)
    }
}
