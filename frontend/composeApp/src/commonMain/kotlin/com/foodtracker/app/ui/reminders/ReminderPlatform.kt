package com.foodtracker.app.ui.reminders

import androidx.compose.runtime.Composable

/** Device-side storage, scheduling and notification permission for meal reminders. */
interface ReminderPlatform {
    fun load(): ReminderSettings

    /** Persists [settings] and makes the scheduled reminders match them. */
    fun save(settings: ReminderSettings)

    /** Whether the app may show notifications right now. */
    fun canNotify(): Boolean

    /** Asks for notification permission where that's a runtime prompt; [onResult] gets whether notifications are now allowed. */
    fun requestNotifications(onResult: (Boolean) -> Unit)

    /** Opens the system screen where the user can allow this app's notifications. */
    fun openNotificationSettings()
}

@Composable
expect fun rememberReminderPlatform(): ReminderPlatform
