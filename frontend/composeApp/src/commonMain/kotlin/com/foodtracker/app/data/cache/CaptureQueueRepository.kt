package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealAnalysis
import com.foodtracker.app.data.Portion
import com.foodtracker.app.data.createHttpClient
import com.foodtracker.app.data.platformHttpClientEngine
import com.foodtracker.app.util.currentInstantIso
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

/** How analyzing a capture went: analyzed now, kept for later, or refused by the backend (not kept). */
sealed interface AnalyzeOutcome {
    data class Analyzed(val analysis: MealAnalysis) : AnalyzeOutcome

    data object Kept : AnalyzeOutcome

    data class Rejected(val error: ApiException) : AnalyzeOutcome
}

/** What an [CaptureQueueRepository.analyzeWaiting] run did: captures made ready, and whether it stopped offline. */
data class AnalyzeReport(val ready: Int, val offline: Boolean)

/** The error of a kept capture whose photo file is gone; the screen shows its own text for it. */
const val MISSING_PHOTO_ERROR = "photo-missing"

/** One analysis at a time, whether the app or the background job runs it. */
private val analyzeLock = Mutex()

private fun isRetryable(status: Int) = status >= 500 || status == 408 || status == 429

@OptIn(ExperimentalUuidApi::class)
private fun randomClientId(): String = Uuid.random().toString()

/**
 * Captures that couldn't be analyzed for lack of a connection (offline-queue): kept with their photo,
 * description, portion, language and time, analyzed later by [analyzeWaiting], and reviewed by the
 * user before anything is saved. Each belongs to the server it was taken for.
 */
class CaptureQueueRepository(
    private val api: FoodTrackerApiClient,
    private val store: CaptureStore,
    private val photos: CapturePhotoStore,
    /** Asks for a background run (WorkManager on Android); called whenever a capture is kept. */
    private val requestSync: () -> Unit,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
    private val capturedAt: () -> String = ::currentInstantIso,
    private val newClientId: () -> String = ::randomClientId,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** Analyzes the capture now, or keeps it when the backend gives no answer (or a retryable one). */
    suspend fun analyzeOrKeep(
        photo: ByteArray?,
        description: String?,
        portion: Portion?,
        language: String,
        ingredients: List<ByteArray> = emptyList(),
    ): AnalyzeOutcome {
        val url = serverUrl()
        val at = capturedAt()
        return try {
            AnalyzeOutcome.Analyzed(api.analyzeMeal(photo, description, portion, language, ingredients = ingredients))
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            if (!isRetryable(e.status)) return AnalyzeOutcome.Rejected(e)
            keep(photo, ingredients, description, portion, language, at, url)
        } catch (_: Exception) {
            keep(photo, ingredients, description, portion, language, at, url)
        }
    }

    /** The current server's captures of [day], in every state. */
    fun observeDay(day: String): Flow<List<CaptureRow>> = store.observeDay(day, serverUrl())

    /** Analyzes the current server's waiting captures, oldest first, until none is left or one gets no answer. */
    suspend fun analyzeWaiting(): AnalyzeReport = analyzeLock.withLock { analyzeQueued() }

    private suspend fun analyzeQueued(): AnalyzeReport {
        val url = serverUrl()
        var ready = 0
        // Re-read after each pass: captures kept meanwhile are analyzed too. Each pass settles every row it reads.
        while (true) {
            val rows = store.waiting(url)
            if (rows.isEmpty()) return AnalyzeReport(ready, offline = false)
            for (row in rows) {
                val photo = row.photoFile?.let { photos.read(it) }
                val ingredients = ingredientFilesOf(row).map { photos.read(it) }
                // A file gone missing (cleared storage) can't be analyzed: the user sees why and decides.
                if ((row.photoFile != null && photo == null) || ingredients.any { it == null }) {
                    store.markFailed(row.id, MISSING_PHOTO_ERROR, null)
                    continue
                }
                try {
                    val analysis = api.analyzeMeal(
                        photo,
                        row.description,
                        row.portion?.let(::portionOf),
                        row.language,
                        ingredients = ingredients.filterNotNull(),
                    )
                    store.markReady(row.id, cacheJson.encodeToString(analysis))
                    ready++
                } catch (e: CancellationException) {
                    throw e
                } catch (e: ApiException) {
                    if (isRetryable(e.status)) return AnalyzeReport(ready, offline = true)
                    store.markFailed(row.id, e.message.orEmpty(), e.status)
                } catch (_: Exception) {
                    return AnalyzeReport(ready, offline = true)
                }
            }
        }
    }

    /** Analyzes a failed capture again (e.g. after fixing the token), with the rest of the waiting ones. */
    suspend fun retry(id: Long): AnalyzeReport {
        store.markWaiting(id)
        return analyzeWaiting()
    }

    /** Drops a capture and its photo, never analyzed or saved. */
    suspend fun discard(id: Long) = remove(id)

    /** Drops a capture once its meal is saved (or queued to be sent). */
    suspend fun complete(id: Long) = remove(id)

    /** A ready capture's analysis, or null when it has none (or it no longer decodes). */
    fun analysisOf(row: CaptureRow): MealAnalysis? = row.analysisJson?.let { decodeOrNull<MealAnalysis>(it) }

    /** The names of a capture's ingredient photos in [CapturePhotoStore], in order. */
    fun ingredientFilesOf(row: CaptureRow): List<String> =
        row.ingredientFiles?.let { decodeOrNull<List<String>>(it) }.orEmpty()

    /** A capture's photo, for its thumbnail; null without one or when the file is gone. */
    suspend fun photoOf(row: CaptureRow): ByteArray? = row.photoFile?.let { photos.read(it) }

    private suspend fun keep(
        photo: ByteArray?,
        ingredients: List<ByteArray>,
        description: String?,
        portion: Portion?,
        language: String,
        at: String,
        url: String,
    ): AnalyzeOutcome.Kept {
        val clientId = newClientId()
        val photoFile = photo?.let { "$clientId.jpg".also { name -> photos.write(name, it) } }
        val ingredientFiles = ingredients.mapIndexed { index, bytes ->
            "$clientId-ingredient-${index + 1}.jpg".also { name -> photos.write(name, bytes) }
        }
        store.insert(
            CaptureRow(
                clientId = clientId,
                day = localDay(at),
                serverUrl = url,
                capturedAt = at,
                description = description?.trim()?.ifBlank { null },
                portion = portion?.apiValue,
                language = language,
                photoFile = photoFile,
                ingredientFiles = ingredientFiles.takeIf { it.isNotEmpty() }?.let { cacheJson.encodeToString(it) },
                createdAt = now(),
            ),
        )
        requestSync()
        return AnalyzeOutcome.Kept
    }

    private suspend fun remove(id: Long) {
        val row = store.get(id) ?: return
        row.photoFile?.let { photos.delete(it) }
        ingredientFilesOf(row).forEach { photos.delete(it) }
        store.delete(id)
    }

    @OptIn(ExperimentalTime::class)
    private fun localDay(instant: String): String = Instant.parse(instant).toLocalDateTime(timeZone()).date.toString()
}

private fun portionOf(apiValue: String): Portion? = Portion.entries.firstOrNull { it.apiValue == apiValue }

/**
 * The capture queue a background run uses: like [storedSettingsOutbox], with the stored server address
 * and access token. Each capture carries its own language. The caller closes [HttpClient].
 */
fun storedSettingsCaptureQueue(
    serverUrl: String,
    accessToken: String?,
    store: CaptureStore,
    photos: CapturePhotoStore,
    engine: HttpClientEngine = platformHttpClientEngine(),
): Pair<CaptureQueueRepository, HttpClient> {
    val httpClient = createHttpClient(engine, tokenProvider = { accessToken })
    val repository = CaptureQueueRepository(
        api = FoodTrackerApiClient(httpClient) { serverUrl },
        store = store,
        photos = photos,
        requestSync = {},
        serverUrl = { serverUrl },
    )
    return repository to httpClient
}
