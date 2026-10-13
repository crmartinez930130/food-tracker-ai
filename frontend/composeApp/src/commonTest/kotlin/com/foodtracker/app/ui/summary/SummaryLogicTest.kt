package com.foodtracker.app.ui.summary

import androidx.compose.ui.geometry.Offset
import com.foodtracker.app.data.DailyTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

class SummaryLogicTest {

    private val months = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")

    @Test
    fun `period label within a month, across months and across years`() {
        assertEquals("7 – 13 oct", periodLabel(LocalDate(2026, 10, 7), LocalDate(2026, 10, 13), 2026, months))
        assertEquals("28 sep – 4 oct", periodLabel(LocalDate(2026, 9, 28), LocalDate(2026, 10, 4), 2026, months))
        assertEquals("28 dic 2025 – 3 ene 2026", periodLabel(LocalDate(2025, 12, 28), LocalDate(2026, 1, 3), 2026, months))
    }

    @Test
    fun `period label in a past year shows the year`() {
        assertEquals("7 oct 2025 – 13 oct 2025", periodLabel(LocalDate(2025, 10, 7), LocalDate(2025, 10, 13), 2026, months))
    }

    private fun day(kcal: Double, protein: Double = 0.0, fat: Double = 0.0, carbs: Double = 0.0) =
        DailyTotals("2026-10-01", kcal, protein, fat, carbs)

    @Test
    fun `stats average over every day, empty days included`() {
        val stats = periodStats(listOf(day(1500.0, 70.0, 50.0, 200.0), day(0.0), day(2400.0, 90.0, 100.0, 280.0)), 2000.0)

        assertEquals(3900.0, stats.totalCalories)
        assertEquals(1300.0, stats.averageCalories)
        assertEquals(1, stats.daysOverGoal)
        assertEquals(3, stats.dayCount)
        assertEquals(160.0 / 3, stats.averageProteinGrams)
        assertEquals(50.0, stats.averageFatGrams)
        assertEquals(160.0, stats.averageCarbsGrams)
    }

    @Test
    fun `stats of an empty period are zeros`() {
        val stats = periodStats(emptyList(), 2000.0)

        assertEquals(0.0, stats.averageCalories)
        assertEquals(0, stats.daysOverGoal)
        assertEquals(0, stats.dayCount)
    }

    @Test
    fun `bar index maps a tap to its slot`() {
        assertEquals(0, barIndexAt(0f, 700f, 7))
        assertEquals(3, barIndexAt(350f, 700f, 7))
        assertEquals(6, barIndexAt(699.9f, 700f, 7))
        assertNull(barIndexAt(-1f, 700f, 7))
        assertNull(barIndexAt(700f, 700f, 7))
        assertNull(barIndexAt(10f, 700f, 0))
    }

    @Test
    fun `smooth curve passes through every point`() {
        val points = listOf(Offset(0f, 10f), Offset(10f, 30f), Offset(20f, 5f), Offset(30f, 20f))

        val segments = smoothControlPoints(points)

        assertEquals(3, segments.size)
        assertEquals(points.drop(1), segments.map { it.end })
        assertTrue(smoothControlPoints(points.take(1)).isEmpty())
    }

    @Test
    fun `a flat series stays flat`() {
        val segments = smoothControlPoints(listOf(Offset(0f, 50f), Offset(10f, 50f), Offset(20f, 50f)))

        segments.forEach {
            assertEquals(50f, it.control1.y)
            assertEquals(50f, it.control2.y)
        }
    }
}
