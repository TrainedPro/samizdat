package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.data.TelemetryEvent
import com.fyp.resilientp2p.data.TelemetryEventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicInteger

/**
 * WorkManager periodic worker that uploads queued telemetry events to the cloud backend.
 *
 * Runs every 15 minutes when network is available. Implements:
 * - Batch upload (max 100 events per run)
 * - Circuit breaker (exponential backoff via WorkManager)
 * - Rate limiting (min 5 min between uploads)
 * - Local file export as fallback when cloud is unavailable
 *
 * The worker uses Firestore REST API directly to avoid the google-services.json requirement.
 * Set [FIREBASE_PROJECT_ID] and [FIREBASE_API_KEY] in companion object to enable cloud upload.
 * When not configured, events are exported to local JSON files for manual collection.
 */
class TelemetryUploadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    companion object {
        private const val TAG = "TelemetryUploadWorker"
        private const val PREFS_NAME = "telemetry_upload_prefs"
        private const val KEY_LAST_UPLOAD_TIME = "last_upload_time"
        private const val KEY_CONSECUTIVE_FAILURES = "consecutive_failures"

        /**
         * Firebase project configuration.
         * Set these to enable cloud upload. Leave empty for local-only export.
         *
         * To get these values:
         * 1. Go to https://console.firebase.google.com
         * 2. Create a project (or use existing)
         * 3. Add an Android app with package name "com.fyp.resilientp2p"
         * 4. Get the project ID from Project Settings
         * 5. Get the Web API Key from Project Settings > General
         * 6. Enable Firestore in the Firebase console
         */
        var FIREBASE_PROJECT_ID = ""
        var FIREBASE_API_KEY = ""

        /** Minimum interval between uploads (5 minutes) */
        private const val MIN_UPLOAD_INTERVAL_MS = 5 * 60 * 1000L

        /** Max consecutive failures before extended backoff */
        private const val MAX_CONSECUTIVE_FAILURES = 5

        /** Whether Firebase is configured */
        val isFirebaseConfigured: Boolean
            get() = FIREBASE_PROJECT_ID.isNotBlank() && FIREBASE_API_KEY.isNotBlank()

        /** Consecutive failure counter for circuit breaker */
        private val consecutiveFailures = AtomicInteger(0)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val db = AppDatabase.getDatabase(applicationContext)
        val telemetryDao = db.telemetryDao()
        val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Rate limit check
        val now = System.currentTimeMillis()
        val lastUpload = prefs.getLong(KEY_LAST_UPLOAD_TIME, 0)
        if (now - lastUpload < MIN_UPLOAD_INTERVAL_MS) {
            Log.d(TAG, "Rate limited — skipping upload (last was ${(now - lastUpload) / 1000}s ago)")
            return@withContext Result.success()
        }

        // Circuit breaker check
        val failures = consecutiveFailures.get()
        if (failures >= MAX_CONSECUTIVE_FAILURES) {
            Log.w(TAG, "Circuit breaker open ($failures consecutive failures) — retry via WorkManager backoff")
            return@withContext Result.retry()
        }

        // Get pending events
        val pending = telemetryDao.getPendingEvents(TelemetryManager.MAX_BATCH_SIZE)
        if (pending.isEmpty()) {
            Log.d(TAG, "No pending telemetry events")
            return@withContext Result.success()
        }

        Log.i(TAG, "Uploading ${pending.size} telemetry events...")

        return@withContext try {
            if (isFirebaseConfigured) {
                // Upload to Firebase Firestore
                uploadToFirestore(pending)
            } else {
                // Fallback: export to local JSON file
                exportToLocalFile(pending)
            }

            // Mark events as uploaded
            val ids = pending.map { it.id }
            telemetryDao.markUploaded(ids)

            // Enforce hard DB cap (retention-based cleanup is handled by TelemetryManager)
            telemetryDao.enforceMaxCount(5000)

            // Reset circuit breaker
            consecutiveFailures.set(0)
            prefs.edit()
                .putLong(KEY_LAST_UPLOAD_TIME, now)
                .putInt(KEY_CONSECUTIVE_FAILURES, 0)
                .apply()

            Log.i(TAG, "Successfully uploaded ${pending.size} events")
            Result.success()
        } catch (e: Exception) {
            val newFailCount = consecutiveFailures.incrementAndGet()
            prefs.edit().putInt(KEY_CONSECUTIVE_FAILURES, newFailCount).apply()
            Log.e(TAG, "Upload failed (attempt $newFailCount): ${e.message}", e)

            // Increment attempt counter for these events
            telemetryDao.incrementAttempts(pending.map { it.id })

            if (newFailCount >= MAX_CONSECUTIVE_FAILURES) {
                Log.w(TAG, "Circuit breaker tripped after $newFailCount failures — backing off")
            }
            Result.retry()
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Firebase Firestore Upload (REST API — no google-services.json needed)
    // ─────────────────────────────────────────────────────────────────

    private fun uploadToFirestore(events: List<TelemetryEvent>) {
        // Group events by type for organized Firestore collections
        val grouped = events.groupBy { it.eventType }

        grouped.forEach { (eventType, typeEvents) ->
            val collection = getFirestoreCollection(eventType)

            typeEvents.forEach { event ->
                val url = URL(
                    "https://firestore.googleapis.com/v1/projects/$FIREBASE_PROJECT_ID/" +
                    "databases/(default)/documents/$collection?key=$FIREBASE_API_KEY"
                )

                val firestoreDoc = convertToFirestoreDocument(event)

                val connection = url.openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000

                    connection.outputStream.use { os ->
                        OutputStreamWriter(os).use { writer ->
                            writer.write(firestoreDoc)
                            writer.flush()
                        }
                    }

                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        val errorBody = connection.errorStream?.bufferedReader()?.readText() ?: "no body"
                        throw Exception("Firestore upload failed: HTTP $responseCode — $errorBody")
                    }
                } finally {
                    connection.disconnect()
                }
            }
        }
    }

    /** Map event type to Firestore collection path */
    private fun getFirestoreCollection(type: TelemetryEventType): String {
        return when (type) {
            TelemetryEventType.DEVICE_REGISTRATION -> "devices"
            TelemetryEventType.STATS_SNAPSHOT -> "stats"
            TelemetryEventType.ROUTING_SNAPSHOT -> "routing"
            TelemetryEventType.CONNECTION_EVENT -> "connections"
            TelemetryEventType.ERROR_LOG -> "error_logs"
            TelemetryEventType.TEST_RESULT -> "test_results"
            TelemetryEventType.STORE_FORWARD_REPORT -> "store_forward"
        }
    }

    /**
     * Convert a TelemetryEvent to Firestore REST API document format.
     * Firestore REST API expects typed field values.
     */
    private fun convertToFirestoreDocument(event: TelemetryEvent): String {
        // Parse the event payload as JSON
        val payloadJson = JSONObject(event.payload)
        val fields = JSONObject()

        // Add metadata fields
        fields.put("deviceId", JSONObject().put("stringValue", event.deviceId))
        fields.put("eventType", JSONObject().put("stringValue", event.eventType.name))
        fields.put("timestamp", JSONObject().put("integerValue", event.timestamp.toString()))

        // Add all payload fields as string values (Firestore can handle the conversion)
        val keys = payloadJson.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val value = payloadJson.get(key)
            when (value) {
                is String -> fields.put(key, JSONObject().put("stringValue", value))
                is Int, is Long -> fields.put(key, JSONObject().put("integerValue", value.toString()))
                is Double, is Float -> fields.put(key, JSONObject().put("doubleValue", value))
                is Boolean -> fields.put(key, JSONObject().put("booleanValue", value))
                else -> fields.put(key, JSONObject().put("stringValue", value.toString()))
            }
        }

        return JSONObject().put("fields", fields).toString()
    }

    // ─────────────────────────────────────────────────────────────────
    // Local File Export (fallback when Firebase is not configured)
    // ─────────────────────────────────────────────────────────────────

    private fun exportToLocalFile(events: List<TelemetryEvent>) {
        val baseDir = applicationContext.getExternalFilesDir(null) ?: applicationContext.filesDir
        val dir = File(baseDir, "telemetry")
        if (!dir.exists()) dir.mkdirs()

        val timestamp = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss", java.util.Locale.getDefault()
        ).format(java.util.Date())

        val file = File(dir, "telemetry_$timestamp.json")

        BufferedWriter(FileWriter(file)).use { writer ->
            writer.write("[\n")
            events.forEachIndexed { index, event ->
                val json = JSONObject().apply {
                    put("id", event.id)
                    put("timestamp", event.timestamp)
                    put("deviceId", event.deviceId)
                    put("eventType", event.eventType.name)
                    put("payload", JSONObject(event.payload))
                    put("uploadAttempts", event.uploadAttempts)
                }
                writer.write("  ${json}")
                if (index < events.size - 1) writer.write(",")
                writer.write("\n")
            }
            writer.write("]\n")
        }

        Log.i(TAG, "Exported ${events.size} events to ${file.absolutePath}")
    }
}
