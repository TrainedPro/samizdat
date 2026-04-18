package com.fyp.resilientp2p.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.fyp.resilientp2p.data.LogLevel
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.fyp.resilientp2p.data.LogDao
import com.fyp.resilientp2p.data.TelemetryDao
import com.fyp.resilientp2p.data.TelemetryEvent
import com.fyp.resilientp2p.data.TelemetryEventType
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import androidx.core.content.edit

/**
 * Cloud telemetry manager for the Samizdat mesh network.
 *
 * Collects periodic stats snapshots, routing table state, connection events, and error logs.
 * Queues them in Room DB as [TelemetryEvent] rows. [TelemetryUploadWorker] handles the
 * actual upload to Firebase/Firestore on a periodic WorkManager schedule.
 *
 * **What gets uploaded:** aggregated stats, connection events, error/warn logs, test results.
 * **What does NOT get uploaded:** chat content, audio, file payloads, raw packets (privacy).
 *
 * **Anti-flooding safeguards:**
 * - Rate limit: Max 1 snapshot per 5 minutes even if polled faster
 * - Batch size cap: Max 100 events per upload
 * - DB hard cap: 5,000 events max, FIFO eviction
 * - Circuit breaker: exponential backoff on consecutive upload failures
 * - WiFi-only option (configurable)
 */
class TelemetryManager(
    private val context: Context,
    private val telemetryDao: TelemetryDao,
    private val logDao: LogDao,
    private val p2pManager: P2PManager
) {
    companion object {
        private const val KEY_DEVICE_ID_FIELD = "deviceId"
        private const val KEY_TIMESTAMP_FIELD = "timestamp"
        private const val PREFS_NAME = "telemetry_prefs"
        private const val KEY_DEVICE_REGISTERED = "device_registered"
        private const val KEY_LAST_SNAPSHOT_TIME = "last_snapshot_time"
        private const val KEY_LAST_LOG_UPLOAD_TIME = "last_log_upload_time"
        private const val KEY_TELEMETRY_ENABLED = "telemetry_enabled"
        private const val KEY_WIFI_ONLY = "wifi_only_upload"

        /** Minimum interval between stats snapshots (5 minutes) */
        const val SNAPSHOT_INTERVAL_MS = 5 * 60 * 1000L

        /** WorkManager periodic upload interval (15 minutes) */
        const val UPLOAD_INTERVAL_MINUTES = 15L

        /** Max events per upload batch */
        const val MAX_BATCH_SIZE = 100

        /** Max total events in DB before FIFO eviction */
        const val MAX_DB_EVENTS = 5000

        /** Retention period for uploaded events before cleanup (24 hours) */
        const val UPLOADED_RETENTION_MS = 24 * 60 * 60 * 1000L

        /** Max upload attempts before discarding an event */
        const val MAX_UPLOAD_ATTEMPTS = 10

        /** Stats snapshot retention in cloud (30 days) */
        const val STATS_RETENTION_DAYS = 30

        /** Routing snapshot retention in cloud (7 days) */
        const val ROUTING_RETENTION_DAYS = 7

        private const val UPLOAD_WORK_NAME = "telemetry_upload"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRunning = AtomicBoolean(false)
    private var snapshotJob: Job? = null

    // Rate limiting
    private val lastSnapshotTime = AtomicLong(prefs.getLong(KEY_LAST_SNAPSHOT_TIME, 0))

    /** The unique device ID used for telemetry (same as P2P identity) */
    val deviceId: String
        get() = p2pManager.state.value.localDeviceName

    /** Whether telemetry collection is enabled */
    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEMETRY_ENABLED, true)
        @Synchronized set(value) {
            prefs.edit { putBoolean(KEY_TELEMETRY_ENABLED, value) }
            if (value) start() else stop()
        }

    /** Whether uploads should only happen on WiFi */
    var wifiOnlyUpload: Boolean
        get() = prefs.getBoolean(KEY_WIFI_ONLY, false)
        set(value) {
            prefs.edit { putBoolean(KEY_WIFI_ONLY, value) }
            // Re-schedule upload worker with updated constraints
            schedulePeriodicUpload()
        }

    // ─────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────

    /**
     * Start telemetry collection. Called from [P2PApplication.onCreate].
     * Registers device if first launch, starts periodic snapshot collection,
     * and schedules the WorkManager upload worker.
     */
    fun start() {
        if (!isEnabled) {
            p2pManager.log("Telemetry disabled, skipping start")
            return
        }
        if (isRunning.getAndSet(true)) return

        p2pManager.log("Starting telemetry for device=$deviceId")

        scope.launch {
            // Register device on first launch
            if (!prefs.getBoolean(KEY_DEVICE_REGISTERED, false)) {
                registerDevice()
            }

            // Enforce DB size limits
            telemetryDao.enforceMaxCount(MAX_DB_EVENTS)
            telemetryDao.deleteFailed(MAX_UPLOAD_ATTEMPTS)
        }

        // Start periodic stats collection
        startSnapshotCollection()

        // Schedule periodic upload via WorkManager
        schedulePeriodicUpload()
    }

    /** Stop telemetry collection. Does NOT cancel pending uploads. */
    fun stop() {
        isRunning.set(false)
        snapshotJob?.cancel()
        snapshotJob = null
        p2pManager.log("Telemetry collection stopped")
    }

    /** Full shutdown — cancels everything including scope */
    fun destroy() {
        stop()
        scope.cancel()
        WorkManager.getInstance(context).cancelUniqueWork(UPLOAD_WORK_NAME)
        p2pManager.log("TelemetryManager destroyed")
    }

    // ─────────────────────────────────────────────────────────────────
    // Device Registration (one-time)
    // ─────────────────────────────────────────────────────────────────

    private suspend fun registerDevice() {
        val payload = JSONObject().apply {
            put(KEY_DEVICE_ID_FIELD, deviceId)
            put("model", Build.MODEL)
            put("manufacturer", Build.MANUFACTURER)
            put("androidVersion", Build.VERSION.RELEASE)
            put("sdkVersion", Build.VERSION.SDK_INT)
            put("appVersion", getAppVersion())
            put("registeredAt", System.currentTimeMillis())
        }

        telemetryDao.insert(
            TelemetryEvent(
                deviceId = deviceId,
                eventType = TelemetryEventType.DEVICE_REGISTRATION,
                payload = payload.toString()
            )
        )
        prefs.edit { putBoolean(KEY_DEVICE_REGISTERED, true) }
        p2pManager.log("Device registered: $deviceId")
    }

    // ─────────────────────────────────────────────────────────────────
    // Periodic Stats Snapshot
    // ─────────────────────────────────────────────────────────────────

    private fun startSnapshotCollection() {
        snapshotJob?.cancel()
        snapshotJob = scope.launch {
            while (isActive) {
                delay(SNAPSHOT_INTERVAL_MS)
                if (isEnabled) {
                    collectStatsSnapshot()
                    collectRoutingSnapshot()
                    collectErrorLogs()
                    cleanupOldEvents()
                }
            }
        }
    }

    /** Collect a stats snapshot and queue it for upload */
    private suspend fun collectStatsSnapshot() {
        val now = System.currentTimeMillis()
        val last = lastSnapshotTime.get()

        // Rate limit: skip if less than SNAPSHOT_INTERVAL_MS since last
        if (now - last < SNAPSHOT_INTERVAL_MS - 5000) return

        lastSnapshotTime.set(now)
        prefs.edit { putLong(KEY_LAST_SNAPSHOT_TIME, now) }

        val state = p2pManager.state.value
        // Use live mutable NetworkStats directly for freshest counters,
        // rather than the potentially stale snapshot inside P2PState.
        val live = p2pManager.networkStats
        val avgRtt = if (live.peerRtt.isNotEmpty()) live.peerRtt.values.average().toLong() else 0L

        val payload = JSONObject().apply {
            put(KEY_DEVICE_ID_FIELD, deviceId)
            put(KEY_TIMESTAMP_FIELD, now)
            put("uptimeMs", now - live.startTimeMs)
            put("batteryLevel", live.batteryLevel)
            put("batteryTemperature", live.batteryTemperature.toDouble())
            put("batteryChargeUah", live.batteryChargeUah)
            put("batteryVoltageMilliV", live.batteryVoltageMilliV)
            put("batteryDesignCapacityMah", live.batteryDesignCapacityMah)
            // Estimated remaining mAh
            val estMah = when {
                live.batteryChargeUah > 0 -> live.batteryChargeUah / 1000.0
                live.batteryLevel in 0..100 && live.batteryDesignCapacityMah > 0 ->
                    live.batteryDesignCapacityMah * (live.batteryLevel / 100.0)
                else -> -1.0
            }
            put("batteryEstimatedMah", estMah)
            put("totalBytesSent", live.totalBytesSent.get())
            put("totalBytesReceived", live.totalBytesReceived.get())
            put("totalPacketsSent", live.totalPacketsSent.get())
            put("totalPacketsReceived", live.totalPacketsReceived.get())
            put("totalPacketsForwarded", live.totalPacketsForwarded.get())
            put("totalPacketsDropped", live.totalPacketsDropped.get())
            put("totalConnectionsEstablished", live.totalConnectionsEstablished.get())
            put("totalConnectionsLost", live.totalConnectionsLost.get())
            put("currentNeighborCount", state.connectedEndpoints.size)
            put("currentRouteCount", state.knownPeers.size)
            put("avgRttMs", avgRtt)
            put("storeForwardQueued", live.storeForwardQueued.get())
            put("storeForwardDelivered", live.storeForwardDelivered.get())
            put("isAdvertising", state.isAdvertising)
            put("isDiscovering", state.isDiscovering)
            put("connectedEndpointCount", state.connectedEndpoints.size)

            // Per-peer stats (read live concurrent maps)
            val peersArray = JSONArray()
            live.peerConnectedSince.forEach { (name, _) ->
                peersArray.put(JSONObject().apply {
                    put("peerName", name)
                    put("lastRttMs", live.peerRtt[name] ?: -1L)
                    put("bytesSent", live.peerBytesSent[name]?.get() ?: 0L)
                    put("bytesReceived", live.peerBytesReceived[name]?.get() ?: 0L)
                    put("packetsSent", live.peerPacketsSent[name]?.get() ?: 0L)
                    put("packetsReceived", live.peerPacketsReceived[name]?.get() ?: 0L)
                    put("disconnectCount", live.peerDisconnectCount[name]?.get() ?: 0L)
                })
            }
            put("peerStats", peersArray)
        }

        telemetryDao.insert(
            TelemetryEvent(
                deviceId = deviceId,
                eventType = TelemetryEventType.STATS_SNAPSHOT,
                payload = payload.toString()
            )
        )
        p2pManager.log("Stats snapshot collected (neighbors=${state.connectedEndpoints.size}, routes=${state.knownPeers.size})", LogLevel.DEBUG)
    }

    /** Collect routing table snapshot */
    private suspend fun collectRoutingSnapshot() {
        val state = p2pManager.state.value
        if (state.knownPeers.isEmpty()) return

        val payload = JSONObject().apply {
            put(KEY_DEVICE_ID_FIELD, deviceId)
            put(KEY_TIMESTAMP_FIELD, System.currentTimeMillis())
            val routesArray = JSONArray()
            state.knownPeers.forEach { (dest, info) ->
                routesArray.put(JSONObject().apply {
                    put("destination", dest)
                    put("nextHop", info.nextHop)
                    put("hopCount", info.hopCount)
                })
            }
            put("routes", routesArray)
            put("routeCount", state.knownPeers.size)
        }

        telemetryDao.insert(
            TelemetryEvent(
                deviceId = deviceId,
                eventType = TelemetryEventType.ROUTING_SNAPSHOT,
                payload = payload.toString()
            )
        )
    }

    /** Collect ERROR and WARN logs since last upload */
    private suspend fun collectErrorLogs() {
        val lastUploadTime = prefs.getLong(KEY_LAST_LOG_UPLOAD_TIME, 0)
        val logs = logDao.getErrorLogsSince(lastUploadTime, MAX_BATCH_SIZE)

        if (logs.isEmpty()) return

        val payload = JSONObject().apply {
            put(KEY_DEVICE_ID_FIELD, deviceId)
            put(KEY_TIMESTAMP_FIELD, System.currentTimeMillis())
            val logsArray = JSONArray()
            logs.forEach { entry ->
                logsArray.put(JSONObject().apply {
                    put(KEY_TIMESTAMP_FIELD, entry.timestamp)
                    put("level", entry.level.name)
                    put("type", entry.logType.name)
                    put("message", entry.message.take(256)) // Truncate for bandwidth
                    entry.peerId?.let { put("peerId", it) }
                    entry.latencyMs?.let { put("latencyMs", it) }
                })
            }
            put("logs", logsArray)
            put("logCount", logs.size)
        }

        telemetryDao.insert(
            TelemetryEvent(
                deviceId = deviceId,
                eventType = TelemetryEventType.ERROR_LOG,
                payload = payload.toString()
            )
        )

        prefs.edit { putLong(KEY_LAST_LOG_UPLOAD_TIME, System.currentTimeMillis()) }
        p2pManager.log("Collected ${logs.size} error/warn logs for upload", LogLevel.DEBUG)
    }

    // ─────────────────────────────────────────────────────────────────
    // Event Recording (called by P2PManager / other components)
    // ─────────────────────────────────────────────────────────────────

    /** Record a connection event (peer connected or disconnected) */
    fun recordConnectionEvent(peerId: String, connected: Boolean, endpointId: String? = null) {
        if (!isEnabled) return
        scope.launch {
            val payload = JSONObject().apply {
                put(KEY_DEVICE_ID_FIELD, deviceId)
                put(KEY_TIMESTAMP_FIELD, System.currentTimeMillis())
                put("peerId", peerId)
                put("event", if (connected) "CONNECTED" else "DISCONNECTED")
                endpointId?.let { put("endpointId", it) }
            }
            telemetryDao.insert(
                TelemetryEvent(
                    deviceId = deviceId,
                    eventType = TelemetryEventType.CONNECTION_EVENT,
                    payload = payload.toString()
                )
            )
        }
    }

    /** Record a store-and-forward delivery report */
    fun recordStoreForwardDelivery(messageId: String, destId: String, queuedAt: Long, deliveredAt: Long) {
        if (!isEnabled) return
        scope.launch {
            val payload = JSONObject().apply {
                put(KEY_DEVICE_ID_FIELD, deviceId)
                put(KEY_TIMESTAMP_FIELD, deliveredAt)
                put("messageId", messageId)
                put("destId", destId)
                put("queuedAt", queuedAt)
                put("deliveredAt", deliveredAt)
                put("deliveryTimeMs", deliveredAt - queuedAt)
            }
            telemetryDao.insert(
                TelemetryEvent(
                    deviceId = deviceId,
                    eventType = TelemetryEventType.STORE_FORWARD_REPORT,
                    payload = payload.toString()
                )
            )
        }
    }

    /** Record test mode results */
    fun recordTestResults(resultsJson: String) {
        if (!isEnabled) return
        scope.launch {
            telemetryDao.insert(
                TelemetryEvent(
                    deviceId = deviceId,
                    eventType = TelemetryEventType.TEST_RESULT,
                    payload = resultsJson
                )
            )
            p2pManager.log("Test results queued for upload")
        }
    }

    /** Record the final endurance test report for cloud upload. */
    fun recordEnduranceReport(reportJson: String) {
        if (!isEnabled) return
        scope.launch {
            telemetryDao.insert(
                TelemetryEvent(
                    deviceId = deviceId,
                    eventType = TelemetryEventType.ENDURANCE_REPORT,
                    payload = reportJson
                )
            )
            p2pManager.log("Endurance report queued for upload")
        }
    }

    /** Record a periodic endurance snapshot for cloud upload. */
    fun recordEnduranceSnapshot(snapshotJson: String) {
        if (!isEnabled) return
        scope.launch {
            telemetryDao.insert(
                TelemetryEvent(
                    deviceId = deviceId,
                    eventType = TelemetryEventType.ENDURANCE_SNAPSHOT,
                    payload = snapshotJson
                )
            )
            p2pManager.log("Endurance snapshot queued for upload", LogLevel.DEBUG)
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // WorkManager Scheduling
    // ─────────────────────────────────────────────────────────────────

    private fun schedulePeriodicUpload() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(
                if (wifiOnlyUpload) NetworkType.UNMETERED else NetworkType.CONNECTED
            )
            .build()

        val uploadRequest = PeriodicWorkRequestBuilder<TelemetryUploadWorker>(
            UPLOAD_INTERVAL_MINUTES, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.MINUTES) // Small initial delay
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UPLOAD_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            uploadRequest
        )
        p2pManager.log("Scheduled periodic upload every ${UPLOAD_INTERVAL_MINUTES}min (wifiOnly=$wifiOnlyUpload)")
    }

    // ─────────────────────────────────────────────────────────────────
    // Cleanup
    // ─────────────────────────────────────────────────────────────────

    private suspend fun cleanupOldEvents() {
        // Delete uploaded events older than retention
        val cutoff = System.currentTimeMillis() - UPLOADED_RETENTION_MS
        telemetryDao.deleteOlderThan(cutoff)

        // Delete events that failed too many times
        telemetryDao.deleteFailed(MAX_UPLOAD_ATTEMPTS)

        // Enforce hard DB cap
        telemetryDao.enforceMaxCount(MAX_DB_EVENTS)

        val remaining = telemetryDao.getTotalCount()
        val pending = telemetryDao.getPendingCount()
        p2pManager.log("Cleanup complete: $remaining total events, $pending pending upload", LogLevel.DEBUG)
    }

    // ─────────────────────────────────────────────────────────────────
    // Manual Trigger (for debugging / test mode)
    // ─────────────────────────────────────────────────────────────────

    /** Force an immediate snapshot + upload attempt (for testing) */
    fun forceSnapshot() {
        scope.launch {
            collectStatsSnapshot()
            collectRoutingSnapshot()
            collectErrorLogs()
        }
    }

    /** Get telemetry status for UI display */
    suspend fun getStatus(): TelemetryStatus {
        return TelemetryStatus(
            enabled = isEnabled,
            wifiOnly = wifiOnlyUpload,
            pendingEvents = telemetryDao.getPendingCount(),
            totalEvents = telemetryDao.getTotalCount(),
            lastSnapshotTime = lastSnapshotTime.get(),
            deviceRegistered = prefs.getBoolean(KEY_DEVICE_REGISTERED, false)
        )
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

    private fun getAppVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) {
            "1.0"
        }
    }

    /** Check if device currently has internet connectivity */
    fun hasInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}

data class TelemetryStatus(
    val enabled: Boolean,
    val wifiOnly: Boolean,
    val pendingEvents: Int,
    val totalEvents: Int,
    val lastSnapshotTime: Long,
    val deviceRegistered: Boolean
)
