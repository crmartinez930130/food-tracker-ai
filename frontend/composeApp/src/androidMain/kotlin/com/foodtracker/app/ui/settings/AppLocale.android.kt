package com.foodtracker.app.ui.settings

import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

actual object LocalAppLocale {
    actual val current: String
        @Composable get() = Locale.getDefault().language

    // The JetBrains-documented approach for Compose Multiplatform 1.9: the default locale drives
    // non-composable getString, the configuration drives stringResource and Material pickers.
    @Suppress("DEPRECATION")
    @Composable
    actual infix fun provides(tag: String): ProvidedValue<*> {
        val configuration = LocalConfiguration.current
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        val resources = LocalContext.current.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        return LocalConfiguration.provides(configuration)
    }
}

// The system-wide configuration, which the app's own override above never touches.
actual fun deviceLanguage(): String = Resources.getSystem().configuration.locales[0].language
