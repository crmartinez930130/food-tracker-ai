package com.foodtracker.app.ui.components

import androidx.compose.ui.geometry.Size
import androidx.graphics.shapes.Morph
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ExpressiveShapesTest {

    private val all = listOf(
        ExpressiveShapes.cookie9,
        ExpressiveShapes.sunny,
        ExpressiveShapes.softBurst,
        ExpressiveShapes.pentagon,
        ExpressiveShapes.circle,
    )

    /** The fitted bounds of [cubics] in [size], computed without drawing (no android.graphics.Path in unit tests). */
    private fun fittedBounds(cubics: List<androidx.graphics.shapes.Cubic>, size: Size): FloatArray {
        val bounds = cubicsBounds(cubics)
        val fit = fitTransform(bounds, size)
        return floatArrayOf(fit.x(bounds[0]), fit.y(bounds[1]), fit.x(bounds[2]), fit.y(bounds[3]))
    }

    @Test
    fun `every shape fits inside the target size`() {
        for (size in listOf(Size(64f, 64f), Size(200f, 100f), Size(40f, 120f))) {
            for (polygon in all) {
                val (left, top, right, bottom) = fittedBounds(polygon.cubics, size).toList()
                assertTrue(left >= -0.01f && top >= -0.01f, "$size: starts outside")
                assertTrue(right <= size.width + 0.01f && bottom <= size.height + 0.01f, "$size: ends outside")
            }
        }
    }

    @Test
    fun `shapes are centered and fill the shorter side`() {
        val size = Size(200f, 100f)
        for (polygon in all) {
            val (left, top, right, bottom) = fittedBounds(polygon.cubics, size).toList()
            assertTrue(abs((left + right) / 2 - 100f) < 0.01f, "not centered horizontally")
            assertTrue(abs((top + bottom) / 2 - 50f) < 0.01f, "not centered vertically")
            assertEquals(100f, bottom - top, 0.01f)
        }
    }

    @Test
    fun `a mid-way morph also fits`() {
        val morph = Morph(ExpressiveShapes.softBurst, ExpressiveShapes.cookie9)
        val (left, top, right, bottom) = fittedBounds(morph.asCubics(0.5f), Size(48f, 48f)).toList()
        assertTrue(left >= -0.01f && right <= 48.01f && top >= -0.01f && bottom <= 48.01f)
    }

    @Test
    fun `shapes are round-ish, not squashed`() {
        for (polygon in all) {
            val (left, top, right, bottom) = cubicsBounds(polygon.cubics).toList()
            val ratio = (right - left) / (bottom - top)
            assertTrue(ratio in 0.85f..1.15f, "aspect ratio $ratio")
        }
    }

    @Test
    fun `morph steps walk the sequence and wrap around`() {
        assertEquals(MorphStep(0, 1, 0f), morphStep(0f, 4))
        assertEquals(1, morphStep(1.25f, 4).from)
        assertEquals(2, morphStep(1.25f, 4).to)
        assertEquals(MorphStep(3, 0, 0f), morphStep(3f, 4))
        assertEquals(MorphStep(0, 1, 0f), morphStep(4f, 4))
    }

    @Test
    fun `morph progress stays within 0 and 1 and eases at the ends`() {
        for (i in 0..40) {
            val progress = morphStep(i / 10f, 4).progress
            assertTrue(progress in 0f..1f, "progress $progress out of range")
        }
        assertTrue(morphStep(0.1f, 4).progress < 0.1f, "should ease in")
        assertTrue(morphStep(0.9f, 4).progress > 0.9f, "should ease out")
        assertEquals(0.5f, morphStep(0.5f, 4).progress, 0.0001f)
    }

    @Test
    fun `morphing needs at least one shape`() {
        assertFailsWith<IllegalArgumentException> { morphStep(0.5f, 0) }
    }
}
