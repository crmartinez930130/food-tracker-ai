package com.foodtracker.app.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable

/**
 * Thrown when the backend responds with a non-2xx status; [message] is the backend's own error
 * text and [status] the HTTP status (401: the access token was missing or rejected).
 */
class ApiException(message: String, val status: Int) : Exception(message)

@Serializable
private data class ApiErrorBody(val error: String)

/**
 * Talks to the food-tracker backend over the REST contract defined in
 * openspec/changes/setup-project-foundation/design.md. The backend base URL
 * is resolved lazily on every call so changing [BackendConfig.baseUrl] at
 * runtime (e.g. from a settings field) takes effect immediately.
 */
class FoodTrackerApiClient(
    private val httpClient: HttpClient,
    private val baseUrlProvider: () -> String = { BackendConfig.baseUrl },
) {
    private val baseUrl: String get() = baseUrlProvider()

    /**
     * Every backend error response is `{"error": "..."}` (see backend/src/app.ts). Ktor's content
     * negotiation deserializes whatever body comes back regardless of status code, so a failed
     * request must be checked *before* parsing it as the expected success type - otherwise the
     * error JSON gets fed into the success type's deserializer and fails with a confusing
     * "missing required fields" exception instead of the backend's actual error message.
     */
    private suspend inline fun <reified T> HttpResponse.parseOrThrow(): T {
        if (!status.isSuccess()) {
            val message = runCatching { body<ApiErrorBody>().error }
                .getOrDefault("Request failed with status ${status.value}")
            throw ApiException(message, status.value)
        }
        return body()
    }

    /**
     * Analyzes a meal from a [photo], a [description], or both (at least one is required by the
     * backend). [portion] steers the quantities; [language] ("es"/"en") is the language the food
     * names and notes come back in. Only the parts that are present are sent.
     */
    suspend fun analyzeMeal(
        photo: ByteArray?,
        description: String?,
        portion: Portion?,
        language: String,
        /** Photos of ingredients used to prepare the meal (labels, packages, raw amounts); at most 5. */
        ingredients: List<ByteArray> = emptyList(),
        filename: String = "meal.jpg",
    ): MealAnalysis {
        return httpClient
            .submitFormWithBinaryData(
                url = "$baseUrl/api/meals/analyze",
                formData = formData {
                    append("language", language)
                    description?.takeIf { it.isNotBlank() }?.let { append("description", it.trim()) }
                    portion?.let { append("portion", it.apiValue) }
                    if (photo != null) {
                        append(
                            "photo",
                            photo,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"$filename\"")
                            },
                        )
                    }
                    ingredients.forEachIndexed { index, bytes ->
                        append(
                            "ingredients",
                            bytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"ingredient-${index + 1}.jpg\"")
                            },
                        )
                    }
                },
            )
            .parseOrThrow()
    }

    suspend fun saveMeal(request: SaveMealRequest): MealEntry {
        return httpClient
            .post("$baseUrl/api/meals") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .parseOrThrow()
    }

    suspend fun listMeals(date: String): List<MealEntry> {
        return httpClient
            .get("$baseUrl/api/meals") { url { parameters.append("date", date) } }
            .parseOrThrow()
    }

    suspend fun getMeal(id: Int): MealEntry {
        return httpClient.get("$baseUrl/api/meals/$id").parseOrThrow()
    }

    suspend fun updateMeal(id: Int, request: UpdateMealRequest): MealEntry {
        return httpClient
            .patch("$baseUrl/api/meals/$id") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .parseOrThrow()
    }

    suspend fun deleteMeal(id: Int) {
        httpClient.delete("$baseUrl/api/meals/$id").parseOrThrow<Unit>()
    }

    suspend fun dailySummary(date: String): NutritionTotals {
        return httpClient
            .get("$baseUrl/api/summary/daily") { url { parameters.append("date", date) } }
            .parseOrThrow()
    }

    suspend fun weeklySummary(startDate: String): WeeklySummary {
        return httpClient
            .get("$baseUrl/api/summary/weekly") { url { parameters.append("startDate", startDate) } }
            .parseOrThrow()
    }

    suspend fun getGoals(): DailyGoal {
        return httpClient.get("$baseUrl/api/goals").parseOrThrow()
    }

    suspend fun updateGoals(request: UpdateGoalsRequest): DailyGoal {
        return httpClient
            .put("$baseUrl/api/goals") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .parseOrThrow()
    }

    suspend fun listFavorites(): List<MealEntry> {
        return httpClient.get("$baseUrl/api/meals/favorites").parseOrThrow()
    }

    suspend fun setFavorite(id: Int, isFavorite: Boolean): MealEntry {
        return httpClient
            .patch("$baseUrl/api/meals/$id") {
                contentType(ContentType.Application.Json)
                setBody(FavoriteUpdateRequest(isFavorite))
            }
            .parseOrThrow()
    }

    /** One entry per day from [startDate] to [endDate], both inclusive (`YYYY-MM-DD`). */
    suspend fun rangeSummary(startDate: String, endDate: String): List<DailyTotals> {
        return httpClient
            .get("$baseUrl/api/summary/range") {
                url {
                    parameters.append("startDate", startDate)
                    parameters.append("endDate", endDate)
                }
            }
            .parseOrThrow()
    }

    /** The local day (YYYY-MM-DD) of the earliest logged meal, or null when none was ever logged. */
    suspend fun firstMealDate(): String? {
        return httpClient.get("$baseUrl/api/meals/first-date").parseOrThrow<FirstMealDate>().date
    }

    /**
     * The CSV export of meals from [startDate] to [endDate] (inclusive), as the backend's raw text -
     * including its UTF-8 byte-order mark, which spreadsheets need to read accents correctly.
     */
    suspend fun exportMealsCsv(startDate: String, endDate: String): String {
        return httpClient
            .get("$baseUrl/api/meals/export") {
                url {
                    parameters.append("startDate", startDate)
                    parameters.append("endDate", endDate)
                }
            }
            .parseOrThrow()
    }

    /** Weight entries with a day from [startDate] to [endDate] (inclusive), oldest first. */
    suspend fun listWeight(startDate: String, endDate: String): List<WeightEntry> {
        return httpClient
            .get("$baseUrl/api/weight") {
                url {
                    parameters.append("startDate", startDate)
                    parameters.append("endDate", endDate)
                }
            }
            .parseOrThrow()
    }

    suspend fun logWeight(request: LogWeightRequest): WeightEntry {
        return httpClient
            .post("$baseUrl/api/weight") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .parseOrThrow()
    }

    suspend fun deleteWeight(id: Int) {
        httpClient.delete("$baseUrl/api/weight/$id").parseOrThrow<Unit>()
    }

    /** The water logged on the local day [date] (YYYY-MM-DD), with its total. */
    suspend fun getWater(date: String): WaterDay {
        return httpClient
            .get("$baseUrl/api/water") {
                url { parameters.append("date", date) }
            }
            .parseOrThrow()
    }

    suspend fun logWater(request: LogWaterRequest): WaterEntry {
        return httpClient
            .post("$baseUrl/api/water") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
            .parseOrThrow()
    }

    suspend fun deleteWater(id: Int) {
        httpClient.delete("$baseUrl/api/water/$id").parseOrThrow<Unit>()
    }
}
