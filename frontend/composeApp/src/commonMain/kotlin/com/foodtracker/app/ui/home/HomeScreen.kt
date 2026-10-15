package com.foodtracker.app.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.cache.DayRepository
import com.foodtracker.app.ui.longDate
import com.foodtracker.app.util.currentLocalDateTime
import com.foodtracker.app.util.millisUntilNextMinute
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.edit_daily_goal
import food_tracker_frontend.composeapp.generated.resources.greeting_afternoon
import food_tracker_frontend.composeapp.generated.resources.greeting_evening
import food_tracker_frontend.composeapp.generated.resources.greeting_morning
import food_tracker_frontend.composeapp.generated.resources.ic_edit
import food_tracker_frontend.composeapp.generated.resources.ic_settings
import food_tracker_frontend.composeapp.generated.resources.open_options
import kotlinx.coroutines.delay
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Home: always today, under a greeting header. */
@Composable
fun HomeScreen(
    apiClient: FoodTrackerApiClient,
    dayRepository: DayRepository,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = rememberCurrentMinute()
    val today = now.date.toString()

    Column(modifier = modifier.fillMaxSize()) {
        HomeHeader(now = now, onEditGoal = onEditGoal, onOpenSettings = onOpenSettings)
        DayContent(apiClient = apiClient, dayRepository = dayRepository, date = today, today = today, onEditGoal = onEditGoal, modifier = Modifier.weight(1f))
    }
}

/**
 * The local date-time, refreshed at every whole minute and on every resume. The resume re-read
 * matters: Android's delay runs on uptime, which stops in deep sleep, so a wait started in the
 * background can end late - and the day may have changed meanwhile.
 */
@Composable
private fun rememberCurrentMinute(): LocalDateTime {
    var now by remember { mutableStateOf(currentLocalDateTime()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(millisUntilNextMinute(currentLocalDateTime()))
            now = currentLocalDateTime()
        }
    }
    LifecycleResumeEffect(Unit) {
        now = currentLocalDateTime()
        onPauseOrDispose {}
    }
    return now
}

@Composable
private fun HomeHeader(
    now: LocalDateTime,
    onEditGoal: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 8.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(
                    when (greetingFor(now.hour)) {
                        Greeting.MORNING -> Res.string.greeting_morning
                        Greeting.AFTERNOON -> Res.string.greeting_afternoon
                        Greeting.EVENING -> Res.string.greeting_evening
                    },
                ),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                longDate(now.date),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenSettings) {
            Icon(painterResource(Res.drawable.ic_settings), contentDescription = stringResource(Res.string.open_options))
        }
        IconButton(onClick = onEditGoal) {
            Icon(painterResource(Res.drawable.ic_edit), contentDescription = stringResource(Res.string.edit_daily_goal))
        }
    }
}
