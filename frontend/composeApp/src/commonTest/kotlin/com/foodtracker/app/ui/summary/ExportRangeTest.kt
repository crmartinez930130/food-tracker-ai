package com.foodtracker.app.ui.summary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ExportRangeTest {

    @Test
    fun `file name carries both dates`() {
        assertEquals("food-tracker-2026-10-01_2026-10-07.csv", exportFileName("2026-10-01", "2026-10-07"))
    }

    @Test
    fun `a single day and exactly 366 days are allowed`() {
        assertNull(exportRangeError("2026-10-07", "2026-10-07"))
        assertNull(exportRangeError("2025-01-01", "2026-01-01"))
    }

    @Test
    fun `367 days are rejected with the limit and the picked length`() {
        assertEquals("Pick at most 366 days (this range has 367)", exportRangeError("2025-01-01", "2026-01-02"))
    }

    @Test
    fun `an incomplete or reversed range is rejected`() {
        assertNotNull(exportRangeError("2026-10-01", null))
        assertNotNull(exportRangeError(null, null))
        assertNotNull(exportRangeError("2026-10-07", "2026-10-01"))
    }

    @Test
    fun `picker millis round-trip to the same date`() {
        for (date in listOf("1970-01-01", "2024-02-29", "2026-10-11", "1969-12-31")) {
            assertEquals(date, pickerMillisToDate(dateToPickerMillis(date)))
        }
        assertEquals(1_760_140_800_000L, dateToPickerMillis("2025-10-11"))
    }

    @Test
    fun `any millis within a UTC day map to that day`() {
        val midnight = dateToPickerMillis("2026-10-11")
        assertEquals("2026-10-11", pickerMillisToDate(midnight + 23 * 60 * 60 * 1000L))
        assertEquals("2026-10-10", pickerMillisToDate(midnight - 1))
    }

    @Test
    fun `picker years start at the earliest meal`() {
        assertEquals(2024..2026, exportYearRange("2024-05-03", periodStart = "2026-10-05", today = "2026-10-11"))
    }

    @Test
    fun `picker years include a period that starts before the first meal`() {
        assertEquals(2025..2026, exportYearRange("2026-01-10", periodStart = "2025-12-29", today = "2026-01-04"))
    }

    @Test
    fun `without a first meal date the picker falls back to recent years`() {
        assertEquals(2016..2026, exportYearRange(null, periodStart = "2026-10-05", today = "2026-10-11"))
    }
}
