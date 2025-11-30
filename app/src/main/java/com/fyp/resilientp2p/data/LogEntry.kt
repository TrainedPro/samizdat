package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // e.g., "CONNECTION", "PING", "ERROR", "INFO"
    val peerId: String? = null,
    val message: String,
    val rssi: Int? = null, // BandwidthInfo.Quality or raw RSSI if available
    val latencyMs: Long? = null,
    val payloadSizeBytes: Int? = null
)
