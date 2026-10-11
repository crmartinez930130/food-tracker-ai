package com.foodtracker.app.ui.reminders

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.foodtracker.app.reminders.ReminderScheduler
import com.foodtracker.app.reminders.ReminderStore

@Composable
actual fun rememberReminderPlatform(): ReminderPlatform {
    val context = LocalContext.current
    val pending = remember { arrayOfNulls<(Boolean) -> Unit>(1) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pending[0]?.invoke(granted && NotificationManagerCompat.from(context).areNotificationsEnabled())
        pending[0] = null
    }

    return remember(context, permissionLauncher) {
        object : ReminderPlatform {
            override fun load() = ReminderStore.load(context)

            override fun save(settings: ReminderSettings) {
                ReminderStore.save(context, settings)
                ReminderScheduler.applyAll(context, settings)
            }

            override fun canNotify() = NotificationManagerCompat.from(context).areNotificationsEnabled()

            override fun requestNotifications(onResult: (Boolean) -> Unit) {
                val needsPrompt = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                    PackageManager.PERMISSION_GRANTED
                if (needsPrompt) {
                    pending[0] = onResult
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Granted (or no runtime permission), but the user may have blocked them in settings.
                    onResult(canNotify())
                }
            }

            override fun openNotificationSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
