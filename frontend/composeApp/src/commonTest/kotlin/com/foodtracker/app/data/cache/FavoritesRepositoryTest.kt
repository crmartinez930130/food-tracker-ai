package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private fun favorite(id: Int) = cacheJson.decodeFromString<MealEntry>(favoriteJson(id))

private fun favoriteJson(id: Int) = """{"id":$id,"occurredAt":"2026-03-05T12:00:00Z","createdAt":"2026-03-05T12:00:00Z","isFavorite":true,
"items":[],"totalCalories":300,"totalProteinGrams":20,"totalFatGrams":10,"totalCarbsGrams":30}"""

class FavoritesRepositoryTest {
    private var server = "http://home.local:3000"
    private var body: String? = "[${favoriteJson(1)}]"
    private var status = HttpStatusCode.OK
    private var clock = 1_000L
    private val store = FakeFavoritesCache()
    private val engine = MockEngine {
        val answer = body ?: throw RuntimeException("Failed to connect")
        respond(ByteReadChannel(answer), status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    private val repository = FavoritesRepository(
        FoodTrackerApiClient(createHttpClient(engine) { null }, baseUrlProvider = { server }),
        store,
        serverUrl = { server },
        now = { clock },
    )

    @Test
    fun `a refresh keeps the list as the copy with its time, and the next one replaces it`() = runBlocking<Unit> {
        assertNull(repository.cached())

        assertEquals(listOf(1), assertIs<Refreshed.Fresh<List<MealEntry>>>(repository.refresh()).value.map { it.id })
        assertEquals(listOf(1), repository.cached()?.value?.map { it.id })
        assertEquals(1_000L, repository.cached()?.refreshedAt)

        body = "[${favoriteJson(1)},${favoriteJson(2)}]"
        clock = 2_000L
        repository.refresh()
        assertEquals(listOf(1, 2), repository.cached()?.value?.map { it.id })
        assertEquals(2_000L, repository.cached()?.refreshedAt)
    }

    @Test
    fun `without an answer the refresh is offline and keeps the previous copy`() = runBlocking<Unit> {
        repository.refresh()
        body = null

        assertIs<Refreshed.Offline>(repository.refresh())

        assertEquals(listOf(1), repository.cached()?.value?.map { it.id })
    }

    @Test
    fun `an error answer fails the refresh and keeps the previous copy`() = runBlocking<Unit> {
        repository.refresh()
        body = """{"error":"Database unavailable"}"""
        status = HttpStatusCode.InternalServerError

        assertEquals(500, assertIs<Refreshed.Failed>(repository.refresh()).error.status)

        assertEquals(listOf(1), repository.cached()?.value?.map { it.id })
    }

    @Test
    fun `another server's copy isn't used`() = runBlocking<Unit> {
        repository.refresh()

        server = "http://other.local:3000"

        assertNull(repository.cached())
    }
}

class FavoritesViewTest {
    private val copy = Snapshot(listOf(favorite(1)), refreshedAt = 1_000L)
    private val fresh = Refreshed.Fresh(listOf(favorite(1), favorite(2)))
    private val offline = Refreshed.Offline(RuntimeException("Failed to connect"))
    private val failed = Refreshed.Failed(ApiException("Database unavailable", 500))

    @Test
    fun `a fresh list is shown without a notice, with or without a copy`() {
        assertEquals(FavoritesView(fresh.value), favoritesView(copy, fresh))
        assertEquals(FavoritesView(fresh.value), favoritesView(null, fresh))
    }

    @Test
    fun `offline shows the copy with its time`() {
        assertEquals(FavoritesView(copy.value, offlineSince = 1_000L), favoritesView(copy, offline))
    }

    @Test
    fun `offline without a copy says the favorites were never loaded`() {
        assertEquals(FavoritesView(null, notLoaded = true), favoritesView(null, offline))
    }

    @Test
    fun `an error answer shows the copy without a notice`() {
        assertEquals(FavoritesView(copy.value), favoritesView(copy, failed))
    }

    @Test
    fun `an error answer without a copy is reported`() {
        assertEquals(FavoritesView(null, error = failed.error), favoritesView(null, failed))
    }
}
