package com.fyp.resilientp2p.managers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.core.content.ContextCompat
import com.fyp.resilientp2p.data.EmergencyMessage
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

/**
 * Manages emergency broadcast functionality including SOS beacon mode.
 *
 * Emergency messages:
 * - Use EMERGENCY packet type with highest priority
 * - Bypass rate limits and store-forward TTL limits
 * - Flood to entire mesh regardless of destination
 * - Include GPS coordinates when available
 * - Persist permanently (never auto-deleted)
 *
 * SOS Beacon Mode:
 * - Periodically broadcasts GPS + battery + distress message every 30s
 * - All peers display SOS alerts prominently
 */
class EmergencyManager(
    private val context: Context,
    private val p2pManager: P2PManager
) {
    companion object {
        private const val TAG = "EmergencyManager"
        const val EMERGENCY_TTL = 15 // Higher TTL than normal (5) for maximum reach
        const val SOS_BEACON_INTERVAL_MS = 30_000L
        const val EMERGENCY_DEST = "EMERGENCY_BROADCAST"
    }

    // SOS beacon state
    private val _sosActive = MutableStateFlow(false)
    val sosActive: StateFlow<Boolean> = _sosActive.asStateFlow()

    // Emergency message events (for UI to display red banners)
    private val _emergencyMessages = MutableSharedFlow<EmergencyMessage>(replay = 10)
    val emergencyMessages: SharedFlow<EmergencyMessage> = _emergencyMessages.asSharedFlow()

    // All emergency messages received (persisted in-memory for UI display)
    private val _emergencyHistory = MutableStateFlow<List<EmergencyMessage>>(emptyList())
    val emergencyHistory: StateFlow<List<EmergencyMessage>> = _emergencyHistory.asStateFlow()

    // Current GPS location (best available)
    private var lastKnownLocation: Location? = null
    private var locationListener: LocationListener? = null

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var beaconJob: Job? = null

    /**
     * Start GPS monitoring for emergency location tagging.
     */
    fun start() {
        startLocationUpdates()
        Log.i(TAG, "EmergencyManager started")
    }

    fun destroy() {
        stopSOSBeacon()
        stopLocationUpdates()
        scope.cancel()
        Log.i(TAG, "EmergencyManager destroyed")
    }

    /**
     * Send a one-shot emergency broadcast to the entire mesh.
     * @param message The distress message text
     */
    fun sendEmergencyBroadcast(message: String) {
        val emergencyPayload = buildEmergencyPayload(message)
        val packet = Packet(
            type = PacketType.EMERGENCY,
            sourceId = p2pManager.getLocalDeviceName(),
            destId = EMERGENCY_DEST,
            payload = emergencyPayload.toByteArray(Charsets.UTF_8),
            ttl = EMERGENCY_TTL,
            timestamp = System.currentTimeMillis()
        )
        // Inject into local mesh — handlePacket will flood it
        p2pManager.injectPacket(packet)
        Log.i(TAG, "EMERGENCY_SENT message='${message.take(50)}'")

        // Also add to our own history
        val selfMsg = parseEmergencyPayload(emergencyPayload, p2pManager.getLocalDeviceName(), System.currentTimeMillis())
        addToHistory(selfMsg)
    }

    /**
     * Toggle SOS Beacon mode on/off.
     * When active, broadcasts GPS + battery + default SOS every 30 seconds.
     */
    fun toggleSOSBeacon(customMessage: String = "SOS - NEED HELP") {
        if (_sosActive.value) {
            stopSOSBeacon()
        } else {
            startSOSBeacon(customMessage)
        }
    }

    /**
     * Process an incoming EMERGENCY packet. Called by P2PManager.processPacket().
     */
    fun handleEmergencyPacket(packet: Packet) {
        try {
            val payloadStr = String(packet.payload, Charsets.UTF_8)
            val msg = parseEmergencyPayload(payloadStr, packet.sourceId, packet.timestamp)
            addToHistory(msg)
            scope.launch { _emergencyMessages.emit(msg) }
            Log.i(TAG, "EMERGENCY_RECEIVED from=${packet.sourceId} type=${msg.type}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse emergency packet: ${e.message}")
        }
    }

    // --- SOS Beacon ---

    private fun startSOSBeacon(message: String) {
        if (beaconJob?.isActive == true) return
        _sosActive.value = true
        Log.i(TAG, "SOS_BEACON_STARTED")

        beaconJob = scope.launch {
            while (isActive) {
                sendEmergencyBroadcast("[SOS BEACON] $message")
                delay(SOS_BEACON_INTERVAL_MS)
            }
        }
    }

    private fun stopSOSBeacon() {
        beaconJob?.cancel()
        beaconJob = null
        _sosActive.value = false
        Log.i(TAG, "SOS_BEACON_STOPPED")
    }

    // --- Payload building ---

    private fun buildEmergencyPayload(message: String): String {
        val json = JSONObject().apply {
            put("message", message)
            put("type", if (_sosActive.value) "SOS_BEACON" else "EMERGENCY")
            put("timestamp", SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.format(Date()))

            // GPS location if available
            lastKnownLocation?.let { loc ->
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
                put("accuracy", loc.accuracy)
                put("altitude", loc.altitude)
            }

            // Battery level
            try {
                val batteryLevel = p2pManager.networkStats.batteryLevel
                put("battery", batteryLevel)
            } catch (_: Exception) { }

            // Device info
            put("device", android.os.Build.MODEL)
        }
        return json.toString()
    }

    private fun parseEmergencyPayload(payloadStr: String, sourceId: String, timestamp: Long): EmergencyMessage {
        return try {
            val json = JSONObject(payloadStr)
            EmergencyMessage(
                id = UUID.randomUUID().toString(),
                sourceId = sourceId,
                message = json.optString("message", payloadStr),
                type = json.optString("type", "EMERGENCY"),
                timestamp = timestamp,
                latitude = if (json.has("latitude")) json.getDouble("latitude") else null,
                longitude = if (json.has("longitude")) json.getDouble("longitude") else null,
                accuracy = if (json.has("accuracy")) json.getDouble("accuracy").toFloat() else null,
                battery = if (json.has("battery")) json.getInt("battery") else null,
                device = json.optString("device", "Unknown")
            )
        } catch (e: Exception) {
            EmergencyMessage(
                id = UUID.randomUUID().toString(),
                sourceId = sourceId,
                message = payloadStr,
                type = "EMERGENCY",
                timestamp = timestamp
            )
        }
    }

    @Synchronized
    private fun addToHistory(msg: EmergencyMessage) {
        val current = _emergencyHistory.value.toMutableList()
        // Dedup by source + message content within 60s window
        val isDupe = current.any {
            it.sourceId == msg.sourceId &&
            it.message == msg.message &&
            kotlin.math.abs(it.timestamp - msg.timestamp) < 60_000
        }
        if (!isDupe) {
            current.add(0, msg) // Newest first
            // Keep max 200 emergency messages
            if (current.size > 200) {
                _emergencyHistory.value = current.take(200)
            } else {
                _emergencyHistory.value = current
            }
        }
    }

    // --- Location ---

    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "No location permission — emergency messages won't include GPS")
            return
        }

        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return

        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                lastKnownLocation = location
            }
            @Deprecated("Deprecated in API 29")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }
            override fun onProviderEnabled(provider: String) { }
            override fun onProviderDisabled(provider: String) { }
        }
        locationListener = listener

        try {
            // Try GPS first, fall back to network
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 30_000L, 10f, listener)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 30_000L, 10f, listener)
            }
            // Get last known as starting point
            lastKnownLocation = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission denied: ${e.message}")
        }
    }

    private fun stopLocationUpdates() {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return
        locationListener?.let {
            try { lm.removeUpdates(it) } catch (_: Exception) { }
        }
        locationListener = null
    }
}
