package com.foodtracker.app.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.ui.history.firstOfMonth
import com.foodtracker.app.ui.summary.SummaryView
import com.foodtracker.app.util.currentLocalDateTime
import kotlinx.datetime.LocalDate

enum class MainTab { HOME, HISTORY, SUMMARY }

sealed interface Screen {
    data object Capture : Screen
    data object Review : Screen
    data object Goals : Screen
    data object Settings : Screen
    data object DesignSample : Screen
    data object Reminders : Screen
    data class Main(val tab: MainTab = MainTab.HOME) : Screen

    /** One day's full content; back returns to [returnTo] (History, or Summary from #44). */
    data class DayDetail(val date: String, val returnTo: Screen) : Screen
}

/**
 * Owns which screen is showing, the in-flight analysis being reviewed, and the navigation state
 * that has to outlive a screen (where Goals returns to, which month History shows).
 */
class AppState {
    var screen: Screen by mutableStateOf(Screen.Main())
        private set

    var pendingAnalysis: MealAnalysis? by mutableStateOf(null)
        private set

    /** Where Goals' back goes: the screen it was opened from (Home or a day's detail). */
    private var goalsReturnTo: Screen = Screen.Main()

    /** The 1st of the month History shows; kept here so it survives opening a day and coming back. */
    var historyMonth: LocalDate by mutableStateOf(firstOfMonth(currentLocalDateTime().date))

    /** Summary's week/month and period; kept here so opening a day from its chart and coming back keeps them. */
    var summaryView: SummaryView by mutableStateOf(SummaryView())

    fun openCapture() {
        screen = Screen.Capture
    }

    fun openReview(analysis: MealAnalysis) {
        pendingAnalysis = analysis
        screen = Screen.Review
    }

    fun openGoals() {
        goalsReturnTo = screen
        screen = Screen.Goals
    }

    fun closeGoals() {
        screen = goalsReturnTo
    }

    /** Opens [date]'s detail; its back returns to the current screen (e.g. Main on History). */
    fun openDayDetail(date: String) {
        screen = Screen.DayDetail(date, returnTo = screen)
    }

    fun closeDayDetail() {
        (screen as? Screen.DayDetail)?.let { screen = it.returnTo }
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
            is Screen.Main, is Screen.DayDetail, Screen.Goals, Screen.Settings, Screen.Reminders, Screen.DesignSample -> openCapture()
            Screen.Capture, Screen.Review -> Unit
        }
    }

    fun openMain(tab: MainTab = MainTab.HOME) {
        pendingAnalysis = null
        screen = Screen.Main(tab)
    }

    /** A tab picked in Main's bottom navigation; kept in [screen] so going back restores it. */
    fun selectMainTab(tab: MainTab) {
        if (screen is Screen.Main) screen = Screen.Main(tab)
    }
}
