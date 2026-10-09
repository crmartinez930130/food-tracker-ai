package com.foodtracker.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.util.todayDateString

enum class MainTab { HOME, SUMMARY }

sealed interface Screen {
    data object Capture : Screen
    data object Review : Screen
    data object Goals : Screen
    data class Main(val tab: MainTab = MainTab.HOME) : Screen
}

/** Owns which screen is showing, the in-flight analysis being reviewed, and the day Home shows. */
class AppState {
    var screen: Screen by mutableStateOf(Screen.Main())
        private set

    var pendingAnalysis: MealAnalysis? by mutableStateOf(null)
        private set

    /**
     * Lives here rather than in HomeScreen so it survives leaving Main (e.g. opening Goals and
     * coming back), which disposes the Home composable.
     */
    var homeDate: String by mutableStateOf(todayDateString())

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

    fun openMain(tab: MainTab = MainTab.HOME) {
        pendingAnalysis = null
        screen = Screen.Main(tab)
    }

    /** After saving a meal: it's logged "now", so show it on today's Home. */
    fun openHomeAfterSave() {
        homeDate = todayDateString()
        openMain(MainTab.HOME)
    }
}
