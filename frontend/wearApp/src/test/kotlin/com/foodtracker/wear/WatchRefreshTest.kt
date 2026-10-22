package com.foodtracker.wear

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val MIN = 15 * 60 * 1000L

class WatchRefreshTest {
    @Test
    fun `the first refresh is always due`() {
        assertTrue(isRefreshDue(lastRequested = 0L, now = 1_000L, minIntervalMillis = MIN))
    }

    @Test
    fun `a tile redraw right after a refresh doesn't ask for another one`() {
        assertFalse(isRefreshDue(lastRequested = 10_000L, now = 10_000L + 60_000L, minIntervalMillis = MIN))
    }

    @Test
    fun `after the interval it's due again`() {
        assertTrue(isRefreshDue(lastRequested = 10_000L, now = 10_000L + MIN, minIntervalMillis = MIN))
    }

    @Test
    fun `a clock set back doesn't block refreshes`() {
        assertTrue(isRefreshDue(lastRequested = 10_000_000L, now = 5_000L, minIntervalMillis = MIN))
    }
}
