package com.foodtracker.app.reminders

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.foodtracker.app.MainActivity
import com.foodtracker.app.R
import com.foodtracker.app.data.MealType
import com.foodtracker.app.ui.reminders.REMINDER_TEXT
import com.foodtracker.app.ui.reminders.reminderTitle

object ReminderNotifications {
    private const val CHANNEL_ID = "meal_reminders"

    /** One id per meal: a new reminder replaces yesterday's still-unread one instead of stacking. */
    private fun notificationId(meal: MealType) = 1000 + meal.ordinal

    fun ensureChannel(context: Context) {
        NotificationManagerCompat.from(context).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName("Meal reminders")
                .setDescription("Optional daily reminders to log breakfast, lunch or dinner")
                .build(),
        )
    }

    /** Posts the reminder unless notifications are off for the app (then it silently does nothing). */
    fun show(context: Context, meal: MealType) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)
        val openCapture = PendingIntent.getActivity(
            context,
            meal.ordinal,
            Intent(context, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_OPEN_CAPTURE, true)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(reminderTitle(meal))
            .setContentText(REMINDER_TEXT)
            .setContentIntent(openCapture)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        try {
            manager.notify(notificationId(meal), notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post: nothing to show.
        }
    }
}
