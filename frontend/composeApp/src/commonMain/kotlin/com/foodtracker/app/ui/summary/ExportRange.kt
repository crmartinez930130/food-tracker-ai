package com.foodtracker.app.ui.summary

import kotlinx.datetime.LocalDate

/** The backend's longest accepted export range, in days (inclusive). */
const val MAX_EXPORT_DAYS = 366

private const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

/** Same pattern as the backend's Content-Disposition file name. */
fun exportFileName(startDate: String, endDate: String): String = "food-tracker-${startDate}_$endDate.csv"

/**
 * Why the picked range can't be exported, or null when it can - checked before calling the
 * backend so its 400 never becomes the user-facing message.
 */
fun exportRangeError(startDate: String?, endDate: String?): String? {
    if (startDate == null || endDate == null) return "Pick a start and an end date"
    val days = LocalDate.parse(endDate).toEpochDays() - LocalDate.parse(startDate).toEpochDays() + 1
    return when {
        days < 1 -> "The end date must not be before the start date"
        days > MAX_EXPORT_DAYS -> "Pick at most $MAX_EXPORT_DAYS days (this range has $days)"
        else -> null
    }
}

/** Years offered when the earliest meal is unknown (no meals yet, or the lookup failed). */
const val FALLBACK_EXPORT_YEARS = 10

/**
 * The picker's years: from the earliest meal's year - or the pre-selected period's, if earlier, since
 * the picker's initial month must lie inside the range - up to the current year.
 */
fun exportYearRange(firstMealDate: String?, periodStart: String, today: String): IntRange {
    val todayYear = LocalDate.parse(today).year
    val periodYear = LocalDate.parse(periodStart).year
    val firstYear = firstMealDate?.let { LocalDate.parse(it).year } ?: (todayYear - FALLBACK_EXPORT_YEARS)
    return minOf(firstYear, periodYear)..todayYear
}

/** The Material date pickers work in UTC-midnight millis; the app and backend in YYYY-MM-DD. */
fun dateToPickerMillis(date: String): Long = LocalDate.parse(date).toEpochDays() * MILLIS_PER_DAY

fun pickerMillisToDate(millis: Long): String = LocalDate.fromEpochDays(millis.floorDiv(MILLIS_PER_DAY)).toString()
