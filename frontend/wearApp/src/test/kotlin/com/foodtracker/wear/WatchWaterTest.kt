package com.foodtracker.wear

import androidx.work.ListenableWorker
import com.foodtracker.app.data.cache.SendReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WatchWaterTest {
    @Test
    fun `the queue is retried while the backend can't be reached`() {
        assertEquals(ListenableWorker.Result.retry(), watchSyncResult(SendReport(sent = 0, offline = true)))
        assertEquals(ListenableWorker.Result.success(), watchSyncResult(SendReport(sent = 2, offline = false)))
        assertEquals(ListenableWorker.Result.success(), watchSyncResult(SendReport(sent = 0, offline = false)))
    }

    @Test
    fun `a tap on the glass button logs a glass`() {
        assertTrue(isNewTileClick(ADD_GLASS_CLICK, lastHandledAt = 0L, now = 10_000L))
    }

    @Test
    fun `other requests don't`() {
        assertFalse(isNewTileClick("", lastHandledAt = 0L, now = 10_000L))
        assertFalse(isNewTileClick("open_app", lastHandledAt = 0L, now = 10_000L))
    }

    @Test
    fun `a repeated request right after a handled tap isn't a second glass`() {
        assertFalse(isNewTileClick(ADD_GLASS_CLICK, lastHandledAt = 10_000L, now = 11_000L))
        assertTrue(isNewTileClick(ADD_GLASS_CLICK, lastHandledAt = 10_000L, now = 12_000L))
    }

    @Test
    fun `a clock set back doesn't block taps`() {
        assertTrue(isNewTileClick(ADD_GLASS_CLICK, lastHandledAt = 50_000L, now = 1_000L))
    }
}
