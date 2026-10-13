package com.foodtracker.app.ui.history

import com.foodtracker.app.data.DailyTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class HistoryLogicTest {

    private fun day(date: String = "2026-10-12", kcal: Double = 0.0, meals: Int = 1, waterMl: Int = 0) =
        DailyTotals(date, kcal, 0.0, 0.0, 0.0, mealCount = meals, waterMl = waterMl)

    @Test
    fun `status boundaries against the goal`() {
        assertEquals(DayStatus.UNDER, dayStatus(day(kcal = 1798.0), 2000.0)) // 89.9%
        assertEquals(DayStatus.ON_TARGET, dayStatus(day(kcal = 1800.0), 2000.0)) // 90%
        assertEquals(DayStatus.ON_TARGET, dayStatus(day(kcal = 2000.0), 2000.0)) // 100%
        assertEquals(DayStatus.OVER, dayStatus(day(kcal = 2002.0), 2000.0)) // 100.1%
    }

    @Test
    fun `no meals is NONE even with water or calories`() {
        assertEquals(DayStatus.NONE, dayStatus(day(meals = 0, waterMl = 500), 2000.0))
        assertEquals(DayStatus.NONE, dayStatus(day(meals = 0), 2000.0))
    }

    @Test
    fun `a zero goal makes any meal over`() {
        assertEquals(DayStatus.OVER, dayStatus(day(kcal = 0.0), 0.0))
        assertEquals(DayStatus.NONE, dayStatus(day(meals = 0), 0.0))
    }

    @Test
    fun `month starting on a Monday has no leading padding`() {
        val grid = monthGrid(LocalDate(2026, 6, 1)) // June 2026 starts on a Monday, 30 days

        assertEquals(5, grid.size)
        assertEquals(LocalDate(2026, 6, 1), grid[0][0])
        assertEquals(LocalDate(2026, 6, 30), grid[4][1])
        assertNull(grid[4][2])
        grid.forEach { assertEquals(7, it.size) }
    }

    @Test
    fun `month starting on a Sunday needs six rows`() {
        val grid = monthGrid(LocalDate(2026, 3, 1)) // March 2026 starts on a Sunday, 31 days

        assertEquals(6, grid.size)
        assertEquals(List(6) { null }, grid[0].take(6))
        assertEquals(LocalDate(2026, 3, 1), grid[0][6])
        assertEquals(LocalDate(2026, 3, 31), grid[5][1])
    }

    @Test
    fun `february in leap and common years`() {
        assertEquals(LocalDate(2028, 2, 29), monthGrid(LocalDate(2028, 2, 1)).flatten().filterNotNull().last())
        assertEquals(LocalDate(2026, 2, 28), monthGrid(LocalDate(2026, 2, 1)).flatten().filterNotNull().last())
        assertEquals(28, monthGrid(LocalDate(2026, 2, 1)).flatten().count { it != null })
    }

    @Test
    fun `months shift across a year boundary`() {
        assertEquals(LocalDate(2027, 1, 1), shiftMonth(LocalDate(2026, 12, 1), 1))
        assertEquals(LocalDate(2025, 12, 1), shiftMonth(LocalDate(2026, 1, 1), -1))
        assertEquals(LocalDate(2026, 10, 1), firstOfMonth(LocalDate(2026, 10, 13)))
        assertEquals(LocalDate(2026, 2, 28), lastOfMonth(LocalDate(2026, 2, 1)))
    }

    @Test
    fun `listed days have meals or water, newest first`() {
        val days = listOf(
            day("2026-10-01", meals = 1),
            day("2026-10-02", meals = 0),
            day("2026-10-03", meals = 0, waterMl = 250),
            day("2026-10-04", meals = 2),
        )

        assertEquals(listOf("2026-10-04", "2026-10-03", "2026-10-01"), daysWithRecords(days).map { it.date })
    }
}
