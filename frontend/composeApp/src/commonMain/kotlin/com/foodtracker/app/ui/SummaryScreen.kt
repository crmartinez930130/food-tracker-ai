package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyTotals
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.ui.summary.CalorieChart
import com.foodtracker.app.ui.summary.isOverGoal
import com.foodtracker.app.util.isBeforeDay
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

/** Rolling windows ending on the selected day (see the add-trend-charts design). */
private enum class SummaryPeriod(val days: Int, val label: String) {
    WEEK(7, "Week"),
    MONTH(30, "Month"),
}

/** Trends over a week or a month. The single-day view lives on Home. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(apiClient: FoodTrackerApiClient, modifier: Modifier = Modifier) {
    val today = todayDateString()
    var period by rememberSaveable { mutableStateOf(SummaryPeriod.WEEK) }
    var endDate by rememberSaveable { mutableStateOf(today) }
    val startDate = endDate.plusDays(-(period.days - 1))
    var days by remember { mutableStateOf<List<DailyTotals>>(emptyList()) }
    var goalCalories by remember { mutableStateOf(0.0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(startDate, endDate) {
        isLoading = true
        errorMessage = null
        try {
            days = apiClient.rangeSummary(startDate, endDate)
            goalCalories = apiClient.getGoals().targetCalories
        } catch (e: Exception) {
            errorMessage = "Couldn't load summary: ${e.message ?: "unknown error"}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SummaryPeriod.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = period == option,
                    onClick = { period = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SummaryPeriod.entries.size),
                ) { Text(option.label) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { endDate = endDate.plusDays(-period.days) }) {
                Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = "Previous ${period.label.lowercase()}")
            }
            Text(
                "$startDate – $endDate",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(
                // Never past today; a jump that would overshoot lands on today instead.
                onClick = { endDate = endDate.plusDays(period.days).let { if (today.isBeforeDay(it)) today else it } },
                enabled = endDate.isBeforeDay(today),
            ) {
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = "Next ${period.label.lowercase()}")
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            isLoading && days.isEmpty() -> CircularProgressIndicator()
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            else -> {
                CalorieChart(days = days, goalCalories = goalCalories)
                Spacer(Modifier.height(16.dp))
                PeriodAverages(days = days, goalCalories = goalCalories)
            }
        }
    }
}

@Composable
private fun PeriodAverages(days: List<DailyTotals>, goalCalories: Double, modifier: Modifier = Modifier) {
    val count = days.size.coerceAtLeast(1)
    // Empty days count as zero, matching the backend's weekly average semantics.
    val totalCalories = days.sumOf { it.totalCalories }
    val overGoal = days.count { isOverGoal(it.totalCalories, goalCalories) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text("Daily average", style = MaterialTheme.typography.titleMedium)
        Text("${(totalCalories / count).roundToInt()} kcal", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Protein ${(days.sumOf { it.totalProteinGrams } / count).roundToInt()} g · " +
                "Fat ${(days.sumOf { it.totalFatGrams } / count).roundToInt()} g · " +
                "Carbs ${(days.sumOf { it.totalCarbsGrams } / count).roundToInt()} g",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "${totalCalories.roundToInt()} kcal over ${days.size} days · $overGoal over goal",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
