package com.foodtracker.app.data.cache

import com.foodtracker.app.data.FoodTrackerApiClient
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
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

private fun favoriteJson(id: Int) = """{"id":$id,"occurredAt":"2026-03-05T12:00:00Z","createdAt":"2026-03-05T12:00:00Z","isFavorite":true,
"items":[],"totalCalories":300,"totalProteinGrams":20,"totalFatGrams":10,"totalCarbsGrams":30}"""

class FavoritesRepositoryTest {
    private var server = "http://home.local:3000"
    private var body: String? = "[${favoriteJson(1)}]"
    private val store = FakeFavoritesCache()
    private val engine = MockEngine {
        val answer = body ?: throw RuntimeException("Failed to connect")
        respond(ByteReadChannel(answer), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    private val repository = FavoritesRepository(
        FoodTrackerApiClient(createHttpClient(engine) { null }, baseUrlProvider = { server }),
        store,
        serverUrl = { server },
        now = { 1_000L },
    )

    @Test
    fun `a load keeps the list as the copy, and the next load replaces it`() = runBlocking<Unit> {
        assertNull(repository.cached())

        repository.load()
        assertEquals(listOf(1), repository.cached()?.map { it.id })

        body = "[${favoriteJson(1)},${favoriteJson(2)}]"
        repository.load()
        assertEquals(listOf(1, 2), repository.cached()?.map { it.id })
    }

    @Test
    fun `a failed load keeps the previous copy`() = runBlocking<Unit> {
        repository.load()
        body = null

        assertFailsWith<RuntimeException> { repository.load() }

        assertEquals(listOf(1), repository.cached()?.map { it.id })
    }

    @Test
    fun `another server's copy isn't used`() = runBlocking<Unit> {
        repository.load()

        server = "http://other.local:3000"

        assertNull(repository.cached())
    }
}
