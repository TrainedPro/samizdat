package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LogDao {
    @Insert
    suspend fun insert(entry: LogEntry)

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<LogEntry>>

    @Query("SELECT * FROM logs ORDER BY timestamp DESC")
    suspend fun getLogsSnapshot(): List<LogEntry>

    @Query("DELETE FROM logs")
    suspend fun clearAll()
}
