package com.foodtracker.app.data.cache

import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry
import kotlinx.coroutines.CancellationException

/** Capture's favorites: the copy last loaded for the current server, so they can be re-logged offline. */
class FavoritesRepository(
    private val api: FoodTrackerApiClient,
    private val store: FavoritesCacheStore,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
) {
    /** The favorites last loaded from the current server and when, or null when there's no usable copy. */
    suspend fun cached(): Snapshot<List<MealEntry>>? {
        val row = store.get()?.takeIf { it.serverUrl == serverUrl() } ?: return null
        return decodeOrNull<List<MealEntry>>(row.favoritesJson)?.let { Snapshot(it, row.refreshedAt) }
    }

    /** Loads the favorites from the backend and keeps them as the new copy; never throws for a failed call. */
    suspend fun refresh(): Refreshed<List<MealEntry>> {
        val url = serverUrl()
        return try {
            val favorites = api.listFavorites()
            // The server changed while this ran: the list belongs to the old one, so don't keep it.
            if (serverUrl() == url) {
                store.save(FavoritesCacheRow(favoritesJson = cacheJson.encodeToString(favorites), serverUrl = url, refreshedAt = now()))
            }
            Refreshed.Fresh(favorites)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Refreshed.Failed(e)
        } catch (e: Exception) {
            Refreshed.Offline(e)
        }
    }
}

/**
 * What Capture's favorites section shows (offline-queue): [favorites] (null while there is nothing
 * to list), and at most one line about them.
 */
data class FavoritesView(
    val favorites: List<MealEntry>?,
    /** When set, the list is a copy from this time (epoch ms), shown because the backend can't be reached. */
    val offlineSince: Long? = null,
    /** No connection and no copy: the favorites were never loaded from this server. */
    val notLoaded: Boolean = false,
    /** The backend answered with an error and there is no copy to show. */
    val error: ApiException? = null,
)

/** The section after a refresh: the fresh list, else the copy (marked offline), else why there's none. */
fun favoritesView(cached: Snapshot<List<MealEntry>>?, refreshed: Refreshed<List<MealEntry>>): FavoritesView = when (refreshed) {
    is Refreshed.Fresh -> FavoritesView(refreshed.value)
    is Refreshed.Offline ->
        if (cached != null) FavoritesView(cached.value, offlineSince = cached.refreshedAt) else FavoritesView(null, notLoaded = true)
    // As before: an answered error stays quiet over a copy.
    is Refreshed.Failed -> if (cached != null) FavoritesView(cached.value) else FavoritesView(null, error = refreshed.error)
}
