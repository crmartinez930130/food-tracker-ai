package com.foodtracker.app.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.key

/**
 * Overrides the locale Compose resources (and Material pickers) resolve against, following the
 * JetBrains "resource environment" pattern for a runtime, in-app language switch.
 */
expect object LocalAppLocale {
    val current: String @Composable get

    @Composable
    infix fun provides(tag: String): ProvidedValue<*>
}

/** The phone's own language (e.g. "en"), unaffected by the app's override. */
expect fun deviceLanguage(): String

/** Renders [content] in [language]; switching re-creates the content so every string re-resolves. */
@Composable
fun AppEnvironment(language: AppLanguage, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalAppLocale provides language.tag) {
        key(language) { content() }
    }
}
