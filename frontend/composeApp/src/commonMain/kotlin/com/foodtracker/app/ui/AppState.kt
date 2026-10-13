package com.foodtracker.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodtracker.app.data.MealAnalysis

enum class MainTab { HOME, SUMMARY }

sealed interface Screen {
    data object Capture : Screen
    data object Review : Screen
    data object Goals : Screen
    data object Settings : Screen
    data object DesignSample : Screen
    data object Reminders : Screen
    data class Main(val tab: MainTab = MainTab.HOME) : Screen
}

/** Owns which screen is showing and the in-flight analysis being reviewed. */
class AppState {
    var screen: Screen by mutableStateOf(Screen.Main())
        private set

    var pendingAnalysis: MealAnalysis? by mutableStateOf(null)
        private set

    fun openCapture() {
        screen = Screen.Capture
    }

    fun openReview(analysis: MealAnalysis) {
        pendingAnalysis = analysis
        screen = Screen.Review
    }

    fun openGoals() {
        screen = Screen.Goals
    }

    fun openSettings() {
        screen = Screen.Settings
    }

    /** Debug-only tool, reached from Options. */
    fun openDesignSample() {
        screen = Screen.DesignSample
    }

    /** Reminders live inside Options, so its back returns there (see [openSettings]). */
    fun openReminders() {
        screen = Screen.Reminders
    }

    /**
     * A tapped reminder notification: go to Capture, unless the user is already capturing or
     * reviewing a meal - that work (and its pending analysis) must never be thrown away.
     */
    fun openCaptureFromReminder() {
        when (screen) {
            is Screen.Main, Screen.Goals, Screen.Settings, Screen.Reminders, Screen.DesignSample -> openCapture()
            Screen.Capture, Screen.Review -> Unit
        }
    }

    fun openMain(tab: MainTab = MainTab.HOME) {
        pendingAnalysis = null
        screen = Screen.Main(tab)
    }
}
