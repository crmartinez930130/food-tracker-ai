package com.foodtracker.app.data.cache

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Index
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import kotlinx.coroutines.flow.Flow

/** Where a kept capture is: waiting for its analysis, analyzed and waiting for review, or refused. */
enum class CaptureStatus { WAITING, READY, FAILED }

/**
 * A meal captured while the backend couldn't be reached (offline-queue): what the analysis takes, kept
 * until it can be analyzed and then reviewed. [photoFile] names the meal photo in [CapturePhotoStore],
 * if there's one, and [ingredientFiles] the ingredient photos; [capturedAt] is the tap's time (ISO), which the saved meal keeps; [day] is its local day.
 */
@Entity(tableName = "capture_queue", indices = [Index(value = ["client_id"], unique = true)])
data class CaptureRow(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "client_id") val clientId: String,
    val day: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "captured_at") val capturedAt: String,
    val description: String? = null,
    val portion: String? = null,
    val language: String,
    @ColumnInfo(name = "photo_file") val photoFile: String? = null,
    /** The ingredient photos' names in [CapturePhotoStore], as a JSON list; null when there are none. */
    @ColumnInfo(name = "ingredient_files") val ingredientFiles: String? = null,
    val status: CaptureStatus = CaptureStatus.WAITING,
    @ColumnInfo(name = "analysis_json") val analysisJson: String? = null,
    val error: String? = null,
    @ColumnInfo(name = "error_status") val errorStatus: Int? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

/** What [CaptureQueueRepository] needs from the capture table, so its tests can use an in-memory fake. */
interface CaptureStore {
    fun observeDay(day: String, serverUrl: String): Flow<List<CaptureRow>>

    /** The captures waiting for their analysis, oldest first. */
    suspend fun waiting(serverUrl: String): List<CaptureRow>

    suspend fun get(id: Long): CaptureRow?

    suspend fun insert(row: CaptureRow): Long

    suspend fun markReady(id: Long, analysisJson: String)

    suspend fun markFailed(id: Long, error: String, status: Int?)

    /** Back to waiting, its error cleared (retry). */
    suspend fun markWaiting(id: Long)

    suspend fun delete(id: Long)
}

@Dao
abstract class CaptureDao : CaptureStore {
    @Query("SELECT * FROM capture_queue WHERE day = :day AND server_url = :serverUrl ORDER BY id")
    abstract override fun observeDay(day: String, serverUrl: String): Flow<List<CaptureRow>>

    @Query("SELECT * FROM capture_queue WHERE server_url = :serverUrl AND status = 'WAITING' ORDER BY id")
    abstract override suspend fun waiting(serverUrl: String): List<CaptureRow>

    @Query("SELECT * FROM capture_queue WHERE id = :id")
    abstract override suspend fun get(id: Long): CaptureRow?

    @Insert
    abstract override suspend fun insert(row: CaptureRow): Long

    @Query("UPDATE capture_queue SET status = 'READY', analysis_json = :analysisJson, error = NULL, error_status = NULL WHERE id = :id")
    abstract override suspend fun markReady(id: Long, analysisJson: String)

    @Query("UPDATE capture_queue SET status = 'FAILED', error = :error, error_status = :status WHERE id = :id")
    abstract override suspend fun markFailed(id: Long, error: String, status: Int?)

    @Query("UPDATE capture_queue SET status = 'WAITING', error = NULL, error_status = NULL WHERE id = :id")
    abstract override suspend fun markWaiting(id: Long)

    @Query("DELETE FROM capture_queue WHERE id = :id")
    abstract override suspend fun delete(id: Long)
}

/** The photos of kept captures, as files in the app's private storage (not backed up). */
interface CapturePhotoStore {
    suspend fun write(name: String, bytes: ByteArray)

    /** The photo, or null when the file is gone. */
    suspend fun read(name: String): ByteArray?

    suspend fun delete(name: String)
}
