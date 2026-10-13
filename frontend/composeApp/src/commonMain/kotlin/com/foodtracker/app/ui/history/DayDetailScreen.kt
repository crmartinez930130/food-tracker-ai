package com.foodtracker.app.ui.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.ui.home.DayContent
import com.foodtracker.app.ui.longDate
import com.foodtracker.app.util.currentLocalDateTime
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.action_back
import food_tracker_frontend.composeapp.generated.resources.ic_arrow_back
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** One day's full content and actions, as Home shows today (opened from History, or Summary in #44). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun DayDetailScreen(
    apiClient: FoodTrackerApiClient,
    date: String,
    onBack: () -> Unit,
    onEditGoal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBack)
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(longDate(LocalDate.parse(date))) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = stringResource(Res.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        DayContent(
            apiClient = apiClient,
            date = date,
            today = currentLocalDateTime().date.toString(),
            onEditGoal = onEditGoal,
            modifier = Modifier.padding(padding),
        )
    }
}
