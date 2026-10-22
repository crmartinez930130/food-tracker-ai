package com.foodtracker.wear

import android.content.Context
import android.content.res.Configuration
import com.foodtracker.app.wear.WatchSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

/**
 * The backend settings the phone app sent (wear-app), kept only on the watch: the app's backup rules
 * exclude every domain, as on the phone.
 */
object WatchSettingsStore {
    private const val PREFS = "watch_settings"
    private var current: MutableStateFlow<WatchSettings?>? = null

    fun load(context: Context): WatchSettings? {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).all
        return WatchSettings.fromMap(stored.mapValues { it.value as? String })
    }

    /** The settings, updated whenever new ones are saved in this process. */
    @Synchronized
    fun flow(context: Context): StateFlow<WatchSettings?> =
        current ?: MutableStateFlow(load(context)).also { current = it }

    fun save(context: Context, settings: WatchSettings) {
        val editor = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear()
        settings.toMap().forEach { (key, value) -> editor.putString(key, value) }
        editor.apply()
        flow(context).let { (it as MutableStateFlow).value = settings }
    }
}

/** This context in the language chosen in the phone app, or as it is until the phone has sent one. */
fun Context.localized(): Context {
    val language = WatchSettingsStore.load(this)?.language ?: return this
    val configuration = Configuration(resources.configuration).apply { setLocale(Locale.forLanguageTag(language)) }
    return createConfigurationContext(configuration)
}
