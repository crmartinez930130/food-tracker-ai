package com.foodtracker.app.data

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun platformHttpClientEngine(): HttpClientEngine

private val jsonConfig = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun createHttpClient(engine: HttpClientEngine = platformHttpClientEngine()): HttpClient {
    return HttpClient(engine) {
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }
}
