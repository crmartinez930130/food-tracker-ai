package com.foodtracker.app.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProgressTest {

    @Test
    fun `under target fills proportionally and reports what is left`() {
        val p = progressOf(consumed = 500.0, target = 2000.0)

        assertEquals(0.25f, p.fraction)
        assertFalse(p.isOver)
        assertEquals(1500.0, p.remaining)
        assertEquals(0.0, p.excess)
    }

    @Test
    fun `exactly at target is full but not over`() {
        val p = progressOf(consumed = 2000.0, target = 2000.0)

        assertEquals(1f, p.fraction)
        assertFalse(p.isOver)
        assertEquals(0.0, p.remaining)
        assertEquals(0.0, p.excess)
    }

    @Test
    fun `over target is clamped to full and reports the excess`() {
        val p = progressOf(consumed = 2300.0, target = 2000.0)

        assertEquals(1f, p.fraction)
        assertTrue(p.isOver)
        assertEquals(0.0, p.remaining)
        assertEquals(300.0, p.excess)
    }

    @Test
    fun `zero target with something consumed is full and over`() {
        val p = progressOf(consumed = 12.0, target = 0.0)

        assertEquals(1f, p.fraction)
        assertTrue(p.isOver)
        assertEquals(12.0, p.excess)
    }

    @Test
    fun `zero target with nothing consumed is empty and not over`() {
        val p = progressOf(consumed = 0.0, target = 0.0)

        assertEquals(0f, p.fraction)
        assertFalse(p.isOver)
    }

    @Test
    fun `nothing consumed is empty`() {
        val p = progressOf(consumed = 0.0, target = 125.0)

        assertEquals(0f, p.fraction)
        assertEquals(125.0, p.remaining)
    }

    @Test
    fun `day labels today, yesterday and older dates`() {
        assertEquals("Today", dayLabel("2026-10-09", today = "2026-10-09"))
        assertEquals("Yesterday", dayLabel("2026-10-08", today = "2026-10-09"))
        assertEquals("2026-10-07", dayLabel("2026-10-07", today = "2026-10-09"))
    }

    @Test
    fun `yesterday works across a month boundary`() {
        assertEquals("Yesterday", dayLabel("2026-09-30", today = "2026-10-01"))
    }
}
