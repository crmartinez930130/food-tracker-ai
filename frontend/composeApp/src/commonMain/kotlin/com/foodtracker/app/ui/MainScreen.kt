package com.foodtracker.app.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.cache.DayRepository
import com.foodtracker.app.ui.components.LocalReduceMotion
import com.foodtracker.app.ui.components.pressBounce
import com.foodtracker.app.ui.history.HistoryScreen
import com.foodtracker.app.ui.home.HomeScreen
import com.foodtracker.app.ui.summary.SummaryView
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
    dayRepository: DayRepository,
    historyMonth: LocalDate,
    onHistoryMonthChange: (LocalDate) -> Unit,
    summaryView: SummaryView,
    onSummaryViewChange: (SummaryView) -> Unit,
    onOpenDay: (String) -> Unit,
    onStartCapture: () -> Unit,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            val fabInteraction = remember { MutableInteractionSource() }
            FloatingActionButton(
                onClick = onStartCapture,
                interactionSource = fabInteraction,
                modifier = Modifier.pressBounce(fabInteraction),
            ) {
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
            // Tabs cross-fade; under reduce motion they switch at once.
            Crossfade(
                targetState = tab,
                animationSpec = if (LocalReduceMotion.current) snap() else tween(durationMillis = 200),
                label = "mainTab",
            ) { shownTab ->
                when (shownTab) {
                    MainTab.HOME -> HomeScreen(
                        apiClient = apiClient,
                        dayRepository = dayRepository,
                        onEditGoal = onEditGoal,
                        onOpenSettings = onOpenSettings,
                    )
                    MainTab.HISTORY -> HistoryScreen(
                        apiClient = apiClient,
                        month = historyMonth,
                        onMonthChange = onHistoryMonthChange,
                        onOpenDay = onOpenDay,
                    )
                    MainTab.SUMMARY -> SummaryScreen(
                        apiClient = apiClient,
                        view = summaryView,
                        onViewChange = onSummaryViewChange,
                        onOpenDay = onOpenDay,
                    )
                }
            }
        }
    }
}
