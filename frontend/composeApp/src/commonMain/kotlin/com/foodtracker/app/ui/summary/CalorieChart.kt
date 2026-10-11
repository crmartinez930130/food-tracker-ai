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
import kotlin.math.min
import kotlin.math.roundToInt

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
                val measured = textMeasurer.measure(label, labelStyle)
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
            val goalLabel = textMeasurer.measure("goal ${goalCalories.roundToInt()}", labelStyle)
            drawText(goalLabel, topLeft = Offset(0f, (goalY - goalLabel.size.height - 2.dp.toPx()).coerceAtLeast(0f)))
        }
    }
}

/** What TalkBack reads for the whole chart, since the bars themselves aren't focusable. */
private fun chartDescription(days: List<DailyTotals>, goalCalories: Double): String {
    if (days.isEmpty()) return "Calories per day: no data"
    val average = days.sumOf { it.totalCalories } / days.size
    val overGoal = days.count { isOverGoal(it.totalCalories, goalCalories) }
    return "Calories per day from ${days.first().date} to ${days.last().date}. " +
        "Average ${average.roundToInt()} kcal. $overGoal of ${days.size} days over the goal of ${goalCalories.roundToInt()} kcal."
}
