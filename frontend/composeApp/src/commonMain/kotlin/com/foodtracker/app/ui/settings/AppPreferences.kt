package com.foodtracker.app.ui.settings

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** What the user picked in Options. [SYSTEM] follows the phone (see [resolveLanguage]). */
@Serializable
enum class LanguagePreference { SYSTEM, SPANISH, ENGLISH }

@Serializable
enum class ThemePreference { SYSTEM, LIGHT, DARK }

/** The languages the app actually ships; [tag] is the resource qualifier / locale language. */
enum class AppLanguage(val tag: String) { SPANISH("es"), ENGLISH("en") }

@Serializable
data class AppPreferences(
    val language: LanguagePreference = LanguagePreference.SYSTEM,
    val theme: ThemePreference = ThemePreference.SYSTEM,
)

private val preferencesJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }

fun encodeAppPreferences(preferences: AppPreferences): String = preferencesJson.encodeToString(preferences)

/** Stored preferences, or the defaults for anything missing or unreadable - never a crash. */
fun decodeAppPreferences(stored: String?): AppPreferences =
    stored?.let { runCatching { preferencesJson.decodeFromString<AppPreferences>(it) }.getOrNull() } ?: AppPreferences()

/**
 * The language to show. Following the phone means English only when the phone is in English;
 * Spanish is the product default, so it covers Spanish and every language the app doesn't ship.
 */
fun resolveLanguage(preference: LanguagePreference, deviceLanguage: String): AppLanguage = when (preference) {
    LanguagePreference.SPANISH -> AppLanguage.SPANISH
    LanguagePreference.ENGLISH -> AppLanguage.ENGLISH
    LanguagePreference.SYSTEM ->
        if (deviceLanguage.lowercase() == AppLanguage.ENGLISH.tag) AppLanguage.ENGLISH else AppLanguage.SPANISH
}

fun resolveDarkTheme(preference: ThemePreference, systemDark: Boolean): Boolean = when (preference) {
    ThemePreference.SYSTEM -> systemDark
    ThemePreference.LIGHT -> false
    ThemePreference.DARK -> true
}
