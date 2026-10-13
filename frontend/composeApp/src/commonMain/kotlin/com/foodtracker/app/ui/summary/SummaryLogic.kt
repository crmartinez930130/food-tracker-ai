package com.foodtracker.app.ui.summary

import androidx.compose.ui.geometry.Offset
import com.foodtracker.app.data.DailyTotals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

/** Week or month, as rolling windows ending on the selected day (see the add-trend-charts design). */
enum class SummaryPeriod(val days: Int) { WEEK(7), MONTH(30) }

/** What Summary shows: the period kind and its last day, null meaning "ending today". */
data class SummaryView(val period: SummaryPeriod = SummaryPeriod.WEEK, val endDate: String? = null)

/**
 * The period as short dates: "7 – 13 oct" within a month, "28 sep – 4 oct" across months, and
 * with each date's year ("28 dic 2025 – 3 ene 2026") when the period crosses years or isn't in
 * [currentYear]. [monthShort] holds the 12 short month names in the app language.
 */
fun periodLabel(start: LocalDate, end: LocalDate, currentYear: Int, monthShort: List<String>): String {
    fun month(date: LocalDate) = monthShort[date.month.number - 1]
    val withYears = start.year != end.year || end.year != currentYear
    return when {
        withYears -> "${start.day} ${month(start)} ${start.year} – ${end.day} ${month(end)} ${end.year}"
        start.month == end.month -> "${start.day} – ${end.day} ${month(end)}"
        else -> "${start.day} ${month(start)} – ${end.day} ${month(end)}"
    }
}

/** The figures on Summary's stat cards. */
data class PeriodStats(
    val totalCalories: Double,
    val averageCalories: Double,
    val daysOverGoal: Int,
    val dayCount: Int,
    val averageProteinGrams: Double,
    val averageFatGrams: Double,
    val averageCarbsGrams: Double,
)

/** Averages over every day of the period: empty days count as zero, like the backend's weekly average. */
fun periodStats(days: List<DailyTotals>, goalCalories: Double): PeriodStats {
    val count = days.size.coerceAtLeast(1)
    val total = days.sumOf { it.totalCalories }
    return PeriodStats(
        totalCalories = total,
        averageCalories = total / count,
        daysOverGoal = days.count { isOverGoal(it.totalCalories, goalCalories) },
        dayCount = days.size,
        averageProteinGrams = days.sumOf { it.totalProteinGrams } / count,
        averageFatGrams = days.sumOf { it.totalFatGrams } / count,
        averageCarbsGrams = days.sumOf { it.totalCarbsGrams } / count,
    )
}

/** Which of [count] equal-width slots across [width] contains [x]; null outside the chart. */
fun barIndexAt(x: Float, width: Float, count: Int): Int? {
    if (count <= 0 || width <= 0f || x < 0f || x >= width) return null
    return (x / (width / count)).toInt().coerceAtMost(count - 1)
}

/** One cubic Bézier piece of a smoothed line, from the previous point to [end]. */
data class CubicSegment(val control1: Offset, val control2: Offset, val end: Offset)

/**
 * A smooth curve through every point (Catmull–Rom, tension 0.5, as cubic Bézier segments); the
 * ends reuse their own point as the missing neighbor. Fewer than 2 points give no segments.
 */
fun smoothControlPoints(points: List<Offset>): List<CubicSegment> =
    (0 until (points.size - 1).coerceAtLeast(0)).map { i ->
        val p0 = points[(i - 1).coerceAtLeast(0)]
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points[(i + 2).coerceAtMost(points.lastIndex)]
        CubicSegment(
            control1 = p1 + (p2 - p0) / 6f,
            control2 = p2 - (p3 - p1) / 6f,
            end = p2,
        )
    }
