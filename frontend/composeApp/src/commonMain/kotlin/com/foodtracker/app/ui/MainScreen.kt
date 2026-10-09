package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
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
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_add
import food_tracker_frontend.composeapp.generated.resources.ic_bar_chart
import food_tracker_frontend.composeapp.generated.resources.ic_flag
import food_tracker_frontend.composeapp.generated.resources.ic_list_alt
import org.jetbrains.compose.resources.painterResource

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
            FloatingActionButton(onClick = onStartCapture) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = "Log a meal")
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { tab = MainTab.HISTORY },
                    icon = { Icon(painterResource(Res.drawable.ic_list_alt), contentDescription = null) },
                    label = { Text("History") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.SUMMARY,
                    onClick = { tab = MainTab.SUMMARY },
                    icon = { Icon(painterResource(Res.drawable.ic_bar_chart), contentDescription = null) },
                    label = { Text("Summary") },
                )
                NavigationBarItem(
                    selected = tab == MainTab.GOALS,
                    onClick = { tab = MainTab.GOALS },
                    icon = { Icon(painterResource(Res.drawable.ic_flag), contentDescription = null) },
                    label = { Text("Goals") },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            when (tab) {
                MainTab.HISTORY -> HistoryScreen(apiClient)
                MainTab.SUMMARY -> SummaryScreen(apiClient)
                MainTab.GOALS -> GoalsScreen(apiClient)
            }
        }
    }
}
