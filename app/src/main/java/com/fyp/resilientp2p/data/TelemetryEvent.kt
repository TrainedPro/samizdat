package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a telemetry event queued for cloud upload.
 * Events are batched and uploaded by TelemetryUploadWorker, then deleted after successful upload.
 */
@Entity(tableName = "telemetry_events")
data class TelemetryEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceId: String,
    val eventType: TelemetryEventType,
    /** JSON-serialized payload — schema depends on eventType */
    val payload: String,
    /** Whether this event has been successfully uploaded */
    val uploaded: Boolean = false,
    /** Number of upload attempts so far */
    val uploadAttempts: Int = 0
)

enum class TelemetryEventType {
    DEVICE_REGISTRATION,
    STATS_SNAPSHOT,
    ROUTING_SNAPSHOT,
    CONNECTION_EVENT,
    ERROR_LOG,
    TEST_RESULT,
    STORE_FORWARD_REPORT
}
