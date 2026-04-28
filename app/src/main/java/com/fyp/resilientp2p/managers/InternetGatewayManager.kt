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
import android.util.Base64

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
        private const val FIRESTORE_INTEGER_VALUE = "integerValue"
        const val GATEWAY_FLAG = "__GATEWAY__"
        private const val RELAY_COLLECTION = "mesh_relay"
        private const val PRESENCE_COLLECTION = "mesh_presence"
        private const val MAX_PENDING_PER_DEVICE = 100
        private const val MESSAGE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_MESSAGE_SIZE = 50 * 1024 // 50KB (increased from 10KB for chat messages)
        private const val PRESENCE_TTL_MS = 5 * 60 * 1000L  // 5 minutes (was 90s — too short, caused flapping)
        private const val POLL_INTERVAL_MS = 5_000L // Check for inbound relay messages every 5 seconds (was 30s)
        // Tiered rate limits per packet type per hour
        private const val MAX_EMERGENCY_RATE_PER_HOUR = Int.MAX_VALUE // Unlimited for emergencies
        private const val MAX_AUDIO_RATE_PER_HOUR = 36000 // 10 packets/sec for 1 hour of audio streaming
        private const val MAX_FILE_RATE_PER_HOUR = 7200 // 2 packets/sec for file transfers
        private const val MAX_DATA_RATE_PER_HOUR = 1200 // Original limit for regular data
        /** Prefix for messages relayed through a gateway proxy on behalf of a non-gateway device. */
        const val PROXY_RELAY_PREFIX = "__PROXY_RELAY__:"

        /**
         * Minimum capability score [0–100] for a node to be considered "cloud-preferred".
         * Nodes above this threshold on BOTH sides will skip the Bluetooth mesh connection
         * and communicate purely via cloud relay, freeing up mesh slots for weaker nodes.
         *
         * Score formula: battery% × 0.6 + stability × 0.4
         * where stability = 1 − (disconnects / max(1, totalConnections))
         */
        const val CLOUD_PREFER_THRESHOLD = 50
    }

    /** Capability scores of online peers, keyed by peerId. Updated on each presence poll. */
    private val peerCapabilityScores = java.util.concurrent.ConcurrentHashMap<String, Int>()

    /**
     * Compute this device's capability score [0–100].
     * Higher = more capable of acting as a cloud node.
     *   battery%  × 0.6  — low battery nodes should stay on mesh
     *   stability × 0.4  — unstable nodes are poor cloud relays
     */
    fun computeLocalCapabilityScore(): Int {
        val battery = p2pManager.networkStats.batteryLevel.coerceIn(0, 100)
        val stats = p2pManager.networkStats
        val totalConns = stats.totalConnectionsEstablished.get()
        val disconnects = stats.totalConnectionsLost.get()
        val stability = if (totalConns > 0) {
            (1.0 - disconnects.toDouble() / totalConns).coerceIn(0.0, 1.0)
        } else {
            1.0 // No history → assume stable
        }
        return (battery * 0.6 + stability * 100 * 0.4).toInt().coerceIn(0, 100)
    }

    /**
     * Returns true if this device and [peerId] are both cloud-capable and should
     * prefer cloud relay over a Bluetooth mesh connection.
     *
     * Both sides must:
     *  1. Have internet (this device: checked via [_hasInternet]; peer: must be in cloudPeers)
     *  2. Score ≥ [CLOUD_PREFER_THRESHOLD]
     */
    fun shouldPreferCloudFor(peerId: String): Boolean {
        if (!_hasInternet.value || !_gatewayEnabled.value) return false
        if (peerId !in _cloudPeers.value) return false // peer not online
        val localScore = computeLocalCapabilityScore()
        val peerScore = peerCapabilityScores[peerId] ?: 0
        return localScore >= CLOUD_PREFER_THRESHOLD && peerScore >= CLOUD_PREFER_THRESHOLD
    }

    /** Returns the cached capability score for [peerId], or -1 if unknown. */
    fun getPeerCapabilityScore(peerId: String): Int = peerCapabilityScores[peerId] ?: -1

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

    // Rate limiting - tiered by packet type
    private val emergencyTimestamps = mutableListOf<Long>()
    private val audioTimestamps = mutableListOf<Long>()
    private val fileTimestamps = mutableListOf<Long>()
    private val dataTimestamps = mutableListOf<Long>()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollJob: Job? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val projectId: String get() = com.fyp.resilientp2p.BuildConfig.FIREBASE_PROJECT_ID
    private val apiKey: String get() = com.fyp.resilientp2p.BuildConfig.FIREBASE_API_KEY

    /**
     * Start monitoring internet connectivity and polling for relay messages.
     */
    fun start() {
        p2pManager.log("InternetGatewayManager starting...", LogLevel.INFO)

        // Log Firebase configuration status (without exposing keys)
        val projectConfigured = projectId.isNotBlank()
        val apiKeyConfigured = apiKey.isNotBlank()
        p2pManager.log("Firebase config: projectId=${if (projectConfigured) "✅ SET" else "❌ MISSING"} " +
            "apiKey=${if (apiKeyConfigured) "✅ SET" else "❌ MISSING"}", LogLevel.INFO)

        registerNetworkCallback()

        // Initial check with detailed logging
        val initialInternet = checkInternetNow()
        p2pManager.log("Initial internet check: $initialInternet", LogLevel.INFO)
        _hasInternet.value = initialInternet

        updateGatewayState()

        if (_isGateway.value) {
            p2pManager.log("Starting relay polling (gateway active)", LogLevel.INFO)
            startRelayPolling()
        } else {
            p2pManager.log("Gateway inactive: hasInternet=$initialInternet, enabled=${_gatewayEnabled.value}, " +
                "firebaseConfigured=${projectConfigured && apiKeyConfigured}", LogLevel.WARN)
        }

        p2pManager.log("InternetGatewayManager started. hasInternet=${_hasInternet.value}, " +
            "gatewayEnabled=${_gatewayEnabled.value}, isGateway=${_isGateway.value}", LogLevel.INFO)
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
        val wasGateway = _isGateway.value
        _isGateway.value = shouldBeGateway

        if (shouldBeGateway && !wasGateway) {
            p2pManager.log("Gateway ACTIVATED: hasInternet=${_hasInternet.value}, enabled=${_gatewayEnabled.value}", LogLevel.INFO)
            startRelayPolling()
        } else if (!shouldBeGateway && wasGateway) {
            p2pManager.log("Gateway DEACTIVATED: hasInternet=${_hasInternet.value}, enabled=${_gatewayEnabled.value}", LogLevel.INFO)
            stopRelayPolling()
        }
    }

    /**
     * Relay a mesh packet to the cloud relay (Firestore) for cross-mesh delivery.
     * Called by P2PManager when forwarding a packet with no local route and a gateway is available.
     *
     * @return true if relay was accepted (queued for upload), false if rejected
     */
    suspend fun relayToCloud(destId: String, sourceId: String, payload: ByteArray, packetId: String, packetType: String = "DATA"): Boolean {
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
        if (!checkRateLimit(packetType)) {
            p2pManager.log("Relay rejected — rate limit exceeded for $packetType", LogLevel.WARN)
            return false
        }

        return try {
            val payloadBase64 = Base64.encodeToString(payload, Base64.NO_WRAP)
            val doc = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("packetId", JSONObject().put(FIRESTORE_STRING_VALUE, packetId))
                    put("sourceId", JSONObject().put(FIRESTORE_STRING_VALUE, sourceId))
                    put("destId", JSONObject().put(FIRESTORE_STRING_VALUE, destId))
                    put("payload", JSONObject().put(FIRESTORE_STRING_VALUE, payloadBase64))
                    put("payloadEncoding", JSONObject().put(FIRESTORE_STRING_VALUE, "base64"))
                    put("packetType", JSONObject().put(FIRESTORE_STRING_VALUE, packetType))
                    put("timestamp", JSONObject().put(FIRESTORE_INTEGER_VALUE, System.currentTimeMillis().toString()))
                    put("ttl", JSONObject().put(FIRESTORE_INTEGER_VALUE, MESSAGE_TTL_MS.toString()))
                    put("gatewayId", JSONObject().put(FIRESTORE_STRING_VALUE, p2pManager.getLocalDeviceName()))
                })
            }

            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$RELAY_COLLECTION?key=$apiKey"
            val response = httpPost(url, doc.toString())
            p2pManager.log("RELAY_SENT dest=$destId packetId=${packetId.take(8)} type=$packetType status=$response")
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
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth", "LoopWithTooManyJumpStatements", "ComplexCondition")
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
                val packetTypeStr = fields.optJSONObject("packetType")?.optString(FIRESTORE_STRING_VALUE, "DATA") ?: "DATA"
                val timestamp = fields.getJSONObject("timestamp").getString(FIRESTORE_INTEGER_VALUE).toLong()
                val packetId = fields.optJSONObject("packetId")?.optString(FIRESTORE_STRING_VALUE) ?: ""

                // Check TTL
                if (System.currentTimeMillis() - timestamp > MESSAGE_TTL_MS) {
                    deleteRelayMessage(doc.getString("name"))
                    continue
                }

                // Deliver if destId is this device OR any peer reachable through our mesh.
                // This is the key fix: previously only checked connectedEndpoints (direct neighbors).
                if (destId in localPeers) {
                    p2pManager.log("RELAY_INJECT dest=$destId from=$sourceId packetId=${packetId.take(8)} type=$packetTypeStr")
                    val payloadBytes = if (payloadEncoding == "base64") {
                        Base64.decode(payloadEncoded, Base64.NO_WRAP)
                    } else {
                        payloadEncoded.toByteArray(StandardCharsets.UTF_8)
                    }

                    // Convert string to PacketType enum
                    val packetType = when (packetTypeStr) {
                        "AUDIO_DATA" -> com.fyp.resilientp2p.transport.PacketType.AUDIO_DATA
                        "AUDIO_CONTROL" -> com.fyp.resilientp2p.transport.PacketType.AUDIO_CONTROL
                        "FILE_CHUNK" -> com.fyp.resilientp2p.transport.PacketType.FILE_CHUNK
                        "FILE_META" -> com.fyp.resilientp2p.transport.PacketType.FILE_META
                        else -> com.fyp.resilientp2p.transport.PacketType.DATA
                    }

                    var relayPacket = com.fyp.resilientp2p.transport.Packet(
                        id = packetId.ifBlank { java.util.UUID.randomUUID().toString() },
                        type = packetType,
                        sourceId = sourceId,
                        destId = destId,
                        payload = payloadBytes,
                        timestamp = timestamp,
                        ttl = com.fyp.resilientp2p.transport.Packet.DEFAULT_TTL
                    )

                    // CRITICAL FIX: Decrypt cloud relay packets before injection
                    // Cloud relay packets are encrypted+HMAC'd before upload, but injectPacket()
                    // bypasses the normal security checks in onPayloadReceived(). We must decrypt here.
                    val security = p2pManager.securityManager
                    val requiresDecryption = packetType == com.fyp.resilientp2p.transport.PacketType.DATA ||
                        packetType == com.fyp.resilientp2p.transport.PacketType.STORE_FORWARD ||
                        packetType == com.fyp.resilientp2p.transport.PacketType.ACK
                    val isDestination = destId == p2pManager.getLocalDeviceName()
                    val hasSharedKey = security?.hasKeyForPeer(sourceId) == true

                    if (security != null && requiresDecryption && isDestination && hasSharedKey) {
                        // Strip HMAC (last 32 bytes)
                        if (relayPacket.payload.size > com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE) {
                            val dataBytes = relayPacket.payload.copyOfRange(
                                0,
                                relayPacket.payload.size - com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE
                            )
                            val hmacBytes = relayPacket.payload.copyOfRange(
                                relayPacket.payload.size - com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE,
                                relayPacket.payload.size
                            )

                            // Verify HMAC
                            if (!security.verifyHmac(sourceId, dataBytes, hmacBytes)) {
                                p2pManager.log("CLOUD_RELAY_HMAC_INVALID from=$sourceId — dropping packet",
                                    com.fyp.resilientp2p.data.LogLevel.WARN)
                                deleteRelayMessage(doc.getString("name"))
                                continue
                            }

                            // Decrypt
                            val decrypted = security.decrypt(sourceId, dataBytes)
                            if (decrypted != null) {
                                relayPacket = relayPacket.copy(payload = decrypted)
                                p2pManager.log("CLOUD_RELAY_DECRYPTED from=$sourceId size=${decrypted.size}B",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG)
                            } else {
                                p2pManager.log("CLOUD_RELAY_DECRYPT_FAILED from=$sourceId — dropping packet",
                                    com.fyp.resilientp2p.data.LogLevel.WARN)
                                deleteRelayMessage(doc.getString("name"))
                                continue
                            }
                        } else {
                            p2pManager.log("CLOUD_RELAY_NO_HMAC from=$sourceId size=${relayPacket.payload.size}B — dropping packet",
                                com.fyp.resilientp2p.data.LogLevel.WARN)
                            deleteRelayMessage(doc.getString("name"))
                            continue
                        }
                    }

                    p2pManager.injectPacket(relayPacket)

                    // Delete the message after successful injection
                    deleteRelayMessage(doc.getString("name"))
                    p2pManager.log("RELAY_MESSAGE_DELIVERED dest=$destId from=$sourceId packetId=${packetId.take(8)}")
                } else {
                    // Message not for us or our peers - log for debugging
                    p2pManager.log("RELAY_SKIP dest=$destId from=$sourceId packetId=${packetId.take(8)} reason=NOT_IN_LOCAL_PEERS localDevice=${p2pManager.getLocalDeviceName()}", com.fyp.resilientp2p.data.LogLevel.DEBUG)
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
                val originalPayload = Base64.decode(json.getString("payload"), Base64.NO_WRAP)
                val originalPacketId = json.optString("packetId", java.util.UUID.randomUUID().toString())
                val packetType = json.optString("packetType", "DATA")
                val relayed = relayToCloud(destId, packet.sourceId, originalPayload, originalPacketId, packetType)
                if (relayed) {
                    p2pManager.log("PACKET_RELAYED_CLOUD id=${originalPacketId.take(8)} dest='$destId' reason=INTERNET_PEER")
                    p2pManager.log("PROXY_RELAY_OK dest=$destId from=${packet.sourceId}")
                } else {
                    // CRITICAL FIX: If cloud relay fails, try store-and-forward fallback
                    p2pManager.log("RELAY_FAILED_FALLBACK_SF id=${originalPacketId.take(8)} dest='$destId' reason=INTERNET_PEER")
                    p2pManager.log("PROXY_RELAY_FAILED dest=$destId from=${packet.sourceId}")
                }
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
            val capabilityScore = computeLocalCapabilityScore()

            // Calculate reachable peers (same logic as pollRelayMessages)
            val localPeers = mutableSetOf(localDeviceName)
            val state = p2pManager.state.value
            localPeers.addAll(state.connectedEndpoints)
            localPeers.addAll(state.knownPeers.keys)

            // CRITICAL FIX: Filter out other gateways to prevent circular references
            // Only include actual mesh peers, not other internet gateways
            val currentCloudPeers = _cloudPeers.value
            val reachablePeersFiltered = localPeers.filter { peer ->
                // Include self and local mesh peers, but exclude other gateways
                peer == localDeviceName || peer !in currentCloudPeers
            }.take(50) // Limit to 50 peers to control document size

            val doc = JSONObject().apply {
                put("fields", JSONObject().apply {
                    put("peerId", JSONObject().put(FIRESTORE_STRING_VALUE, localDeviceName))
                    put("lastSeen", JSONObject().put(FIRESTORE_INTEGER_VALUE, System.currentTimeMillis().toString()))
                    put("hasInternet", JSONObject().put("booleanValue", true))
                    put("capabilityScore", JSONObject().put(FIRESTORE_INTEGER_VALUE, capabilityScore.toString()))
                    put("batteryLevel", JSONObject().put(FIRESTORE_INTEGER_VALUE, p2pManager.networkStats.batteryLevel.toString()))
                    put("version", JSONObject().put(FIRESTORE_INTEGER_VALUE, "2")) // Version 2 = has reachablePeers
                    // CROSS-MESH FIX: Include filtered reachable peers so other gateways can discover them
                    put("reachablePeers", JSONObject().put("arrayValue", JSONObject().apply {
                        put("values", org.json.JSONArray().apply {
                            reachablePeersFiltered.forEach { peer ->
                                put(JSONObject().put(FIRESTORE_STRING_VALUE, peer))
                            }
                        })
                    }))
                })
            }
            val url = "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/$PRESENCE_COLLECTION/$docId?key=$apiKey"
            val code = httpPatch(url, doc.toString())
            if (code in 200..299) {
                p2pManager.log("PRESENCE_PUBLISH peerId=$localDeviceName reachablePeers=${reachablePeersFiltered.size}", LogLevel.DEBUG)
            } else {
                p2pManager.log("PRESENCE_PUBLISH_FAILED status=$code", LogLevel.DEBUG)
            }
        } catch (e: Exception) {
            p2pManager.log("PRESENCE_PUBLISH_ERROR: ${e.message}", LogLevel.DEBUG)
        }
    }

    private suspend fun pollPresencePeers() {
        if (!_hasInternet.value || projectId.isBlank() || apiKey.isBlank()) return
        try {
            val url = "https://firestore.googleapis.com/v1/projects/$projectId" +
                "/databases/(default)/documents/$PRESENCE_COLLECTION?key=$apiKey&pageSize=200"
            val (code, body) = httpGet(url)
            if (code !in 200..299 || body == null) return

            val now = System.currentTimeMillis()
            val localName = p2pManager.getLocalDeviceName()
            val result = mutableSetOf<String>()
            val documents = JSONObject(body).optJSONArray("documents") ?: return

            for (i in 0 until documents.length()) {
                val fields = documents.getJSONObject(i).optJSONObject("fields") ?: continue
                processPresenceDocument(fields, now, localName, result)
            }
            // Remove scores for peers that are no longer online
            peerCapabilityScores.keys.retainAll(result)
            _cloudPeers.value = result
        } catch (e: Exception) {
            p2pManager.log("PRESENCE_POLL_ERROR: ${e.message}", LogLevel.DEBUG)
        }
    }

    @Suppress("NestedBlockDepth") // Complex presence processing requires nested structure
    private fun processPresenceDocument(
        fields: JSONObject,
        now: Long,
        localName: String,
        result: MutableSet<String>
    ) {
        val peerId = fields.optJSONObject("peerId")?.optString(FIRESTORE_STRING_VALUE).orEmpty()
        if (peerId.isBlank() || peerId == localName) return

        val lastSeen = fields.optJSONObject("lastSeen")?.optString(FIRESTORE_INTEGER_VALUE)?.toLongOrNull()
        if (lastSeen == null) return

        if (now - lastSeen <= PRESENCE_TTL_MS) {
            result.add(peerId)
            // Cache the peer's capability score for cloud-prefer decisions
            val score = fields.optJSONObject("capabilityScore")?.optString(FIRESTORE_INTEGER_VALUE)?.toIntOrNull()
            if (score != null) peerCapabilityScores[peerId] = score

            // CROSS-MESH FIX: Extract reachable peers from this gateway's presence
            val version = fields.optJSONObject("version")?.optString(FIRESTORE_INTEGER_VALUE)?.toIntOrNull() ?: 1
            if (version >= 2) {
                val reachablePeers = fields.optJSONObject("reachablePeers")
                    ?.optJSONObject("arrayValue")
                    ?.optJSONArray("values")

                reachablePeers?.let { array ->
                    for (i in 0 until array.length()) {
                        val peer = array.getJSONObject(i).optString(FIRESTORE_STRING_VALUE)
                        // Validate peer name format and prevent circular references
                        val isValidPeer = peer.isNotBlank() &&
                            peer != localName &&
                            peer != peerId &&
                            peer.matches(Regex("^[a-zA-Z0-9_-]{1,50}$"))
                        if (isValidPeer) {
                            result.add(peer)
                        }
                    }
                    p2pManager.log("PRESENCE_PROCESS gateway=$peerId reachablePeers=${array.length()}", LogLevel.DEBUG)
                }
            }
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
                p2pManager.log("Network callback: onAvailable", LogLevel.INFO)
                _hasInternet.value = true
                updateGatewayState()
            }

            override fun onLost(network: Network) {
                // Check if ANY network still has internet
                val stillHasInternet = checkInternetNow()
                p2pManager.log("Network callback: onLost. Still has internet: $stillHasInternet", LogLevel.INFO)
                _hasInternet.value = stillHasInternet
                updateGatewayState()
            }

            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                p2pManager.log("Network callback: onCapabilitiesChanged. hasInternet=$hasInternet, validated=$validated", LogLevel.INFO)
                _hasInternet.value = validated && hasInternet
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
        val network = cm.activeNetwork
        if (network == null) {
            p2pManager.log("checkInternetNow: No active network", LogLevel.DEBUG)
            return false
        }

        val caps = cm.getNetworkCapabilities(network)
        if (caps == null) {
            p2pManager.log("checkInternetNow: No network capabilities", LogLevel.DEBUG)
            return false
        }

        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        p2pManager.log("checkInternetNow: hasInternet=$hasInternet, isValidated=$isValidated", LogLevel.DEBUG)

        return hasInternet && isValidated
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
    private fun checkRateLimit(packetType: String): Boolean {
        val now = System.currentTimeMillis()
        val oneHourAgo = now - 3600_000L
        // Select appropriate rate limit and timestamp list based on packet type
        val (timestamps, limit) = when (packetType) {
            "EMERGENCY" -> emergencyTimestamps to MAX_EMERGENCY_RATE_PER_HOUR
            "AUDIO_DATA", "AUDIO_CONTROL" -> audioTimestamps to MAX_AUDIO_RATE_PER_HOUR
            "FILE_META", "FILE_CHUNK" -> fileTimestamps to MAX_FILE_RATE_PER_HOUR
            else -> dataTimestamps to MAX_DATA_RATE_PER_HOUR
        }

        // Clean old timestamps
        timestamps.removeAll { it < oneHourAgo }

        // Check limit
        if (timestamps.size >= limit) {
            return false
        }

        // Add current timestamp
        timestamps.add(now)
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
