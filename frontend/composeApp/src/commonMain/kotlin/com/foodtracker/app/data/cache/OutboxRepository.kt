package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.LogWaterRequest
import com.foodtracker.app.data.MealEntry
import com.foodtracker.app.data.SaveMealRequest
import com.foodtracker.app.data.WaterDay
import com.foodtracker.app.data.WaterEntry
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.data.platformHttpClientEngine
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** How saving went: stored now, queued to be sent later, or refused by the backend (not queued). */
sealed interface SaveOutcome<out T> {
    data class Saved<T>(val entry: T) : SaveOutcome<T>

    data object Queued : SaveOutcome<Nothing>

    data class Rejected(val error: ApiException) : SaveOutcome<Nothing>
}

/** What a [OutboxRepository.sendAll] run did: how many rows it sent, and whether it stopped offline. */
data class SendReport(val sent: Int, val offline: Boolean)

@OptIn(ExperimentalUuidApi::class)
private fun randomClientId(): String = Uuid.random().toString()

/** The app and the background worker may both send; one at a time, so a row is never sent twice at once. */
private val sendLock = Mutex()

/**
 * Statuses worth trying again later rather than reporting to the user: the backend is struggling or
 * busy (5xx, request timeout, rate limit). Any other error status is a refusal.
 */
private fun isRetryable(status: Int) = status >= 500 || status == 408 || status == 429

/**
 * Meals and water the user logs (offline-queue): sent at once when the backend answers, otherwise
 * kept in the outbox with their clientId and time and sent later, oldest first, by [sendAll]. A sent
 * entry is merged into its cached day before its row goes, so it never disappears from Home.
 */
class OutboxRepository(
    private val api: FoodTrackerApiClient,
    private val outbox: OutboxStore,
    private val dayCache: DayCacheStore,
    /** Asks for a background send (WorkManager on Android); called whenever something is queued. */
    private val requestSync: () -> Unit,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
    private val newClientId: () -> String = ::randomClientId,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** Saves a meal stamped [SaveMealRequest.occurredAt] (the phone's time), queueing it when there's no answer. */
    suspend fun saveMeal(request: SaveMealRequest): SaveOutcome<MealEntry> {
        val withId = request.copy(clientId = request.clientId ?: newClientId())
        val day = localDay(requireNotNull(withId.occurredAt) { "A saved meal needs its occurredAt" })
        return sendOrQueue(OutboxKind.MEAL, day, withId.clientId!!, cacheJson.encodeToString(withId)) {
            api.saveMeal(withId)
        }
    }

    /** Logs a glass counting for [day], queueing it when there's no answer. */
    suspend fun logWater(request: LogWaterRequest, day: String): SaveOutcome<WaterEntry> {
        val withId = request.copy(clientId = request.clientId ?: newClientId())
        return sendOrQueue(OutboxKind.WATER, day, withId.clientId!!, cacheJson.encodeToString(withId)) {
            api.logWater(withId)
        }
    }

    /** The current server's queued rows (pending and rejected) counting for [day]. */
    fun observeDay(day: String): Flow<List<OutboxRow>> = outbox.observeDay(day, serverUrl())

    /**
     * Removes [day]'s newest queued glass, never sent. Returns false when there is none, so the caller
     * deletes the newest stored glass instead. Waits for a send in progress, which may take that glass.
     */
    suspend fun undoPendingWater(day: String): Boolean = sendLock.withLock {
        val newest = outbox.pendingOfDay(day, serverUrl()).lastOrNull { it.kind == OutboxKind.WATER } ?: return false
        outbox.delete(newest.id)
        true
    }

    /** Sends the current server's queued rows, oldest first, until none is left or the backend stops answering. */
    suspend fun sendAll(): SendReport = sendLock.withLock { sendQueued() }

    private suspend fun sendQueued(): SendReport {
        val url = serverUrl()
        var sent = 0
        // Re-read after each pass: rows queued meanwhile are sent too. Each pass deletes or rejects every row it reads.
        while (true) {
            val rows = outbox.sendable(url)
            if (rows.isEmpty()) return SendReport(sent, offline = false)
            for (row in rows) {
                try {
                    send(row)
                    sent++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    if (isRetryable(e.status)) return SendReport(sent, offline = true)
                    outbox.markRejected(row.id, e.message.orEmpty(), e.status)
                } catch (_: Exception) {
                    return SendReport(sent, offline = true)
                }
            }
        }
    }

    /** Sends a rejected row again (e.g. after fixing the token), with the rest of the queue. */
    suspend fun retry(id: Long): SendReport {
        outbox.clearError(id)
        return sendAll()
    }

    suspend fun discard(id: Long) = outbox.delete(id)

    private suspend fun <T> sendOrQueue(
        kind: OutboxKind,
        day: String,
        clientId: String,
        payloadJson: String,
        send: suspend () -> T,
    ): SaveOutcome<T> {
        val url = serverUrl()
        return try {
            SaveOutcome.Saved(send())
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (!isRetryable(e.status)) return SaveOutcome.Rejected(e)
            queue(kind, day, clientId, payloadJson, url)
        } catch (_: Exception) {
            queue(kind, day, clientId, payloadJson, url)
        }
    }

    private suspend fun queue(kind: OutboxKind, day: String, clientId: String, payloadJson: String, url: String): SaveOutcome.Queued {
        outbox.insert(OutboxRow(clientId = clientId, kind = kind, day = day, serverUrl = url, payloadJson = payloadJson, createdAt = now()))
        requestSync()
        return SaveOutcome.Queued
    }

    /** Sends one row, then merges the stored entry into its cached day before deleting the row. */
    private suspend fun send(row: OutboxRow) {
        when (row.kind) {
            OutboxKind.MEAL -> {
                val entry = api.saveMeal(cacheJson.decodeFromString<SaveMealRequest>(row.payloadJson))
                mergeIntoDay(row) { meals, water -> withMeal(meals, entry) to water }
            }
            OutboxKind.WATER -> {
                val entry = api.logWater(cacheJson.decodeFromString<LogWaterRequest>(row.payloadJson))
                mergeIntoDay(row) { meals, water -> meals to withWaterEntry(water, entry) }
            }
        }
        outbox.delete(row.id)
    }

    /** Applies [merge] to [row]'s cached day, if that day is cached for the same server and still decodes. */
    private suspend fun mergeIntoDay(row: OutboxRow, merge: (List<MealEntry>, WaterDay) -> Pair<List<MealEntry>, WaterDay>) {
        val cached = dayCache.day(row.day)?.takeIf { it.serverUrl == row.serverUrl } ?: return
        val meals = decodeOrNull<List<MealEntry>>(cached.mealsJson) ?: return
        val water = decodeOrNull<WaterDay>(cached.waterJson) ?: return
        val (newMeals, newWater) = merge(meals, water)
        dayCache.saveDay(cached.copy(mealsJson = cacheJson.encodeToString(newMeals), waterJson = cacheJson.encodeToString(newWater)))
    }

    @OptIn(ExperimentalTime::class)
    private fun localDay(instant: String): String = Instant.parse(instant).toLocalDateTime(timeZone()).date.toString()
}

/** [meals] with [entry] appended, unless a meal with its clientId is already there. */
internal fun withMeal(meals: List<MealEntry>, entry: MealEntry): List<MealEntry> =
    if (entry.clientId != null && meals.any { it.clientId == entry.clientId }) meals else meals + entry

/** [water] with [entry] appended and counted, unless an entry with its clientId is already there. */
internal fun withWaterEntry(water: WaterDay, entry: WaterEntry): WaterDay =
    if (entry.clientId != null && water.entries.any { it.clientId == entry.clientId }) {
        water
    } else {
        water.copy(totalMl = water.totalMl + entry.amountMl, entries = water.entries + entry)
    }

/**
 * The outbox a background sender uses (offline-queue): a fresh process never set BackendConfig, so
 * the stored server address and access token are passed in. The caller closes [httpClient].
 */
fun storedSettingsOutbox(
    serverUrl: String,
    accessToken: String?,
    outbox: OutboxStore,
    dayCache: DayCacheStore,
    engine: HttpClientEngine = platformHttpClientEngine(),
): Pair<OutboxRepository, HttpClient> {
    val httpClient = createHttpClient(engine, tokenProvider = { accessToken })
    val repository = OutboxRepository(
        api = FoodTrackerApiClient(httpClient) { serverUrl },
        outbox = outbox,
        dayCache = dayCache,
        requestSync = {},
        serverUrl = { serverUrl },
    )
    return repository to httpClient
}
