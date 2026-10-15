package com.foodtracker.app.ui.home

import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.WaterEntry
import com.foodtracker.app.data.cache.OutboxKind
import com.foodtracker.app.data.cache.OutboxRow
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

private val json = Json { encodeDefaults = true }
private val rice = MealItem("Rice", 150.0, Confidence.HIGH, 200.0, 4.0, 1.0, 44.0)

private fun mealRow(id: Long, clientId: String, error: String? = null) = OutboxRow(
    id = id, clientId = clientId, kind = OutboxKind.MEAL, day = "2026-03-05", serverUrl = "s", createdAt = 0,
    payloadJson = json.encodeToString(SaveMealRequest(listOf(rice), occurredAt = "2026-03-05T16:05:00Z", mealType = MealType.LUNCH, clientId = clientId)),
    error = error, errorStatus = error?.let { 401 },
)

private fun waterRow(id: Long, clientId: String) = OutboxRow(
    id = id, clientId = clientId, kind = OutboxKind.WATER, day = "2026-03-05", serverUrl = "s", createdAt = 0,
    payloadJson = json.encodeToString(LogWaterRequest(250, loggedAt = "2026-03-05T16:05:00Z", clientId = clientId)),
)

private val stored = MealEntry(1, "2026-03-05T12:00:00Z", "2026-03-05T12:00:00Z", items = listOf(rice), totalCalories = 200.0, totalProteinGrams = 4.0, totalFatGrams = 1.0, totalCarbsGrams = 44.0, clientId = "stored")
private val noWater = WaterDay("2026-03-05", 0, emptyList())

class PendingDayTest {
    @Test
    fun `pending meals and glasses are added, marked and counted`() {
        val shown = mergePending(listOf(stored), noWater, listOf(mealRow(7, "a"), waterRow(8, "b"), waterRow(9, "c")))

        assertEquals(listOf(1, -7), shown.meals.map { it.id })
        assertEquals(setOf(-7), shown.pendingMealIds)
        assertEquals(MealType.LUNCH, shown.meals.last().mealType)
        assertEquals(200.0, shown.meals.last().totalCalories)
        assertEquals(500, shown.water.totalMl)
        assertEquals(2, shown.water.entries.size)
    }

    @Test
    fun `rejected entries are listed apart and not counted`() {
        val shown = mergePending(listOf(stored), noWater, listOf(mealRow(7, "a", error = "Invalid access token")))

        assertEquals(listOf(1), shown.meals.map { it.id })
        assertEquals(listOf(7L), shown.rejected.map { it.id })
        assertEquals(401, shown.rejected.single().error.status)
    }

    @Test
    fun `an entry already stored under its clientId is shown once, as stored`() {
        val storedGlass = WaterDay("2026-03-05", 250, listOf(WaterEntry(3, 250, "2026-03-05T16:05:00Z", clientId = "b")))

        val shown = mergePending(listOf(stored.copy(clientId = "a")), storedGlass, listOf(mealRow(7, "a"), waterRow(8, "b")))

        assertEquals(listOf(1), shown.meals.map { it.id })
        assertEquals(emptySet(), shown.pendingMealIds)
        assertEquals(250, shown.water.totalMl)
    }
}
