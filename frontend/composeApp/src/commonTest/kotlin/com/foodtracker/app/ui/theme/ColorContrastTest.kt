package com.foodtracker.app.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertTrue

/** WCAG 2.x contrast ratio between two opaque colors (1.0 to 21.0). */
private fun contrastRatio(a: Color, b: Color): Double {
    val la = a.luminance().toDouble()
    val lb = b.luminance().toDouble()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/** Normal-size text needs 4.5:1 under WCAG AA. */
private const val MIN_TEXT_CONTRAST = 4.5

class ColorContrastTest {

    /** Foreground/background role pairs the app's screens actually render text or icons with. */
    private fun pairsUsedByTheApp(scheme: ColorScheme): Map<String, Pair<Color, Color>> = mapOf(
        "onSurface/surface" to (scheme.onSurface to scheme.surface),
        "onSurfaceVariant/surface" to (scheme.onSurfaceVariant to scheme.surface),
        "onBackground/background" to (scheme.onBackground to scheme.background),
        "onPrimary/primary" to (scheme.onPrimary to scheme.primary),
        "primary/surface" to (scheme.primary to scheme.surface),
        "onPrimaryContainer/primaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
        "onSecondaryContainer/secondaryContainer" to (scheme.onSecondaryContainer to scheme.secondaryContainer),
        "onSurfaceVariant/surfaceContainer" to (scheme.onSurfaceVariant to scheme.surfaceContainer),
        "error/surface" to (scheme.error to scheme.surface),
        // Home's cards (water, calorie badge, meal groups and their type squares).
        "onTertiaryContainer/tertiaryContainer" to (scheme.onTertiaryContainer to scheme.tertiaryContainer),
        "onTertiary/tertiary" to (scheme.onTertiary to scheme.tertiary),
        "onErrorContainer/errorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
        "error/surfaceContainer" to (scheme.error to scheme.surfaceContainer),
        "primary/surfaceContainer" to (scheme.primary to scheme.surfaceContainer),
        "onSurface/surfaceContainerLow" to (scheme.onSurface to scheme.surfaceContainerLow),
        "onSurfaceVariant/surfaceContainerLow" to (scheme.onSurfaceVariant to scheme.surfaceContainerLow),
        "onSurfaceVariant/surfaceContainerHighest" to (scheme.onSurfaceVariant to scheme.surfaceContainerHighest),
    )

    private fun assertLegible(schemeName: String, scheme: ColorScheme) {
        val failures = pairsUsedByTheApp(scheme)
            .mapValues { (_, colors) -> contrastRatio(colors.first, colors.second) }
            .filterValues { it < MIN_TEXT_CONTRAST }
        assertTrue(failures.isEmpty(), "$schemeName pairs below $MIN_TEXT_CONTRAST:1 -> $failures")
    }

    @Test
    fun `light scheme text and icon pairs meet WCAG AA contrast`() = assertLegible("light", LightColors)

    @Test
    fun `dark scheme text and icon pairs meet WCAG AA contrast`() = assertLegible("dark", DarkColors)

    @Test
    fun `contrast helper matches known reference values`() {
        // Sanity checks so a broken helper can't make every pair pass.
        assertTrue(contrastRatio(Color.Black, Color.White) in 20.9..21.1)
        assertTrue(contrastRatio(Color.White, Color.White) in 0.99..1.01)
    }
}
