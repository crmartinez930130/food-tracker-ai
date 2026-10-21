package com.foodtracker.app.util

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
fun todayDateString(): String = Clock.System.todayIn(TimeZone.currentSystemDefault()).toString()

fun String.plusDays(days: Int): String = LocalDate.parse(this).plus(DatePeriod(days = days)).toString()

fun currentLocalHour(): Int = currentLocalDateTime().hour

/** True when this `YYYY-MM-DD` date is strictly before [other] - e.g. whether "next day" is allowed. */
fun String.isBeforeDay(other: String): Boolean = LocalDate.parse(this) < LocalDate.parse(other)

/**
 * The current instant as an ISO-8601 UTC timestamp, e.g. for the time a meal was logged. Whole
 * milliseconds, like the backend's own timestamps, so they all sort and compare alike.
 */
@OptIn(ExperimentalTime::class)
fun currentInstantIso(): String = Instant.fromEpochMilliseconds(Clock.System.now().toEpochMilliseconds()).toString()

@OptIn(ExperimentalTime::class)
fun currentLocalDateTime(): LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

/** How long until the clock reaches the next whole minute after [now]: 1..60000 ms. */
fun millisUntilNextMinute(now: LocalDateTime): Long {
    val intoMinute = now.second * 1_000L + now.nanosecond / 1_000_000
    return 60_000L - intoMinute
}
