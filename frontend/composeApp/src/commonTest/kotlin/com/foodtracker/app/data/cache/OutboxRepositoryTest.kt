package com.foodtracker.app.data.cache

import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val HOME = "http://home.local:3000"
private const val DAY = "2026-03-05"
private val ZONE = TimeZone.of("America/Argentina/Buenos_Aires")

/** 13:05 local on [DAY] (16:05 UTC). */
private const val LUNCH_AT = "2026-03-05T16:05:00Z"

private val rice = MealItem(name = "Rice", estimatedGrams = 150.0, confidence = Confidence.HIGH, calories = 200.0, proteinGrams = 4.0, fatGrams = 1.0, carbsGrams = 44.0)

/** How the fake backend answers a POST: store it (201), an error status, or no answer at all. */
private sealed interface Answer {
    data object Store : Answer
    data class Status(val code: HttpStatusCode) : Answer
    data object None : Answer
}

private class OutboxFixture {
    val log = mutableListOf<String>()
    val outbox = FakeOutbox(log)
    val dayCache = FakeDayCache(log)
    var server = HOME
    var syncRequests = 0
    private var nextClientId = 0

    /** Answers in order; the last one repeats. */
    var answers = listOf<Answer>(Answer.Store)
    private var answered = 0

    /** The clientIds the backend received, in order. */
    val received = mutableListOf<String>()

    private val engine = MockEngine { request ->
        val answer = answers[minOf(answered, answers.lastIndex)].also { answered++ }
        if (answer == Answer.None) throw RuntimeException("Failed to connect to /10.0.2.2:3000")
        val body = Json.parseToJsonElement(request.body.toByteArray().decodeToString()).jsonObject
        val clientId = body["clientId"]!!.jsonPrimitive.content
        received += clientId
        if (answer is Answer.Status) {
            return@MockEngine respond("""{"error":"Nope"}""", answer.code, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val json = if (request.url.encodedPath == "/api/water") {
            """{"id":${received.size},"amountMl":${body["amountMl"]!!.jsonPrimitive.int},"loggedAt":${body["loggedAt"]},"clientId":"$clientId"}"""
        } else {
            """{"id":${received.size},"occurredAt":${body["occurredAt"]},"createdAt":"2026-03-05T20:00:00Z","items":[],
               "totalCalories":200,"totalProteinGrams":4,"totalFatGrams":1,"totalCarbsGrams":44,"clientId":"$clientId"}"""
        }
        respond(ByteReadChannel(json), HttpStatusCode.Created, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    val repository = OutboxRepository(
        api = FoodTrackerApiClient(createHttpClient(engine) { null }, baseUrlProvider = { server }),
        outbox = outbox,
        dayCache = dayCache,
        requestSync = { syncRequests++ },
        serverUrl = { server },
        now = { 1_000L },
        newClientId = { "client-${++nextClientId}" },
        timeZone = { ZONE },
    )

    fun lunch() = SaveMealRequest(items = listOf(rice), occurredAt = LUNCH_AT, mealType = MealType.LUNCH)

    fun glass() = LogWaterRequest(amountMl = 250, loggedAt = LUNCH_AT)

    /** Caches [DAY] for [HOME], with no meals and no water. */
    suspend fun cacheEmptyDay() = dayCache.saveDay(
        DayCacheRow(DAY, "[]", """{"date":"$DAY","totalMl":0,"entries":[]}""", HOME, 500L),
    )

    fun cachedMeals() = Json.parseToJsonElement(dayCache.days.value.getValue(DAY).mealsJson).jsonArray

    fun cachedWaterTotal() = Json.parseToJsonElement(dayCache.days.value.getValue(DAY).waterJson).jsonObject["totalMl"]!!.jsonPrimitive.int
}

class OutboxRepositoryTest {
    @Test
    fun `a stored meal is Saved and nothing is queued`() = runBlocking<Unit> {
        val f = OutboxFixture()

        val outcome = f.repository.saveMeal(f.lunch())

        assertEquals("client-1", assertIs<SaveOutcome.Saved<*>>(outcome).let { (it.entry as MealEntry).clientId })
        assertEquals(emptyList(), f.outbox.rows.value)
        assertEquals(0, f.syncRequests)
    }

    @Test
    fun `no answer and retryable statuses queue the meal on its local day and ask for a sync`() = runBlocking<Unit> {
        for (answer in listOf(Answer.None, Answer.Status(HttpStatusCode.InternalServerError), Answer.Status(HttpStatusCode.RequestTimeout), Answer.Status(HttpStatusCode.TooManyRequests))) {
            val f = OutboxFixture().apply { answers = listOf(answer) }

            assertEquals(SaveOutcome.Queued, f.repository.saveMeal(f.lunch()), "for $answer")

            val row = f.outbox.rows.value.single()
            assertEquals(OutboxKind.MEAL, row.kind)
            assertEquals(DAY, row.day)
            assertEquals(HOME, row.serverUrl)
            assertEquals("client-1", row.clientId)
            val payload = Json.parseToJsonElement(row.payloadJson).jsonObject
            assertEquals(LUNCH_AT, payload["occurredAt"]!!.jsonPrimitive.content)
            assertEquals("client-1", payload["clientId"]!!.jsonPrimitive.content)
            assertEquals(1, f.syncRequests)
        }
    }

    @Test
    fun `a refusal is Rejected and not queued`() = runBlocking<Unit> {
        for (code in listOf(HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized)) {
            val f = OutboxFixture().apply { answers = listOf(Answer.Status(code)) }

            val outcome = assertIs<SaveOutcome.Rejected>(f.repository.logWater(f.glass(), DAY))

            assertEquals(code.value, outcome.error.status)
            assertEquals(emptyList(), f.outbox.rows.value)
            assertEquals(0, f.syncRequests)
        }
    }

    @Test
    fun `sendAll sends oldest first and merges each entry into the cached day before deleting its row`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.cacheEmptyDay()
        f.repository.saveMeal(f.lunch())
        f.repository.logWater(f.glass(), DAY)
        f.repository.saveMeal(f.lunch())
        f.received.clear()
        f.log.clear()
        f.answers = listOf(Answer.Store)

        assertEquals(SendReport(sent = 3, offline = false), f.repository.sendAll())

        assertEquals(listOf("client-1", "client-2", "client-3"), f.received)
        assertEquals(emptyList(), f.outbox.rows.value)
        assertEquals(2, f.cachedMeals().size)
        assertEquals(250, f.cachedWaterTotal())
        assertEquals(listOf("cache $DAY", "delete 1", "cache $DAY", "delete 2", "cache $DAY", "delete 3"), f.log)
    }

    @Test
    fun `sendAll stops at the first unanswered row and keeps the rest`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.saveMeal(f.lunch())
        f.repository.saveMeal(f.lunch())

        assertEquals(SendReport(sent = 0, offline = true), f.repository.sendAll())

        assertEquals(2, f.outbox.rows.value.size)
        assertTrue(f.outbox.rows.value.all { it.error == null })
    }

    @Test
    fun `sendAll marks refused rows and goes on with the next`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.saveMeal(f.lunch())
        f.repository.saveMeal(f.lunch())
        f.answers = listOf(Answer.None, Answer.None, Answer.Status(HttpStatusCode.Unauthorized), Answer.Store)

        assertEquals(SendReport(sent = 1, offline = false), f.repository.sendAll())

        val rejected = f.outbox.rows.value.single()
        assertEquals("client-1", rejected.clientId)
        assertEquals(401, rejected.errorStatus)
        assertEquals("Nope", rejected.error)
        // Not sent again on its own.
        f.received.clear()
        assertEquals(SendReport(sent = 0, offline = false), f.repository.sendAll())
        assertEquals(emptyList(), f.received)
    }

    @Test
    fun `a meal already in the cached day is not appended twice`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.saveMeal(f.lunch())
        f.dayCache.saveDay(
            DayCacheRow(
                DAY,
                """[{"id":9,"occurredAt":"$LUNCH_AT","createdAt":"$LUNCH_AT","items":[],"totalCalories":200,
                   "totalProteinGrams":4,"totalFatGrams":1,"totalCarbsGrams":44,"clientId":"client-1"}]""",
                """{"date":"$DAY","totalMl":0,"entries":[]}""",
                HOME,
                500L,
            ),
        )
        f.answers = listOf(Answer.Store)

        f.repository.sendAll()

        assertEquals(1, f.cachedMeals().size)
    }

    @Test
    fun `retry sends a refused row again and discard deletes one`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.saveMeal(f.lunch())
        f.repository.saveMeal(f.lunch())
        f.answers = listOf(Answer.Status(HttpStatusCode.Unauthorized))
        f.repository.sendAll()
        val (first, second) = f.outbox.rows.value
        f.answers = listOf(Answer.Store)

        assertEquals(SendReport(sent = 1, offline = false), f.repository.retry(first.id))
        f.repository.discard(second.id)

        assertEquals(emptyList(), f.outbox.rows.value)
    }

    @Test
    fun `another server's rows are neither observed nor sent`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.saveMeal(f.lunch())
        f.server = "http://other.local:3000"
        f.received.clear()
        f.answers = listOf(Answer.Store)

        assertEquals(SendReport(sent = 0, offline = false), f.repository.sendAll())
        assertEquals(emptyList(), f.repository.observeDay(DAY).first())
        assertEquals(emptyList(), f.received)

        f.server = HOME
        assertEquals(SendReport(sent = 1, offline = false), f.repository.sendAll())
    }

    @Test
    fun `undo removes the day's newest pending glass, and reports when there is none`() = runBlocking<Unit> {
        val f = OutboxFixture().apply { answers = listOf(Answer.None) }
        f.repository.logWater(f.glass(), DAY)
        f.repository.saveMeal(f.lunch())
        f.repository.logWater(f.glass().copy(amountMl = 500), DAY)

        assertTrue(f.repository.undoPendingWater(DAY))
        assertEquals(listOf(250), f.outbox.rows.value.filter { it.kind == OutboxKind.WATER }.map { Json.parseToJsonElement(it.payloadJson).jsonObject["amountMl"]!!.jsonPrimitive.int })
        assertTrue(f.repository.undoPendingWater(DAY))
        assertEquals(false, f.repository.undoPendingWater(DAY))
        assertEquals(1, f.outbox.rows.value.size)
    }
}
