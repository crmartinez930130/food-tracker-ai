package com.foodtracker.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.ui.AppState
import com.foodtracker.app.ui.CaptureScreen
import com.foodtracker.app.ui.MainScreen
import com.foodtracker.app.ui.MainTab
import com.foodtracker.app.ui.ReviewScreen
import com.foodtracker.app.ui.Screen

@Composable
fun App() {
    val apiClient = remember { FoodTrackerApiClient(createHttpClient()) }
    val appState = remember { AppState() }

    MaterialTheme {
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
                        onSaved = { appState.openMain(MainTab.HISTORY) },
                        onCancel = { appState.openMain() },
                    )
                }
            }

            is Screen.Main -> MainScreen(
                initialTab = screen.tab,
                apiClient = apiClient,
                onStartCapture = { appState.openCapture() },
            )
        }
    }
}
