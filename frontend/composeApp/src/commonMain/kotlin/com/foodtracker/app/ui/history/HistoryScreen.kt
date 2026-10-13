package com.foodtracker.app.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyTotals
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.ui.LoadingAware
import com.foodtracker.app.ui.components.BarSegment
import com.foodtracker.app.ui.components.SegmentedBar
import com.foodtracker.app.ui.errorText
import com.foodtracker.app.ui.loadingPresentation
import com.foodtracker.app.ui.monthTitle
import com.foodtracker.app.ui.weekdayName
import com.foodtracker.app.util.currentLocalDateTime
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.day_today
import food_tracker_frontend.composeapp.generated.resources.day_yesterday
import food_tracker_frontend.composeapp.generated.resources.error_load_history
import food_tracker_frontend.composeapp.generated.resources.history_day_kcal
import food_tracker_frontend.composeapp.generated.resources.history_day_label
import food_tracker_frontend.composeapp.generated.resources.history_day_water
import food_tracker_frontend.composeapp.generated.resources.history_empty_month
import food_tracker_frontend.composeapp.generated.resources.history_status_none
import food_tracker_frontend.composeapp.generated.resources.history_status_on_target
import food_tracker_frontend.composeapp.generated.resources.history_status_over
import food_tracker_frontend.composeapp.generated.resources.history_status_under
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_left
import food_tracker_frontend.composeapp.generated.resources.ic_chevron_right
import food_tracker_frontend.composeapp.generated.resources.macro_carbs
import food_tracker_frontend.composeapp.generated.resources.macro_fat
import food_tracker_frontend.composeapp.generated.resources.macro_protein
import food_tracker_frontend.composeapp.generated.resources.summary_next_month
import food_tracker_frontend.composeapp.generated.resources.summary_previous_month
import food_tracker_frontend.composeapp.generated.resources.weekday_short_fri
import food_tracker_frontend.composeapp.generated.resources.weekday_short_mon
import food_tracker_frontend.composeapp.generated.resources.weekday_short_sat
import food_tracker_frontend.composeapp.generated.resources.weekday_short_sun
import food_tracker_frontend.composeapp.generated.resources.weekday_short_thu
import food_tracker_frontend.composeapp.generated.resources.weekday_short_tue
import food_tracker_frontend.composeapp.generated.resources.weekday_short_wed
import kotlin.math.roundToInt
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private val WEEKDAY_INITIALS = listOf(
    Res.string.weekday_short_mon, Res.string.weekday_short_tue, Res.string.weekday_short_wed,
    Res.string.weekday_short_thu, Res.string.weekday_short_fri, Res.string.weekday_short_sat,
    Res.string.weekday_short_sun,
)

/** Container and content colors for a day's status in the calendar (container null = none). */
private data class StatusColors(val container: Color?, val content: Color)

@Composable
private fun DayStatus.colors(): StatusColors {
    val colors = MaterialTheme.colorScheme
    return when (this) {
        DayStatus.NONE -> StatusColors(null, colors.onSurfaceVariant)
        DayStatus.UNDER -> StatusColors(colors.secondaryContainer, colors.onSecondaryContainer)
        DayStatus.ON_TARGET -> StatusColors(colors.primary, colors.onPrimary)
        DayStatus.OVER -> StatusColors(colors.errorContainer, colors.onErrorContainer)
    }
}

/**
 * Earlier days: the month starting on [month] as a calendar colored by calorie status, plus the
 * month's days with records. Tapping a day (up to today) calls [onOpenDay] with its ISO date.
 */
@Composable
fun HistoryScreen(
    apiClient: FoodTrackerApiClient,
    month: LocalDate,
    onMonthChange: (LocalDate) -> Unit,
    onOpenDay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = currentLocalDateTime().date
    val currentMonth = firstOfMonth(today)
    var days by remember { mutableStateOf<List<DailyTotals>>(emptyList()) }
    var targetCalories by remember { mutableStateOf<Double?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(month) {
        isLoading = true
        errorMessage = null
        try {
            days = apiClient.rangeSummary(month.toString(), lastOfMonth(month).toString())
            targetCalories = apiClient.getGoals().targetCalories
        } catch (e: Exception) {
            errorMessage = errorText(Res.string.error_load_history, e)
        } finally {
            isLoading = false
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        MonthHeader(
            month = month,
            canGoForward = month < currentMonth,
            onPrevious = { onMonthChange(shiftMonth(month, -1)) },
            onNext = { onMonthChange(shiftMonth(month, 1)) },
        )
        val target = targetCalories
        when {
            errorMessage != null -> Text(
                errorMessage.orEmpty(),
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            // Dims the previous month while the new one loads.
            else -> LoadingAware(loadingPresentation(isLoading, hasContent = target != null), Modifier.weight(1f)) {
                if (target != null) {
                    val byDate = days.associateBy { it.date }
                    val listed = daysWithRecords(days)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "calendar") {
                            Calendar(
                                month = month,
                                today = today,
                                statusOf = { date -> byDate[date.toString()]?.let { dayStatus(it, target) } ?: DayStatus.NONE },
                                onOpenDay = { onOpenDay(it.toString()) },
                            )
                        }
                        item(key = "legend") { Legend() }
                        if (listed.isEmpty()) {
                            item(key = "empty") {
                                Text(
                                    stringResource(Res.string.history_empty_month),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                )
                            }
                        }
                        items(listed, key = { it.date }) { day ->
                            DayCard(
                                day = day,
                                today = today,
                                status = dayStatus(day, target),
                                targetCalories = target,
                                onClick = { onOpenDay(day.date) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: LocalDate,
    canGoForward: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp),
    ) {
        IconButton(onClick = onPrevious) {
            Icon(painterResource(Res.drawable.ic_chevron_left), contentDescription = stringResource(Res.string.summary_previous_month))
        }
        Text(
            monthTitle(month),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext, enabled = canGoForward) {
            Icon(painterResource(Res.drawable.ic_chevron_right), contentDescription = stringResource(Res.string.summary_next_month))
        }
    }
}

@Composable
private fun Calendar(
    month: LocalDate,
    today: LocalDate,
    statusOf: (LocalDate) -> DayStatus,
    onOpenDay: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            WEEKDAY_INITIALS.forEach {
                Text(
                    stringResource(it),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                )
            }
        }
        monthGrid(month).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp)) {
                        if (date != null) {
                            DayCell(date, today, statusOf(date), onClick = { onOpenDay(date) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(date: LocalDate, today: LocalDate, status: DayStatus, onClick: () -> Unit) {
    val isFuture = date > today
    val (container, content) = status.colors()
    var cell = Modifier.fillMaxSize().clip(CircleShape)
    if (container != null) cell = cell.background(container)
    if (date == today) cell = cell.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
    cell = if (isFuture) cell.alpha(0.38f) else cell.clickable(onClick = onClick)
    Box(contentAlignment = Alignment.Center, modifier = cell) {
        Text(
            "${date.day}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (date == today) FontWeight.Bold else null,
            color = if (container == null) MaterialTheme.colorScheme.onSurface else content,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(modifier: Modifier = Modifier) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = modifier.fillMaxWidth()) {
        listOf(
            DayStatus.NONE to Res.string.history_status_none,
            DayStatus.UNDER to Res.string.history_status_under,
            DayStatus.ON_TARGET to Res.string.history_status_on_target,
            DayStatus.OVER to Res.string.history_status_over,
        ).forEach { (status, label) ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                StatusDot(status)
                Text(
                    stringResource(label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** A filled dot in the status color; an outlined one for days without meals. */
@Composable
private fun StatusDot(status: DayStatus, modifier: Modifier = Modifier) {
    val container = status.colors().container
    Box(
        modifier
            .size(12.dp)
            .then(
                if (container != null) {
                    Modifier.background(container, CircleShape)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                },
            ),
    )
}

@Composable
private fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
    today -> stringResource(Res.string.day_today)
    today.minus(DatePeriod(days = 1)) -> stringResource(Res.string.day_yesterday)
    else -> stringResource(Res.string.history_day_label, weekdayName(date), date.day).replaceFirstChar { it.titlecase() }
}

@Composable
private fun DayCard(
    day: DailyTotals,
    today: LocalDate,
    status: DayStatus,
    targetCalories: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow, contentColor = colors.onSurface),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(status)
                Text(
                    dayLabel(LocalDate.parse(day.date), today),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).padding(start = 10.dp),
                )
                Text(
                    stringResource(Res.string.history_day_kcal, day.totalCalories.roundToInt(), targetCalories.roundToInt()),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (status == DayStatus.OVER) colors.error else colors.onSurfaceVariant,
                )
            }
            if (day.mealCount > 0) {
                SegmentedBar(
                    segments = listOf(
                        BarSegment(stringResource(Res.string.macro_protein), day.totalProteinGrams, colors.primary),
                        BarSegment(stringResource(Res.string.macro_fat), day.totalFatGrams, colors.tertiary),
                        BarSegment(stringResource(Res.string.macro_carbs), day.totalCarbsGrams, colors.secondary),
                    ),
                    showLegend = false,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
            if (day.waterMl > 0) {
                Text(
                    stringResource(Res.string.history_day_water, day.waterMl),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}
