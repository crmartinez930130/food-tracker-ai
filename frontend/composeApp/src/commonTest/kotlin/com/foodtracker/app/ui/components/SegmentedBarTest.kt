package com.foodtracker.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class SegmentedBarTest {

    @Test
    fun `fractions are each value's share and add up to one`() {
        val fractions = segmentFractions(listOf(45.0, 46.0, 9.0))

        assertEquals(listOf(0.45f, 0.46f, 0.09f), fractions.map { (it * 100).toInt() / 100f })
        assertEquals(1f, fractions.sum(), 0.0001f)
    }

    @Test
    fun `nothing positive gives an empty bar, not an even split`() {
        assertEquals(listOf(0f, 0f, 0f), segmentFractions(listOf(0.0, 0.0, 0.0)))
        assertEquals(emptyList(), segmentFractions(emptyList()))
    }

    @Test
    fun `negative values count as zero`() {
        assertEquals(listOf(0f, 1f), segmentFractions(listOf(-5.0, 20.0)))
    }

    @Test
    fun `a single positive segment takes the whole bar`() {
        assertEquals(listOf(0f, 1f, 0f), segmentFractions(listOf(0.0, 12.5, 0.0)))
    }
}
