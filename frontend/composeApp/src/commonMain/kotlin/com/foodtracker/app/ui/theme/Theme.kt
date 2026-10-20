package com.foodtracker.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * The app's single theme entry point: the Greenwave palette, Google Sans Flex and the rounder
 * shape scale. Follows the given light/dark choice - on Android the activity handles `uiMode`
 * changes itself, so this recomposes in place instead of restarting.
 *
 * The root [Surface] gives screens that aren't inside a Scaffold (Capture, Review) a themed
 * background and the matching default content color.
 */
@Composable
fun FoodTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Leaves the root surface clear, e.g. for a screen drawn over the wallpaper (the widget settings). */
    transparentBackground: Boolean = false,
    content: @Composable () -> Unit,
) {
    val family = googleSansFlex()
    val typography = remember(family) { expressiveTypography(family) }
    val tokens = remember(typography) { typeTokens(typography) }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = typography,
        shapes = AppShapes,
    ) {
        CompositionLocalProvider(LocalTypeTokens provides tokens) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (transparentBackground) Color.Transparent else MaterialTheme.colorScheme.background,
            ) {
                content()
            }
        }
    }
}

/** Theme values beyond MaterialTheme's, like `MaterialTheme.typography` for the app's extra styles. */
object FoodTrackerTheme {
    val typeTokens: TypeTokens
        @Composable get() = LocalTypeTokens.current
}
