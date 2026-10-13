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
import androidx.compose.ui.Modifier
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.ui.history.HistoryScreen
import com.foodtracker.app.ui.home.HomeScreen
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_log_meal
import food_tracker_frontend.composeapp.generated.resources.ic_add
import food_tracker_frontend.composeapp.generated.resources.ic_bar_chart
import food_tracker_frontend.composeapp.generated.resources.ic_calendar_month
import food_tracker_frontend.composeapp.generated.resources.ic_home
import food_tracker_frontend.composeapp.generated.resources.nav_history
import food_tracker_frontend.composeapp.generated.resources.nav_home
import food_tracker_frontend.composeapp.generated.resources.nav_summary
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun MainScreen(
    tab: MainTab,
    onTabChange: (MainTab) -> Unit,
    apiClient: FoodTrackerApiClient,
    historyMonth: LocalDate,
    onHistoryMonthChange: (LocalDate) -> Unit,
    onOpenDay: (String) -> Unit,
    onStartCapture: () -> Unit,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    onClick = { onTabChange(MainTab.HOME) },
                    icon = { Icon(painterResource(Res.drawable.ic_home), contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_home)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.HISTORY,
                    onClick = { onTabChange(MainTab.HISTORY) },
                    icon = { Icon(painterResource(Res.drawable.ic_calendar_month), contentDescription = null) },
                    label = { Text(stringResource(Res.string.nav_history)) },
                )
                NavigationBarItem(
                    selected = tab == MainTab.SUMMARY,
                    onClick = { onTabChange(MainTab.SUMMARY) },
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
                    onEditGoal = onEditGoal,
                    onOpenSettings = onOpenSettings,
                )
                MainTab.HISTORY -> HistoryScreen(
                    apiClient = apiClient,
                    month = historyMonth,
                    onMonthChange = onHistoryMonthChange,
                    onOpenDay = onOpenDay,
                )
                MainTab.SUMMARY -> SummaryScreen(apiClient)
            }
        }
    }
}
