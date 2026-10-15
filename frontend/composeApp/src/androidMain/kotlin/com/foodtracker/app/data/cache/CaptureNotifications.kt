package com.foodtracker.app.data.cache

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.foodtracker.app.MainActivity
import com.foodtracker.app.R
import com.foodtracker.app.ui.settings.applyAppLocale
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.capture_ready_channel_description
import food_tracker_frontend.composeapp.generated.resources.capture_ready_channel_name
import food_tracker_frontend.composeapp.generated.resources.capture_ready_text
import food_tracker_frontend.composeapp.generated.resources.capture_ready_title
import org.jetbrains.compose.resources.getString

/** "You have meals ready to review", posted when the background run analyzed kept captures (offline-queue). */
object CaptureNotifications {
    private const val CHANNEL_ID = "captures_ready"

    /** A single id: a newer notice replaces an unread one instead of stacking. */
    private const val NOTIFICATION_ID = 2000

    /** Only when the run made something ready; analyses refused or still waiting aren't news. */
    fun shouldNotify(report: AnalyzeReport): Boolean = report.ready > 0

    /** Posts the notice unless notifications are off for the app (then it silently does nothing). */
    suspend fun showReady(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        // A fresh worker process has the phone's locale; texts must use the app's language.
        applyAppLocale(context)
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(getString(Res.string.capture_ready_channel_name))
                .setDescription(getString(Res.string.capture_ready_channel_description))
                .build(),
        )
        val openHome = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(Res.string.capture_ready_title))
            .setContentText(getString(Res.string.capture_ready_text))
            .setContentIntent(openHome)
            .setAutoCancel(true)
            .build()
        try {
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: SecurityException) {
            // Permission revoked between the check and the post: nothing to show.
        }
    }
}
