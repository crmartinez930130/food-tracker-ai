package com.foodtracker.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.google_sans_flex
import org.jetbrains.compose.resources.Font

/**
 * Google Sans Flex, the variable font bundled unmodified (SIL OFL 1.1, license in
 * composeResources/files/google_sans_flex_OFL.txt). Each weight is an instance of its `wght` axis.
 */
@Composable
fun googleSansFlex(): FontFamily {
    val weights = listOf(FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold, FontWeight.ExtraBold)
    val fonts = weights.map { Font(Res.font.google_sans_flex, weight = it) }
    return remember(fonts) { FontFamily(fonts) }
}

/**
 * The M3 type scale in Google Sans Flex, pushed toward the expressive samples: heavier, slightly
 * tighter display and headline styles; semibold titles; labels and body as in the baseline.
 */
fun expressiveTypography(family: FontFamily): Typography {
    val base = Typography()
    fun TextStyle.withFamily(weight: FontWeight? = null, tracking: Double? = null) = copy(
        fontFamily = family,
        fontWeight = weight ?: fontWeight,
        letterSpacing = tracking?.em ?: letterSpacing,
    )
    return Typography(
        displayLarge = base.displayLarge.withFamily(FontWeight.Bold, -0.02),
        displayMedium = base.displayMedium.withFamily(FontWeight.Bold, -0.02),
        displaySmall = base.displaySmall.withFamily(FontWeight.Bold, -0.015),
        headlineLarge = base.headlineLarge.withFamily(FontWeight.Bold, -0.01),
        headlineMedium = base.headlineMedium.withFamily(FontWeight.Bold, -0.01),
        headlineSmall = base.headlineSmall.withFamily(FontWeight.SemiBold),
        titleLarge = base.titleLarge.withFamily(FontWeight.SemiBold),
        titleMedium = base.titleMedium.withFamily(FontWeight.SemiBold),
        titleSmall = base.titleSmall.withFamily(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.withFamily(),
        bodyMedium = base.bodyMedium.withFamily(),
        bodySmall = base.bodySmall.withFamily(),
        labelLarge = base.labelLarge.withFamily(FontWeight.Medium),
        labelMedium = base.labelMedium.withFamily(FontWeight.Medium),
        labelSmall = base.labelSmall.withFamily(FontWeight.Medium),
    )
}

/** Styles outside the M3 Typography contract, read through `FoodTrackerTheme.typeTokens`. */
@Immutable
data class TypeTokens(
    /** Big figures ("1240" kcal): extra bold and slightly tight. */
    val emphasizedNumber: TextStyle,
    /** The small unit next to a big figure ("kcal"). */
    val numberUnit: TextStyle,
)

fun typeTokens(typography: Typography): TypeTokens = TypeTokens(
    emphasizedNumber = typography.displayMedium.copy(
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.02).em,
    ),
    numberUnit = typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
)

internal val LocalTypeTokens = staticCompositionLocalOf<TypeTokens> { error("FoodTrackerTheme not applied") }
