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
        assertTrue(isNewTileClick(glassClickId(1_000L), lastHandledId = null))
    }

    @Test
    fun `other requests don't`() {
        assertFalse(isNewTileClick("", lastHandledId = null))
        assertFalse(isNewTileClick("open_app", lastHandledId = null))
    }

    @Test
    fun `a request repeating a handled tap's id never logs a second glass, however late`() {
        val tap = glassClickId(1_000L)
        assertFalse(isNewTileClick(tap, lastHandledId = tap))
    }

    @Test
    fun `a tap on a newer drawing of the tile is a new glass`() {
        assertTrue(isNewTileClick(glassClickId(2_000L), lastHandledId = glassClickId(1_000L)))
    }
}
