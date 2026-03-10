package com.fyp.resilientp2p.managers

import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.fyp.resilientp2p.BuildConfig
import com.fyp.resilientp2p.data.LogDao
import com.fyp.resilientp2p.data.LogEntry
import com.fyp.resilientp2p.data.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Uploads system logs to Firestore in batches for remote monitoring.
 *
 * Devices with internet upload directly. Devices without internet relay
 * compressed log batches through mesh gateway peers, which upload on their behalf.
 *
 * Firestore collection: `device_logs`
 * Each document = one batch with device metadata + JSON array of log entries.
 * Documents auto-expire after [LOG_TTL_DAYS] days (cleaned up on each poll cycle).
 */
class CloudLogManager(
    private val context: Context,
    private val logDao: LogDao,
    private val p2pManager: P2PManager
) {
    companion object {
        private const val PREFS_NAME = "cloud_log_prefs"
        private const val KEY_LAST_UPLOADED_ID = "last_uploaded_log_id"
        private const val KEY_ENABLED = "cloud_log_enabled"
        private const val KEY_LAST_CLEANUP_TIME = "last_cleanup_time"
        private const val KEY_LAST_RELAY_TIME = "last_relay_time"

        private const val COLLECTION = "device_logs"
        /** Max logs per Firestore document batch */
        private const val BATCH_SIZE = 50
        /** Upload interval when internet is available */
        private const val UPLOAD_INTERVAL_MS = 120_000L // 2 minutes
        /** How long logs stay in Firestore before auto-cleanup */
        private const val LOG_TTL_DAYS = 3
        private const val LOG_TTL_MS = LOG_TTL_DAYS * 24 * 60 * 60 * 1000L
        /** Cleanup runs at most once per hour */
        private const val CLEANUP_INTERVAL_MS = 60 * 60 * 1000L
        /** Max logs per relay packet (keeps under 10KB) */
        private const val RELAY_BATCH_SIZE = 25
        /** Min interval between relay attempts (5 minutes) */
        private const val RELAY_INTERVAL_MS = 5 * 60 * 1000L
        /** Prefix identifying a log relay packet */
        const val LOG_RELAY_PREFIX = "[LOG_RELAY:]"
        private const val FIRESTORE_API = "https://firestore.googleapis.com/v1"
        private const val FIRESTORE_BASE = "$FIRESTORE_API/projects/"
        private const val SV = "stringValue"
        private const val IV = "integerValue"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var uploadJob: Job? = null

    private val projectId: String get() = BuildConfig.FIREBASE_PROJECT_ID
    private val apiKey: String get() = BuildConfig.FIREBASE_API_KEY
    private val isFirebaseConfigured: Boolean
        get() = projectId.isNotBlank() && apiKey.isNotBlank()

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, true)
        set(value) {
            prefs.edit { putBoolean(KEY_ENABLED, value) }
            if (value) startUploadLoop() else stopUploadLoop()
        }

    private val deviceId: String get() = p2pManager.state.value.localDeviceName
    private val appVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (_: Exception) { "1.0" }

    /** Number of logs uploaded since manager started (for UI status) */
    @Volatile var totalUploaded: Long = 0L
        private set

    /** Timestamp of last successful upload */
    @Volatile var lastUploadTime: Long = 0L
        private set

    /** Last upload error message (null = no error) for UI display */
    @Volatile var uploadError: String? = null
        private set

    fun start() {
        p2pManager.log("CloudLogManager starting. enabled=$isEnabled " +
            "firebaseConfigured=$isFirebaseConfigured projectId='$projectId'")
        if (isEnabled) startUploadLoop()
    }

    fun destroy() {
        stopUploadLoop()
        scope.cancel()
        p2pManager.log("CloudLogManager destroyed")
    }

    private fun startUploadLoop() {
        if (uploadJob?.isActive == true) return
        uploadJob = scope.launch {
            while (isActive) {
                delay(UPLOAD_INTERVAL_MS)
                if (!isEnabled) continue
                try {
                    uploadPendingLogs()
                } catch (e: Exception) {
                    p2pManager.log("Upload cycle error: ${e.message}", LogLevel.ERROR)
                }
            }
        }
    }

    private fun stopUploadLoop() {
        uploadJob?.cancel()
        uploadJob = null
    }

    /** Force an immediate upload attempt (for UI "Sync Now" button) */
    fun forceUpload() {
        scope.launch {
            p2pManager.log("FORCE_UPLOAD_START firebase=$isFirebaseConfigured " +
                "internet=${p2pManager.internetGatewayManager?.hasInternet?.value == true} " +
                "enabled=$isEnabled")
            try {
                uploadError = null
                uploadPendingLogs()
            } catch (e: Exception) {
                val errorMsg = "Upload failed: ${e.message}"
                p2pManager.log("FORCE_UPLOAD_FAILED: ${e.message}", LogLevel.ERROR)
                uploadError = errorMsg
            }
        }
    }

    private suspend fun uploadPendingLogs() {
        val lastId = prefs.getLong(KEY_LAST_UPLOADED_ID, 0)
        val logs = logDao.getLogsSince(lastId, BATCH_SIZE)
        if (logs.isEmpty()) {
            p2pManager.log("UPLOAD_SKIP reason=NO_PENDING_LOGS lastId=$lastId", LogLevel.DEBUG)
            return
        }

        val gateway = p2pManager.internetGatewayManager
        val hasInternet = gateway?.hasInternet?.value == true

        p2pManager.log("UPLOAD_PENDING count=${logs.size} internet=$hasInternet " +
            "firebase=$isFirebaseConfigured", LogLevel.DEBUG)

        if (hasInternet && isFirebaseConfigured) {
            uploadBatchToFirestore(logs, deviceId)
            val maxId = logs.maxOf { it.id }
            prefs.edit { putLong(KEY_LAST_UPLOADED_ID, maxId) }
            totalUploaded += logs.size
            lastUploadTime = System.currentTimeMillis()
            uploadError = null
            p2pManager.log("LOGS_UPLOADED count=${logs.size} lastId=$maxId total=$totalUploaded")
            runCleanupIfDue()
        } else if (!isFirebaseConfigured) {
            val reason = "Firebase not configured (projectId='$projectId')"
            p2pManager.log("CLOUD_UPLOAD_SKIP reason=NOT_CONFIGURED projectId='$projectId'", LogLevel.WARN)
            uploadError = reason
        } else {
            p2pManager.log("UPLOAD_SKIP reason=NO_INTERNET, attempting mesh relay", LogLevel.DEBUG)
            relayLogsViaMesh(logs)
        }
    }

    private fun uploadBatchToFirestore(logs: List<LogEntry>, uploadDevice: String) {
        val logsArray = JSONArray()
        for (entry in logs) {
            logsArray.put(JSONObject().apply {
                put("id", entry.id)
                put("ts", entry.timestamp)
                put("msg", entry.message)
                put("lvl", entry.level.name)
                put("type", entry.logType.name)
                if (entry.peerId != null) put("peer", entry.peerId)
                if (entry.rssi != null) put("rssi", entry.rssi)
                if (entry.latencyMs != null) put("lat", entry.latencyMs)
                if (entry.payloadSizeBytes != null) put("sz", entry.payloadSizeBytes)
            })
        }

        val doc = JSONObject().apply {
            put("fields", JSONObject().apply {
                put("deviceId", JSONObject().put(SV, uploadDevice))
                put("deviceModel", JSONObject().put(SV, Build.MODEL))
                put("appVersion", JSONObject().put(SV, appVersion))
                put("uploadedBy", JSONObject().put(SV, deviceId))
                put("batchTimestamp", JSONObject().put(IV,
                    System.currentTimeMillis().toString()))
                put("expiresAt", JSONObject().put(IV,
                    (System.currentTimeMillis() + LOG_TTL_MS).toString()))
                put("logCount", JSONObject().put(IV, logs.size.toString()))
                put("logs", JSONObject().put(SV, logsArray.toString()))
            })
        }

        val url = "${FIRESTORE_BASE}$projectId" +
            "/databases/(default)/documents/$COLLECTION?key=$apiKey"
        val (code, errorBody) = httpPost(url, doc.toString())
        if (code !in 200..299) {
            val hint = when (code) {
                403 -> "Check Firestore security rules allow writes to '$COLLECTION'"
                404 -> "Firestore database may not exist in project '$projectId'"
                401 -> "API key may be invalid or Firestore API not enabled"
                else -> ""
            }
            val errorMsg = "Firestore HTTP $code: ${errorBody?.take(200) ?: "no body"}" +
                if (hint.isNotEmpty()) " — $hint" else ""
            p2pManager.log("CLOUD_UPLOAD_FAILED HTTP $code projectId='$projectId'" +
                if (hint.isNotEmpty()) " — $hint" else "", LogLevel.ERROR)
            throw java.io.IOException(errorMsg)
        }
    }

    /**
     * Relay log batch through mesh to a gateway peer for cloud upload.
     * The gateway recognizes [LOG_RELAY_PREFIX] and uploads on behalf.
     */
    private fun relayLogsViaMesh(logs: List<LogEntry>) {
        val now = System.currentTimeMillis()
        val lastRelay = prefs.getLong(KEY_LAST_RELAY_TIME, 0)
        if (now - lastRelay < RELAY_INTERVAL_MS) return

        // Find a gateway peer in connected endpoints
        val state = p2pManager.state.value
        if (state.connectedEndpoints.isEmpty()) return

        val batch = logs.take(RELAY_BATCH_SIZE)
        val logsArray = JSONArray()
        for (entry in batch) {
            logsArray.put(JSONObject().apply {
                put("id", entry.id)
                put("ts", entry.timestamp)
                put("msg", entry.message)
                put("lvl", entry.level.name)
                put("type", entry.logType.name)
                if (entry.peerId != null) put("peer", entry.peerId)
            })
        }

        val relayPayload = JSONObject().apply {
            put("deviceId", deviceId)
            put("deviceModel", Build.MODEL)
            put("appVersion", appVersion)
            put("logs", logsArray)
        }

        val message = "$LOG_RELAY_PREFIX${relayPayload}"
        // Broadcast to all connected peers — gateways will pick it up
        p2pManager.broadcastMessage(message)
        prefs.edit { putLong(KEY_LAST_RELAY_TIME, now) }

        val maxId = batch.maxOf { it.id }
        prefs.edit { putLong(KEY_LAST_UPLOADED_ID, maxId) }
        totalUploaded += batch.size
        p2pManager.log("LOGS_RELAYED_MESH count=${batch.size}")
    }

    /**
     * Called by P2PManager when a [LOG_RELAY_PREFIX] message is received.
     * If this device has internet, uploads the relayed logs to Firestore.
     */
    fun handleRelayedLogs(payload: String) {
        val gateway = p2pManager.internetGatewayManager
        if (gateway?.hasInternet?.value != true || !isFirebaseConfigured) return

        scope.launch {
            try {
                val json = JSONObject(payload)
                val sourceDevice = json.getString("deviceId")
                val model = json.optString("deviceModel", "unknown")
                val version = json.optString("appVersion", "1.0")
                val logsArray = json.getJSONArray("logs")

                val doc = JSONObject().apply {
                    put("fields", JSONObject().apply {
                        put("deviceId", JSONObject().put(SV, sourceDevice))
                        put("deviceModel", JSONObject().put(SV, model))
                        put("appVersion", JSONObject().put(SV, version))
                        put("uploadedBy", JSONObject().put(SV, deviceId))
                        put("batchTimestamp", JSONObject().put(IV,
                            System.currentTimeMillis().toString()))
                        put("expiresAt", JSONObject().put(IV,
                            (System.currentTimeMillis() + LOG_TTL_MS).toString()))
                        put("logCount", JSONObject().put(IV,
                            logsArray.length().toString()))
                        put("logs", JSONObject().put(SV, logsArray.toString()))
                    })
                }

                val url = "${FIRESTORE_BASE}$projectId" +
                    "/databases/(default)/documents/$COLLECTION?key=$apiKey"
                val (code, _) = httpPost(url, doc.toString())
                if (code in 200..299) {
                    p2pManager.log("RELAY_LOGS_UPLOADED for=$sourceDevice count=${logsArray.length()}")
                } else {
                    p2pManager.log("RELAY_LOGS_UPLOAD_FAILED for=$sourceDevice HTTP=$code", LogLevel.WARN)
                }
            } catch (e: Exception) {
                p2pManager.log("Failed to handle relayed logs: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    /**
     * Delete expired log documents from Firestore (older than [LOG_TTL_DAYS] days).
     * Runs at most once per hour on devices with internet.
     */
    private fun runCleanupIfDue() {
        val now = System.currentTimeMillis()
        val lastCleanup = prefs.getLong(KEY_LAST_CLEANUP_TIME, 0)
        if (now - lastCleanup < CLEANUP_INTERVAL_MS) return
        prefs.edit { putLong(KEY_LAST_CLEANUP_TIME, now) }

        scope.launch {
            try {
                cleanupExpiredLogs()
            } catch (e: Exception) {
                p2pManager.log("Cleanup error: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    @Suppress("NestedBlockDepth")
    private fun cleanupExpiredLogs() {
        val now = System.currentTimeMillis()
        // Fetch recent documents and check expiresAt
        val url = "${FIRESTORE_BASE}$projectId" +
            "/databases/(default)/documents/$COLLECTION?key=$apiKey&pageSize=100"
        val (code, body) = httpGet(url)
        if (code !in 200..299 || body == null) return

        val json = JSONObject(body)
        val documents = json.optJSONArray("documents") ?: return
        var deleted = 0

        for (i in 0 until documents.length()) {
            val doc = documents.getJSONObject(i)
            val fields = doc.optJSONObject("fields") ?: continue
            val expiresAtStr = fields.optJSONObject("expiresAt")
                ?.optString(IV, "0")
            val expiresAt = expiresAtStr?.toLongOrNull() ?: continue
            if (expiresAt < now) {
                val docPath = doc.getString("name")
                httpDelete("$FIRESTORE_API/$docPath?key=$apiKey")
                deleted++
            }
        }
        if (deleted > 0) {
            p2pManager.log("CLEANUP deleted=$deleted expired log batches")
        }
    }

    // --- HTTP helpers (same pattern as InternetGatewayManager) ---

    private fun httpPost(urlStr: String, body: String): Pair<Int, String?> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return try {
            OutputStreamWriter(conn.outputStream, StandardCharsets.UTF_8).use { it.write(body) }
            val code = conn.responseCode
            val errorBody = if (code !in 200..299) {
                try {
                    conn.errorStream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }
                } catch (_: Exception) { null }
            } else { null }
            Pair(code, errorBody)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(urlStr: String): Pair<Int, String?> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return try {
            val code = conn.responseCode
            val body = if (code in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream, StandardCharsets.UTF_8))
                    .use { it.readText() }
            } else {
                null
            }
            Pair(code, body)
        } finally {
            conn.disconnect()
        }
    }

    private fun httpDelete(urlStr: String): Int {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "DELETE"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return try {
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }
}
