package com.foodtracker.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.IntOffset
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.cache.DayCacheStore
import com.foodtracker.app.data.cache.DayRepository
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.ui.AppState
import com.foodtracker.app.ui.CaptureScreen
import com.foodtracker.app.ui.GoalsScreen
import com.foodtracker.app.ui.MainScreen
import com.foodtracker.app.ui.ReviewScreen
import com.foodtracker.app.ui.Screen
import com.foodtracker.app.ui.TransitionDirection
import com.foodtracker.app.ui.components.DesignSampleScreen
import com.foodtracker.app.ui.components.LocalReduceMotion
import com.foodtracker.app.ui.history.DayDetailScreen
import com.foodtracker.app.ui.reminders.RemindersScreen
import com.foodtracker.app.ui.settings.AppEnvironment
import com.foodtracker.app.ui.settings.AppPreferences
import com.foodtracker.app.ui.settings.SettingsScreen
import com.foodtracker.app.ui.settings.deviceLanguage
import com.foodtracker.app.ui.settings.resolveDarkTheme
import com.foodtracker.app.ui.settings.resolveLanguage
import com.foodtracker.app.ui.systemAnimationsDisabled
import com.foodtracker.app.ui.theme.FoodTrackerTheme
import com.foodtracker.app.ui.transitionDirection

/**
 * [cacheStore] is the platform's local database for the days already seen (offline-cache).
 * [openCaptureRequests] is bumped by the platform each time a meal reminder is tapped; every new
 * value asks to open Capture (see AppState.openCaptureFromReminder).
 */
@Composable
fun App(
    cacheStore: DayCacheStore,
    preferences: AppPreferences = AppPreferences(),
    onPreferencesChange: (AppPreferences) -> Unit = {},
    openCaptureRequests: Int = 0,
) {
    val apiClient = remember { FoodTrackerApiClient(createHttpClient()) }
    val dayRepository = remember(cacheStore) { DayRepository(apiClient, cacheStore) }
    // Outside AppEnvironment's language key, so switching language keeps the current screen.
    val appState = remember { AppState() }
    val language = resolveLanguage(preferences.language, deviceLanguage())
    val darkTheme = resolveDarkTheme(preferences.theme, isSystemInDarkTheme())

    LaunchedEffect(openCaptureRequests) {
        if (openCaptureRequests > 0) appState.openCaptureFromReminder()
    }

    AppEnvironment(language) {
        FoodTrackerTheme(darkTheme = darkTheme) {
            val reduceMotion = systemAnimationsDisabled()
            CompositionLocalProvider(LocalReduceMotion provides reduceMotion) {
                // Keyed by screen type, so a tab change inside Main isn't a screen transition.
                AnimatedContent(
                    targetState = appState.screen,
                    contentKey = { it::class },
                    transitionSpec = { screenTransition(reduceMotion) },
                    label = "screen",
                ) { screen ->
                    when (screen) {
                        is Screen.Capture -> CaptureScreen(
                            apiClient = apiClient,
                            onAnalyzed = { analysis -> appState.openReview(analysis) },
                            onCancel = { appState.openMain() },
                        )

                        is Screen.Review -> {
                            // Captured on entry: leaving clears pendingAnalysis, and Review must keep its
                            // content while it animates out.
                            val analysis = remember { appState.pendingAnalysis }
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
                            dayRepository = dayRepository,
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
                            dayRepository = dayRepository,
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
    }
}

/**
 * Deeper screens slide in from the right over a fade, going back slides them out to the right,
 * and screens at the same depth cross-fade. Instant under reduce motion.
 */
private fun AnimatedContentTransitionScope<Screen>.screenTransition(reduceMotion: Boolean): ContentTransform {
    if (reduceMotion) return EnterTransition.None togetherWith ExitTransition.None
    val slide = spring(stiffness = Spring.StiffnessMediumLow, visibilityThreshold = IntOffset.VisibilityThreshold)
    val fade = tween<Float>(durationMillis = 220)
    return when (transitionDirection(initialState, targetState)) {
        TransitionDirection.FORWARD ->
            (slideInHorizontally(slide) { it / 3 } + fadeIn(fade)) togetherWith (slideOutHorizontally(slide) { -it / 8 } + fadeOut(fade))
        // The leaving (deeper) screen stays on top while it slides away.
        TransitionDirection.BACK ->
            ((slideInHorizontally(slide) { -it / 8 } + fadeIn(fade)) togetherWith (slideOutHorizontally(slide) { it / 3 } + fadeOut(fade)))
                .apply { targetContentZIndex = -1f }
        TransitionDirection.FADE -> fadeIn(fade) togetherWith fadeOut(fade)
    }
}
