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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.reminders.ReminderNotifications
import com.foodtracker.app.reminders.ReminderScheduler
import com.foodtracker.app.reminders.ReminderStore
import com.foodtracker.app.ui.settings.PreferencesStore
import com.foodtracker.app.ui.settings.applyAppLocale
import com.foodtracker.app.ui.settings.effectiveServerUrl
import com.foodtracker.app.ui.settings.resolveDarkTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    /** Bumped each time a reminder notification is tapped; App opens Capture on every new value. */
    private val openCaptureRequests = mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restores reminder alarms dropped by a force-stop (the settings are the source of truth).
        ReminderScheduler.applyAll(this, ReminderStore.load(this))
        // On recreation the request was already handled before.
        if (savedInstanceState == null) handleReminderTap(intent)
        syncNotificationChannel()
        // Every API client reads BackendConfig lazily, so this covers all requests from here on.
        BackendConfig.baseUrl = effectiveServerUrl(PreferencesStore.load(this))
        setContent {
            // Options choices: owned here so the system bars can follow the chosen theme too.
            var preferences by remember { mutableStateOf(PreferencesStore.load(this)) }
            val darkTheme = resolveDarkTheme(preferences.theme, isSystemInDarkTheme())
            // The manifest handles uiMode changes without recreating the activity, so the system
            // bar icon colors must be re-applied here whenever the effective light/dark flips.
            DisposableEffect(darkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                    navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { darkTheme },
                )
                onDispose {}
            }
            App(
                preferences = preferences,
                onPreferencesChange = { updated ->
                    PreferencesStore.save(this, updated)
                    preferences = updated
                    BackendConfig.baseUrl = effectiveServerUrl(updated)
                    // Keeps the channel name (shown in system settings) in the chosen language.
                    syncNotificationChannel()
                },
                openCaptureRequests = openCaptureRequests.intValue,
            )
        }
    }

    private fun syncNotificationChannel() {
        lifecycleScope.launch {
            applyAppLocale(this@MainActivity)
            ReminderNotifications.ensureChannel(this@MainActivity)
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
