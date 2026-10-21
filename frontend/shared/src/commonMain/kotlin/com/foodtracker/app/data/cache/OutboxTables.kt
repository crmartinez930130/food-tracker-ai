package com.foodtracker.app.data.cache

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

/** What an outbox row will send. */
enum class OutboxKind { MEAL, WATER }

/**
 * A meal or glass saved while the backend couldn't be reached, waiting to be sent (offline-queue).
 * [payloadJson] is the full request as it will be sent, its clientId and time included; [day] is the
 * local day it counts for. A row with [error] set was rejected by the backend and isn't sent again
 * until the user retries it.
 */
@Entity(tableName = "outbox", indices = [Index(value = ["client_id"], unique = true)])
data class OutboxRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_id") val clientId: String,
    val kind: OutboxKind,
    val day: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "payload_json") val payloadJson: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val error: String? = null,
    @ColumnInfo(name = "error_status") val errorStatus: Int? = null,
)

/** What [OutboxRepository] needs from the outbox table, so its tests can use an in-memory fake. */
interface OutboxStore {
    fun observeDay(day: String, serverUrl: String): Flow<List<OutboxRow>>

    /** The rows that may be sent (not rejected), oldest first. */
    suspend fun sendable(serverUrl: String): List<OutboxRow>

    suspend fun pendingOfDay(day: String, serverUrl: String): List<OutboxRow>

    suspend fun get(id: Long): OutboxRow?

    suspend fun insert(row: OutboxRow): Long

    suspend fun markRejected(id: Long, error: String, status: Int)

    suspend fun clearError(id: Long)

    suspend fun delete(id: Long)
}

@Dao
abstract class OutboxDao : OutboxStore {
    @Query("SELECT * FROM outbox WHERE day = :day AND server_url = :serverUrl ORDER BY id")
    abstract override fun observeDay(day: String, serverUrl: String): Flow<List<OutboxRow>>

    @Query("SELECT * FROM outbox WHERE server_url = :serverUrl AND error IS NULL ORDER BY id")
    abstract override suspend fun sendable(serverUrl: String): List<OutboxRow>

    @Query("SELECT * FROM outbox WHERE day = :day AND server_url = :serverUrl AND error IS NULL ORDER BY id")
    abstract override suspend fun pendingOfDay(day: String, serverUrl: String): List<OutboxRow>

    @Query("SELECT * FROM outbox WHERE id = :id")
    abstract override suspend fun get(id: Long): OutboxRow?

    @Insert
    abstract override suspend fun insert(row: OutboxRow): Long

    @Query("UPDATE outbox SET error = :error, error_status = :status WHERE id = :id")
    abstract override suspend fun markRejected(id: Long, error: String, status: Int)

    @Query("UPDATE outbox SET error = NULL, error_status = NULL WHERE id = :id")
    abstract override suspend fun clearError(id: Long)

    @Query("DELETE FROM outbox WHERE id = :id")
    abstract override suspend fun delete(id: Long)
}

/** The favorites list Capture last loaded, so it can be shown (and re-logged) offline; a single row. */
@Entity(tableName = "favorites_cache")
data class FavoritesCacheRow(
    @PrimaryKey val id: Int = FAVORITES_ROW_ID,
    @ColumnInfo(name = "favorites_json") val favoritesJson: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "refreshed_at") val refreshedAt: Long,
)

const val FAVORITES_ROW_ID = 1

interface FavoritesCacheStore {
    suspend fun get(): FavoritesCacheRow?

    suspend fun save(row: FavoritesCacheRow)

    /** Like [DayCacheStore.deleteOtherServers]: a server change keeps only the new server's copy. */
    suspend fun deleteOtherServers(url: String)
}

@Dao
abstract class FavoritesCacheDao : FavoritesCacheStore {
    @Query("SELECT * FROM favorites_cache WHERE id = $FAVORITES_ROW_ID")
    abstract override suspend fun get(): FavoritesCacheRow?

    @Upsert
    abstract override suspend fun save(row: FavoritesCacheRow)

    @Query("DELETE FROM favorites_cache WHERE server_url != :url")
    abstract override suspend fun deleteOtherServers(url: String)
}
