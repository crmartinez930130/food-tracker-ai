package com.foodtracker.app.ui.home

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.WaterEntry
import com.foodtracker.app.data.cache.OutboxKind
import com.foodtracker.app.data.cache.OutboxRow
import com.foodtracker.app.data.cache.decodeOrNull
import com.foodtracker.app.ui.mealTotals

/** A queued entry the backend refused: shown with its error, to retry or discard (offline-queue). */
data class RejectedEntry(val id: Long, val kind: OutboxKind, val meal: SaveMealRequest?, val water: LogWaterRequest?, val error: ApiException)

/**
 * The day as Home shows it: the cached [meals] and [water] plus the queued ones, which [pendingMealIds]
 * marks (by meal id) so they show as pending, and the [rejected] entries, which don't count.
 */
data class ShownDay(
    val meals: List<MealEntry>,
    val pendingMealIds: Set<Int>,
    val water: WaterDay,
    val rejected: List<RejectedEntry>,
)

/**
 * Merges [rows] (the day's outbox) into the cached [meals] and [water]. A queued entry whose clientId
 * is already among the cached ones was sent meanwhile and is shown only once, as stored. Pending meals
 * get negative ids, so they never collide with stored ones.
 */
fun mergePending(meals: List<MealEntry>, water: WaterDay, rows: List<OutboxRow>): ShownDay {
    val storedMealIds = meals.mapNotNullTo(mutableSetOf()) { it.clientId }
    val storedWaterIds = water.entries.mapNotNullTo(mutableSetOf()) { it.clientId }
    val pendingMeals = mutableListOf<MealEntry>()
    val pendingWater = mutableListOf<WaterEntry>()
    val rejected = mutableListOf<RejectedEntry>()
    for (row in rows) {
        if (row.clientId in storedMealIds || row.clientId in storedWaterIds) continue
        val meal = if (row.kind == OutboxKind.MEAL) decodeOrNull<SaveMealRequest>(row.payloadJson) ?: continue else null
        val glass = if (row.kind == OutboxKind.WATER) decodeOrNull<LogWaterRequest>(row.payloadJson) ?: continue else null
        if (row.error != null) {
            rejected += RejectedEntry(row.id, row.kind, meal, glass, ApiException(row.error, row.errorStatus ?: 0))
            continue
        }
        if (meal != null) pendingMeals += meal.asPendingEntry(row)
        if (glass != null) pendingWater += WaterEntry(id = -row.id.toInt(), amountMl = glass.amountMl, loggedAt = glass.loggedAt.orEmpty(), clientId = row.clientId)
    }
    return ShownDay(
        meals = meals + pendingMeals,
        pendingMealIds = pendingMeals.mapTo(mutableSetOf()) { it.id },
        water = water.copy(totalMl = water.totalMl + pendingWater.sumOf { it.amountMl }, entries = water.entries + pendingWater),
        rejected = rejected,
    )
}

private fun SaveMealRequest.asPendingEntry(row: OutboxRow): MealEntry {
    val totals = mealTotals(items)
    val at = occurredAt.orEmpty()
    return MealEntry(
        id = -row.id.toInt(),
        occurredAt = at,
        createdAt = at,
        notes = notes,
        mealType = mealType,
        items = items,
        totalCalories = totals.calories,
        totalProteinGrams = totals.proteinGrams,
        totalFatGrams = totals.fatGrams,
        totalCarbsGrams = totals.carbsGrams,
        clientId = row.clientId,
    )
}
