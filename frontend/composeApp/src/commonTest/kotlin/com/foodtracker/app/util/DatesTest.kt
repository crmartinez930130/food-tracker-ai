package com.foodtracker.app.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDateTime

class DatesTest {

    @Test
    fun `isBeforeDay compares calendar days`() {
        assertTrue("2026-10-08".isBeforeDay("2026-10-09"))
        assertFalse("2026-10-09".isBeforeDay("2026-10-09"))
        assertFalse("2026-10-10".isBeforeDay("2026-10-09"))
        assertTrue("2025-12-31".isBeforeDay("2026-01-01"))
    }

    @Test
    fun `plusDays crosses month and year boundaries`() {
        assertEquals("2026-11-01", "2026-10-31".plusDays(1))
        assertEquals("2025-12-31", "2026-01-01".plusDays(-1))
    }

    @Test
    fun `millisUntilNextMinute counts to the next whole minute`() {
        assertEquals(60_000L, millisUntilNextMinute(LocalDateTime(2026, 10, 13, 23, 59, 0, 0)))
        assertEquals(30_000L, millisUntilNextMinute(LocalDateTime(2026, 10, 13, 23, 59, 30, 0)))
        assertEquals(1L, millisUntilNextMinute(LocalDateTime(2026, 10, 13, 23, 59, 59, 999_000_000)))
        assertEquals(1L, millisUntilNextMinute(LocalDateTime(2026, 10, 13, 23, 59, 59, 999_999_999)))
    }
}
