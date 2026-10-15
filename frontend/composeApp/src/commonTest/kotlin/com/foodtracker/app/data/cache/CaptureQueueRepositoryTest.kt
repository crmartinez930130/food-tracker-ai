package com.foodtracker.app.data.cache

import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.Portion
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val HOME_SERVER = "http://home.local:3000"

/** 13:05 local in Buenos Aires on 2026-03-05. */
private const val CAPTURED_AT = "2026-03-05T16:05:00Z"
private const val ANALYSIS_JSON = """{"items":[{"name":"Rice","estimatedGrams":150,"confidence":"high","calories":200,"proteinGrams":4,"fatGrams":1,"carbsGrams":44}],
"totalCalories":200,"totalProteinGrams":4,"totalFatGrams":1,"totalCarbsGrams":44}"""

/** How the fake backend answers an analysis. */
private sealed interface Reply {
    data object Analysis : Reply
    data class Status(val code: HttpStatusCode) : Reply
    data object None : Reply
}

private class CaptureFixture {
    val store = FakeCaptureStore()
    val photos = FakePhotoStore()
    var server = HOME_SERVER
    var syncRequests = 0
    private var ids = 0

    /** Replies in order; the last one repeats. */
    var replies = listOf<Reply>(Reply.Analysis)
    private var replied = 0

    /** The multipart bodies the backend received, as text. */
    val received = mutableListOf<String>()

    private val engine = MockEngine { request ->
        val reply = replies[minOf(replied, replies.lastIndex)].also { replied++ }
        if (reply == Reply.None) throw RuntimeException("Failed to connect to /10.0.2.2:3000")
        received += request.body.toByteArray().decodeToString()
        if (reply is Reply.Status) {
            return@MockEngine respond("""{"error":"Nope"}""", reply.code, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        respond(ByteReadChannel(ANALYSIS_JSON), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
    }

    val repository = CaptureQueueRepository(
        api = FoodTrackerApiClient(createHttpClient(engine) { null }, baseUrlProvider = { server }),
        store = store,
        photos = photos,
        requestSync = { syncRequests++ },
        serverUrl = { server },
        now = { 1_000L },
        capturedAt = { CAPTURED_AT },
        newClientId = { "capture-${++ids}" },
        timeZone = { TimeZone.of("America/Argentina/Buenos_Aires") },
    )

    suspend fun keepPhoto(bytes: ByteArray = byteArrayOf(1, 2, 3)) {
        val previous = replies
        replies = listOf(Reply.None)
        repository.analyzeOrKeep(bytes, "milanesa", Portion.LARGE, "es")
        replies = previous
        replied = 0
    }
}

class CaptureQueueRepositoryTest {
    @Test
    fun `an answer is returned and nothing is kept`() = runBlocking<Unit> {
        val f = CaptureFixture()

        val outcome = f.repository.analyzeOrKeep(byteArrayOf(1), null, null, "es")

        assertEquals(200.0, assertIs<AnalyzeOutcome.Analyzed>(outcome).analysis.totalCalories)
        assertEquals(emptyList(), f.store.rows.value)
        assertEquals(0, f.syncRequests)
    }

    @Test
    fun `no answer and retryable statuses keep the capture with everything the analysis takes`() = runBlocking<Unit> {
        for (reply in listOf(Reply.None, Reply.Status(HttpStatusCode.InternalServerError), Reply.Status(HttpStatusCode.TooManyRequests))) {
            val f = CaptureFixture().apply { replies = listOf(reply) }

            assertEquals(AnalyzeOutcome.Kept, f.repository.analyzeOrKeep(byteArrayOf(9, 8), " milanesa ", Portion.LARGE, "es"), "for $reply")

            val row = f.store.rows.value.single()
            assertEquals(CaptureStatus.WAITING, row.status)
            assertEquals("2026-03-05", row.day)
            assertEquals(CAPTURED_AT, row.capturedAt)
            assertEquals(HOME_SERVER, row.serverUrl)
            assertEquals("milanesa", row.description)
            assertEquals("large", row.portion)
            assertEquals("es", row.language)
            assertContentEquals(byteArrayOf(9, 8), f.photos.files[row.photoFile])
            assertEquals(1, f.syncRequests)
        }
    }

    @Test
    fun `a description-only capture keeps no photo`() = runBlocking<Unit> {
        val f = CaptureFixture().apply { replies = listOf(Reply.None) }

        f.repository.analyzeOrKeep(null, "ensalada", null, "en")

        assertEquals(null, f.store.rows.value.single().photoFile)
        assertTrue(f.photos.files.isEmpty())
    }

    @Test
    fun `a refusal is Rejected and keeps nothing`() = runBlocking<Unit> {
        val f = CaptureFixture().apply { replies = listOf(Reply.Status(HttpStatusCode.BadRequest)) }

        val outcome = assertIs<AnalyzeOutcome.Rejected>(f.repository.analyzeOrKeep(byteArrayOf(1), null, null, "es"))

        assertEquals(400, outcome.error.status)
        assertEquals(emptyList(), f.store.rows.value)
        assertTrue(f.photos.files.isEmpty())
    }

    @Test
    fun `waiting captures are analyzed oldest first and become ready, sending what was kept`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.keepPhoto()

        assertEquals(AnalyzeReport(ready = 2, offline = false), f.repository.analyzeWaiting())

        assertTrue(f.store.rows.value.all { it.status == CaptureStatus.READY })
        assertEquals(200.0, f.repository.analysisOf(f.store.rows.value.first())?.totalCalories)
        val first = f.received.first()
        assertTrue("milanesa" in first && "large" in first && "es" in first, first)
    }

    @Test
    fun `analysis stops at the first unanswered capture`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.keepPhoto()
        f.replies = listOf(Reply.Analysis, Reply.None)

        assertEquals(AnalyzeReport(ready = 1, offline = true), f.repository.analyzeWaiting())

        assertEquals(listOf(CaptureStatus.READY, CaptureStatus.WAITING), f.store.rows.value.map { it.status })
    }

    @Test
    fun `a refused analysis fails the capture, goes on, and isn't retried on its own`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.keepPhoto()
        f.replies = listOf(Reply.Status(HttpStatusCode.Unauthorized), Reply.Analysis)

        assertEquals(AnalyzeReport(ready = 1, offline = false), f.repository.analyzeWaiting())

        val failed = f.store.rows.value.first()
        assertEquals(CaptureStatus.FAILED, failed.status)
        assertEquals(401, failed.errorStatus)
        f.received.clear()
        assertEquals(AnalyzeReport(ready = 0, offline = false), f.repository.analyzeWaiting())
        assertEquals(emptyList(), f.received)
    }

    @Test
    fun `a missing photo file fails the capture`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.photos.files.clear()

        f.repository.analyzeWaiting()

        val row = f.store.rows.value.single()
        assertEquals(CaptureStatus.FAILED, row.status)
        assertEquals(MISSING_PHOTO_ERROR, row.error)
    }

    @Test
    fun `retry analyzes a failed capture again`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.replies = listOf(Reply.Status(HttpStatusCode.Unauthorized))
        f.repository.analyzeWaiting()
        f.replies = listOf(Reply.Analysis)

        assertEquals(AnalyzeReport(ready = 1, offline = false), f.repository.retry(f.store.rows.value.single().id))

        assertEquals(CaptureStatus.READY, f.store.rows.value.single().status)
    }

    @Test
    fun `discard and complete delete the capture and its photo`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.keepPhoto()
        val (first, second) = f.store.rows.value

        f.repository.discard(first.id)
        f.repository.complete(second.id)

        assertEquals(emptyList(), f.store.rows.value)
        assertTrue(f.photos.files.isEmpty())
    }

    @Test
    fun `another server's captures are neither observed nor analyzed`() = runBlocking<Unit> {
        val f = CaptureFixture()
        f.keepPhoto()
        f.server = "http://other.local:3000"

        assertEquals(AnalyzeReport(ready = 0, offline = false), f.repository.analyzeWaiting())
        assertEquals(emptyList(), f.repository.observeDay("2026-03-05").first())
        assertEquals(emptyList(), f.received)
    }

    @Test
    fun `the background run analyzes with the stored server and token, and the capture's language`() = runBlocking<Unit> {
        val store = FakeCaptureStore()
        val photos = FakePhotoStore()
        val stored = "http://192.168.1.129:3000"
        store.insert(CaptureRow(clientId = "c1", day = "2026-03-05", serverUrl = stored, capturedAt = CAPTURED_AT, description = "sopa", language = "en", createdAt = 0))
        var url = ""
        var authorization: String? = null
        var body = ""
        val engine = MockEngine { request ->
            url = request.url.toString()
            authorization = request.headers[HttpHeaders.Authorization]
            body = request.body.toByteArray().decodeToString()
            respond(ByteReadChannel(ANALYSIS_JSON), HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        val (repository, httpClient) = storedSettingsCaptureQueue(stored, "stored-token", store, photos, engine)

        val report = repository.analyzeWaiting()
        httpClient.close()

        assertEquals(AnalyzeReport(ready = 1, offline = false), report)
        assertEquals("$stored/api/meals/analyze", url)
        assertEquals("Bearer stored-token", authorization)
        assertTrue("sopa" in body && "en" in body, body)
    }
}
