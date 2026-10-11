package com.foodtracker.app.ui.summary

import com.foodtracker.app.ui.home.progressOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlin.math.ceil

/**
 * Top of the chart's value axis: covers both the highest bar and the goal line with a little
 * headroom, rounded up to a readable step. Never 0, so an empty period still draws.
 */
fun chartScaleMax(values: List<Double>, goal: Double): Double {
    val highest = maxOf(values.maxOrNull() ?: 0.0, goal)
    if (highest <= 0.0) return 100.0
    // The step depends on the value itself, not on the headroom, so a 2000 kcal goal tops out at
    // 2250 rather than jumping to the next band and wasting a quarter of the chart.
    val step = when {
        highest <= 500.0 -> 100.0
        highest <= 2000.0 -> 250.0
        else -> 500.0
    }
    return ceil(highest * 1.05 / step) * step
}

/** Height of a bar (or the goal line's position) as a fraction of the chart, clamped to 0..1. */
fun barFraction(value: Double, scaleMax: Double): Float =
    if (scaleMax <= 0.0) 0f else (value / scaleMax).coerceIn(0.0, 1.0).toFloat()

/** Same rule as Home's calorie ring, so a day reads "over" in both places or in neither. */
fun isOverGoal(value: Double, goal: Double): Boolean = progressOf(value, goal).isOver

/**
 * X-axis label for the bar at [index] of [count]: a two-letter weekday for a week (one letter would
 * repeat T/T and S/S), the day of the
 * month every 5th bar for longer periods (so labels don't collide), or null for no label.
 */
fun xAxisLabel(date: String, index: Int, count: Int): String? {
    val day = LocalDate.parse(date)
    return when {
        count <= 7 -> day.dayOfWeek.shortName()
        index % 5 == 0 -> day.day.toString()
        else -> null
    }
}

private fun DayOfWeek.shortName(): String = when (this) {
    DayOfWeek.MONDAY -> "Mo"
    DayOfWeek.TUESDAY -> "Tu"
    DayOfWeek.WEDNESDAY -> "We"
    DayOfWeek.THURSDAY -> "Th"
    DayOfWeek.FRIDAY -> "Fr"
    DayOfWeek.SATURDAY -> "Sa"
    DayOfWeek.SUNDAY -> "Su"
}
