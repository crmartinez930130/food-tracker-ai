package com.foodtracker.app.ui.home

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/** When a cached copy was refreshed, as shown in the offline notice: [date] only when it isn't today. */
data class CacheAge(val date: LocalDate?, val time: String)

@OptIn(ExperimentalTime::class)
fun cacheAge(refreshedAt: Long, today: LocalDate, timeZone: TimeZone = TimeZone.currentSystemDefault()): CacheAge {
    val at = Instant.fromEpochMilliseconds(refreshedAt).toLocalDateTime(timeZone)
    val time = "${at.hour.toString().padStart(2, '0')}:${at.minute.toString().padStart(2, '0')}"
    return CacheAge(date = at.date.takeIf { it != today }, time = time)
}
