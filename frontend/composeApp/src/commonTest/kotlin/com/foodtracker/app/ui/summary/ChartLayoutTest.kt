package com.foodtracker.app.ui.summary

import com.foodtracker.app.data.WeightEntry
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
    fun `a week labels every bar with an unambiguous two-letter weekday`() {
        // 2026-10-05 is a Monday.
        val labels = (0 until 7).map { i -> xAxisLabel("2026-10-" + (5 + i).toString().padStart(2, '0'), i, 7) }

        assertEquals(listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su"), labels)
    }

    @Test
    fun `a month labels every fifth bar with the day of the month`() {
        assertEquals("1", xAxisLabel("2026-10-01", index = 0, count = 30))
        assertNull(xAxisLabel("2026-10-02", index = 1, count = 30))
        assertEquals("6", xAxisLabel("2026-10-06", index = 5, count = 30))
        assertNull(xAxisLabel("2026-10-30", index = 29, count = 30))
    }

    private fun entry(id: Int, day: String, createdAt: String, kg: Double) = WeightEntry(id, kg, day, createdAt)

    @Test
    fun `latestPerDay keeps the last entry of each day, in day order`() {
        val entries = listOf(
            entry(1, "2026-03-03", "2026-03-03T07:00:00.000Z", 73.0),
            entry(2, "2026-03-01", "2026-03-01T07:00:00.000Z", 72.0),
            entry(3, "2026-03-03", "2026-03-03T21:00:00.000Z", 72.6),
        )

        val latest = latestPerDay(entries)

        assertEquals(listOf("2026-03-01" to 72.0, "2026-03-03" to 72.6), latest.map { it.day to it.weightKg })
    }

    @Test
    fun `weight scale pads half a kilo each side on whole kilos`() {
        assertEquals(70.0..75.0, weightScale(listOf(70.8, 72.0, 74.3)))
    }

    @Test
    fun `weight scale spans at least 2 kg for a flat series`() {
        val range = weightScale(listOf(72.0, 72.0))

        assertTrue(range.endInclusive - range.start >= 2.0, "range $range")
        assertTrue(72.0 in range)
    }

    @Test
    fun `weight scale works for a single value`() {
        val range = weightScale(listOf(80.2))

        assertTrue(80.2 in range)
        assertTrue(range.endInclusive - range.start >= 2.0)
    }

    @Test
    fun `weight formatting uses one decimal and a signed change`() {
        assertEquals("72.4 kg", formatKg(72.38))
        assertEquals("+0.6 kg", formatKgChange(0.6))
        assertEquals("−0.8 kg", formatKgChange(-0.8))
        assertEquals("±0 kg", formatKgChange(0.01))
    }

    @Test
    fun `weight input accepts 0 to 500 kg, with comma or dot`() {
        assertEquals(72.4, parseWeightInput("72.4"))
        assertEquals(72.4, parseWeightInput(" 72,4 "))
        assertEquals(500.0, parseWeightInput("500"))
        assertNull(parseWeightInput("0"))
        assertNull(parseWeightInput("600"))
        assertNull(parseWeightInput("abc"))
        assertNull(parseWeightInput(""))
    }
}

