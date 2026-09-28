package com.foodtracker.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodtracker.app.data.MealAnalysis

enum class MainTab { HISTORY, SUMMARY }

sealed interface Screen {
    data object Capture : Screen
    data object Review : Screen
    data class Main(val tab: MainTab = MainTab.HISTORY) : Screen
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

    fun openMain(tab: MainTab = MainTab.HISTORY) {
        pendingAnalysis = null
        screen = Screen.Main(tab)
    }
}
