package com.fyp.resilientp2p.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages internet gateway detection and cloud relay for cross-mesh messaging.
 *
 * When this device has internet, it advertises itself as __GATEWAY__ in ROUTE_ANNOUNCE.
 * Other mesh nodes can forward internet-bound messages to this gateway, which relays
 * them via Firestore as a transient message buffer (24h TTL).
 *
 * Cloud relay is NOT a message store — it's a transient router with TTL.
 */
class InternetGatewayManager(
    private val context: Context,
    private val p2pManager: P2PManager
) {
    companion object {
        private const val TAG = "InternetGateway"
        const val GATEWAY_FLAG = "__GATEWAY__"
        private const val RELAY_COLLECTION = "mesh_relay"
        private const val MAX_PENDING_PER_DEVICE = 100
        private const val MESSAGE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_MESSAGE_SIZE = 10 * 1024 // 10KB text only
        private const val POLL_INTERVAL_MS = 30_000L // Check for inbound relay messages
        private const val MAX_SEND_RATE_PER_HOUR = 60
    }

    // Internet state
    private val _hasInternet = MutableStateFlow(false)
    val hasInternet: StateFlow<Boolean> = _hasInternet.asStateFlow()

    private val _isGateway = MutableStateFlow(false)
    val isGateway: StateFlow<Boolean> = _isGateway.asStateFlow()

    // Rate limiting
    private val sendTimestamps = mutableListOf<Long>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val projectId: String get() = com.fyp.resilientp2p.BuildConfig.FIREBASE_PROJECT_ID
    private val apiKey: String get() = com.fyp.resilientp2p.BuildConfig.FIREBASE_API_KEY

    /**
     * Start monitoring internet connectivity and polling for relay messages.
     */
    fun start() {
        registerNetworkCallback()
        // Initial check
        _hasInternet.value = checkInternetNow()
        _isGateway.value = _hasInternet.value
        if (_isGateway.value) {
            startRelayPolling()
        }
        Log.i(TAG, "InternetGatewayManager started. hasInternet=${_hasInternet.value}")
    }

    fun destroy() {
        stopRelayPolling()
        unregisterNetworkCallback()
        scope.cancel()
        Log.i(TAG, "InternetGatewayManager destroyed")
    }

    /**
     * Returns true if this device should advertise as a gateway.
     * Called by P2PManager when building ROUTE_ANNOUNCE payloads.
     */
    fun shouldAdvertiseGateway(): Boolean = _isGateway.value

    /**
     * Relay a mesh packet to the cloud relay (Firestore) for cross-mesh delivery.
     * Called by P2PManager when forwarding a packet with no local route and a gateway is available.
     *
     * @return true if relay was accepted (queued for upload), false if rejected
     */
    suspend fun relayToCloud(destId: String, sourceId: String, payload: String, packetId: String): Boolean {
        if (!_hasInternet.value) {
            Log.w(TAG, "Cannot relay — no internet")
            return false
        }
        if (projectId.isBlank() || apiKey.isBlank()) {
            Log.w(TAG, "Cannot relay — Firebase not configured")
            return false
        }
        if (payload.length > MAX_MESSAGE_SIZE) {
            Log.w(TAG, "Relay rejected — message too large: ${payload.length}")
            return false
        }
        if (!checkRateLimit()) {
            Log.w(TAG, "Relay rejected — rate limit exceeded")
            return false
        }

        return try {
            val doc = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("packetId", JSONObject().put("stringValue", packetId))
                    put("sourceId", JSONObject().put("stringValue", sourceId))
                    put("destId", JSONObject().put("stringValue", destId))
                    put("payload", JSONObject().put("stringValue", payload))
                    put("timestamp", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                    put("ttl", JSONObject().put("integerValue", MESSAGE_TTL_MS.toString()))
                    put("gatewayId", JSONObject().put("stringValue", p2pManager.getLocalDeviceName()))
                })
            }

            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$RELAY_COLLECTION?key=$apiKey"
            val response = httpPost(url, doc.toString())
            Log.i(TAG, "RELAY_SENT dest=$destId packetId=${packetId.take(8)} status=$response")
            response in 200..299
        } catch (e: Exception) {
            Log.e(TAG, "RELAY_FAILED dest=$destId error=${e.message}")
            false
        }
    }

    /**
     * Poll Firestore for messages destined for peers on our local mesh.
     * Called periodically when this device is a gateway.
     */
    private suspend fun pollRelayMessages() {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) return

        try {
            // Query for messages where destId matches any of our known mesh peers
            val localPeers = mutableSetOf(p2pManager.getLocalDeviceName())
            val state = p2pManager.state.value
            localPeers.addAll(state.connectedEndpoints)
            localPeers.addAll(state.knownPeers.keys)

            // Fetch all relay messages (we filter client-side — Firestore REST has limited query)
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$RELAY_COLLECTION?key=$apiKey&pageSize=100"
            val (code, body) = httpGet(url)

            if (code !in 200..299 || body == null) return

            val json = JSONObject(body)
            val documents = json.optJSONArray("documents") ?: return

            for (i in 0 until documents.length()) {
                val doc = documents.getJSONObject(i)
                val fields = doc.getJSONObject("fields")
                val destId = fields.getJSONObject("destId").getString("stringValue")
                val sourceId = fields.getJSONObject("sourceId").getString("stringValue")
                val payload = fields.getJSONObject("payload").getString("stringValue")
                val timestamp = fields.getJSONObject("timestamp").getString("integerValue").toLong()
                val packetId = fields.optJSONObject("packetId")?.optString("stringValue") ?: ""

                // Check TTL
                if (System.currentTimeMillis() - timestamp > MESSAGE_TTL_MS) {
                    // Expired — delete from relay
                    deleteRelayMessage(doc.getString("name"))
                    continue
                }

                // Check if destId is on our mesh
                if (destId in localPeers) {
                    Log.i(TAG, "RELAY_INJECT dest=$destId from=$sourceId packetId=${packetId.take(8)}")
                    // Inject into local mesh via P2PManager
                    p2pManager.sendData(destId, "[RELAY:$sourceId] $payload")
                    // Delete from relay after delivery
                    deleteRelayMessage(doc.getString("name"))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "RELAY_POLL_ERROR: ${e.message}")
        }
    }

    private suspend fun deleteRelayMessage(documentPath: String) {
        try {
            val url = "https://firestore.googleapis.com/v1/$documentPath?key=$apiKey"
            httpDelete(url)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete relay message: ${e.message}")
        }
    }

    // --- Network monitoring ---

    private fun registerNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.i(TAG, "Internet available")
                _hasInternet.value = true
                _isGateway.value = true
                startRelayPolling()
            }

            override fun onLost(network: Network) {
                // Check if ANY network still has internet
                val stillHasInternet = checkInternetNow()
                Log.i(TAG, "Network lost. Still has internet: $stillHasInternet")
                _hasInternet.value = stillHasInternet
                _isGateway.value = stillHasInternet
                if (!stillHasInternet) {
                    stopRelayPolling()
                }
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _hasInternet.value = validated
                _isGateway.value = validated
            }
        }
        networkCallback = callback
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    private fun unregisterNetworkCallback() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        networkCallback?.let {
            try { cm.unregisterNetworkCallback(it) } catch (_: Exception) { }
        }
        networkCallback = null
    }

    private fun checkInternetNow(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    // --- Relay polling loop ---

    private fun startRelayPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                pollRelayMessages()
            }
        }
    }

    private fun stopRelayPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    // --- Rate limiting ---

    @Synchronized
    private fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        sendTimestamps.removeAll { it < oneHourAgo }
        if (sendTimestamps.size >= MAX_SEND_RATE_PER_HOUR) return false
        sendTimestamps.add(now)
        return true
    }

    // --- HTTP helpers ---

    private fun httpPost(urlStr: String, body: String): Int {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return try {
            OutputStreamWriter(conn.outputStream).use { it.write(body) }
            conn.responseCode
        } finally {
            conn.disconnect()
        }
    }

    private fun httpGet(urlStr: String): Pair<Int, String?> {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }
        return try {
            val code = conn.responseCode
            val body = if (code in 200..299) {
                BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            } else null
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
