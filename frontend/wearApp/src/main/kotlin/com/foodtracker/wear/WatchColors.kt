package com.foodtracker.wear

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.protolayout.types.argb

/**
 * Food Tracker's colors on the watch (wear-app): the app's dark scheme, copied from DarkColors in
 * composeApp/.../ui/theme/Color.kt (the watch can't depend on that module) - regenerate both together.
 * A watch UI is always dark; its background stays black, as Wear OS asks of OLED screens.
 */
object WatchColors {
    const val PRIMARY = 0xFF95D5A8
    const val ON_PRIMARY = 0xFF00391D
    const val PRIMARY_CONTAINER = 0xFF0F512F
    const val ON_PRIMARY_CONTAINER = 0xFFB0F1C3
    const val SECONDARY = 0xFFB6CCB9
    const val ON_SECONDARY = 0xFF223527
    const val SECONDARY_CONTAINER = 0xFF384B3D
    const val ON_SECONDARY_CONTAINER = 0xFFD1E8D4
    const val TERTIARY = 0xFFA3CDDA
    const val ON_TERTIARY = 0xFF023640
    const val TERTIARY_CONTAINER = 0xFF214C57
    const val ON_TERTIARY_CONTAINER = 0xFFBEEAF7
    const val SURFACE_CONTAINER_LOW = 0xFF181D19
    const val SURFACE_CONTAINER = 0xFF1C211C
    const val SURFACE_CONTAINER_HIGH = 0xFF262B27
    const val ON_SURFACE = 0xFFDFE4DD
    const val ON_SURFACE_VARIANT = 0xFFC0C9BF
    const val OUTLINE = 0xFF8B938A
    const val OUTLINE_VARIANT = 0xFF414942
    const val BACKGROUND = 0xFF000000
    const val ERROR = 0xFFFFB4AB
    const val ON_ERROR = 0xFF690005
    const val ERROR_CONTAINER = 0xFF93000A
    const val ON_ERROR_CONTAINER = 0xFFFFDAD6

    /** For the app's Compose UI. */
    val compose = ColorScheme(
        primary = Color(PRIMARY),
        primaryDim = Color(PRIMARY),
        primaryContainer = Color(PRIMARY_CONTAINER),
        onPrimary = Color(ON_PRIMARY),
        onPrimaryContainer = Color(ON_PRIMARY_CONTAINER),
        secondary = Color(SECONDARY),
        secondaryDim = Color(SECONDARY),
        secondaryContainer = Color(SECONDARY_CONTAINER),
        onSecondary = Color(ON_SECONDARY),
        onSecondaryContainer = Color(ON_SECONDARY_CONTAINER),
        tertiary = Color(TERTIARY),
        tertiaryDim = Color(TERTIARY),
        tertiaryContainer = Color(TERTIARY_CONTAINER),
        onTertiary = Color(ON_TERTIARY),
        onTertiaryContainer = Color(ON_TERTIARY_CONTAINER),
        surfaceContainerLow = Color(SURFACE_CONTAINER_LOW),
        surfaceContainer = Color(SURFACE_CONTAINER),
        surfaceContainerHigh = Color(SURFACE_CONTAINER_HIGH),
        onSurface = Color(ON_SURFACE),
        onSurfaceVariant = Color(ON_SURFACE_VARIANT),
        outline = Color(OUTLINE),
        outlineVariant = Color(OUTLINE_VARIANT),
        background = Color(BACKGROUND),
        onBackground = Color(ON_SURFACE),
        error = Color(ERROR),
        errorDim = Color(ERROR),
        errorContainer = Color(ERROR_CONTAINER),
        onError = Color(ON_ERROR),
        onErrorContainer = Color(ON_ERROR_CONTAINER),
    )

    /** For the tile (protolayout-material3), used with dynamic theming off. */
    val tile = androidx.wear.protolayout.material3.ColorScheme(
        primary = PRIMARY.toInt().argb,
        primaryDim = PRIMARY.toInt().argb,
        primaryContainer = PRIMARY_CONTAINER.toInt().argb,
        onPrimary = ON_PRIMARY.toInt().argb,
        onPrimaryContainer = ON_PRIMARY_CONTAINER.toInt().argb,
        secondary = SECONDARY.toInt().argb,
        secondaryDim = SECONDARY.toInt().argb,
        secondaryContainer = SECONDARY_CONTAINER.toInt().argb,
        onSecondary = ON_SECONDARY.toInt().argb,
        onSecondaryContainer = ON_SECONDARY_CONTAINER.toInt().argb,
        tertiary = TERTIARY.toInt().argb,
        tertiaryDim = TERTIARY.toInt().argb,
        tertiaryContainer = TERTIARY_CONTAINER.toInt().argb,
        onTertiary = ON_TERTIARY.toInt().argb,
        onTertiaryContainer = ON_TERTIARY_CONTAINER.toInt().argb,
        surfaceContainerLow = SURFACE_CONTAINER_LOW.toInt().argb,
        surfaceContainer = SURFACE_CONTAINER.toInt().argb,
        surfaceContainerHigh = SURFACE_CONTAINER_HIGH.toInt().argb,
        onSurface = ON_SURFACE.toInt().argb,
        onSurfaceVariant = ON_SURFACE_VARIANT.toInt().argb,
        outline = OUTLINE.toInt().argb,
        outlineVariant = OUTLINE_VARIANT.toInt().argb,
        background = BACKGROUND.toInt().argb,
        onBackground = ON_SURFACE.toInt().argb,
        error = ERROR.toInt().argb,
        errorDim = ERROR.toInt().argb,
        errorContainer = ERROR_CONTAINER.toInt().argb,
        onError = ON_ERROR.toInt().argb,
        onErrorContainer = ON_ERROR_CONTAINER.toInt().argb,
    )
}
