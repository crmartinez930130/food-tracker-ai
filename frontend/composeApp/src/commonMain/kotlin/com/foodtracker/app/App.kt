package com.foodtracker.app

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.ui.AppState
import com.foodtracker.app.ui.CaptureScreen
import com.foodtracker.app.ui.GoalsScreen
import com.foodtracker.app.ui.MainScreen
import com.foodtracker.app.ui.ReviewScreen
import com.foodtracker.app.ui.Screen
import com.foodtracker.app.ui.components.DesignSampleScreen
import com.foodtracker.app.ui.history.DayDetailScreen
import com.foodtracker.app.ui.reminders.RemindersScreen
import com.foodtracker.app.ui.settings.AppEnvironment
import com.foodtracker.app.ui.settings.AppPreferences
import com.foodtracker.app.ui.settings.SettingsScreen
import com.foodtracker.app.ui.settings.deviceLanguage
import com.foodtracker.app.ui.settings.resolveDarkTheme
import com.foodtracker.app.ui.settings.resolveLanguage
import com.foodtracker.app.ui.theme.FoodTrackerTheme

/**
 * [openCaptureRequests] is bumped by the platform each time a meal reminder is tapped; every new
 * value asks to open Capture (see AppState.openCaptureFromReminder).
 */
@Composable
fun App(
    preferences: AppPreferences = AppPreferences(),
    onPreferencesChange: (AppPreferences) -> Unit = {},
    openCaptureRequests: Int = 0,
) {
    val apiClient = remember { FoodTrackerApiClient(createHttpClient()) }
    // Outside AppEnvironment's language key, so switching language keeps the current screen.
    val appState = remember { AppState() }
    val language = resolveLanguage(preferences.language, deviceLanguage())
    val darkTheme = resolveDarkTheme(preferences.theme, isSystemInDarkTheme())

    LaunchedEffect(openCaptureRequests) {
        if (openCaptureRequests > 0) appState.openCaptureFromReminder()
    }

    AppEnvironment(language) {
        FoodTrackerTheme(darkTheme = darkTheme) {
            when (val screen = appState.screen) {
                is Screen.Capture -> CaptureScreen(
                    apiClient = apiClient,
                    onAnalyzed = { analysis -> appState.openReview(analysis) },
                    onCancel = { appState.openMain() },
                )

                is Screen.Review -> {
                    val analysis = appState.pendingAnalysis
                    if (analysis != null) {
                        ReviewScreen(
                            initialAnalysis = analysis,
                            apiClient = apiClient,
                            onSaved = { appState.openMain() },
                            onCancel = { appState.openMain() },
                        )
                    }
                }

                is Screen.Goals -> GoalsScreen(
                    apiClient = apiClient,
                    onBack = { appState.closeGoals() },
                )

                is Screen.DayDetail -> DayDetailScreen(
                    apiClient = apiClient,
                    date = screen.date,
                    onBack = { appState.closeDayDetail() },
                    onEditGoal = { appState.openGoals() },
                )

                is Screen.Settings -> SettingsScreen(
                    preferences = preferences,
                    onPreferencesChange = onPreferencesChange,
                    onOpenReminders = { appState.openReminders() },
                    onOpenDesignSample = { appState.openDesignSample() },
                    onBack = { appState.openMain() },
                )

                is Screen.DesignSample -> DesignSampleScreen(onBack = { appState.openSettings() })

                is Screen.Reminders -> RemindersScreen(onBack = { appState.openSettings() })

                is Screen.Main -> MainScreen(
                    tab = screen.tab,
                    onTabChange = { appState.selectMainTab(it) },
                    apiClient = apiClient,
                    historyMonth = appState.historyMonth,
                    onHistoryMonthChange = { appState.historyMonth = it },
                    summaryView = appState.summaryView,
                    onSummaryViewChange = { appState.summaryView = it },
                    onOpenDay = { appState.openDayDetail(it) },
                    onStartCapture = { appState.openCapture() },
                    onEditGoal = { appState.openGoals() },
                    onOpenSettings = { appState.openSettings() },
                )
            }
        }
    }
}
