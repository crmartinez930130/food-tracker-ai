package com.foodtracker.app.widget

import com.foodtracker.app.data.Confidence
import com.foodtracker.app.data.DEFAULT_GOAL
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.MealItem
import com.foodtracker.app.data.MealType
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.WaterEntry
import com.foodtracker.app.data.cache.CachedDay
import com.foodtracker.app.data.cache.DayCacheRow
import com.foodtracker.app.data.cache.FakeDayCache
import com.foodtracker.app.data.cache.FakeOutbox
import com.foodtracker.app.data.cache.GoalCacheRow
import com.foodtracker.app.data.cache.OutboxKind
import com.foodtracker.app.data.cache.OutboxRow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val TODAY = "2026-03-05"
private const val NOW = 1_000_000_000L
private val json = Json { encodeDefaults = true }
private val rice = MealItem("Rice", 150.0, Confidence.HIGH, 200.4, 4.2, 1.0, 44.0)
private val goal = DailyGoal(2100.0, 130.0, 70.0, 230.0, targetWaterMl = 2500, waterGlassMl = 300, isDefault = false)

private fun meal(id: Int, calories: Double, clientId: String) = MealEntry(
    id, "2026-03-05T12:00:00Z", "2026-03-05T12:00:00Z", items = listOf(rice),
    totalCalories = calories, totalProteinGrams = 10.4, totalFatGrams = 5.0, totalCarbsGrams = 20.0, clientId = clientId,
)

private fun water(totalMl: Int) = WaterDay(TODAY, totalMl, if (totalMl == 0) emptyList() else listOf(WaterEntry(1, totalMl, "2026-03-05T10:00:00Z", clientId = "w0")))

private fun day(meals: List<MealEntry>, totalWaterMl: Int = 0, refreshedAt: Long? = NOW) =
    CachedDay(meals, water(totalWaterMl), goal, refreshedAt)

private fun mealRow(id: Long, clientId: String, error: String? = null) = OutboxRow(
    id = id, clientId = clientId, kind = OutboxKind.MEAL, day = TODAY, serverUrl = "s", createdAt = 0,
    payloadJson = json.encodeToString(SaveMealRequest(listOf(rice), occurredAt = "2026-03-05T16:05:00Z", mealType = MealType.LUNCH, clientId = clientId)),
    error = error, errorStatus = error?.let { 400 },
)

private fun waterRow(id: Long, clientId: String) = OutboxRow(
    id = id, clientId = clientId, kind = OutboxKind.WATER, day = TODAY, serverUrl = "s", createdAt = 0,
    payloadJson = json.encodeToString(LogWaterRequest(300, loggedAt = "2026-03-05T16:05:00Z", clientId = clientId)),
)

class TodayWidgetStateTest {
    @Test
    fun `totals, macros and water come from the cached day, rounded, against its goal`() {
        val state = todayWidgetState(day(listOf(meal(1, 500.4, "a"), meal(2, 300.3, "b")), totalWaterMl = 750), DEFAULT_GOAL, emptyList(), TODAY, NOW)

        assertEquals(801, state.calories)
        assertEquals(2100, state.targetCalories)
        assertEquals(21, state.proteinGrams)
        assertEquals(130, state.targetProteinGrams)
        assertEquals(10, state.fatGrams)
        assertEquals(40, state.carbsGrams)
        assertEquals(750, state.waterMl)
        assertEquals(2500, state.targetWaterMl)
        assertEquals(0.3f, state.waterFraction)
        assertEquals(300, state.glassMl)
        assertEquals(WidgetFreshness.Fresh, state.freshness)
    }

    @Test
    fun `queued meals and glasses count, rejected ones don't`() {
        val rows = listOf(mealRow(7, "p"), waterRow(8, "g1"), waterRow(9, "g2"), mealRow(10, "r", error = "bad"))

        val state = todayWidgetState(day(listOf(meal(1, 500.0, "a"))), DEFAULT_GOAL, rows, TODAY, NOW)

        assertEquals(700, state.calories)
        assertEquals(600, state.waterMl)
    }

    @Test
    fun `a queued entry already stored counts once`() {
        val state = todayWidgetState(day(listOf(meal(1, 500.0, "a"))), DEFAULT_GOAL, listOf(mealRow(7, "a")), TODAY, NOW)

        assertEquals(500, state.calories)
    }

    @Test
    fun `without a copy of today it shows the queue against the fallback goal, and says there is no data`() {
        val state = todayWidgetState(null, DEFAULT_GOAL, listOf(waterRow(8, "g1")), TODAY, NOW)

        assertEquals(0, state.calories)
        assertEquals(2000, state.targetCalories)
        assertEquals(300, state.waterMl)
        assertEquals(250, state.glassMl)
        assertEquals(WidgetFreshness.NoData, state.freshness)
    }

    @Test
    fun `a copy older than an hour is stale, from when it was refreshed`() {
        val refreshedAt = NOW - STALE_AFTER_MILLIS - 1

        val state = todayWidgetState(day(emptyList(), refreshedAt = refreshedAt), DEFAULT_GOAL, emptyList(), TODAY, NOW)

        assertEquals(WidgetFreshness.Stale(refreshedAt), state.freshness)
    }

    @Test
    fun `a copy exactly an hour old is still fresh`() {
        val state = todayWidgetState(day(emptyList(), refreshedAt = NOW - STALE_AFTER_MILLIS), DEFAULT_GOAL, emptyList(), TODAY, NOW)

        assertEquals(WidgetFreshness.Fresh, state.freshness)
    }

    @Test
    fun `over the goal there is nothing left and the excess shows`() {
        val state = todayWidgetState(day(listOf(meal(1, 2300.0, "a"))), DEFAULT_GOAL, emptyList(), TODAY, NOW)

        assertTrue(state.calorieProgress.isOver)
        assertEquals(0.0, state.calorieProgress.remaining)
        assertEquals(200.0, state.calorieProgress.excess)
        assertEquals(1f, state.calorieProgress.fraction)
    }

    @Test
    fun `under the goal the kcal left show`() {
        val state = todayWidgetState(day(listOf(meal(1, 968.0, "a"))), DEFAULT_GOAL, emptyList(), TODAY, NOW)

        assertFalse(state.calorieProgress.isOver)
        assertEquals(1132.0, state.calorieProgress.remaining)
    }

    @Test
    fun `the state flow reads the current server's copy and follows the queue`() = runBlocking<Unit> {
        val days = FakeDayCache()
        val outbox = FakeOutbox()
        days.save(
            DayCacheRow(TODAY, json.encodeToString(listOf(meal(1, 500.0, "a"))), json.encodeToString(water(0)), "home", NOW),
            GoalCacheRow(goalJson = json.encodeToString(goal), serverUrl = "home", refreshedAt = NOW),
        )
        val states = todayWidgetStates(days, outbox, "home", TODAY) { NOW }

        val collected = mutableListOf<TodayWidgetState>()
        withTimeout(5_000) {
            val job = launch { states.take(2).toList(collected) }
            // Queue the glass only once the first state arrived, so it comes as a second one.
            while (collected.isEmpty()) yield()
            outbox.insert(waterRow(0, "g1").copy(serverUrl = "home"))
            job.join()
        }

        assertEquals(listOf(0, 300), collected.map { it.waterMl })
        assertEquals(500, collected.last().calories)
    }

    @Test
    fun `another server's copy and goal don't show`() = runBlocking<Unit> {
        val days = FakeDayCache()
        days.save(
            DayCacheRow(TODAY, json.encodeToString(listOf(meal(1, 500.0, "a"))), json.encodeToString(water(0)), "old", NOW),
            GoalCacheRow(goalJson = json.encodeToString(goal), serverUrl = "old", refreshedAt = NOW),
        )

        val state = todayWidgetStates(days, FakeOutbox(), "home", TODAY) { NOW }.first()

        assertEquals(0, state.calories)
        assertEquals(DEFAULT_GOAL.targetCalories.toInt(), state.targetCalories)
        assertEquals(WidgetFreshness.NoData, state.freshness)
    }
}
