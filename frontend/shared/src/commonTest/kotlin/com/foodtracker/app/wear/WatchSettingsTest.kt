package com.foodtracker.app.wear

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WatchSettingsTest {
    @Test
    fun `settings survive the trip through the Data Layer map`() {
        val settings = WatchSettings("http://192.168.1.129:3000", "token-123", "es")

        assertEquals(settings, WatchSettings.fromMap(settings.toMap()))
    }

    @Test
    fun `without a token the key is left out and comes back as null`() {
        val settings = WatchSettings("http://10.0.2.2:3000", null, "en")

        assertEquals(setOf(WatchSettings.KEY_SERVER_URL, WatchSettings.KEY_LANGUAGE), settings.toMap().keys)
        assertEquals(settings, WatchSettings.fromMap(settings.toMap()))
    }

    @Test
    fun `a blank token counts as none`() {
        val map = mapOf(WatchSettings.KEY_SERVER_URL to "http://h:3000", WatchSettings.KEY_ACCESS_TOKEN to " ", WatchSettings.KEY_LANGUAGE to "es")

        assertNull(WatchSettings.fromMap(map)?.accessToken)
    }

    @Test
    fun `an item without server or language is ignored`() {
        assertNull(WatchSettings.fromMap(mapOf(WatchSettings.KEY_LANGUAGE to "es")))
        assertNull(WatchSettings.fromMap(mapOf(WatchSettings.KEY_SERVER_URL to "http://h:3000")))
        assertNull(WatchSettings.fromMap(mapOf(WatchSettings.KEY_SERVER_URL to "", WatchSettings.KEY_LANGUAGE to "es")))
    }
}
