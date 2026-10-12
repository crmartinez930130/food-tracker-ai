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
import com.foodtracker.app.ui.home.HomeScreen
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_log_meal
import food_tracker_frontend.composeapp.generated.resources.ic_add
import food_tracker_frontend.composeapp.generated.resources.ic_bar_chart
import food_tracker_frontend.composeapp.generated.resources.ic_home
import food_tracker_frontend.composeapp.generated.resources.nav_home
import food_tracker_frontend.composeapp.generated.resources.nav_summary
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScreen(
    initialTab: MainTab,
    apiClient: FoodTrackerApiClient,
    homeDate: String,
    onHomeDateChange: (String) -> Unit,
    onStartCapture: () -> Unit,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var tab by remember(initialTab) { mutableStateOf(initialTab) }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = onStartCapture) {
                Icon(painterResource(Res.drawable.ic_add), contentDescription = stringResource(Res.string.action_log_meal))
            }
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == MainTab.HOME,
                    onClick = { tab = MainTab.HOME },
                    icon = { Icon(painterResource(Res.drawable.ic_home), contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.SUMMARY,
                    onClick = { tab = MainTab.SUMMARY },
                    icon = { Icon(painterResource(Res.drawable.ic_bar_chart), contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_summary)) },
                )
            }
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            when (tab) {
                MainTab.HOME -> HomeScreen(
                    apiClient = apiClient,
                    date = homeDate,
                    onDateChange = onHomeDateChange,
                    onEditGoal = onEditGoal,
                    onOpenSettings = onOpenSettings,
                )
                MainTab.SUMMARY -> SummaryScreen(apiClient)
            }
        }
    }
}
