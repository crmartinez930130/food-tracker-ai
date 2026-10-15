package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.data.DailyGoal
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.WaterDay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/** A day as last returned by the backend; [refreshedAt] (epoch ms) is when its oldest part was fetched. */
data class CachedDay(
    val meals: List<MealEntry>,
    val water: WaterDay,
    val goal: DailyGoal,
    val refreshedAt: Long,
)

/** How a refresh went: the backend answered ([Success], [Failed]) or couldn't be reached ([Offline]). */
sealed interface RefreshResult {
    data object Success : RefreshResult

    /** No answer: no network, unknown host, refused or timed-out connection. */
    data class Offline(val cause: Exception) : RefreshResult

    data class Failed(val error: ApiException) : RefreshResult
}

@OptIn(ExperimentalTime::class)
internal fun epochMillisNow(): Long = Clock.System.now().toEpochMilliseconds()

internal val cacheJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** [value] decoded, or null when it no longer matches the type (SerializationException is one). */
internal inline fun <reified T> decodeOrNull(value: String): T? =
    try {
        cacheJson.decodeFromString<T>(value)
    } catch (_: IllegalArgumentException) {
        null
    }

/**
 * The days Home and the day detail show: the cached copy first ([observe]), then the backend's
 * ([refresh]), which replaces it. Rows are tagged with the server they came from and only the
 * current server's are shown, so a refresh that lands after a server change never leaks into it.
 */
class DayRepository(
    private val api: FoodTrackerApiClient,
    private val store: DayCacheStore,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
) {
    /** The cached [date], or null when it (or the goal) isn't cached for the current server. */
    fun observe(date: String): Flow<CachedDay?> =
        combine(store.observeDay(date), store.observeGoal()) { day, goal ->
            val url = serverUrl()
            if (day == null || goal == null || day.serverUrl != url || goal.serverUrl != url) return@combine null
            // Decoded apart, so a row written in another shape only takes itself out of the cache.
            val goalValue = decodeOrNull<DailyGoal>(goal.goalJson)
            val meals = decodeOrNull<List<MealEntry>>(day.mealsJson)
            val water = decodeOrNull<WaterDay>(day.waterJson)
            if (goalValue == null) store.deleteGoal()
            if (meals == null || water == null) store.deleteDay(date)
            if (goalValue == null || meals == null || water == null) return@combine null
            CachedDay(meals, water, goalValue, refreshedAt = minOf(day.refreshedAt, goal.refreshedAt))
        }

    /** Fetches [date]'s meals and water and the goal, and caches them; never throws for a failed call. */
    suspend fun refresh(date: String): RefreshResult {
        val url = serverUrl()
        return try {
            val meals = api.listMeals(date)
            val goal = api.getGoals()
            val water = api.getWater(date)
            // The server changed while this ran: its data belongs to the old one, so don't cache it.
            if (serverUrl() == url) {
                val fetchedAt = now()
                store.save(
                    DayCacheRow(date, cacheJson.encodeToString(meals), cacheJson.encodeToString(water), url, fetchedAt),
                    GoalCacheRow(goalJson = cacheJson.encodeToString(goal), serverUrl = url, refreshedAt = fetchedAt),
                )
            }
            RefreshResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            RefreshResult.Failed(e)
        } catch (e: Exception) {
            RefreshResult.Offline(e)
        }
    }

    /** Drops every cached row of a server other than [currentUrl]; called when the server changes. */
    suspend fun deleteOtherServers(currentUrl: String) = store.deleteOtherServers(currentUrl)
}
