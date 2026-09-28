package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.foodtracker.app.data.FoodTrackerApiClient

@Composable
fun MainScreen(
    initialTab: MainTab,
    apiClient: FoodTrackerApiClient,
    onStartCapture: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onStartCapture) { Text("+") }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { tab = MainTab.HISTORY },
                    icon = { Text("📋") },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.SUMMARY,
                    onClick = { tab = MainTab.SUMMARY },
                    icon = { Text("📊") },
                    label = { Text("Summary") },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            when (tab) {
                MainTab.HISTORY -> HistoryScreen(apiClient)
                MainTab.SUMMARY -> SummaryScreen(apiClient)
            }
        }
    }
}
