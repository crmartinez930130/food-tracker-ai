package com.foodtracker.app

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableIntStateOf
import com.foodtracker.app.reminders.ReminderNotifications
import com.foodtracker.app.reminders.ReminderScheduler
import com.foodtracker.app.reminders.ReminderStore

class MainActivity : ComponentActivity() {
    /** Bumped each time a reminder notification is tapped; App opens Capture on every new value. */
    private val openCaptureRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restores reminder alarms dropped by a force-stop (the settings are the source of truth).
        ReminderNotifications.ensureChannel(this)
        ReminderScheduler.applyAll(this, ReminderStore.load(this))
        // On recreation the request was already handled before.
        if (savedInstanceState == null) handleReminderTap(intent)
        setContent {
            val darkTheme = isSystemInDarkTheme()
            // The manifest handles uiMode changes without recreating the activity, so the system
            // bar icon colors must be re-applied here whenever light/dark flips.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            App(openCaptureRequests = openCaptureRequests.intValue)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReminderTap(intent)
    }

    private fun handleReminderTap(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_CAPTURE, false) == true) {
            openCaptureRequests.intValue++
        }
    }

    companion object {
        const val EXTRA_OPEN_CAPTURE = "open_capture"
    }
}
