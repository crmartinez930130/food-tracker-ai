package com.foodtracker.app.ui

import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.Portion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CaptureLogicTest {

    @Test
    fun `a photo, a description or both can be analyzed`() {
        assertTrue(canAnalyze(hasPhoto = true, description = ""))
        assertTrue(canAnalyze(hasPhoto = false, description = "una manzana"))
        assertTrue(canAnalyze(hasPhoto = true, description = "sin aceite"))
    }

    @Test
    fun `nothing or a blank description can't be analyzed`() {
        assertFalse(canAnalyze(hasPhoto = false, description = ""))
        assertFalse(canAnalyze(hasPhoto = false, description = "   \n"))
        assertFalse(canAnalyze(hasPhoto = false, description = "", hasIngredients = false))
    }

    @Test
    fun `ingredient photos alone are enough to analyze`() {
        assertTrue(canAnalyze(hasPhoto = false, description = "", hasIngredients = true))
    }

    @Test
    fun `dictation is appended after what was typed`() {
        assertEquals("milanesa al horno", appendDictation("", " milanesa al horno "))
        assertEquals("milanesa con puré", appendDictation("milanesa ", "con puré"))
        assertEquals("milanesa", appendDictation("milanesa", "  "))
    }

    @Test
    fun `dictation never pushes the description past the limit`() {
        val typed = "a".repeat(MAX_DESCRIPTION_LENGTH - 3)

        assertEquals(MAX_DESCRIPTION_LENGTH, appendDictation(typed, "bbbbbb").length)
    }

    @Test
    fun `tapping a portion selects it, another switches, the same one clears`() {
        assertEquals(Portion.SMALL, toggledPortion(null, Portion.SMALL))
        assertEquals(Portion.LARGE, toggledPortion(Portion.SMALL, Portion.LARGE))
        assertNull(toggledPortion(Portion.SMALL, Portion.SMALL))
    }

    @Test
    fun `review numbers accept a decimal comma and count anything else as zero`() {
        assertEquals(12.5, editableNumber("12,5"))
        assertEquals(12.5, editableNumber(" 12.5 "))
        assertEquals(0.0, editableNumber(""))
        assertEquals(0.0, editableNumber("abc"))
    }

    private fun item(kcal: Double, p: Double, f: Double, c: Double) = MealItem("x", 100.0, Confidence.HIGH, kcal, p, f, c)

    @Test
    fun `meal totals sum every food`() {
        val totals = mealTotals(listOf(item(300.0, 20.0, 10.0, 30.0), item(436.0, 5.5, 12.0, 60.0)))

        assertEquals(MealTotals(736.0, 25.5, 22.0, 90.0), totals)
        assertEquals(MealTotals(0.0, 0.0, 0.0, 0.0), mealTotals(emptyList()))
    }
}
