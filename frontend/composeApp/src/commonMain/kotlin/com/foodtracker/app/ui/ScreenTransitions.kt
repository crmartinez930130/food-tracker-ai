package com.foodtracker.app.ui

import androidx.compose.runtime.Composable

/** How far a screen is from Main: deeper screens slide in from the right, going back slides out. */
fun screenDepth(screen: Screen): Int = when (screen) {
    is Screen.Main -> 0
    Screen.Capture, Screen.Goals, Screen.Settings, is Screen.DayDetail -> 1
    Screen.Review, Screen.Reminders, Screen.DesignSample -> 2
}

enum class TransitionDirection { FORWARD, BACK, FADE }

fun transitionDirection(from: Screen, to: Screen): TransitionDirection {
    val delta = screenDepth(to) - screenDepth(from)
    return when {
        delta > 0 -> TransitionDirection.FORWARD
        delta < 0 -> TransitionDirection.BACK
        else -> TransitionDirection.FADE
    }
}

/** Whether the system asks to remove animations (Android: animator duration scale 0). */
@Composable
expect fun systemAnimationsDisabled(): Boolean
