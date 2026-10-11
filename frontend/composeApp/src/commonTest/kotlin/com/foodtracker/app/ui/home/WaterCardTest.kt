package com.foodtracker.app.ui.home

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class WaterCardTest {

    @Test
    fun `label shows consumed over target in ml`() {
        assertEquals("750 / 2000 ml", waterLabel(750, 2000))
        assertEquals("2250 / 2000 ml", waterLabel(2250, 2000))
    }

    @Test
    fun `today logs now, so no timestamp is sent`() {
        assertNull(loggedAtFor("2026-10-11", "2026-10-11"))
    }

    @Test
    fun `a past day logs at local noon`() {
        val zone = TimeZone.of("America/Argentina/Buenos_Aires")

        assertEquals("2026-10-10T15:00:00Z", loggedAtFor("2026-10-10", "2026-10-11", zone))
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `a past-day timestamp falls on that local day in any zone, across DST changes`() {
        for (zoneId in listOf("Europe/Madrid", "America/New_York", "Pacific/Auckland", "UTC")) {
            val zone = TimeZone.of(zoneId)
            for (date in listOf("2026-03-29", "2026-11-01", "2026-04-05")) {
                val local = Instant.parse(loggedAtFor(date, "2026-12-31", zone)!!).toLocalDateTime(zone)
                assertEquals(date, local.date.toString(), "$date in $zoneId")
                assertEquals(12, local.hour, "$date in $zoneId")
            }
        }
    }
}
