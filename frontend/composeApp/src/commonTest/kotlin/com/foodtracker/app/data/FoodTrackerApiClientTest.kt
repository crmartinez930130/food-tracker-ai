package com.foodtracker.app.data

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
}
