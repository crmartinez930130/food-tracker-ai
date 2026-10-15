package com.foodtracker.app.data.cache

import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
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

/** An in-memory [DayCacheStore], so the repository is tested without Android or a database. */
private class FakeStore : DayCacheStore {
    val days = MutableStateFlow<Map<String, DayCacheRow>>(emptyMap())
    val goal = MutableStateFlow<GoalCacheRow?>(null)

    override fun observeDay(date: String): Flow<DayCacheRow?> = days.map { it[date] }

    override fun observeGoal(): Flow<GoalCacheRow?> = goal

    override suspend fun save(day: DayCacheRow, goal: GoalCacheRow) {
        days.value = days.value + (day.date to day)
        this.goal.value = goal
    }

    override suspend fun deleteDay(date: String) {
        days.value = days.value - date
    }

    override suspend fun deleteGoal() {
        goal.value = null
    }

    override suspend fun clear() {
        days.value = emptyMap()
        goal.value = null
    }
}

/** What the fake backend does: answer with JSON, answer with an error status, or not answer at all. */
private sealed interface Backend {
    data object Ok : Backend
    data class Error(val status: HttpStatusCode) : Backend
    data object Unreachable : Backend
}

private class Fixture(var backend: Backend = Backend.Ok, var server: String = SERVER) {
    val store = FakeStore()
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
    fun `a refresh caches the day and the goal`() = runBlocking {
        val f = Fixture()

        assertEquals(RefreshResult.Success, f.repository.refresh(DAY))
        val cached = assertNotNull(f.repository.observe(DAY).first())

        assertEquals(listOf("Rice"), cached.meals.single().items.map { it.name })
        assertEquals(250, cached.water.totalMl)
        assertEquals(2000.0, cached.goal.targetCalories)
        assertEquals(1_000L, cached.refreshedAt)
    }

    @Test
    fun `nothing is cached before the first refresh`() = runBlocking {
        assertNull(Fixture().repository.observe(DAY).first())
    }

    @Test
    fun `an unreachable backend is Offline and keeps the cached copy`() = runBlocking {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.backend = Backend.Unreachable
        f.clock = 9_000L

        assertIs<RefreshResult.Offline>(f.repository.refresh(DAY))
        assertEquals(1_000L, f.repository.observe(DAY).first()?.refreshedAt)
    }

    @Test
    fun `an error answer is Failed with its status and keeps the cached copy`() = runBlocking {
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
    fun `another server's rows are not shown`() = runBlocking {
        val f = Fixture()
        f.repository.refresh(DAY)

        f.server = "http://other.local:3000"

        assertNull(f.repository.observe(DAY).first())
    }

    @Test
    fun `a refresh whose server changed while it ran is not cached`() = runBlocking {
        val f = Fixture()
        f.onRequest = { f.server = "http://other.local:3000" }

        f.repository.refresh(DAY)

        assertEquals(emptyMap(), f.store.days.value)
        assertNull(f.store.goal.value)
    }

    @Test
    fun `a row that no longer decodes is treated as missing and deleted`() = runBlocking {
        val f = Fixture()
        f.repository.refresh(DAY)
        f.store.days.value = f.store.days.value.mapValues { it.value.copy(mealsJson = """{"unexpected":true}""") }

        assertNull(f.repository.observe(DAY).first())
        assertEquals(emptyMap(), f.store.days.value)
    }

    @Test
    fun `clear empties the cache`() = runBlocking {
        val f = Fixture()
        f.repository.refresh(DAY)

        f.repository.clear()

        assertNull(f.repository.observe(DAY).first())
        assertNull(f.store.goal.value)
    }
}
