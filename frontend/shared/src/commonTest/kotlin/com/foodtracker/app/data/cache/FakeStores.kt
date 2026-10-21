package com.foodtracker.app.data.cache

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** An in-memory [DayCacheStore], so the repositories are tested without Android or a database. */
class FakeDayCache(private val log: MutableList<String> = mutableListOf()) : DayCacheStore {
    val days = MutableStateFlow<Map<String, DayCacheRow>>(emptyMap())
    val goal = MutableStateFlow<GoalCacheRow?>(null)

    override fun observeDay(date: String): Flow<DayCacheRow?> = days.map { it[date] }

    override fun observeGoal(): Flow<GoalCacheRow?> = goal

    override suspend fun day(date: String): DayCacheRow? = days.value[date]

    override suspend fun saveDay(day: DayCacheRow) {
        log += "cache ${day.date}"
        days.value = days.value + (day.date to day)
    }

    override suspend fun save(day: DayCacheRow, goal: GoalCacheRow) {
        saveDay(day)
        this.goal.value = goal
    }

    override suspend fun deleteDay(date: String) {
        days.value = days.value - date
    }

    override suspend fun deleteGoal() {
        goal.value = null
    }

    override suspend fun deleteOtherServers(url: String) {
        days.value = days.value.filterValues { it.serverUrl == url }
        if (goal.value?.serverUrl != url) goal.value = null
    }
}

/** An in-memory [OutboxStore] with the same ordering and filters as the Room DAO. */
class FakeOutbox(private val log: MutableList<String> = mutableListOf()) : OutboxStore {
    val rows = MutableStateFlow<List<OutboxRow>>(emptyList())
    private var nextId = 1L

    override fun observeDay(day: String, serverUrl: String): Flow<List<OutboxRow>> =
        rows.map { all -> all.filter { it.day == day && it.serverUrl == serverUrl } }

    override suspend fun sendable(serverUrl: String) = rows.value.filter { it.serverUrl == serverUrl && it.error == null }

    override suspend fun pendingOfDay(day: String, serverUrl: String) =
        rows.value.filter { it.day == day && it.serverUrl == serverUrl && it.error == null }

    override suspend fun get(id: Long) = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(row: OutboxRow): Long {
        require(rows.value.none { it.clientId == row.clientId }) { "client_id must be unique" }
        val id = nextId++
        rows.value = rows.value + row.copy(id = id)
        return id
    }

    override suspend fun markRejected(id: Long, error: String, status: Int) = update(id) { it.copy(error = error, errorStatus = status) }

    override suspend fun clearError(id: Long) = update(id) { it.copy(error = null, errorStatus = null) }

    override suspend fun delete(id: Long) {
        log += "delete $id"
        rows.value = rows.value.filterNot { it.id == id }
    }

    private fun update(id: Long, change: (OutboxRow) -> OutboxRow) {
        rows.value = rows.value.map { if (it.id == id) change(it) else it }
    }
}

class FakeFavoritesCache : FavoritesCacheStore {
    var row: FavoritesCacheRow? = null

    override suspend fun get() = row

    override suspend fun save(row: FavoritesCacheRow) {
        this.row = row
    }

    override suspend fun deleteOtherServers(url: String) {
        if (row?.serverUrl != url) row = null
    }
}

/** An in-memory [CaptureStore] with the same ordering and filters as the Room DAO. */
class FakeCaptureStore : CaptureStore {
    val rows = MutableStateFlow<List<CaptureRow>>(emptyList())
    private var nextId = 1L

    override fun observeDay(day: String, serverUrl: String): Flow<List<CaptureRow>> =
        rows.map { all -> all.filter { it.day == day && it.serverUrl == serverUrl } }

    override suspend fun waiting(serverUrl: String) = rows.value.filter { it.serverUrl == serverUrl && it.status == CaptureStatus.WAITING }

    override suspend fun get(id: Long) = rows.value.firstOrNull { it.id == id }

    override suspend fun insert(row: CaptureRow): Long {
        val id = nextId++
        rows.value = rows.value + row.copy(id = id)
        return id
    }

    override suspend fun markReady(id: Long, analysisJson: String) =
        update(id) { it.copy(status = CaptureStatus.READY, analysisJson = analysisJson, error = null, errorStatus = null) }

    override suspend fun markFailed(id: Long, error: String, status: Int?) =
        update(id) { it.copy(status = CaptureStatus.FAILED, error = error, errorStatus = status) }

    override suspend fun markWaiting(id: Long) = update(id) { it.copy(status = CaptureStatus.WAITING, error = null, errorStatus = null) }

    override suspend fun delete(id: Long) {
        rows.value = rows.value.filterNot { it.id == id }
    }

    private fun update(id: Long, change: (CaptureRow) -> CaptureRow) {
        rows.value = rows.value.map { if (it.id == id) change(it) else it }
    }
}

class FakePhotoStore : CapturePhotoStore {
    val files = mutableMapOf<String, ByteArray>()

    override suspend fun write(name: String, bytes: ByteArray) {
        files[name] = bytes
    }

    override suspend fun read(name: String) = files[name]

    override suspend fun delete(name: String) {
        files -= name
    }
}

class FakeSnapshotStore : SnapshotStore {
    val rows = mutableMapOf<String, SnapshotRow>()

    override suspend fun get(key: String) = rows[key]

    override suspend fun save(row: SnapshotRow) {
        rows[row.key] = row
    }

    override suspend fun delete(key: String) {
        rows -= key
    }

    override suspend fun deleteOtherServers(url: String) {
        rows.values.removeAll { it.serverUrl != url }
    }
}
