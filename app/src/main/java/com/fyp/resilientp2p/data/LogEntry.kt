package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logs")
data class LogEntry(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val timestamp: Long = System.currentTimeMillis(),
        val message: String,
        val level: LogLevel = LogLevel.INFO,
        val logType: LogType = LogType.SYSTEM,
        val peerId: String? = null, // Optional, for filtering by peer
        val rssi: Int? = null,
        val latencyMs: Long? = null,
        val payloadSizeBytes: Int? = null
) {
    val formattedTimestamp: String
        get() =
                java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(timestamp))
}

enum class LogType {
    SYSTEM,
    CHAT
}

enum class LogLevel {
    ERROR, // 0
    WARN, // 1
    METRIC, // 2
    INFO, // 3
    DEBUG, // 4
    TRACE // 5
}
