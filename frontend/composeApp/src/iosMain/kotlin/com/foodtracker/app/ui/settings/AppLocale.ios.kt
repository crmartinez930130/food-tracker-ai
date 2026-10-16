package com.foodtracker.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

// iOS is a secondary target: the override is tracked for Compose only (no NSUserDefaults "AppleLanguages").
private val LocalIosAppLocale = staticCompositionLocalOf { NSLocale.currentLocale.languageCode }

actual object LocalAppLocale {
    actual val current: String
        @Composable get() = LocalIosAppLocale.current

    @Composable
    actual infix fun provides(tag: String): ProvidedValue<*> = LocalIosAppLocale.provides(tag)
}

@Composable
actual fun deviceLanguage(): String = NSLocale.currentLocale.languageCode
