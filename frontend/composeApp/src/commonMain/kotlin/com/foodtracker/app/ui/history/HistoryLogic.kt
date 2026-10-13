package com.foodtracker.app.ui.history

import com.foodtracker.app.data.DailyTotals
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** How a day went against the calorie goal, for the calendar's colors. */
enum class DayStatus { NONE, UNDER, ON_TARGET, OVER }

/** Below this share of the goal a day counts as "under"; above 100% it is "over". */
private const val ON_TARGET_FLOOR = 0.9

/**
 * [DayStatus.NONE] without meals (water alone doesn't color a day); otherwise under 90% of the
 * goal, on target from 90% to 100%, over above it. A non-positive goal makes any meal "over",
 * like Home's progress.
 */
fun dayStatus(day: DailyTotals, targetCalories: Double): DayStatus = when {
    day.mealCount == 0 -> DayStatus.NONE
    targetCalories <= 0.0 -> DayStatus.OVER
    day.totalCalories > targetCalories -> DayStatus.OVER
    day.totalCalories >= targetCalories * ON_TARGET_FLOOR -> DayStatus.ON_TARGET
    else -> DayStatus.UNDER
}

/** The 1st of the month [date] is in. */
fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

/** The 1st of the month after (or, with a negative [months], before) the one starting on [first]. */
fun shiftMonth(first: LocalDate, months: Int): LocalDate =
    if (months >= 0) first.plus(DatePeriod(months = months)) else first.minus(DatePeriod(months = -months))

/** The last day of the month starting on [first]. */
fun lastOfMonth(first: LocalDate): LocalDate = shiftMonth(first, 1).minus(DatePeriod(days = 1))

/**
 * The month starting on [first] as weeks of 7 cells, Monday first; null pads the cells before the
 * 1st and after the last day.
 */
fun monthGrid(first: LocalDate): List<List<LocalDate?>> {
    val leading = first.dayOfWeek.isoDayNumber - DayOfWeek.MONDAY.isoDayNumber
    val days = (0 until lastOfMonth(first).day).map { first.plus(DatePeriod(days = it)) }
    val cells: List<LocalDate?> = List(leading) { null } + days
    return cells.chunked(7).map { week -> week + List(7 - week.size) { null } }
}

/** The days worth listing: those with meals or water, newest first. */
fun daysWithRecords(days: List<DailyTotals>): List<DailyTotals> =
    days.filter { it.mealCount > 0 || it.waterMl > 0 }.sortedByDescending { it.date }
