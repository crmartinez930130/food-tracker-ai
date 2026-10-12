package com.foodtracker.app.ui.summary

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.foodtracker.app.data.DailyTotals
import food_tracker_frontend.composeapp.generated.resources.Res
import food_tracker_frontend.composeapp.generated.resources.chart_calories_description
import food_tracker_frontend.composeapp.generated.resources.chart_calories_no_data
import food_tracker_frontend.composeapp.generated.resources.chart_goal_label
import food_tracker_frontend.composeapp.generated.resources.weekday_short_fri
import food_tracker_frontend.composeapp.generated.resources.weekday_short_mon
import food_tracker_frontend.composeapp.generated.resources.weekday_short_sat
import food_tracker_frontend.composeapp.generated.resources.weekday_short_sun
import food_tracker_frontend.composeapp.generated.resources.weekday_short_thu
import food_tracker_frontend.composeapp.generated.resources.weekday_short_tue
import food_tracker_frontend.composeapp.generated.resources.weekday_short_wed
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.datetime.DayOfWeek
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Bars of calories per day with a dashed line at the daily goal. Days over the goal use the error
 * color, like Home's ring. All the math lives in ChartLayout.kt; this only draws.
 */
@Composable
fun CalorieChart(days: List<DailyTotals>, goalCalories: Double, modifier: Modifier = Modifier) {
    val textMeasurer = rememberTextMeasurer()
    val colors = MaterialTheme.colorScheme
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = colors.onSurfaceVariant)
    val values = days.map { it.totalCalories }
    val scaleMax = chartScaleMax(values, goalCalories)
    val description = chartDescription(days, goalCalories)
    // Resolved here: the Canvas draw scope isn't composable, so it can't read resources itself.
    val weekdayNames = DayOfWeek.entries.associateWith { stringResource(it.shortNameRes) }
    val goalText = stringResource(Res.string.chart_goal_label, goalCalories.roundToInt())

    Canvas(modifier = modifier.fillMaxWidth().height(220.dp).semantics { contentDescription = description }) {
        if (days.isEmpty()) return@Canvas
        val labelArea = 20.dp.toPx()
        val chartHeight = size.height - labelArea
        val slot = size.width / days.size
        val barWidth = slot * 0.6f
        val corner = min(barWidth / 2f, 4.dp.toPx())

        days.forEachIndexed { index, day ->
            val barHeight = chartHeight * barFraction(day.totalCalories, scaleMax)
            if (barHeight > 0f) {
                drawRoundRect(
                    color = if (isOverGoal(day.totalCalories, goalCalories)) colors.error else colors.primary,
                    topLeft = Offset(index * slot + (slot - barWidth) / 2f, chartHeight - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(corner, corner),
                )
            }
            xAxisLabel(day.date, index, days.size)?.let { label ->
                val text = when (label) {
                    is AxisLabel.Weekday -> weekdayNames.getValue(label.day)
                    is AxisLabel.DayOfMonth -> label.day.toString()
                }
                val measured = textMeasurer.measure(text, labelStyle)
                drawText(
                    measured,
                    topLeft = Offset(
                        x = index * slot + (slot - measured.size.width) / 2f,
                        y = chartHeight + (labelArea - measured.size.height) / 2f,
                    ),
                )
            }
        }

        drawLine(
            color = colors.outlineVariant,
            start = Offset(0f, chartHeight),
            end = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx(),
        )

        if (goalCalories > 0.0) {
            val goalY = chartHeight * (1f - barFraction(goalCalories, scaleMax))
            drawLine(
                color = colors.outline,
                start = Offset(0f, goalY),
                end = Offset(size.width, goalY),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
            )
            val goalLabel = textMeasurer.measure(goalText, labelStyle)
            drawText(goalLabel, topLeft = Offset(0f, (goalY - goalLabel.size.height - 2.dp.toPx()).coerceAtLeast(0f)))
        }
    }
}

/** What TalkBack reads for the whole chart, since the bars themselves aren't focusable. */
@Composable
private fun chartDescription(days: List<DailyTotals>, goalCalories: Double): String {
    if (days.isEmpty()) return stringResource(Res.string.chart_calories_no_data)
    val average = days.sumOf { it.totalCalories } / days.size
    val overGoal = days.count { isOverGoal(it.totalCalories, goalCalories) }
    return pluralStringResource(
        Res.plurals.chart_calories_description,
        days.size,
        days.first().date,
        days.last().date,
        average.roundToInt(),
        overGoal,
        days.size,
        goalCalories.roundToInt(),
    )
}

/** Two-letter weekday names for the week chart's axis. */
private val DayOfWeek.shortNameRes: StringResource
    get() = when (this) {
        DayOfWeek.MONDAY -> Res.string.weekday_short_mon
        DayOfWeek.TUESDAY -> Res.string.weekday_short_tue
        DayOfWeek.WEDNESDAY -> Res.string.weekday_short_wed
        DayOfWeek.THURSDAY -> Res.string.weekday_short_thu
        DayOfWeek.FRIDAY -> Res.string.weekday_short_fri
        DayOfWeek.SATURDAY -> Res.string.weekday_short_sat
        DayOfWeek.SUNDAY -> Res.string.weekday_short_sun
    }
