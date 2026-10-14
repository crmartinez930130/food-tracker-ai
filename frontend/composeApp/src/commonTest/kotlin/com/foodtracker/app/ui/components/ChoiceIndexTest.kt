package com.foodtracker.app.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class ChoiceIndexTest {

    private val options = listOf("system" to "Teléfono", "es" to "Español", "en" to "English")

    @Test
    fun `each stored value maps to its button`() {
        assertEquals(0, choiceIndex(options, "system"))
        assertEquals(1, choiceIndex(options, "es"))
        assertEquals(2, choiceIndex(options, "en"))
    }

    @Test
    fun `an unknown value selects nothing`() {
        assertEquals(-1, choiceIndex(options, "fr"))
    }
}
