package com.foodtracker.app.ui.settings

import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.createHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerVersionTest {

    private val seenPaths = mutableListOf<String>()

    private fun api(status: HttpStatusCode, body: String, contentType: String = "application/json", delayMs: Long = 0) =
        FoodTrackerApiClient(
            createHttpClient(
                MockEngine { request ->
                    seenPaths += request.url.encodedPath
                    delay(delayMs)
                    respond(ByteReadChannel(body), status, headersOf(HttpHeaders.ContentType, contentType))
                },
            ),
            baseUrlProvider = { "http://test.local" },
        )

    private fun fetch(api: FoodTrackerApiClient, timeoutMs: Long = 5_000) = runBlocking { fetchServerVersion(api, timeoutMs) }

    @Test
    fun `a server with the endpoint reports its version`() {
        assertEquals(ServerVersionState.Known("0.2.0"), fetch(api(HttpStatusCode.OK, """{"version":"0.2.0"}""")))
        assertEquals(listOf("/api/version"), seenPaths)
    }

    @Test
    fun `a server without the endpoint is older than 0_2_0`() {
        // Express's default 404 is an HTML page, not the usual {"error": ...}.
        val old = api(HttpStatusCode.NotFound, "<pre>Cannot GET /api/version</pre>", contentType = "text/html")
        assertEquals(ServerVersionState.Older, fetch(old))
    }

    @Test
    fun `a rejected token or a server error is unavailable`() {
        assertEquals(ServerVersionState.Unavailable, fetch(api(HttpStatusCode.Unauthorized, """{"error":"Missing or invalid access token"}""")))
        assertEquals(ServerVersionState.Unavailable, fetch(api(HttpStatusCode.InternalServerError, """{"error":"Internal server error"}""")))
    }

    @Test
    fun `an unreadable answer is unavailable`() {
        assertEquals(ServerVersionState.Unavailable, fetch(api(HttpStatusCode.OK, "not json")))
    }

    @Test
    fun `a network failure is unavailable`() {
        val failing = FoodTrackerApiClient(
            createHttpClient(MockEngine { error("connection refused") }),
            baseUrlProvider = { "http://test.local" },
        )
        assertEquals(ServerVersionState.Unavailable, fetch(failing))
    }

    @Test
    fun `a server slower than the timeout is unavailable`() {
        assertEquals(ServerVersionState.Unavailable, fetch(api(HttpStatusCode.OK, """{"version":"0.2.0"}""", delayMs = 2_000), timeoutMs = 100))
    }
}
