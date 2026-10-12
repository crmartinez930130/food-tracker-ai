package com.foodtracker.app.ui.reminders

import androidx.compose.runtime.Composable

// iOS local notifications (UNUserNotificationCenter) are deferred, like the other iOS actuals -
// Android is the target reminders are verified on. Nothing is stored or scheduled here.
@Composable
actual fun rememberReminderPlatform(): ReminderPlatform {
    return object : ReminderPlatform {
        override fun load() = ReminderSettings.DEFAULT
        override fun save(settings: ReminderSettings) = Unit
        override fun canNotify() = false
        override fun requestNotifications(onResult: (Boolean) -> Unit) = onResult(false)
        override fun openNotificationSettings() = Unit
    }
}
