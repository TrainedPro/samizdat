package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `encounter_log` table ([EncounterLog]).
 *
 * Records DTN (Delay-Tolerant Networking) encounters between mesh nodes
 * for analytics and store-forward optimization.
 *
 * @see EncounterLog
 * @see AppDatabase
 */
@Dao
interface EncounterDao {

    @Insert
    suspend fun insert(encounter: EncounterLog): Long

    /** Update encounter end-time and exchange stats on disconnection. */
    @Query("UPDATE encounter_log SET endTime = :endTime, packetsExchanged = :packets, bytesExchanged = :bytes WHERE id = :id")
    suspend fun finishEncounter(id: Long, endTime: Long, packets: Int, bytes: Long)

    /** Get all encounters, newest first. */
    @Query("SELECT * FROM encounter_log ORDER BY startTime DESC")
    fun getAllEncounters(): Flow<List<EncounterLog>>

    /** Get encounters with a specific peer. */
    @Query("SELECT * FROM encounter_log WHERE remotePeer = :peer ORDER BY startTime DESC")
    fun getEncountersWith(peer: String): Flow<List<EncounterLog>>

    /** Get encounters in a time range (for analytics). */
    @Query("SELECT * FROM encounter_log WHERE startTime >= :from AND startTime <= :to ORDER BY startTime ASC")
    suspend fun getEncountersInRange(from: Long, to: Long): List<EncounterLog>

    /** Count total encounters (for dashboard). */
    @Query("SELECT COUNT(*) FROM encounter_log")
    suspend fun totalEncounters(): Int

    /** Get the most recent encounter with a peer. */
    @Query("SELECT * FROM encounter_log WHERE remotePeer = :peer ORDER BY startTime DESC LIMIT 1")
    suspend fun lastEncounterWith(peer: String): EncounterLog?

    /** Delete all encounters. */
    @Query("DELETE FROM encounter_log")
    suspend fun deleteAll()
}
