package com.foodtracker.app.data.cache

import androidx.room3.AutoMigration
import androidx.room3.ColumnInfo
import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.Transaction
import androidx.room3.Upsert
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow

/**
 * One day as the backend last returned it: its meals and water, as the API's JSON (see design D2).
 * [serverUrl] is the backend it came from; [refreshedAt] is epoch milliseconds.
 */
@Entity(tableName = "day_cache")
data class DayCacheRow(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "meals_json") val mealsJson: String,
    @ColumnInfo(name = "water_json") val waterJson: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "refreshed_at") val refreshedAt: Long,
)

/** The daily goal as the backend last returned it; a single row, always [GOAL_ROW_ID]. */
@Entity(tableName = "goal_cache")
data class GoalCacheRow(
    @PrimaryKey val id: Int = GOAL_ROW_ID,
    @ColumnInfo(name = "goal_json") val goalJson: String,
    @ColumnInfo(name = "server_url") val serverUrl: String,
    @ColumnInfo(name = "refreshed_at") val refreshedAt: Long,
)

const val GOAL_ROW_ID = 1

/** What [DayRepository] needs from the cache, so its tests can use an in-memory fake. */
interface DayCacheStore {
    fun observeDay(date: String): Flow<DayCacheRow?>

    fun observeGoal(): Flow<GoalCacheRow?>

    suspend fun day(date: String): DayCacheRow?

    /** Writes one day alone, e.g. with a just-sent entry merged in (offline-queue). */
    suspend fun saveDay(day: DayCacheRow)

    /** Writes a refreshed day and the goal together. */
    suspend fun save(day: DayCacheRow, goal: GoalCacheRow)

    suspend fun deleteDay(date: String)

    suspend fun deleteGoal()

    /** Deletes the cached rows of every server but [url], so a refresh of [url] that already landed stays. */
    suspend fun deleteOtherServers(url: String)
}

@Dao
abstract class DayCacheDao : DayCacheStore {
    @Query("SELECT * FROM day_cache WHERE date = :date")
    abstract override fun observeDay(date: String): Flow<DayCacheRow?>

    @Query("SELECT * FROM goal_cache WHERE id = $GOAL_ROW_ID")
    abstract override fun observeGoal(): Flow<GoalCacheRow?>

    @Query("SELECT * FROM day_cache WHERE date = :date")
    abstract override suspend fun day(date: String): DayCacheRow?

    @Upsert
    abstract override suspend fun saveDay(day: DayCacheRow)

    @Upsert
    abstract suspend fun upsertGoal(goal: GoalCacheRow)

    @Transaction
    override suspend fun save(day: DayCacheRow, goal: GoalCacheRow) {
        saveDay(day)
        upsertGoal(goal)
    }

    @Query("DELETE FROM day_cache WHERE date = :date")
    abstract override suspend fun deleteDay(date: String)

    @Query("DELETE FROM goal_cache")
    abstract override suspend fun deleteGoal()

    @Query("DELETE FROM day_cache WHERE server_url != :url")
    abstract suspend fun deleteDaysOfOtherServers(url: String)

    @Query("DELETE FROM goal_cache WHERE server_url != :url")
    abstract suspend fun deleteGoalOfOtherServers(url: String)

    @Transaction
    override suspend fun deleteOtherServers(url: String) {
        deleteDaysOfOtherServers(url)
        deleteGoalOfOtherServers(url)
    }
}

/**
 * The app's local database: the cache of what was seen (offline-cache) and the queue of what waits
 * to be sent (offline-queue). Version 2 added the outbox and the favorites cache; version 3 the
 * captures kept for analysis; version 4 the History and Summary copies; version 5 the kept captures'
 * ingredient photos.
 */
@Database(
    entities = [DayCacheRow::class, GoalCacheRow::class, OutboxRow::class, FavoritesCacheRow::class, CaptureRow::class, SnapshotRow::class],
    version = 5,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
    ],
)
@ConstructedBy(CacheDatabaseConstructor::class)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun dayCacheDao(): DayCacheDao

    abstract fun outboxDao(): OutboxDao

    abstract fun favoritesCacheDao(): FavoritesCacheDao

    abstract fun captureDao(): CaptureDao

    abstract fun snapshotDao(): SnapshotDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object CacheDatabaseConstructor : RoomDatabaseConstructor<CacheDatabase> {
    override fun initialize(): CacheDatabase
}

const val CACHE_DATABASE_NAME = "food-tracker-cache.db"

/** Finishes a platform's builder: the bundled SQLite (the same on every platform) and IO queries. */
fun RoomDatabase.Builder<CacheDatabase>.buildCacheDatabase(): CacheDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
