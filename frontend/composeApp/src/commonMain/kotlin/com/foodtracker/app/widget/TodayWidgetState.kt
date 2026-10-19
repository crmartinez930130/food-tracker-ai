package com.foodtracker.app.widget

import com.foodtracker.app.data.DEFAULT_GOAL
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.cache.CachedDay
import com.foodtracker.app.data.cache.DayCacheStore
import com.foodtracker.app.data.cache.OutboxRow
import com.foodtracker.app.data.cache.OutboxStore
import com.foodtracker.app.data.cache.cachedGoal
import com.foodtracker.app.data.cache.observeCachedDay
import com.foodtracker.app.ui.home.Progress
import com.foodtracker.app.ui.home.mergePending
import com.foodtracker.app.ui.home.progressOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.math.roundToInt

/** How old the copy is that a widget shows (home-widgets). */
sealed interface WidgetFreshness {
    data object Fresh : WidgetFreshness

    /** The copy is older than [STALE_AFTER_MILLIS]: it was refreshed at [since] (epoch ms). */
    data class Stale(val since: Long) : WidgetFreshness

    /** There is no copy of today on the phone yet. */
    data object NoData : WidgetFreshness
}

/**
 * The background refresh runs every 30 minutes, so a copy older than an hour means refreshes are failing:
 * only then does the widget say how old its data is.
 */
const val STALE_AFTER_MILLIS = 60 * 60 * 1000L

/** Today as the widgets draw it: the same totals Home shows, already rounded. */
data class TodayWidgetState(
    val calories: Int,
    val targetCalories: Int,
    val calorieProgress: Progress,
    val proteinGrams: Int,
    val targetProteinGrams: Int,
    val fatGrams: Int,
    val targetFatGrams: Int,
    val carbsGrams: Int,
    val targetCarbsGrams: Int,
    val waterMl: Int,
    val targetWaterMl: Int,
    val waterFraction: Float,
    /** What the glass action logs, from the goal. */
    val glassMl: Int,
    val freshness: WidgetFreshness,
)

/**
 * Builds what the widgets show for [today]: the cached [day] (null when there is no copy of today) plus
 * the day's queued [rows], counted as on Home, against the day's goal, or [fallbackGoal] without a copy.
 */
fun todayWidgetState(day: CachedDay?, fallbackGoal: DailyGoal, rows: List<OutboxRow>, today: String, now: Long): TodayWidgetState {
    val shown = mergePending(day?.meals.orEmpty(), day?.water ?: WaterDay(today, 0, emptyList()), rows)
    val goal = day?.goal ?: fallbackGoal
    val calories = shown.meals.sumOf { it.totalCalories }
    val refreshedAt = day?.refreshedAt
    return TodayWidgetState(
        calories = calories.roundToInt(),
        targetCalories = goal.targetCalories.roundToInt(),
        calorieProgress = progressOf(calories, goal.targetCalories),
        proteinGrams = shown.meals.sumOf { it.totalProteinGrams }.roundToInt(),
        targetProteinGrams = goal.targetProteinGrams.roundToInt(),
        fatGrams = shown.meals.sumOf { it.totalFatGrams }.roundToInt(),
        targetFatGrams = goal.targetFatGrams.roundToInt(),
        carbsGrams = shown.meals.sumOf { it.totalCarbsGrams }.roundToInt(),
        targetCarbsGrams = goal.targetCarbsGrams.roundToInt(),
        waterMl = shown.water.totalMl,
        targetWaterMl = goal.targetWaterMl,
        waterFraction = progressOf(shown.water.totalMl.toDouble(), goal.targetWaterMl.toDouble()).fraction,
        glassMl = goal.waterGlassMl,
        freshness = when {
            refreshedAt == null -> WidgetFreshness.NoData
            now - refreshedAt > STALE_AFTER_MILLIS -> WidgetFreshness.Stale(refreshedAt)
            else -> WidgetFreshness.Fresh
        },
    )
}

/**
 * [today]'s widget state from the phone's copy ([days]) and the server [serverUrl]'s queue ([outbox]),
 * again whenever either changes. Reads only the copy, so it needs no backend client.
 */
fun todayWidgetStates(days: DayCacheStore, outbox: OutboxStore, serverUrl: String, today: String, now: () -> Long): Flow<TodayWidgetState> =
    combine(observeCachedDay(days, today) { serverUrl }, days.observeGoal(), outbox.observeDay(today, serverUrl)) { day, goal, rows ->
        todayWidgetState(day, cachedGoal(goal, serverUrl) ?: DEFAULT_GOAL, rows, today, now())
    }
