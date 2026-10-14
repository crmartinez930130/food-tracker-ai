package com.foodtracker.app.ui.settings

import com.foodtracker.app.data.defaultBackendBaseUrl
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
    /** Backend address chosen in Options; null means the platform default (see [effectiveServerUrl]). */
    val serverUrl: String? = null,
    /** The backend's API_TOKEN, sent with every request; null until the user enters one. */
    val accessToken: String? = null,
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

/** The address every backend request should use. */
fun effectiveServerUrl(preferences: AppPreferences): String = preferences.serverUrl ?: defaultBackendBaseUrl()

private val SERVER_URL = Regex("""^(https?)://([A-Za-z0-9._-]+)(?::(\d{1,5}))?/?$""", RegexOption.IGNORE_CASE)

/**
 * The canonical form of a typed backend address (`http://192.168.1.50:3000`), or null when it
 * isn't one: an http(s) scheme, a host, an optional port 1..65535 and nothing after it (a single
 * trailing slash is dropped). Paths aren't allowed - the client appends `/api/...` itself.
 */
fun normalizeServerUrl(input: String): String? {
    val match = SERVER_URL.matchEntire(input.trim()) ?: return null
    val (scheme, host, port) = match.destructured
    if (host.isEmpty() || host.startsWith('.') || host.endsWith('.')) return null
    if (port.isNotEmpty() && port.toInt() !in 1..65535) return null
    return buildString {
        append(scheme.lowercase()).append("://").append(host)
        if (port.isNotEmpty()) append(':').append(port.toInt())
    }
}

/** A typed access token as stored: trimmed, and null when blank (no token is sent then). */
fun normalizeAccessToken(input: String): String? = input.trim().ifEmpty { null }
