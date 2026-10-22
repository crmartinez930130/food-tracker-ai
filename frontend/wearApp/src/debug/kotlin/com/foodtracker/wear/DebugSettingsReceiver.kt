package com.foodtracker.wear

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.foodtracker.app.wear.WatchSettings

/**
 * Debug builds only (wear-app): loads test settings on an emulator, where pairing with a phone needs a Google
 * account. Guarded by the DUMP permission, which only the shell and the system hold:
 * `adb shell am broadcast -n com.foodtracker.app/com.foodtracker.wear.DebugSettingsReceiver --es serverUrl … --es accessToken … --es language es`
 */
class DebugSettingsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val map = listOf(WatchSettings.KEY_SERVER_URL, WatchSettings.KEY_ACCESS_TOKEN, WatchSettings.KEY_LANGUAGE)
            .associateWith { intent.getStringExtra(it) }
        WatchSettings.fromMap(map)?.let { applySettings(context, it) }
    }
}
