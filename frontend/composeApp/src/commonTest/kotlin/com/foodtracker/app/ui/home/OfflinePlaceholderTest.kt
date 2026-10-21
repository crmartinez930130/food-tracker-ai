package com.foodtracker.app.ui.home

import com.foodtracker.app.data.DEFAULT_GOAL
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.cache.CachedDay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

private val placeholder = CachedDay(emptyList(), WaterDay("2026-10-21", 0, emptyList()), DEFAULT_GOAL, refreshedAt = null)

class OfflinePlaceholderTest {
    @Test
    fun `a copy that shows while the placeholder is built wins`() = runBlocking {
        var copyShown = false
        val result = placeholderIfStillMissing(isShown = { copyShown }) {
            // The cached copy is collected while the placeholder reads the goal (the cold-start race).
            copyShown = true
            placeholder
        }

        assertNull(result)
    }

    @Test
    fun `with no copy the placeholder is used`() = runBlocking {
        val result = placeholderIfStillMissing(isShown = { false }) { placeholder }

        assertEquals(placeholder, result)
    }

    @Test
    fun `with the copy already shown nothing is built`() = runBlocking {
        var built = false
        val result = placeholderIfStillMissing(isShown = { true }) {
            built = true
            placeholder
        }

        assertNull(result)
        assertFalse(built)
    }
}
