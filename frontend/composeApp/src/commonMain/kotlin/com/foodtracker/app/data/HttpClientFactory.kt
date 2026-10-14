package com.foodtracker.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun platformHttpClientEngine(): HttpClientEngine

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * The app's HTTP client. Every request carries `Authorization: Bearer <token>` when [tokenProvider]
 * returns one; it's read per request, so a token saved in Options applies to the very next call.
 */
fun createHttpClient(
    engine: HttpClientEngine = platformHttpClientEngine(),
    tokenProvider: () -> String? = { BackendConfig.accessToken },
): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
        defaultRequest {
            tokenProvider()?.let { headers.append(HttpHeaders.Authorization, "Bearer $it") }
        }
    }
}
