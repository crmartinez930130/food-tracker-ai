package com.foodtracker.app.ui.summary

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChartLayoutTest {

    @Test
    fun `scale covers the goal when every day is below it`() {
        val max = chartScaleMax(listOf(800.0, 1200.0, 0.0), goal = 2000.0)

        assertTrue(max >= 2000.0, "scale $max should reach the goal")
        assertEquals(2250.0, max) // 2000 * 1.05 = 2100, rounded up to the 250 step
    }

    @Test
    fun `scale covers the highest day when it is above the goal`() {
        val max = chartScaleMax(listOf(1500.0, 3100.0), goal = 2000.0)

        assertTrue(max >= 3100.0)
        assertEquals(3500.0, max) // 3100 * 1.05 = 3255, rounded up to the 500 step
    }

    @Test
    fun `scale is never zero for an empty period`() {
        assertEquals(100.0, chartScaleMax(emptyList(), goal = 0.0))
        assertEquals(100.0, chartScaleMax(listOf(0.0, 0.0), goal = 0.0))
    }

    @Test
    fun `bar fractions are proportional and clamped to 0 to 1`() {
        assertEquals(0.5f, barFraction(1000.0, 2000.0))
        assertEquals(1f, barFraction(2500.0, 2000.0))
        assertEquals(0f, barFraction(-5.0, 2000.0))
        assertEquals(0f, barFraction(100.0, 0.0))
    }

    @Test
    fun `over goal means strictly above it, like Home`() {
        assertTrue(isOverGoal(2001.0, 2000.0))
        assertFalse(isOverGoal(2000.0, 2000.0))
        assertFalse(isOverGoal(1500.0, 2000.0))
    }

    @Test
    fun `a week labels every bar with its weekday initial`() {
        // 2026-10-05 is a Monday.
        val labels = (0 until 7).map { i -> xAxisLabel("2026-10-" + (5 + i).toString().padStart(2, '0'), i, 7) }

        assertEquals(listOf("M", "T", "W", "T", "F", "S", "S"), labels)
    }

    @Test
    fun `a month labels every fifth bar with the day of the month`() {
        assertEquals("1", xAxisLabel("2026-10-01", index = 0, count = 30))
        assertNull(xAxisLabel("2026-10-02", index = 1, count = 30))
        assertEquals("6", xAxisLabel("2026-10-06", index = 5, count = 30))
        assertNull(xAxisLabel("2026-10-30", index = 29, count = 30))
    }
}
