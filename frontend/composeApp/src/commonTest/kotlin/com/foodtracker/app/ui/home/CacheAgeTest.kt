package com.foodtracker.app.ui.home

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class CacheAgeTest {
    private val zone = TimeZone.of("America/Argentina/Buenos_Aires")
    private fun at(dateTime: String) = LocalDateTime.parse(dateTime).toInstant(zone).toEpochMilliseconds()

    @Test
    fun `a copy from today shows only the time, zero-padded`() {
        assertEquals(CacheAge(date = null, time = "09:05"), cacheAge(at("2026-10-15T09:05"), LocalDate(2026, 10, 15), zone))
    }

    @Test
    fun `a copy from another day shows its date too`() {
        assertEquals(
            CacheAge(date = LocalDate(2026, 10, 14), time = "23:59"),
            cacheAge(at("2026-10-14T23:59"), LocalDate(2026, 10, 15), zone),
        )
    }

    @Test
    fun `the day is the local one, not UTC's`() {
        // 22:30 in Buenos Aires is already the next day in UTC.
        assertEquals(CacheAge(date = null, time = "22:30"), cacheAge(at("2026-10-15T22:30"), LocalDate(2026, 10, 15), zone))
    }
}
