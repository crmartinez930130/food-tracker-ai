package com.foodtracker.app.ui.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppPreferencesTest {

    @Test
    fun `defaults follow the phone for language and theme`() {
        assertEquals(LanguagePreference.SYSTEM, AppPreferences().language)
        assertEquals(ThemePreference.SYSTEM, AppPreferences().theme)
    }

    @Test
    fun `preferences round-trip through JSON`() {
        val preferences = AppPreferences(LanguagePreference.ENGLISH, ThemePreference.DARK)

        assertEquals(preferences, decodeAppPreferences(encodeAppPreferences(preferences)))
    }

    @Test
    fun `missing, corrupt or unknown stored values fall back to the defaults`() {
        assertEquals(AppPreferences(), decodeAppPreferences(null))
        assertEquals(AppPreferences(), decodeAppPreferences("not json"))
        assertEquals(AppPreferences(), decodeAppPreferences("""{"language":"KLINGON","theme":"NEON"}"""))
        assertEquals(
            AppPreferences(theme = ThemePreference.LIGHT),
            decodeAppPreferences("""{"theme":"LIGHT"}"""),
        )
    }

    @Test
    fun `phone language picks English only for an English phone`() {
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(LanguagePreference.SYSTEM, "en"))
        assertEquals(AppLanguage.SPANISH, resolveLanguage(LanguagePreference.SYSTEM, "es"))
        assertEquals(AppLanguage.SPANISH, resolveLanguage(LanguagePreference.SYSTEM, "fr"))
        assertEquals(AppLanguage.SPANISH, resolveLanguage(LanguagePreference.SYSTEM, "pt"))
    }

    @Test
    fun `a fixed language ignores the phone`() {
        assertEquals(AppLanguage.SPANISH, resolveLanguage(LanguagePreference.SPANISH, "en"))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(LanguagePreference.ENGLISH, "es"))
        assertEquals(AppLanguage.ENGLISH, resolveLanguage(LanguagePreference.ENGLISH, "fr"))
    }

    @Test
    fun `theme preference overrides the system mode unless it is System`() {
        assertTrue(resolveDarkTheme(ThemePreference.SYSTEM, systemDark = true))
        assertFalse(resolveDarkTheme(ThemePreference.SYSTEM, systemDark = false))
        assertFalse(resolveDarkTheme(ThemePreference.LIGHT, systemDark = true))
        assertTrue(resolveDarkTheme(ThemePreference.DARK, systemDark = false))
    }
}
