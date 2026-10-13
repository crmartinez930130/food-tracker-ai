package com.foodtracker.app.ui.home

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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
    fun `goal percent rounds and is not capped`() {
        assertEquals(62, goalPercent(consumed = 1240.0, target = 2000.0))
        assertEquals(112, goalPercent(consumed = 2240.0, target = 2000.0))
        assertEquals(34, goalPercent(consumed = 675.0, target = 2000.0)) // 33.75
        assertEquals(0, goalPercent(consumed = 0.0, target = 2000.0))
    }

    @Test
    fun `goal percent is null without a positive target`() {
        assertNull(goalPercent(consumed = 500.0, target = 0.0))
        assertNull(goalPercent(consumed = 500.0, target = -10.0))
    }
}
