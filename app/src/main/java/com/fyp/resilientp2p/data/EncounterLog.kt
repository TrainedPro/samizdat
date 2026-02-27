package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Records a DTN (Delay-Tolerant Networking) encounter between two devices.
 *
 * When devices meet on the mesh they exchange buffered data.
 * The encounter log records who met, when, and how much data was exchanged.
 * This is used for DTN analytics, sneakernet performance measurement,
 * and to avoid redundant store-forward flushes.
 *
 * @property id Auto-generated primary key.
 * @property localPeer This device's peer name at the time of encounter.
 * @property remotePeer The other device's peer name.
 * @property startTime Epoch-ms when the encounter began (connection established).
 * @property endTime Epoch-ms when the encounter ended (disconnection).
 * @property packetsExchanged Number of store-forward packets flushed during encounter.
 * @property bytesExchanged Total bytes exchanged during encounter.
 * @property rssi Signal strength of the connection (or 0 if unknown).
 */
@Entity(
    tableName = "encounter_log",
    indices = [
        Index(value = ["remotePeer"]),
        Index(value = ["startTime"])
    ]
)
data class EncounterLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localPeer: String,
    val remotePeer: String,
    val startTime: Long,
    val endTime: Long = 0L,
    val packetsExchanged: Int = 0,
    val bytesExchanged: Long = 0L,
    val rssi: Int = 0
)
