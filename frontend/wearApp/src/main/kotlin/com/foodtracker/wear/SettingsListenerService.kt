package com.foodtracker.wear

import android.content.Context
import com.foodtracker.app.wear.WatchSettings
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

/** Receives the backend settings the phone app publishes at [WatchSettings.PATH] (wear-app). */
class SettingsListenerService : WearableListenerService() {
    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // The buffer is released when this returns, so the items are read here, synchronously.
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == WatchSettings.PATH) {
                applySettingsItem(this, event.dataItem)
            }
        }
    }
}

/** Saves the settings in [item], then refreshes today and redraws the tile with them. */
fun applySettingsItem(context: Context, item: DataItem) {
    val map = DataMapItem.fromDataItem(item).dataMap
    val settings = WatchSettings.fromMap(map.keySet().associateWith { map.getString(it) }) ?: return
    applySettings(context, settings)
}

fun applySettings(context: Context, settings: WatchSettings) {
    if (settings == WatchSettingsStore.load(context)) return
    WatchSettingsStore.save(context, settings)
    WatchRefresh.request(context)
    requestTileUpdate(context)
}

/** The settings already published, in case an event was missed (e.g. the app was installed after the phone sent them). */
suspend fun loadPublishedSettings(context: Context) {
    try {
        val items = Wearable.getDataClient(context).dataItems.await()
        try {
            items.firstOrNull { it.uri.path == WatchSettings.PATH }?.let { applySettingsItem(context, it) }
        } finally {
            items.release()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        // No Play services or no paired phone yet: the listener gets them when they arrive.
    }
}
