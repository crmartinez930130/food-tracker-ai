package com.foodtracker.app.data.cache

import com.foodtracker.app.data.DEFAULT_GOAL
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private const val DAY = "2026-03-05"
private const val SERVER = "http://home.local:3000"

private const val MEALS_JSON = """[{"id":1,"occurredAt":"2026-03-05T12:00:00.000Z","createdAt":"2026-03-05T12:00:01.000Z",
"items":[{"name":"Rice","estimatedGrams":150,"confidence":"high","calories":200,"proteinGrams":4,"fatGrams":1,"carbsGrams":44}],
"totalCalories":200,"totalProteinGrams":4,"totalFatGrams":1,"totalCarbsGrams":44,"clientId":null}]"""
private const val GOAL_JSON = """{"targetCalories":2000,"targetProteinGrams":125,"targetFatGrams":67,"targetCarbsGrams":225,
"targetWaterMl":2000,"waterGlassMl":250,"isDefault":false}"""
private const val WATER_JSON = """{"date":"2026-03-05","totalMl":250,"entries":[{"id":1,"amountMl":250,"loggedAt":"2026-03-05T12:00:00.000Z","clientId":null}]}"""

/** What the fake backend does: answer with JSON, answer with an error status, or not answer at all. */
private sealed interface Backend {
    data object Ok : Backend
    data class Error(val status: HttpStatusCode) : Backend
    data object Unreachable : Backend
}

private class Fixture(var backend: Backend = Backend.Ok, var server: String = SERVER) {
    val store = FakeDayCache()
    var clock = 1_000L
    /** Runs on each request, before it is answered (e.g. to switch servers mid-refresh). */
    var onRequest: () -> Unit = {}

    private val engine = MockEngine { request ->
        onRequest()
        when (val b = backend) {
            Backend.Unreachable -> throw RuntimeException("Failed to connect to /10.0.2.2:3000")
            is Backend.Error -> respond("""{"error":"Nope"}""", b.status, headersOf(HttpHeaders.ContentType, "application/json"))
            Backend.Ok -> {
                val body = when (request.url.encodedPath) {
                    "/api/meals" -> MEALS_JSON
                    "/api/goals" -> GOAL_JSON
                    else -> WATER_JSON
                }
                respond(ByteReadChannel(body), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
            }
        }
    }

    val repository = DayRepository(
        api = FoodTrackerApiClient(createHttpClient(engine) { null }, baseUrlProvider = { server }),
        store = store,
        serverUrl = { server },
        now = { clock },
    )
}

class DayRepositoryTest {
    @Test
    fun `a refresh caches the day and the goal`() = runBlocking<Unit> {
        val f = Fixture()

        assertEquals(RefreshResult.Success, f.repository.refresh(DAY))
        val cached = assertNotNull(f.repository.observe(DAY).first())

        assertEquals(listOf("Rice"), cached.meals.single().items.map { it.name })
        assertEquals(250, cached.water.totalMl)
        assertEquals(2000.0, cached.goal.targetCalories)
        assertEquals(1_000L, cached.refreshedAt)
    }

    @Test
    fun `nothing is cached before the first refresh`() = runBlocking<Unit> {
        assertNull(Fixture().repository.observe(DAY).first())
    }

    @Test
    fun `an unreachable backend is Offline and keeps the cached copy`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.backend = Backend.Unreachable
        f.clock = 9_000L

        assertIs<RefreshResult.Offline>(f.repository.refresh(DAY))
        assertEquals(1_000L, f.repository.observe(DAY).first()?.refreshedAt)
    }

    @Test
    fun `an error answer is Failed with its status and keeps the cached copy`() = runBlocking<Unit> {
        for (status in listOf(HttpStatusCode.Unauthorized, HttpStatusCode.InternalServerError)) {
            val f = Fixture()
            f.repository.refresh(DAY)
            f.backend = Backend.Error(status)

            val result = assertIs<RefreshResult.Failed>(f.repository.refresh(DAY))

            assertEquals(status.value, result.error.status)
            assertNotNull(f.repository.observe(DAY).first())
        }
    }

    @Test
    fun `another server's rows are not shown`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)

        f.server = "http://other.local:3000"

        assertNull(f.repository.observe(DAY).first())
    }

    @Test
    fun `a refresh whose server changed while it ran is not cached`() = runBlocking<Unit> {
        val f = Fixture()
        f.onRequest = { f.server = "http://other.local:3000" }

        f.repository.refresh(DAY)

        assertEquals(emptyMap(), f.store.days.value)
        assertNull(f.store.goal.value)
    }

    @Test
    fun `a day that no longer decodes is deleted alone, keeping the goal`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.store.days.value = f.store.days.value.mapValues { it.value.copy(mealsJson = """{"unexpected":true}""") }

        assertNull(f.repository.observe(DAY).first())
        assertEquals(emptyMap(), f.store.days.value)
        assertNotNull(f.store.goal.value)
    }

    @Test
    fun `a goal that no longer decodes is deleted alone, keeping the day`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.store.goal.value = f.store.goal.value?.copy(goalJson = "[]")

        assertNull(f.repository.observe(DAY).first())
        assertNull(f.store.goal.value)
        assertNotNull(f.store.days.value[DAY])
    }

    @Test
    fun `deleteOtherServers keeps the current server's rows and drops the rest`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.server = "http://other.local:3000"
        f.repository.refresh("2026-03-06")

        f.repository.deleteOtherServers("http://other.local:3000")

        assertEquals(setOf("2026-03-06"), f.store.days.value.keys)
        assertEquals("http://other.local:3000", f.store.goal.value?.serverUrl)
    }

    @Test
    fun `the offline placeholder has no meals or water, and the cached goal`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.store.days.value = emptyMap()

        val placeholder = f.repository.offlinePlaceholder("2026-03-06")

        assertEquals(emptyList(), placeholder.meals)
        assertEquals(WaterDay("2026-03-06", 0, emptyList()), placeholder.water)
        assertEquals(2000.0, placeholder.goal.targetCalories)
        assertEquals(false, placeholder.goal.isDefault)
        assertNull(placeholder.refreshedAt)
        assertEquals(emptyMap(), f.store.days.value)
    }

    @Test
    fun `the offline placeholder uses the default goal without a cached one for this server`() = runBlocking<Unit> {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.server = "http://other.local:3000"

        assertEquals(DEFAULT_GOAL, f.repository.offlinePlaceholder(DAY).goal)
    }
}
