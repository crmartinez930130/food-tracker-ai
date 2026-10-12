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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyTotals
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.LogWeightRequest
import com.foodtracker.app.data.WeightEntry
import com.foodtracker.app.ui.summary.CalorieChart
import com.foodtracker.app.ui.summary.ExportSection
import com.foodtracker.app.ui.summary.WeightSection
import com.foodtracker.app.ui.summary.isOverGoal
import com.foodtracker.app.util.isBeforeDay
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.error_load_summary
import food_tracker_frontend.composeapp.generated.resources.error_load_weight
import food_tracker_frontend.composeapp.generated.resources.error_save_weight
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import food_tracker_frontend.composeapp.generated.resources.summary_average_macros
import food_tracker_frontend.composeapp.generated.resources.summary_daily_average
import food_tracker_frontend.composeapp.generated.resources.summary_month
import food_tracker_frontend.composeapp.generated.resources.summary_next_month
import food_tracker_frontend.composeapp.generated.resources.summary_next_week
import food_tracker_frontend.composeapp.generated.resources.summary_period_totals
import food_tracker_frontend.composeapp.generated.resources.summary_previous_month
import food_tracker_frontend.composeapp.generated.resources.summary_previous_week
import food_tracker_frontend.composeapp.generated.resources.summary_range
import food_tracker_frontend.composeapp.generated.resources.summary_week
import food_tracker_frontend.composeapp.generated.resources.unit_kcal
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Rolling windows ending on the selected day (see the add-trend-charts design). */
private enum class SummaryPeriod(
    val days: Int,
    val label: StringResource,
    val previousLabel: StringResource,
    val nextLabel: StringResource,
) {
    WEEK(7, Res.string.summary_week, Res.string.summary_previous_week, Res.string.summary_next_week),
    MONTH(30, Res.string.summary_month, Res.string.summary_previous_month, Res.string.summary_next_month),
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
    var weightEntries by remember { mutableStateOf<List<WeightEntry>>(emptyList()) }
    // The days the loaded entries belong to; kept with them so a stale (dimmed) chart still lines up.
    var weightDays by remember { mutableStateOf<List<String>>(emptyList()) }
    var lastKnownWeight by remember { mutableStateOf<Double?>(null) }
    var weightError by remember { mutableStateOf<String?>(null) }
    var isWeightLoading by remember { mutableStateOf(true) }
    // An empty list is a valid result, so "has content" means "loaded at least once".
    var weightLoadedOnce by remember { mutableStateOf(false) }
    var weightReloadKey by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val periodDays = (0 until period.days).map { startDate.plusDays(it) }

    LaunchedEffect(startDate, endDate) {
        isLoading = true
        errorMessage = null
        try {
            days = apiClient.rangeSummary(startDate, endDate)
            goalCalories = apiClient.getGoals().targetCalories
        } catch (e: Exception) {
            errorMessage = errorText(Res.string.error_load_summary, e)
        } finally {
            isLoading = false
        }
    }

    // Weight reloads on its own key, so logging or deleting doesn't refetch the calorie chart.
    LaunchedEffect(startDate, endDate, weightReloadKey) {
        weightError = null
        isWeightLoading = true
        try {
            val entries = apiClient.listWeight(startDate, endDate)
            weightEntries = entries
            weightDays = periodDays
            // For the log dialog's pre-fill: the latest entry overall. When the period ends today and
            // has entries, its last one already is (there are no future entries), so skip the lookup.
            lastKnownWeight = if (endDate == today && entries.isNotEmpty()) {
                entries.last().weightKg
            } else {
                apiClient.listWeight(today.plusDays(-365), today).lastOrNull()?.weightKg
            }
            weightLoadedOnce = true
        } catch (e: Exception) {
            weightError = errorText(Res.string.error_load_weight, e)
        } finally {
            isWeightLoading = false
        }
    }

    fun changeWeight(action: suspend () -> Unit) {
        scope.launch {
            try {
                action()
                weightReloadKey++
            } catch (e: Exception) {
                weightError = errorText(Res.string.error_save_weight, e)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            SummaryPeriod.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = period == option,
                    onClick = { period = option },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = SummaryPeriod.entries.size),
                ) { Text(stringResource(option.label)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { endDate = endDate.plusDays(-period.days) }) {
                Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = stringResource(period.previousLabel))
            }
            Text(
                stringResource(Res.string.summary_range, startDate, endDate),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(
                // Never past today; a jump that would overshoot lands on today instead.
                onClick = { endDate = endDate.plusDays(period.days).let { if (today.isBeforeDay(it)) today else it } },
                enabled = endDate.isBeforeDay(today),
            ) {
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = stringResource(period.nextLabel))
            }
        }

        Spacer(Modifier.height(8.dp))

        when {
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            // Dims the previous period while the new one loads, instead of showing it unmarked.
            else -> LoadingAware(loadingPresentation(isLoading, hasContent = days.isNotEmpty())) {
                Column {
                    CalorieChart(days = days, goalCalories = goalCalories)
                    Spacer(Modifier.height(16.dp))
                    PeriodAverages(days = days, goalCalories = goalCalories)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        weightError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        LoadingAware(loadingPresentation(isWeightLoading, hasContent = weightLoadedOnce)) {
            WeightSection(
                periodDays = weightDays,
                entries = weightEntries,
                lastKnownWeight = lastKnownWeight,
                onLog = { kg -> changeWeight { apiClient.logWeight(LogWeightRequest(weightKg = kg)) } },
                onDelete = { entry -> changeWeight { apiClient.deleteWeight(entry.id) } },
            )
        }

        Spacer(Modifier.height(24.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(16.dp))
        ExportSection(apiClient = apiClient, periodStart = startDate, periodEnd = endDate, today = today)
    }
}

@Composable
private fun PeriodAverages(days: List<DailyTotals>, goalCalories: Double, modifier: Modifier = Modifier) {
    val count = days.size.coerceAtLeast(1)
    // Empty days count as zero, matching the backend's weekly average semantics.
    val totalCalories = days.sumOf { it.totalCalories }
    val overGoal = days.count { isOverGoal(it.totalCalories, goalCalories) }
    Column(modifier = modifier.fillMaxWidth()) {
        Text(stringResource(Res.string.summary_daily_average), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(Res.string.unit_kcal, (totalCalories / count).roundToInt()), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(
                Res.string.summary_average_macros,
                (days.sumOf { it.totalProteinGrams } / count).roundToInt(),
                (days.sumOf { it.totalFatGrams } / count).roundToInt(),
                (days.sumOf { it.totalCarbsGrams } / count).roundToInt(),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            pluralStringResource(Res.plurals.summary_period_totals, days.size, totalCalories.roundToInt(), days.size, overGoal),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
