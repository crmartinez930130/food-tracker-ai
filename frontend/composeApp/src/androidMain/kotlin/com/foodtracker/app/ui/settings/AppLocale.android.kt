package com.foodtracker.app.ui.settings

import android.app.LocaleManager
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import java.util.Locale

actual object LocalAppLocale {
    actual val current: String
        @Composable get() = Locale.getDefault().language

    // The JetBrains-documented approach for Compose Multiplatform 1.9: the default locale drives
    // non-composable getString, the configuration drives stringResource and Material pickers.
    @Suppress("DEPRECATION")
    @Composable
    actual infix fun provides(tag: String): ProvidedValue<*> {
        val context = LocalContext.current
        val configuration = LocalConfiguration.current
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        val resources = context.resources
        resources.updateConfiguration(configuration, resources.displayMetrics)
        // Android re-applies its own configuration without notice (e.g. when the in-app camera
        // closes), which undoes the override above. As the per-app language, Android applies it itself.
        // The activity handles locale changes, so this doesn't recreate it.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            LaunchedEffect(tag) {
                val manager = context.getSystemService(LocaleManager::class.java)
                if (manager.applicationLocales.toLanguageTags() != tag) {
                    manager.applicationLocales = LocaleList.forLanguageTags(tag)
                }
            }
        }
        return LocalConfiguration.provides(configuration)
    }
}

/**
 * The phone's own language. With a per-app language set (Android 13+), Resources.getSystem()
 * reports that one instead, so the system locales come from LocaleManager there.
 */
fun deviceLanguage(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val system = context.getSystemService(LocaleManager::class.java).systemLocales
        if (!system.isEmpty) return system[0].language
    }
    return Resources.getSystem().configuration.locales[0].language
}

// Android doesn't tell an app with its own language that the phone's changed; changing it means
// leaving the app, so it is re-read on every return.
@Composable
actual fun deviceLanguage(): String {
    val context = LocalContext.current
    var language by remember { mutableStateOf(deviceLanguage(context)) }
    LifecycleResumeEffect(context) {
        language = deviceLanguage(context)
        onPauseOrDispose {}
    }
    return language
}
