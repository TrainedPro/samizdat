package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Room DAO for the `telemetry_events` table ([TelemetryEvent]).
 *
 * Manages the upload lifecycle: events are inserted as "pending", batch-uploaded
 * by [com.fyp.resilientp2p.managers.TelemetryUploadWorker], and then marked uploaded
 * or cleaned up. A hard cap of 5 000 rows prevents unbounded DB growth.
 *
 * @see TelemetryEvent
 * @see com.fyp.resilientp2p.managers.TelemetryManager
 */
@Dao
interface TelemetryDao {
    @Insert
    suspend fun insert(event: TelemetryEvent)

    @Insert
    suspend fun insertAll(events: List<TelemetryEvent>)

    /** Get the oldest un-uploaded events, up to [limit] for batch upload */
    @Query("SELECT * FROM telemetry_events WHERE uploaded = 0 ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingEvents(limit: Int = 100): List<TelemetryEvent>

    /** Count pending (not yet uploaded) events */
    @Query("SELECT COUNT(*) FROM telemetry_events WHERE uploaded = 0")
    suspend fun getPendingCount(): Int

    /** Mark a batch of events as uploaded by their IDs */
    @Query("UPDATE telemetry_events SET uploaded = 1 WHERE id IN (:ids)")
    suspend fun markUploaded(ids: List<Long>)

    /** Increment upload attempt counter for failed events */
    @Query("UPDATE telemetry_events SET uploadAttempts = uploadAttempts + 1 WHERE id IN (:ids)")
    suspend fun incrementAttempts(ids: List<Long>)

    /** Delete uploaded events (cleanup after confirmed upload) */
    @Query("DELETE FROM telemetry_events WHERE uploaded = 1")
    suspend fun deleteUploaded()

    /** Delete events older than cutoff (retention policy) */
    @Query("DELETE FROM telemetry_events WHERE timestamp < :cutoff")
    suspend fun deleteOlderThan(cutoff: Long)

    /** Delete events that have failed too many times */
    @Query("DELETE FROM telemetry_events WHERE uploadAttempts >= :maxAttempts")
    suspend fun deleteFailed(maxAttempts: Int = 10)

    /** Total count of all events (for monitoring DB size) */
    @Query("SELECT COUNT(*) FROM telemetry_events")
    suspend fun getTotalCount(): Int

    /** Hard cap: delete oldest events if total exceeds limit */
    @Query("DELETE FROM telemetry_events WHERE id NOT IN (SELECT id FROM telemetry_events ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun enforceMaxCount(keepCount: Int = 5000)

    @Query("DELETE FROM telemetry_events")
    suspend fun deleteAll()
}
