package com.foodtracker.app.ui.settings

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.FoodTrackerApiClient
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.cancellation.CancellationException

/** What Options shows as the server's version. */
sealed interface ServerVersionState {
    data object Loading : ServerVersionState
    data class Known(val version: String) : ServerVersionState
    /** The server answered but has no version endpoint: it predates 0.2.0. */
    data object Older : ServerVersionState
    /** Unreachable, too slow, token rejected or any other error. */
    data object Unavailable : ServerVersionState
}

private const val HTTP_NOT_FOUND = 404

/** Asks [api] for the server's version, giving up after [timeoutMs]. Never throws but for cancellation. */
suspend fun fetchServerVersion(api: FoodTrackerApiClient, timeoutMs: Long): ServerVersionState = try {
    withTimeoutOrNull(timeoutMs) { ServerVersionState.Known(api.getVersion()) } ?: ServerVersionState.Unavailable
} catch (e: CancellationException) {
    throw e
} catch (e: ApiException) {
    if (e.status == HTTP_NOT_FOUND) ServerVersionState.Older else ServerVersionState.Unavailable
} catch (e: Exception) {
    ServerVersionState.Unavailable
}
