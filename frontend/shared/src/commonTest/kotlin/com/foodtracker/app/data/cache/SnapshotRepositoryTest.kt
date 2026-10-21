package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@Serializable
private data class Month(val days: List<Int>, val target: Double)

private const val KEY = "history:2026-03-01"

class SnapshotRepositoryTest {
    private val store = FakeSnapshotStore()
    private var server = "http://home.local:3000"
    private var clock = 1_000L
    private val repository = SnapshotRepository(store, serverUrl = { server }, now = { clock })

    @Test
    fun `a refresh keeps the value, and cached returns it with its time`() = runBlocking<Unit> {
        val fresh = repository.refresh(KEY, Month.serializer()) { Month(listOf(1, 2), 2000.0) }

        assertEquals(Month(listOf(1, 2), 2000.0), assertIs<Refreshed.Fresh<Month>>(fresh).value)
        assertEquals(Snapshot(Month(listOf(1, 2), 2000.0), 1_000L), repository.cached(KEY, Month.serializer()))
    }

    @Test
    fun `no answer is Offline and keeps the copy`() = runBlocking<Unit> {
        repository.refresh(KEY, Month.serializer()) { Month(listOf(1), 1800.0) }
        clock = 9_000L

        val result = repository.refresh<Month>(KEY, Month.serializer()) { throw RuntimeException("Failed to connect") }

        assertIs<Refreshed.Offline>(result)
        assertEquals(1_000L, repository.cached(KEY, Month.serializer())?.refreshedAt)
    }

    @Test
    fun `an error answer is Failed with its status and keeps the copy`() = runBlocking<Unit> {
        repository.refresh(KEY, Month.serializer()) { Month(listOf(1), 1800.0) }

        val result = repository.refresh<Month>(KEY, Month.serializer()) { throw ApiException("Invalid access token", 401) }

        assertEquals(401, assertIs<Refreshed.Failed>(result).error.status)
        assertEquals(Month(listOf(1), 1800.0), repository.cached(KEY, Month.serializer())?.value)
    }

    @Test
    fun `another server's copy isn't returned`() = runBlocking<Unit> {
        repository.refresh(KEY, Month.serializer()) { Month(listOf(1), 1800.0) }

        server = "http://other.local:3000"

        assertNull(repository.cached(KEY, Month.serializer()))
    }

    @Test
    fun `a copy that no longer decodes is dropped`() = runBlocking<Unit> {
        store.save(SnapshotRow(KEY, """{"unexpected":true}""", server, 1L))

        assertNull(repository.cached(KEY, Month.serializer()))
        assertEquals(emptyMap(), store.rows)
    }

    @Test
    fun `a refresh whose server changed while it ran isn't kept`() = runBlocking<Unit> {
        repository.refresh(KEY, Month.serializer()) {
            server = "http://other.local:3000"
            Month(listOf(1), 1800.0)
        }

        assertEquals(emptyMap(), store.rows)
    }

    @Test
    fun `deleteOtherServers keeps only the current server's copies`() = runBlocking<Unit> {
        repository.refresh(KEY, Month.serializer()) { Month(listOf(1), 1800.0) }
        server = "http://other.local:3000"
        repository.refresh("summary:2026-03-01:2026-03-07", Month.serializer()) { Month(listOf(2), 1800.0) }

        repository.deleteOtherServers(server)

        assertEquals(setOf("summary:2026-03-01:2026-03-07"), store.rows.keys)
    }
}
