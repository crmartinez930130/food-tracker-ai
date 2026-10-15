package com.foodtracker.app.data.cache

import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals

class OutboxSyncTest {
    @Test
    fun `the worker retries later while the backend can't be reached`() {
        assertEquals(ListenableWorker.Result.retry(), OutboxSync.resultFor(SendReport(sent = 1, offline = true)))
    }

    @Test
    fun `the worker is done when nothing is left to send, refused rows included`() {
        assertEquals(ListenableWorker.Result.success(), OutboxSync.resultFor(SendReport(sent = 2, offline = false)))
        assertEquals(ListenableWorker.Result.success(), OutboxSync.resultFor(SendReport(sent = 0, offline = false)))
    }

    @Test
    fun `the send waits for a network and backs off exponentially from 30 seconds`() {
        val spec = OutboxSync.syncRequest().workSpec

        assertEquals(OutboxSyncWorker::class.java.name, spec.workerClassName)
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(30_000L, spec.backoffDelayDuration)
    }
}
