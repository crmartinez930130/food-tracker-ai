package com.foodtracker.app.ui.settings

import com.foodtracker.app.data.defaultBackendBaseUrl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ServerUrlTest {

    @Test
    fun `valid addresses come back in canonical form`() {
        assertEquals("http://192.168.1.50:3000", normalizeServerUrl("http://192.168.1.50:3000"))
        assertEquals("http://192.168.1.50:3000", normalizeServerUrl("  HTTP://192.168.1.50:3000/  "))
        assertEquals("http://localhost:3000", normalizeServerUrl("http://localhost:3000"))
        assertEquals("https://food.home.lan", normalizeServerUrl("https://food.home.lan"))
        assertEquals("http://my-pc_1:8080", normalizeServerUrl("http://my-pc_1:8080"))
        assertEquals("http://10.0.2.2:3000", normalizeServerUrl("http://10.0.2.2:03000"))
    }

    @Test
    fun `addresses without a usable scheme are rejected`() {
        assertNull(normalizeServerUrl("192.168.1.50:3000"))
        assertNull(normalizeServerUrl("ftp://192.168.1.50:3000"))
        assertNull(normalizeServerUrl("http:/192.168.1.50:3000"))
        assertNull(normalizeServerUrl(""))
    }

    @Test
    fun `bad hosts and ports are rejected`() {
        assertNull(normalizeServerUrl("http://"))
        assertNull(normalizeServerUrl("http://:3000"))
        assertNull(normalizeServerUrl("http://.local:3000"))
        assertNull(normalizeServerUrl("http://192.168.1.50:0"))
        assertNull(normalizeServerUrl("http://192.168.1.50:65536"))
        assertNull(normalizeServerUrl("http://192.168.1.50:abc"))
        assertNull(normalizeServerUrl("http://192.168 .1.50:3000"))
    }

    @Test
    fun `paths, queries and fragments are rejected`() {
        assertNull(normalizeServerUrl("http://192.168.1.50:3000/api"))
        assertNull(normalizeServerUrl("http://192.168.1.50:3000?x=1"))
        assertNull(normalizeServerUrl("http://192.168.1.50:3000#top"))
        assertNull(normalizeServerUrl("http://192.168.1.50:3000//"))
    }

    @Test
    fun `the effective address falls back to the platform default`() {
        assertEquals(defaultBackendBaseUrl(), effectiveServerUrl(AppPreferences()))
        assertEquals("http://192.168.1.50:3000", effectiveServerUrl(AppPreferences(serverUrl = "http://192.168.1.50:3000")))
    }

    @Test
    fun `preferences saved before the server field existed keep the default`() {
        val old = decodeAppPreferences("""{"language":"ENGLISH","theme":"DARK"}""")

        assertNull(old.serverUrl)
        assertEquals(LanguagePreference.ENGLISH, old.language)
    }

    @Test
    fun `a saved server address round-trips`() {
        val preferences = AppPreferences(serverUrl = "http://192.168.1.50:3000")

        assertEquals(preferences, decodeAppPreferences(encodeAppPreferences(preferences)))
    }
}
