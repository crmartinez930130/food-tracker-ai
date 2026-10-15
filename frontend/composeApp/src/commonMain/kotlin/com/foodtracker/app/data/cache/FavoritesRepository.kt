package com.foodtracker.app.data.cache

import com.foodtracker.app.data.BackendConfig
import com.foodtracker.app.data.FoodTrackerApiClient
import com.foodtracker.app.data.MealEntry

/** Capture's favorites: the copy last loaded for the current server, so they can be re-logged offline. */
class FavoritesRepository(
    private val api: FoodTrackerApiClient,
    private val store: FavoritesCacheStore,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
) {
    /** The favorites last loaded from the current server, or null when there's no usable copy. */
    suspend fun cached(): List<MealEntry>? =
        store.get()?.takeIf { it.serverUrl == serverUrl() }?.let { decodeOrNull<List<MealEntry>>(it.favoritesJson) }

    /** Loads the favorites from the backend and keeps them as the new copy; throws like the API call. */
    suspend fun load(): List<MealEntry> {
        val url = serverUrl()
        val favorites = api.listFavorites()
        if (serverUrl() == url) {
            store.save(FavoritesCacheRow(favoritesJson = cacheJson.encodeToString(favorites), serverUrl = url, refreshedAt = now()))
        }
        return favorites
    }
}
