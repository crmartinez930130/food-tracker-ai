package com.foodtracker.app.data.cache

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import com.foodtracker.app.data.ApiException
import com.foodtracker.app.data.BackendConfig
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.KSerializer

/**
 * The latest copy of a screen's data for one key, e.g. a History month or a Summary period
 * (offline-cache): the value as JSON, the server it came from and when it was fetched (epoch ms).
 */
@Entity(tableName = "snapshot_cache")
data class SnapshotRow(
    @PrimaryKey val key: String,
    val json: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "refreshed_at") val refreshedAt: Long,
)

/** What [SnapshotRepository] needs from the table, so its tests can use an in-memory fake. */
interface SnapshotStore {
    suspend fun get(key: String): SnapshotRow?

    suspend fun save(row: SnapshotRow)

    suspend fun delete(key: String)

    /** Like [DayCacheStore.deleteOtherServers]: a server change keeps only the new server's copies. */
    suspend fun deleteOtherServers(url: String)
}

@Dao
abstract class SnapshotDao : SnapshotStore {
    @Query("SELECT * FROM snapshot_cache WHERE `key` = :key")
    abstract override suspend fun get(key: String): SnapshotRow?

    @Upsert
    abstract override suspend fun save(row: SnapshotRow)

    @Query("DELETE FROM snapshot_cache WHERE `key` = :key")
    abstract override suspend fun delete(key: String)

    @Query("DELETE FROM snapshot_cache WHERE server_url != :url")
    abstract override suspend fun deleteOtherServers(url: String)
}

/** A cached value and when it was fetched. */
data class Snapshot<T>(val value: T, val refreshedAt: Long)

/** How a refresh went: a fresh value, no answer, or an error answer. */
sealed interface Refreshed<out T> {
    data class Fresh<T>(val value: T) : Refreshed<T>

    data class Offline(val cause: Exception) : Refreshed<Nothing>

    data class Failed(val error: ApiException) : Refreshed<Nothing>
}

/**
 * The latest copy of what History and Summary show, per key and server (offline-cache): read it at
 * once with [cached], then [refresh] it from the backend, which replaces it when the backend answers.
 */
class SnapshotRepository(
    private val store: SnapshotStore,
    private val serverUrl: () -> String = { BackendConfig.baseUrl },
    private val now: () -> Long = ::epochMillisNow,
) {
    /** The current server's copy of [key], or null without one (a copy that no longer decodes is dropped). */
    suspend fun <T> cached(key: String, serializer: KSerializer<T>): Snapshot<T>? {
        val row = store.get(key)?.takeIf { it.serverUrl == serverUrl() } ?: return null
        val value = try {
            cacheJson.decodeFromString(serializer, row.json)
        } catch (_: IllegalArgumentException) {
            store.delete(key)
            return null
        }
        return Snapshot(value, row.refreshedAt)
    }

    /** Fetches [key]'s value and keeps it as the new copy; never throws for a failed call. */
    suspend fun <T> refresh(key: String, serializer: KSerializer<T>, fetch: suspend () -> T): Refreshed<T> {
        val url = serverUrl()
        return try {
            val value = fetch()
            // The server changed while this ran: the value belongs to the old one, so don't keep it.
            if (serverUrl() == url) store.save(SnapshotRow(key, cacheJson.encodeToString(serializer, value), url, now()))
            Refreshed.Fresh(value)
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            Refreshed.Failed(e)
        } catch (e: Exception) {
            Refreshed.Offline(e)
        }
    }

    suspend fun deleteOtherServers(currentUrl: String) = store.deleteOtherServers(currentUrl)
}
