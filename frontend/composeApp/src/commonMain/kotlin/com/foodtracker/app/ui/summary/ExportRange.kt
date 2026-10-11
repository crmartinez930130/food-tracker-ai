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

/** The Material date pickers work in UTC-midnight millis; the app and backend in YYYY-MM-DD. */
fun dateToPickerMillis(date: String): Long = LocalDate.parse(date).toEpochDays() * MILLIS_PER_DAY

fun pickerMillisToDate(millis: Long): String = LocalDate.fromEpochDays(millis.floorDiv(MILLIS_PER_DAY)).toString()
