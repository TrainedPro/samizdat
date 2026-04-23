package com.fyp.resilientp2p.managers

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.fyp.resilientp2p.data.LogLevel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64

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
        private const val FIRESTORE_STRING_VALUE = "stringValue"
        const val GATEWAY_FLAG = "__GATEWAY__"
        private const val RELAY_COLLECTION = "mesh_relay"
        private const val PRESENCE_COLLECTION = "mesh_presence"
        private const val MAX_PENDING_PER_DEVICE = 100
        private const val MESSAGE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_MESSAGE_SIZE = 50 * 1024 // 50KB (increased from 10KB for chat messages)
        private const val PRESENCE_TTL_MS = 5 * 60 * 1000L  // 5 minutes (was 90s — too short, caused flapping)
        private const val POLL_INTERVAL_MS = 30_000L // Check for inbound relay messages
        private const val MAX_SEND_RATE_PER_HOUR = 120 // Increased from 60 for active chat
        /** Prefix for messages relayed through a gateway proxy on behalf of a non-gateway device. */
        const val PROXY_RELAY_PREFIX = "__PROXY_RELAY__:"
    }

    // Internet state
    private val _hasInternet = MutableStateFlow(false)
    val hasInternet: StateFlow<Boolean> = _hasInternet.asStateFlow()

    private val _isGateway = MutableStateFlow(false)
    val isGateway: StateFlow<Boolean> = _isGateway.asStateFlow()

    /** User-controlled toggle: when false, gateway is disabled even if internet is available. */
    private val _gatewayEnabled = MutableStateFlow(true)
    val gatewayEnabled: StateFlow<Boolean> = _gatewayEnabled.asStateFlow()

    private val _cloudPeers = MutableStateFlow(emptySet<String>())
    val cloudPeers: StateFlow<Set<String>> = _cloudPeers.asStateFlow()

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
        updateGatewayState()
        if (_isGateway.value) {
            startRelayPolling()
        }
        p2pManager.log("InternetGatewayManager started. hasInternet=${_hasInternet.value}, gatewayEnabled=${_gatewayEnabled.value}")
    }

    fun destroy() {
        stopRelayPolling()
        unregisterNetworkCallback()
        scope.cancel()
        p2pManager.log("InternetGatewayManager destroyed")
    }

    /**
     * Returns true if this device should advertise as a gateway.
     * Called by P2PManager when building ROUTE_ANNOUNCE payloads.
     */
    fun shouldAdvertiseGateway(): Boolean = _isGateway.value

    /**
     * Toggle gateway behaviour on/off. When disabled, this device will not
     * advertise as a gateway even if internet is available.
     */
    fun setGatewayEnabled(enabled: Boolean) {
        _gatewayEnabled.value = enabled
        updateGatewayState()
        p2pManager.log("Gateway enabled=$enabled, isGateway=${_isGateway.value}")
    }

    /** Recompute [_isGateway] from [_hasInternet] and [_gatewayEnabled]. */
    private fun updateGatewayState() {
        val shouldBeGateway = _hasInternet.value && _gatewayEnabled.value
        _isGateway.value = shouldBeGateway
        if (shouldBeGateway) startRelayPolling() else stopRelayPolling()
    }

    /**
     * Relay a mesh packet to the cloud relay (Firestore) for cross-mesh delivery.
     * Called by P2PManager when forwarding a packet with no local route and a gateway is available.
     *
     * @return true if relay was accepted (queued for upload), false if rejected
     */
    suspend fun relayToCloud(destId: String, sourceId: String, payload: ByteArray, packetId: String): Boolean {
        if (!_hasInternet.value) {
            p2pManager.log("Cannot relay — no internet", LogLevel.WARN)
            return false
        }
        if (projectId.isBlank() || apiKey.isBlank()) {
            p2pManager.log("Cannot relay — Firebase not configured", LogLevel.WARN)
            return false
        }
        if (payload.size > MAX_MESSAGE_SIZE) {
            p2pManager.log("Relay rejected — message too large: ${payload.size}", LogLevel.WARN)
            return false
        }
        if (!checkRateLimit()) {
            p2pManager.log("Relay rejected — rate limit exceeded", LogLevel.WARN)
            return false
        }

        return try {
            val payloadBase64 = Base64.getEncoder().encodeToString(payload)
            val doc = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("packetId", JSONObject().put(FIRESTORE_STRING_VALUE, packetId))
                    put("sourceId", JSONObject().put(FIRESTORE_STRING_VALUE, sourceId))
                    put("destId", JSONObject().put(FIRESTORE_STRING_VALUE, destId))
                    put("payload", JSONObject().put(FIRESTORE_STRING_VALUE, payloadBase64))
                    put("payloadEncoding", JSONObject().put(FIRESTORE_STRING_VALUE, "base64"))
                    put("timestamp", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                    put("ttl", JSONObject().put("integerValue", MESSAGE_TTL_MS.toString()))
                    put("gatewayId", JSONObject().put(FIRESTORE_STRING_VALUE, p2pManager.getLocalDeviceName()))
                })
            }

            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$RELAY_COLLECTION?key=$apiKey"
            val response = httpPost(url, doc.toString())
            p2pManager.log("RELAY_SENT dest=$destId packetId=${packetId.take(8)} status=$response")
            response in 200..299
        } catch (e: Exception) {
            p2pManager.log("RELAY_FAILED dest=$destId error=${e.message}", LogLevel.ERROR)
            false
        }
    }

    /**
     * Poll Firestore for messages destined for this device or any peer on our local mesh.
     * Called periodically when this device is a gateway.
     */
    private suspend fun pollRelayMessages() {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) return

        try {
            // Deliver to: this device + all directly connected mesh peers + all routed peers.
            // Previously only included connectedEndpoints (direct neighbors), which meant
            // messages for routed peers were never injected. Fixed to include all knownPeers.
            val localPeers = mutableSetOf(p2pManager.getLocalDeviceName())
            val state = p2pManager.state.value
            localPeers.addAll(state.connectedEndpoints)
            localPeers.addAll(state.knownPeers.keys)

            // Fetch all relay messages (we filter client-side — Firestore REST has limited query)
            val url = "https://firestore.googleapis.com/v1/projects/$projectId" +
                "/databases/(default)/documents/$RELAY_COLLECTION?key=$apiKey&pageSize=100"
            val (code, body) = httpGet(url)

            if (code !in 200..299 || body == null) return

            val json = JSONObject(body)
            val documents = json.optJSONArray("documents") ?: return

            for (i in 0 until documents.length()) {
                if (i >= MAX_PENDING_PER_DEVICE) {
                    p2pManager.log("RELAY_POLL capped at $MAX_PENDING_PER_DEVICE messages per poll", LogLevel.WARN)
                    break
                }
                val doc = documents.getJSONObject(i)
                val fields = doc.getJSONObject("fields")
                val destId = fields.getJSONObject("destId").getString(FIRESTORE_STRING_VALUE)
                val sourceId = fields.getJSONObject("sourceId").getString(FIRESTORE_STRING_VALUE)
                val payloadEncoded = fields.getJSONObject("payload").getString(FIRESTORE_STRING_VALUE)
                val payloadEncoding = fields.optJSONObject("payloadEncoding")?.optString(FIRESTORE_STRING_VALUE, "plain") ?: "plain"
                val timestamp = fields.getJSONObject("timestamp").getString("integerValue").toLong()
                val packetId = fields.optJSONObject("packetId")?.optString(FIRESTORE_STRING_VALUE) ?: ""

                // Check TTL
                if (System.currentTimeMillis() - timestamp > MESSAGE_TTL_MS) {
                    deleteRelayMessage(doc.getString("name"))
                    continue
                }

                // Deliver if destId is this device OR any peer reachable through our mesh.
                // This is the key fix: previously only checked connectedEndpoints (direct neighbors).
                if (destId in localPeers) {
                    p2pManager.log("RELAY_INJECT dest=$destId from=$sourceId packetId=${packetId.take(8)}")
                    val payloadBytes = when (payloadEncoding) {
                        "base64" -> Base64.getDecoder().decode(payloadEncoded)
                        else -> payloadEncoded.toByteArray(StandardCharsets.UTF_8)
                    }
                    val relayPacket = com.fyp.resilientp2p.transport.Packet(
                        id = packetId.ifBlank { java.util.UUID.randomUUID().toString() },
                        type = com.fyp.resilientp2p.transport.PacketType.DATA,
                        sourceId = sourceId,
                        destId = destId,
                        payload = payloadBytes,
                        timestamp = timestamp,
                        ttl = com.fyp.resilientp2p.transport.Packet.DEFAULT_TTL
                    )
                    p2pManager.injectPacket(relayPacket)
                    deleteRelayMessage(doc.getString("name"))
                }
            }
        } catch (e: Exception) {
            p2pManager.log("RELAY_POLL_ERROR: ${e.message}", LogLevel.ERROR)
        }
    }

    /**
     * Handle a proxy relay request from a non-gateway mesh peer.
     * Called by P2PManager when a DATA packet with [PROXY_RELAY_PREFIX] is received.
     * The gateway unpacks the original destId/payload and relays to Firestore.
     */
    fun handleProxyRelayRequest(packet: com.fyp.resilientp2p.transport.Packet) {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) {
            p2pManager.log("PROXY_RELAY_REJECTED reason=NO_INTERNET_OR_CONFIG", LogLevel.WARN)
            return
        }
        scope.launch {
            try {
                val payloadStr = String(packet.payload, StandardCharsets.UTF_8)
                val json = JSONObject(payloadStr.removePrefix(PROXY_RELAY_PREFIX))
                val destId = json.getString("destId")
                val originalPayload = Base64.getDecoder().decode(json.getString("payload"))
                val originalPacketId = json.optString("packetId", java.util.UUID.randomUUID().toString())
                val relayed = relayToCloud(destId, packet.sourceId, originalPayload, originalPacketId)
                p2pManager.log("PROXY_RELAY_${if (relayed) "OK" else "FAILED"} dest=$destId from=${packet.sourceId}")
            } catch (e: Exception) {
                p2pManager.log("PROXY_RELAY_ERROR: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    private suspend fun publishPresence() {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) return
        try {
            val localDeviceName = p2pManager.getLocalDeviceName()
            val docId = URLEncoder.encode(localDeviceName, StandardCharsets.UTF_8.name())
            val doc = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("peerId", JSONObject().put(FIRESTORE_STRING_VALUE, localDeviceName))
                    put("lastSeen", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
                    put("hasInternet", JSONObject().put("booleanValue", true))
                })
            }
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$PRESENCE_COLLECTION/$docId?key=$apiKey"
            val code = httpPatch(url, doc.toString())
            if (code !in 200..299) {
                p2pManager.log("PRESENCE_PUBLISH_FAILED status=$code", LogLevel.DEBUG)
            }
        } catch (e: Exception) {
            p2pManager.log("PRESENCE_PUBLISH_ERROR: ${e.message}", LogLevel.DEBUG)
        }
    }

    private suspend fun pollPresencePeers() {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) return
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$PRESENCE_COLLECTION?key=$apiKey&pageSize=200"
            val (code, body) = httpGet(url)
            if (code !in 200..299 || body == null) return

            val now = System.currentTimeMillis()
            val localName = p2pManager.getLocalDeviceName()
            val result = mutableSetOf<String>()
            val documents = JSONObject(body).optJSONArray("documents") ?: return

            for (i in 0 until documents.length()) {
                val fields = documents.getJSONObject(i).optJSONObject("fields") ?: continue
                val peerId = fields.optJSONObject("peerId")?.optString(FIRESTORE_STRING_VALUE).orEmpty()
                if (peerId.isBlank() || peerId == localName) continue
                val lastSeen = fields.optJSONObject("lastSeen")?.optString("integerValue")?.toLongOrNull() ?: continue
                if (now - lastSeen <= PRESENCE_TTL_MS) {
                    result.add(peerId)
                }
            }
            _cloudPeers.value = result
        } catch (e: Exception) {
            p2pManager.log("PRESENCE_POLL_ERROR: ${e.message}", LogLevel.DEBUG)
        }
    }

    private suspend fun deleteRelayMessage(documentPath: String) {
        try {
            val url = "https://firestore.googleapis.com/v1/$documentPath?key=$apiKey"
            httpDelete(url)
        } catch (e: Exception) {
            p2pManager.log("Failed to delete relay message: ${e.message}", LogLevel.WARN)
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
                p2pManager.log("Internet available")
                _hasInternet.value = true
                updateGatewayState()
            }

            override fun onLost(network: Network) {
                // Check if ANY network still has internet
                val stillHasInternet = checkInternetNow()
                p2pManager.log("Network lost. Still has internet: $stillHasInternet")
                _hasInternet.value = stillHasInternet
                updateGatewayState()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                _hasInternet.value = validated
                updateGatewayState()
            }
        }
        networkCallback = callback
        try {
            cm.registerNetworkCallback(request, callback)
        } catch (e: Exception) {
            p2pManager.log("Failed to register network callback: ${e.message}", LogLevel.ERROR)
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
                publishPresence()
                pollPresencePeers()
                pollRelayMessages()
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopRelayPolling() {
        pollJob?.cancel()
        pollJob = null
        _cloudPeers.value = emptySet()
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

    private fun httpPatch(urlStr: String, body: String): Int {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            requestMethod = "PATCH"
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
}
