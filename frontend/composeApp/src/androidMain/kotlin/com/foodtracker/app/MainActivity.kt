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
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.cache.AndroidCapturePhotoStore
import com.foodtracker.app.data.cache.CaptureQueueRepository
import com.foodtracker.app.data.cache.OutboxRepository
import com.foodtracker.app.data.cache.OutboxSync
import com.foodtracker.app.data.cache.cacheDatabase
import com.foodtracker.app.data.createHttpClient
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

    /** Send the offline queue and analyze kept captures whenever the app comes to the foreground (offline-queue). */
    private lateinit var outbox: OutboxRepository
    private lateinit var captures: CaptureQueueRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Restores reminder alarms dropped by a force-stop (the settings are the source of truth).
        ReminderScheduler.applyAll(this, ReminderStore.load(this))
        // On recreation the request was already handled before.
        if (savedInstanceState == null) handleReminderTap(intent)
        syncNotificationChannel()
        // Every API client reads BackendConfig lazily, so this covers all requests from here on.
        PreferencesStore.load(this).let { stored ->
            BackendConfig.baseUrl = effectiveServerUrl(stored)
            BackendConfig.accessToken = stored.accessToken
        }
        val database = cacheDatabase(this)
        val cacheStore = database.dayCacheDao()
        outbox = OutboxRepository(
            FoodTrackerApiClient(createHttpClient()),
            database.outboxDao(),
            cacheStore,
            requestSync = { OutboxSync.request(this) },
        )
        val capturePhotos = AndroidCapturePhotoStore(this)
        captures = CaptureQueueRepository(
            FoodTrackerApiClient(createHttpClient()),
            database.captureDao(),
            capturePhotos,
            requestSync = { OutboxSync.request(this) },
        )
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
                database = database,
                capturePhotos = capturePhotos,
                requestSync = { OutboxSync.request(this) },
                preferences = preferences,
                onPreferencesChange = { updated ->
                    PreferencesStore.save(this, updated)
                    preferences = updated
                    // Another server's data must never show: drop the cache (a new token keeps it).
                    // Only the other servers' rows: a refresh of the new one may already have landed.
                    val serverUrl = effectiveServerUrl(updated)
                    if (serverUrl != BackendConfig.baseUrl) {
                        lifecycleScope.launch {
                            cacheStore.deleteOtherServers(serverUrl)
                            database.favoritesCacheDao().deleteOtherServers(serverUrl)
                        }
                    }
                    BackendConfig.baseUrl = effectiveServerUrl(updated)
                    BackendConfig.accessToken = updated.accessToken
                    // Keeps the channel name (shown in system settings) in the chosen language.
                    syncNotificationChannel()
                },
                openCaptureRequests = openCaptureRequests.intValue,
            )
        }
    }

    override fun onStart() {
        super.onStart()
        // Whatever couldn't be sent or analyzed goes now if the backend answers; otherwise WorkManager keeps trying.
        lifecycleScope.launch {
            val sent = outbox.sendAll()
            val analyzed = captures.analyzeWaiting()
            if (sent.offline || analyzed.offline) OutboxSync.request(this@MainActivity)
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
