package com.foodtracker.app.data

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpMethod
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SAMPLE_MEALS_JSON = """
[
  {
    "id": 1,
    "occurredAt": "2026-01-01T08:00:00.000Z",
    "createdAt": "2026-01-01T08:00:01.000Z",
    "notes": null,
    "items": [
      {
        "name": "Grilled chicken breast",
        "estimatedGrams": 150,
        "confidence": "high",
        "calories": 248,
        "proteinGrams": 46.5,
        "fatGrams": 5.4,
        "carbsGrams": 0
      }
    ],
    "totalCalories": 248,
    "totalProteinGrams": 46.5,
    "totalFatGrams": 5.4,
    "totalCarbsGrams": 0
  }
]
"""

class FoodTrackerApiClientTest {

    private fun mockedApiClient(responseBody: String): FoodTrackerApiClient {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel(responseBody),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        return FoodTrackerApiClient(createHttpClient(engine), baseUrlProvider = { "http://test.local" })
    }

    @Test
    fun `deserializes a meal list response into MealEntry objects`() = runBlocking {
        val api = mockedApiClient(SAMPLE_MEALS_JSON)

        val meals = api.listMeals("2026-01-01")

        assertEquals(1, meals.size)
        assertEquals("Grilled chicken breast", meals[0].items[0].name)
        assertEquals(Confidence.HIGH, meals[0].items[0].confidence)
        assertEquals(248.0, meals[0].totalCalories)
        assertEquals(null, meals[0].mealType)
    }

    @Test
    fun `deserializes a mealType when present in the response`() = runBlocking {
        val api = mockedApiClient(
            """
            [
              {
                "id": 2,
                "occurredAt": "2026-01-01T08:00:00.000Z",
                "createdAt": "2026-01-01T08:00:01.000Z",
                "notes": null,
                "mealType": "breakfast",
                "items": [],
                "totalCalories": 0,
                "totalProteinGrams": 0,
                "totalFatGrams": 0,
                "totalCarbsGrams": 0
              }
            ]
            """.trimIndent(),
        )

        val meals = api.listMeals("2026-01-01")

        assertEquals(MealType.BREAKFAST, meals[0].mealType)
    }

    @Test
    fun `deserializes an empty list response`() = runBlocking {
        val api = mockedApiClient("[]")

        val meals = api.listMeals("2026-01-02")

        assertEquals(0, meals.size)
    }

    @Test
    fun `a non-2xx response throws ApiException with the backend's error message`() = runBlocking {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"error":"Vision analysis request failed"}"""),
                status = HttpStatusCode.BadGateway,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = FoodTrackerApiClient(createHttpClient(engine), baseUrlProvider = { "http://test.local" })

        val exception = assertFailsWith<ApiException> {
            api.analyzeMealPhoto(byteArrayOf(1, 2, 3))
        }
        assertEquals("Vision analysis request failed", exception.message)
    }

    @Test
    fun `deserializes the default goal`() = runBlocking {
        val api = mockedApiClient(
            """{"targetCalories":2000,"targetProteinGrams":125,"targetFatGrams":67,"targetCarbsGrams":225,"isDefault":true}""",
        )

        val goal = api.getGoals()

        assertEquals(2000.0, goal.targetCalories)
        assertEquals(67.0, goal.targetFatGrams)
        assertTrue(goal.isDefault)
    }

    @Test
    fun `deserializes a configured goal`() = runBlocking {
        val api = mockedApiClient(
            """{"targetCalories":1800,"targetProteinGrams":140.5,"targetFatGrams":60,"targetCarbsGrams":170,"isDefault":false}""",
        )

        val goal = api.getGoals()

        assertEquals(1800.0, goal.targetCalories)
        assertEquals(140.5, goal.targetProteinGrams)
        assertFalse(goal.isDefault)
    }

    @Test
    fun `updateGoals sends a PUT with the targets as JSON`() = runBlocking {
        var sentMethod: HttpMethod? = null
        var sentPath: String? = null
        var sentBody = ""
        val engine = MockEngine { request ->
            sentMethod = request.method
            sentPath = request.url.encodedPath
            sentBody = request.body.toByteArray().decodeToString()
            respond(
                content = ByteReadChannel(
                    """{"targetCalories":1800,"targetProteinGrams":140,"targetFatGrams":60,"targetCarbsGrams":170,"isDefault":false}""",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = FoodTrackerApiClient(createHttpClient(engine), baseUrlProvider = { "http://test.local" })

        val saved = api.updateGoals(UpdateGoalsRequest(1800.0, 140.0, 60.0, 170.0))

        assertEquals(HttpMethod.Put, sentMethod)
        assertEquals("/api/goals", sentPath)
        val body = Json.parseToJsonElement(sentBody).jsonObject
        assertEquals(JsonPrimitive(1800.0), body["targetCalories"])
        assertEquals(JsonPrimitive(140.0), body["targetProteinGrams"])
        assertEquals(JsonPrimitive(60.0), body["targetFatGrams"])
        assertEquals(JsonPrimitive(170.0), body["targetCarbsGrams"])
        assertFalse(saved.isDefault)
    }

    @Test
    fun `updateGoals surfaces a validation error as ApiException`() = runBlocking {
        val engine = MockEngine { _ ->
            respond(
                content = ByteReadChannel("""{"error":"targetCalories: Number must be greater than 0"}"""),
                status = HttpStatusCode.BadRequest,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = FoodTrackerApiClient(createHttpClient(engine), baseUrlProvider = { "http://test.local" })

        val exception = assertFailsWith<ApiException> {
            api.updateGoals(UpdateGoalsRequest(0.0, 140.0, 60.0, 170.0))
        }
        assertEquals("targetCalories: Number must be greater than 0", exception.message)
    }

    @Test
    fun `deserializes favorites, defaulting isFavorite to false when absent`() = runBlocking {
        val api = mockedApiClient(
            """
            [
              {"id": 7, "occurredAt": "2026-01-01T08:00:00.000Z", "createdAt": "2026-01-01T08:00:01.000Z",
               "isFavorite": true, "items": [], "totalCalories": 300, "totalProteinGrams": 20,
               "totalFatGrams": 10, "totalCarbsGrams": 30},
              {"id": 8, "occurredAt": "2026-01-02T08:00:00.000Z", "createdAt": "2026-01-02T08:00:01.000Z",
               "items": [], "totalCalories": 0, "totalProteinGrams": 0, "totalFatGrams": 0, "totalCarbsGrams": 0}
            ]
            """.trimIndent(),
        )

        val meals = api.listFavorites()

        assertTrue(meals[0].isFavorite)
        assertFalse(meals[1].isFavorite)
    }

    @Test
    fun `setFavorite sends a PATCH with only isFavorite in the body`() = runBlocking {
        var sentMethod: HttpMethod? = null
        var sentPath: String? = null
        var sentBody = ""
        val engine = MockEngine { request ->
            sentMethod = request.method
            sentPath = request.url.encodedPath
            sentBody = request.body.toByteArray().decodeToString()
            respond(
                content = ByteReadChannel(
                    """{"id":7,"occurredAt":"2026-01-01T08:00:00.000Z","createdAt":"2026-01-01T08:00:01.000Z",""" +
                        """"isFavorite":true,"items":[],"totalCalories":0,"totalProteinGrams":0,"totalFatGrams":0,"totalCarbsGrams":0}""",
                ),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val api = FoodTrackerApiClient(createHttpClient(engine), baseUrlProvider = { "http://test.local" })

        val updated = api.setFavorite(7, true)

        assertEquals(HttpMethod.Patch, sentMethod)
        assertEquals("/api/meals/7", sentPath)
        // Exactly one key: any extra (e.g. "notes": null) would clear data on the backend.
        assertEquals(mapOf("isFavorite" to JsonPrimitive(true)), Json.parseToJsonElement(sentBody).jsonObject.toMap())
        assertTrue(updated.isFavorite)
    }

    @Test
    fun `toReviewAnalysis carries items, totals and notes over`() {
        val item = MealItem("Oatmeal", 250.0, Confidence.HIGH, 300.0, 10.0, 6.0, 50.0)
        val entry = MealEntry(
            id = 3,
            occurredAt = "2026-01-01T08:00:00.000Z",
            createdAt = "2026-01-01T08:00:01.000Z",
            notes = "with honey",
            mealType = MealType.BREAKFAST,
            isFavorite = true,
            items = listOf(item),
            totalCalories = 300.0,
            totalProteinGrams = 10.0,
            totalFatGrams = 6.0,
            totalCarbsGrams = 50.0,
        )

        val analysis = entry.toReviewAnalysis()

        assertEquals(listOf(item), analysis.items)
        assertEquals(300.0, analysis.totalCalories)
        assertEquals(10.0, analysis.totalProteinGrams)
        assertEquals(6.0, analysis.totalFatGrams)
        assertEquals(50.0, analysis.totalCarbsGrams)
        assertEquals("with honey", analysis.notes)
    }
}
