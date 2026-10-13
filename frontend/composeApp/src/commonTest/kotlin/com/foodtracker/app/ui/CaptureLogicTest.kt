package com.foodtracker.app.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
