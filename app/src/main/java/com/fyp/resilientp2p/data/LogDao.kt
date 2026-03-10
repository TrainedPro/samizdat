package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `logs` table ([LogEntry]).
 *
 * Provides reactive [Flow]-based queries for the UI log viewer and snapshot
 * queries for CSV export and telemetry upload.
 *
 * @see LogEntry
 * @see AppDatabase
 */
@Dao
interface LogDao {
    @Insert suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC") fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    suspend fun getLogsSnapshot(): List<LogEntry>

    @Query("DELETE FROM logs") suspend fun deleteAll()

    @Query("DELETE FROM logs WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    @Query("SELECT * FROM logs WHERE (level = 'ERROR' OR level = 'WARN') AND timestamp > :since ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getErrorLogsSince(since: Long, limit: Int): List<LogEntry>

    @Query("SELECT * FROM logs WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun getLogsSince(afterId: Long, limit: Int): List<LogEntry>
}
