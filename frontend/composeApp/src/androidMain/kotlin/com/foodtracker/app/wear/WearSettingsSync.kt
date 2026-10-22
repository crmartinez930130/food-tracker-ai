package com.foodtracker.app.wear

import android.content.Context
import com.foodtracker.app.ui.settings.AppPreferences
import com.foodtracker.app.ui.settings.deviceLanguage
import com.foodtracker.app.ui.settings.effectiveServerUrl
import com.foodtracker.app.ui.settings.resolveLanguage
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Sends the backend settings to the Wear OS app (wear-app) over the Data Layer, which only reaches this
 * same app (same package and signature) on the user's paired watch, over an encrypted channel.
 */
object WearSettingsSync {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Publishes [preferences]' server, token and language; the Data Layer skips it when nothing changed. */
    fun push(context: Context, preferences: AppPreferences) {
        val app = context.applicationContext
        val settings = WatchSettings(
            serverUrl = effectiveServerUrl(preferences),
            accessToken = preferences.accessToken,
            language = resolveLanguage(preferences.language, deviceLanguage(app)).tag,
        )
        // A new item replaces the previous one whole, so a removed token disappears from the watch too.
        val request = PutDataMapRequest.create(WatchSettings.PATH).apply {
            settings.toMap().forEach { (key, value) -> dataMap.putString(key, value) }
        }.asPutDataRequest().setUrgent()
        scope.launch {
            try {
                Wearable.getDataClient(app).putDataItem(request).await()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // No paired watch or no Play services: there's nobody to tell, and the phone works the same.
            }
        }
    }
}
