package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single log line persisted to the local database.
 *
 * Logs are written by every subsystem (P2PManager, HeartbeatManager, etc.) and
 * displayed in the UI's scrolling log view. Indexed by [timestamp] for efficient
 * range queries and CSV export.
 *
 * @property id Auto-generated primary key.
 * @property timestamp Unix epoch millis when the log was created.
 * @property message Human-readable log text.
 * @property level Severity — controls UI filtering and color coding.
 * @property logType Distinguishes SYSTEM logs from CHAT transcript entries.
 * @property peerId Optional peer device name for per-peer filtering.
 * @property rssi Optional signal strength measurement captured at log time.
 * @property latencyMs Optional round-trip latency captured at log time.
 * @property payloadSizeBytes Optional payload size captured at log time.
 *
 * @see LogDao
 * @see LogLevel
 * @see LogType
 */
@Entity(
    tableName = "logs",
    indices = [Index(value = ["timestamp"])]
)
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
        get() = LOG_TIMESTAMP_FORMAT.format(java.util.Date(timestamp))

    companion object {
        /** Cached formatter — only used on the main thread, so thread safety is not a concern. */
        private val LOG_TIMESTAMP_FORMAT = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    }
}

/**
 * Category of log entry. Used to separate system/diagnostic logs from chat
 * transcript entries so each can be queried independently.
 */
enum class LogType {
    SYSTEM,
    CHAT
}

/**
 * Log severity levels ordered from most to least critical.
 *
 * Ordinal order matters: the UI log filter shows entries where
 * `entry.level.ordinal <= selectedLevel.ordinal`, so ERROR (0) is always
 * visible and TRACE (5) only when maximum verbosity is selected.
 */
enum class LogLevel {
    ERROR, // 0
    WARN, // 1
    METRIC, // 2
    INFO, // 3
    DEBUG, // 4
    TRACE // 5
}
