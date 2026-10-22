package com.foodtracker.app.wear

/**
 * What the phone app sends the watch app (wear-app): the backend to talk to, its access token and the app's
 * resolved language ("es" or "en"). Sent as plain strings over the Wear OS Data Layer.
 */
data class WatchSettings(val serverUrl: String, val accessToken: String?, val language: String) {
    fun toMap(): Map<String, String> = buildMap {
        put(KEY_SERVER_URL, serverUrl)
        accessToken?.let { put(KEY_ACCESS_TOKEN, it) }
        put(KEY_LANGUAGE, language)
    }

    companion object {
        /** The Data Layer path the phone writes the settings to. */
        const val PATH = "/settings"

        const val KEY_SERVER_URL = "serverUrl"
        const val KEY_ACCESS_TOKEN = "accessToken"
        const val KEY_LANGUAGE = "language"

        /** The settings in [map], or null when it lacks a server address or a language (an older or broken item). */
        fun fromMap(map: Map<String, String?>): WatchSettings? {
            val serverUrl = map[KEY_SERVER_URL]?.takeIf { it.isNotBlank() } ?: return null
            val language = map[KEY_LANGUAGE]?.takeIf { it.isNotBlank() } ?: return null
            return WatchSettings(serverUrl, map[KEY_ACCESS_TOKEN]?.takeIf { it.isNotBlank() }, language)
        }
    }
}
