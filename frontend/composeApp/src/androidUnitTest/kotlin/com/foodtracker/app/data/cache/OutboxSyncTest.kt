package com.foodtracker.app.data.cache

import androidx.work.BackoffPolicy
import androidx.work.ListenableWorker
import androidx.work.NetworkType
import kotlin.test.Test
import kotlin.test.assertEquals

class OutboxSyncTest {
    private val done = AnalyzeReport(ready = 0, offline = false)
    private val sent = SendReport(sent = 0, offline = false)

    @Test
    fun `the worker retries later while either queue waits for the backend`() {
        assertEquals(ListenableWorker.Result.retry(), OutboxSync.resultFor(SendReport(sent = 1, offline = true), done))
        assertEquals(ListenableWorker.Result.retry(), OutboxSync.resultFor(sent, AnalyzeReport(ready = 1, offline = true)))
        assertEquals(ListenableWorker.Result.retry(), OutboxSync.resultFor(SendReport(0, true), AnalyzeReport(0, true)))
    }

    @Test
    fun `the worker is done when nothing is left, refused rows included`() {
        assertEquals(ListenableWorker.Result.success(), OutboxSync.resultFor(SendReport(sent = 2, offline = false), AnalyzeReport(ready = 1, offline = false)))
        assertEquals(ListenableWorker.Result.success(), OutboxSync.resultFor(sent, done))
    }

    @Test
    fun `the ready notice is posted only when captures became ready`() {
        assertEquals(true, CaptureNotifications.shouldNotify(AnalyzeReport(ready = 2, offline = true)))
        assertEquals(false, CaptureNotifications.shouldNotify(AnalyzeReport(ready = 0, offline = false)))
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
