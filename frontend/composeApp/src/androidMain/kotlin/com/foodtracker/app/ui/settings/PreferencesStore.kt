package com.foodtracker.app.ui.settings

import android.content.Context

/** Options choices live only on the device, as JSON in SharedPreferences (like reminder settings). */
object PreferencesStore {
    private const val PREFS = "app_preferences"
    private const val KEY = "preferences"

    fun load(context: Context): AppPreferences =
        decodeAppPreferences(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null))

    fun save(context: Context, preferences: AppPreferences) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, encodeAppPreferences(preferences)).apply()
    }
}

/**
 * Makes non-Compose code (the reminder receiver, channel setup) resolve strings in the app's
 * language: Compose resources' getString reads Locale.getDefault() on Android.
 */
fun applyAppLocale(context: Context) {
    val language = resolveLanguage(PreferencesStore.load(context).language, deviceLanguage(context))
    java.util.Locale.setDefault(java.util.Locale.forLanguageTag(language.tag))
}
