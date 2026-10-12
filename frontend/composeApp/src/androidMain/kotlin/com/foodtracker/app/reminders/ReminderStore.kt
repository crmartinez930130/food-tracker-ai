package com.foodtracker.app.reminders

import android.content.Context
import com.foodtracker.app.ui.reminders.ReminderSettings
import com.foodtracker.app.ui.reminders.decodeReminderSettings
import com.foodtracker.app.ui.reminders.encodeReminderSettings

/** Reminder settings live only on the device (no backend), as JSON in SharedPreferences. */
object ReminderStore {
    private const val PREFS = "meal_reminders"
    private const val KEY_SETTINGS = "settings"

    fun load(context: Context): ReminderSettings =
        decodeReminderSettings(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SETTINGS, null))

    fun save(context: Context, settings: ReminderSettings) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_SETTINGS, encodeReminderSettings(settings))
            .apply()
    }
}
