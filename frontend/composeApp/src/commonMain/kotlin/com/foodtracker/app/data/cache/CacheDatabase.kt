package com.foodtracker.app.data.cache

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

    /** Writes a refreshed day and the goal together. */
    suspend fun save(day: DayCacheRow, goal: GoalCacheRow)

    suspend fun deleteDay(date: String)

    suspend fun deleteGoal()

    suspend fun clear()
}

@Dao
abstract class DayCacheDao : DayCacheStore {
    @Query("SELECT * FROM day_cache WHERE date = :date")
    abstract override fun observeDay(date: String): Flow<DayCacheRow?>

    @Query("SELECT * FROM goal_cache WHERE id = $GOAL_ROW_ID")
    abstract override fun observeGoal(): Flow<GoalCacheRow?>

    @Upsert
    abstract suspend fun upsertDay(day: DayCacheRow)

    @Upsert
    abstract suspend fun upsertGoal(goal: GoalCacheRow)

    @Transaction
    override suspend fun save(day: DayCacheRow, goal: GoalCacheRow) {
        upsertDay(day)
        upsertGoal(goal)
    }

    @Query("DELETE FROM day_cache WHERE date = :date")
    abstract override suspend fun deleteDay(date: String)

    @Query("DELETE FROM goal_cache")
    abstract override suspend fun deleteGoal()

    @Query("DELETE FROM day_cache")
    abstract suspend fun deleteAllDays()

    @Transaction
    override suspend fun clear() {
        deleteAllDays()
        deleteGoal()
    }
}

/** The app's local database. It only holds the cache for now; #70 adds the send queue. */
@Database(entities = [DayCacheRow::class, GoalCacheRow::class], version = 1, exportSchema = true)
@ConstructedBy(CacheDatabaseConstructor::class)
abstract class CacheDatabase : RoomDatabase() {
    abstract fun dayCacheDao(): DayCacheDao
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
