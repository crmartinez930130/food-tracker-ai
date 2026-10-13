package com.foodtracker.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyTotals
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.LogWeightRequest
import com.foodtracker.app.data.WeightEntry
import com.foodtracker.app.ui.components.BarSegment
import com.foodtracker.app.ui.components.ConnectedButtonGroup
import com.foodtracker.app.ui.components.SegmentedBar
import com.foodtracker.app.ui.components.StatCard
import com.foodtracker.app.ui.summary.CalorieChart
import com.foodtracker.app.ui.summary.ExportSection
import com.foodtracker.app.ui.summary.PeriodStats
import com.foodtracker.app.ui.summary.SummaryPeriod
import com.foodtracker.app.ui.summary.SummaryView
import com.foodtracker.app.ui.summary.WeightSection
import com.foodtracker.app.ui.summary.periodLabel
import com.foodtracker.app.ui.summary.periodStats
import com.foodtracker.app.util.isBeforeDay
import com.foodtracker.app.util.plusDays
import com.foodtracker.app.util.todayDateString
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.error_load_summary
import food_tracker_frontend.composeapp.generated.resources.error_load_weight
import food_tracker_frontend.composeapp.generated.resources.error_save_weight
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.month_short_names
import food_tracker_frontend.composeapp.generated.resources.summary_average_macros_title
import food_tracker_frontend.composeapp.generated.resources.summary_daily_average
import food_tracker_frontend.composeapp.generated.resources.summary_month
import food_tracker_frontend.composeapp.generated.resources.summary_next_month
import food_tracker_frontend.composeapp.generated.resources.summary_next_week
import food_tracker_frontend.composeapp.generated.resources.summary_over_goal
import food_tracker_frontend.composeapp.generated.resources.summary_period_total
import food_tracker_frontend.composeapp.generated.resources.summary_previous_month
import food_tracker_frontend.composeapp.generated.resources.summary_previous_week
import food_tracker_frontend.composeapp.generated.resources.summary_week
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

private val SummaryPeriod.label: StringResource
    get() = if (this == SummaryPeriod.WEEK) Res.string.summary_week else Res.string.summary_month

private val SummaryPeriod.previousLabel: StringResource
    get() = if (this == SummaryPeriod.WEEK) Res.string.summary_previous_week else Res.string.summary_previous_month

private val SummaryPeriod.nextLabel: StringResource
    get() = if (this == SummaryPeriod.WEEK) Res.string.summary_next_week else Res.string.summary_next_month

/**
 * Trends over a week or a month. The single-day view lives on Home; tapping a chart bar opens
 * that day's detail through [onOpenDay]. [view] lives in AppState so it survives that round trip.
 */
@Composable
fun SummaryScreen(
    apiClient: FoodTrackerApiClient,
    view: SummaryView,
    onViewChange: (SummaryView) -> Unit,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = todayDateString()
    val period = view.period
    val endDate = view.endDate ?: today
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

    // Ending today is stored as null, so "this week" keeps following today.
    fun showPeriodEndingOn(date: String) {
        onViewChange(view.copy(endDate = date.takeIf { it.isBeforeDay(today) }))
    }

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

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ConnectedButtonGroup(
            options = SummaryPeriod.entries.map { stringResource(it.label) },
            selectedIndex = period.ordinal,
            onSelect = { onViewChange(view.copy(period = SummaryPeriod.entries[it])) },
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { showPeriodEndingOn(endDate.plusDays(-period.days)) }) {
                Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = stringResource(period.previousLabel))
            }
            Text(
                periodLabel(
                    LocalDate.parse(startDate),
                    LocalDate.parse(endDate),
                    currentYear = LocalDate.parse(today).year,
                    monthShort = stringArrayResource(Res.array.month_short_names),
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
            IconButton(
                // Never past today; a jump that would overshoot lands on today instead.
                onClick = { showPeriodEndingOn(endDate.plusDays(period.days).let { if (today.isBeforeDay(it)) today else it }) },
                enabled = endDate.isBeforeDay(today),
            ) {
                Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = stringResource(period.nextLabel))
            }
        }

        when {
            errorMessage != null -> Text(errorMessage.orEmpty(), color = MaterialTheme.colorScheme.error)
            // Dims the previous period while the new one loads, instead of showing it unmarked.
            else -> LoadingAware(loadingPresentation(isLoading, hasContent = days.isNotEmpty())) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCards(periodStats(days, goalCalories))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    ) {
                        CalorieChart(
                            days = days,
                            goalCalories = goalCalories,
                            onBarTap = { index -> onOpenDay(days[index].date) },
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }

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

        ExportSection(apiClient = apiClient, periodStart = startDate, periodEnd = endDate, today = today)
        Spacer(Modifier.height(72.dp))
    }
}

/** Daily average and days over the goal, the macro averages as a bar, and the period's total. */
@Composable
private fun StatCards(stats: PeriodStats, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            StatCard(
                label = stringResource(Res.string.summary_daily_average),
                value = "${stats.averageCalories.roundToInt()}",
                unit = "kcal",
                containerColor = colors.primaryContainer,
                modifier = Modifier.weight(1.4f),
            )
            StatCard(
                label = stringResource(Res.string.summary_over_goal),
                value = "${stats.daysOverGoal}",
                unit = "/ ${stats.dayCount}",
                containerColor = if (stats.daysOverGoal > 0) colors.errorContainer else colors.tertiaryContainer,
                modifier = Modifier.weight(1f),
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh, contentColor = colors.onSurface),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(Res.string.summary_average_macros_title), style = MaterialTheme.typography.labelLarge)
                SegmentedBar(
                    segments = listOf(
                        BarSegment(stringResource(Res.string.macro_protein), stats.averageProteinGrams, colors.primary, "${stats.averageProteinGrams.roundToInt()} g"),
                        BarSegment(stringResource(Res.string.macro_fat), stats.averageFatGrams, colors.tertiary, "${stats.averageFatGrams.roundToInt()} g"),
                        BarSegment(stringResource(Res.string.macro_carbs), stats.averageCarbsGrams, colors.secondary, "${stats.averageCarbsGrams.roundToInt()} g"),
                    ),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
        Text(
            stringResource(Res.string.summary_period_total, stats.totalCalories.roundToInt()),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}
