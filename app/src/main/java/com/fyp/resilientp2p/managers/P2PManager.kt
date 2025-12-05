package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.LogDao
import com.fyp.resilientp2p.data.LogEntry
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages all interactions with the Nearby Connections API. Follows the Singleton pattern to ensure
 * a single point of truth for network state.
 */
class P2PManager(
        private val context: Context,
        private val logDao: com.fyp.resilientp2p.data.LogDao,
        private val packetDao: com.fyp.resilientp2p.data.PacketDao
) {

    companion object {
        private const val TAG = "P2PManager"
        private const val SERVICE_ID = "com.fyp.resilientp2p.p2p"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val localUsername = "P2P-Node-${(100..999).random()}"
    private val scope = CoroutineScope(Dispatchers.IO)

    // State Management
    data class P2PState(
            val isAdvertising: Boolean = false,
            val isDiscovering: Boolean = false,
            val connectedEndpoints: Set<String> = emptySet(),
            val logs: List<String> = emptyList(),
            val isLowPower: Boolean = false,
            val isLoading: Boolean = false,
            val authenticationDigits: String? = null,
            val authenticatingEndpointId: String? = null,
            val isHybridMode: Boolean = false,
            val isDutyCycleActive: Boolean = false, // Added for UI sync
            val isUwbSupported: Boolean = false, // Added for hardware check
            val isUwbPermissionGranted: Boolean = false, // Added for permission check
            val knownPeers: Map<String, RouteInfo> = emptyMap() // Added for Mesh Contacts
    )

    private val _state = MutableStateFlow(P2PState())
    val state: StateFlow<P2PState> = _state.asStateFlow()

    // Payload Event Stream
    data class PayloadEvent(val endpointId: String, val packet: Packet)
    private val _payloadEvents = MutableSharedFlow<PayloadEvent>()
    val payloadEvents: SharedFlow<PayloadEvent> = _payloadEvents.asSharedFlow()

    // Payload Progress Stream
    data class PayloadProgressEvent(
            val endpointId: String,
            val progress: Int,
            val totalBytes: Long
    )
    private val _payloadProgressEvents = MutableSharedFlow<PayloadProgressEvent>()
    val payloadProgressEvents: SharedFlow<PayloadProgressEvent> =
            _payloadProgressEvents.asSharedFlow()

    // Bandwidth Event Stream
    private val _bandwidthEvents = MutableSharedFlow<BandwidthInfo>()
    val bandwidthEvents: SharedFlow<BandwidthInfo> = _bandwidthEvents.asSharedFlow()

    fun getLocalDeviceName(): String = localUsername

    init {
        // Check for UWB Hardware Support on init
        val hasUwb = context.packageManager.hasSystemFeature("android.hardware.uwb")
        _state.update { it.copy(isUwbSupported = hasUwb) }

        // Ensure clean state on startup
        connectionsClient.stopAllEndpoints()
    }

    fun setLowPower(enabled: Boolean) {
        _state.update { it.copy(isLowPower = enabled) }
        log("Low Power Mode set to $enabled. Restart advertising/discovery to apply.")
    }

    fun setHybridMode(enabled: Boolean) {
        _state.update { it.copy(isHybridMode = enabled) }
        log("Hybrid Mode set to $enabled. Restart advertising/discovery to apply.")
    }

    fun updateUwbPermission(granted: Boolean) {
        _state.update { it.copy(isUwbPermissionGranted = granted) }
        if (!granted) {
            log("UWB Permission revoked. Radar disabled.", "WARN")
        } else {
            log("UWB Permission granted.")
        }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
        scope.launch {
            // Optional: Clear database logs if needed, or just clear UI logs
            // logDao.deleteAll() // Uncomment if we want to clear DB too
        }
        log("Logs cleared from UI.")
    }

    private fun getBatteryLevel(): Int {
        return try {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as android.os.BatteryManager
            bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
        } catch (e: Exception) {
            log("Failed to get battery level: ${e.message}", "WARN")
            100 // Default to 100 if we can't get it
        }
    }

    fun startAdvertising() {
        if (_state.value.isLoading || _state.value.isAdvertising) return
        _state.update { it.copy(isLoading = true) }

        val lowPower = _state.value.isLowPower
        val optionsBuilder =
                AdvertisingOptions.Builder().setStrategy(STRATEGY).setLowPower(lowPower)

        // Resilience Mode: If Low Power is OFF (meaning high performance/emergency), use DISRUPTIVE
        // Hybrid Mode: Prioritize LAN/BLE (NON_DISRUPTIVE)
        if (_state.value.isHybridMode) {
            optionsBuilder.setConnectionType(ConnectionType.NON_DISRUPTIVE)
        } else if (!lowPower) {
            optionsBuilder.setConnectionType(ConnectionType.DISRUPTIVE)
        } else {
            optionsBuilder.setConnectionType(ConnectionType.BALANCED)
        }

        val options = optionsBuilder.build()

        // Metadata: Flags|Battery|Name
        // Flags: Bit 0=Internet, Bit 1=Charging
        val battery = getBatteryLevel()
        val metadata = "1|$battery|$localUsername"

        connectionsClient
                .startAdvertising(metadata, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener {
                    log("Advertising started (LowPower=$lowPower). Battery: $battery%")
                    _state.update { it.copy(isAdvertising = true, isLoading = false) }
                }
                .addOnFailureListener { e ->
                    handleApiError(e, "Advertising")
                    _state.update { it.copy(isLoading = false) }
                }
    }

    fun startDiscovery() {
        if (_state.value.isLoading || _state.value.isDiscovering) return
        _state.update { it.copy(isLoading = true) }

        val lowPower = _state.value.isLowPower
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).setLowPower(lowPower).build()

        connectionsClient
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    log("Discovery started (LowPower=$lowPower).")
                    _state.update { it.copy(isDiscovering = true, isLoading = false) }
                }
                .addOnFailureListener { e ->
                    handleApiError(e, "Discovery")
                    _state.update { it.copy(isLoading = false) }
                }
    }

    // Duty Cycling
    private var dutyCycleJob: kotlinx.coroutines.Job? = null

    fun setDutyCycle(enabled: Boolean) {
        _state.update { it.copy(isDutyCycleActive = enabled) }

        if (enabled) {
            if (dutyCycleJob != null) return
            log("Starting Duty Cycle (Resilient Mode)...")
            dutyCycleJob =
                    scope.launch {
                        while (true) {
                            // Phase 1: Discovery (Listen)
                            // Randomize duration (10s - 15s) to avoid phase synchronization
                            val discoveryDuration = (10000..15000).random().toLong()

                            if (!_state.value.isDiscovering) startDiscovery()
                            if (_state.value.isAdvertising) stopAdvertising()
                            kotlinx.coroutines.delay(discoveryDuration)

                            // Phase 2: Advertising (Announce)
                            // Randomize duration (10s - 15s)
                            val advertisingDuration = (10000..15000).random().toLong()

                            if (_state.value.isDiscovering) stopDiscovery()
                            if (!_state.value.isAdvertising) startAdvertising()
                            kotlinx.coroutines.delay(advertisingDuration)
                        }
                    }
        } else {
            log("Stopping Duty Cycle.")
            dutyCycleJob?.cancel()
            dutyCycleJob = null
            stopAdvertising()
            stopDiscovery()
        }
    }

    private fun handleApiError(e: Exception, context: String) {
        if (e is com.google.android.gms.common.api.ApiException) {
            when (e.statusCode) {
                ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING -> {
                    _state.update { it.copy(isAdvertising = true) }
                    log("$context: Already running.")
                }
                ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING -> {
                    _state.update { it.copy(isDiscovering = true) }
                    log("$context: Already running.")
                }
                ConnectionsStatusCodes.MISSING_PERMISSION_NEARBY_WIFI_DEVICES -> {
                    log("$context: Missing NEARBY_WIFI_DEVICES permission.", "ERROR")
                }
                ConnectionsStatusCodes.STATUS_RADIO_ERROR -> {
                    log("$context: Radio Error. Try restarting Bluetooth/WiFi.", "ERROR")
                }
                else ->
                        log(
                                "$context failed: ${ConnectionsStatusCodes.getStatusCodeString(e.statusCode)}",
                                "ERROR"
                        )
            }
        } else {
            log("$context failed: ${e.message}", "ERROR")
        }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _state.update { it.copy(isAdvertising = false) }
        log("Advertising stopped.")
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _state.update { it.copy(isDiscovering = false) }
        log("Discovery stopped.")
    }

    fun stopAll() {
        connectionsClient.stopAllEndpoints()
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        // Reset advanced states
        setDutyCycle(false)

        _state.update {
            it.copy(
                    isAdvertising = false,
                    isDiscovering = false,
                    connectedEndpoints = emptySet(),
                    isLoading = false
            )
        }
        log("Stopped all connections and reset state.")
    }

    // Reliability & Mesh
    private val pendingAcks = java.util.concurrent.ConcurrentHashMap<String, Packet>()
    private val RETRY_LIMIT = 3
    private val TIMEOUT_MS = 5000L

    private val neighbors =
            java.util.concurrent.ConcurrentHashMap<String, com.fyp.resilientp2p.data.Neighbor>()

    // Routing Table: TargetID -> RouteInfo
    data class RouteInfo(val nextHop: String, val hopCount: Int, val lastSeen: Long)
    private val routingTable = java.util.concurrent.ConcurrentHashMap<String, RouteInfo>()

    private val messageCache = com.fyp.resilientp2p.transport.MessageCache()

    private val activeFilePayloads = java.util.concurrent.ConcurrentHashMap<Long, Long>()
    private var stallCheckJob: kotlinx.coroutines.Job? = null
    private val STALL_TIMEOUT_MS = 15000L
    private val MAX_BYTES_DATA_SIZE = 32 * 1024

    var uwbManager: UwbManager? = null

    fun sendData(endpointId: String, bytes: ByteArray) {
        if (bytes.size > MAX_BYTES_DATA_SIZE) {
            log(
                    "Packet too large (${bytes.size} bytes). Limit is $MAX_BYTES_DATA_SIZE. Dropping.",
                    "ERROR"
            )
            return
        }
        val packet =
                Packet(
                        type = PacketType.DATA,
                        sourceId = localUsername,
                        destId = endpointId,
                        payload = bytes
                )
        sendPacketReliably(packet)
    }

    fun sendUwbAddress(endpointId: String, address: ByteArray) {
        val packet =
                Packet(
                        type = PacketType.UWB_ADDRESS,
                        sourceId = localUsername,
                        destId = endpointId,
                        payload = address
                )
        sendPacketReliably(packet)
    }

    fun sendGossip() {
        // Create Gossip Payload: My ID + List of all nodes I can reach
        // Format: [Count:Int] [ID:String, Hops:Int]...
        val baos = java.io.ByteArrayOutputStream()
        val dos = java.io.DataOutputStream(baos)

        // Include direct neighbors (Hops=1)
        val allNodes = HashMap<String, Int>()
        neighbors.values.forEach { allNodes[it.peerName] = 1 }

        // Include routing table nodes (Hops=Route.Hops + 1)
        routingTable.forEach { (id, route) ->
            if (!allNodes.containsKey(id)) {
                allNodes[id] = route.hopCount + 1
            }
        }

        dos.writeInt(allNodes.size)
        allNodes.forEach { (id, hops) ->
            dos.writeUTF(id)
            dos.writeInt(hops)
        }

        val payload = baos.toByteArray()

        val packet =
                Packet(
                        type = PacketType.GOSSIP,
                        sourceId = localUsername,
                        destId = "BROADCAST",
                        payload = payload,
                        ttl = 1 // Gossip only goes to immediate neighbors
                )
        broadcastPacket(packet)
    }

    fun sendFile(file: java.io.File) {
        if (neighbors.isEmpty()) {
            log("No neighbors to send file to.", "WARN")
            return
        }

        val targetEndpoint = neighbors.keys.first()

        val payload = Payload.fromFile(file)
        activeFilePayloads[payload.id] = System.currentTimeMillis()
        startStallDetector()

        connectionsClient
                .sendPayload(targetEndpoint, payload)
                .addOnSuccessListener { log("Sending file ${file.name} to $targetEndpoint...") }
                .addOnFailureListener { e ->
                    log("Failed to send file: ${e.message}", "ERROR")
                    activeFilePayloads.remove(payload.id)
                }
    }

    private fun startStallDetector() {
        if (stallCheckJob != null) return
        stallCheckJob =
                scope.launch {
                    while (true) {
                        kotlinx.coroutines.delay(5000)
                        if (activeFilePayloads.isEmpty()) {
                            stallCheckJob = null
                            return@launch
                        }

                        val now = System.currentTimeMillis()
                        val stalled =
                                activeFilePayloads.filter { (_, lastUpdate) ->
                                    now - lastUpdate > STALL_TIMEOUT_MS
                                }

                        stalled.forEach { (id, _) ->
                            log("Payload $id stalled (>15s). Cancelling.", "WARN")
                            connectionsClient.cancelPayload(id)
                            activeFilePayloads.remove(id)
                        }
                    }
                }
    }

    private fun cancelAllFileTransfers() {
        if (activeFilePayloads.isNotEmpty()) {
            log(
                    "Cancelling ${activeFilePayloads.size} active file transfers for high-priority traffic.",
                    "WARN"
            )
            activeFilePayloads.keys.forEach { id -> connectionsClient.cancelPayload(id) }
            activeFilePayloads.clear()
        }
    }
    fun sendPing(endpointId: String, bytes: ByteArray) {
        cancelAllFileTransfers()

        // Get peer's username from neighbors map
        val peerName = neighbors[endpointId]?.peerName ?: endpointId

        val packet =
                Packet(
                        type = PacketType.PING,
                        sourceId = localUsername,
                        destId = peerName,
                        payload = bytes
                )
        if (neighbors.containsKey(endpointId)) {
            sendUnicastPacket(endpointId, packet)
        } else {
            broadcastPacket(packet)
        }
    }

    private fun sendAck(endpointId: String, packetId: String) {
        cancelAllFileTransfers()

        val packet =
                Packet(
                        type = PacketType.ACK,
                        sourceId = localUsername,
                        destId = endpointId,
                        payload = packetId.toByteArray()
                )
        if (neighbors.containsKey(endpointId)) {
            sendUnicastPacket(endpointId, packet)
        } else {
            broadcastPacket(packet)
        }
    }

    private fun sendPong(sourceId: String, originalPayload: ByteArray) {
        val packet =
                Packet(
                        type = PacketType.PONG,
                        sourceId = localUsername,
                        destId = sourceId,
                        payload = originalPayload
                )
        // Try to find direct route to source, otherwise broadcast
        val directEndpoint = neighbors.entries.find { it.value.peerName == sourceId }?.key
        if (directEndpoint != null) {
            sendUnicastPacket(directEndpoint, packet)
        } else {
            broadcastPacket(packet)
        }
    }

    private fun sendUnicastPacket(endpointId: String, packet: Packet) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(packet.toBytes()))
                .addOnFailureListener { e ->
                    log("Failed to send unicast packet to $endpointId: ${e.message}", "WARN")
                }
    }

    private fun broadcastPacket(packet: Packet, excludeEndpointId: String? = null) {
        messageCache.markSeen(packet.id)

        if (neighbors.containsKey(packet.destId) && packet.destId != excludeEndpointId) {
            sendUnicastPacket(packet.destId, packet)
            return
        }

        val targetEndpoints =
                neighbors.entries
                        .filter { (id, neighbor) ->
                            id != excludeEndpointId &&
                                    (packet.type != PacketType.DATA || neighbor.quality > 1)
                        }
                        .map { it.key }

        if (targetEndpoints.isEmpty()) return

        connectionsClient.sendPayload(targetEndpoints, Payload.fromBytes(packet.toBytes()))
                .addOnFailureListener { e -> log("Broadcast failed: ${e.message}", "WARN") }
    }

    private fun sendPacketReliably(packet: Packet, attempt: Int = 1) {
        pendingAcks[packet.id] = packet

        // SMART ROUTING
        val directEndpoint = neighbors.entries.find { it.value.peerName == packet.destId }?.key
        if (directEndpoint != null) {
            sendUnicastPacket(directEndpoint, packet)
        } else {
            val route = routingTable[packet.destId]
            if (route != null) {
                val nextHopEndpoint =
                        neighbors.entries.find { it.value.peerName == route.nextHop }?.key
                if (nextHopEndpoint != null) {
                    log("Routing packet for ${packet.destId} via ${route.nextHop}")
                    sendUnicastPacket(nextHopEndpoint, packet)
                } else {
                    log("Route broken for ${packet.destId}. Broadcasting.", "WARN")
                    broadcastPacket(packet)
                }
            } else {
                broadcastPacket(packet)
            }
        }

        scope.launch {
            kotlinx.coroutines.delay(TIMEOUT_MS)
            if (pendingAcks.containsKey(packet.id)) {
                if (attempt < RETRY_LIMIT) {
                    log(
                            "ACK not received for ${packet.id}. Retrying ($attempt/$RETRY_LIMIT)...",
                            "WARN"
                    )
                    sendPacketReliably(packet, attempt + 1)
                } else {
                    log(
                            "Failed to deliver packet ${packet.id} after $RETRY_LIMIT attempts.",
                            "ERROR"
                    )
                    pendingAcks.remove(packet.id)
                }
            }
        }
    }

    private val connectionLifecycleCallback =
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    // Extract peer name from metadata
                    val parts = info.endpointName.split("|")
                    val peerName = if (parts.size >= 3) parts[2] else info.endpointName

                    log(
                            "Connection initiated by $peerName. Auth Token: ${info.authenticationDigits}"
                    )
                    _state.update {
                        it.copy(
                                authenticationDigits = info.authenticationDigits,
                                authenticatingEndpointId = endpointId
                        )
                    }

                    // Store peer name
                    neighbors[endpointId] =
                            com.fyp.resilientp2p.data.Neighbor(endpointId, peerName = peerName)
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    when (result.status.statusCode) {
                        ConnectionsStatusCodes.STATUS_OK -> {
                            log("Connected to $endpointId")
                            // Extract peer name from endpoint metadata if available (will be
                            // updated later)
                            neighbors[endpointId] =
                                    com.fyp.resilientp2p.data.Neighbor(endpointId, peerName = "")
                            _state.update {
                                it.copy(
                                        connectedEndpoints = it.connectedEndpoints + endpointId,
                                        authenticationDigits = null,
                                        authenticatingEndpointId = null
                                )
                            }

                            // CHECK UWB SUPPORT BEFORE RANGING
                            if (_state.value.isUwbSupported) {
                                uwbManager?.startRanging(endpointId)
                            } else {
                                log("Skipping UWB ranging (HW Not Supported).")
                            }

                            scope.launch {
                                val pendingPackets = packetDao.getPacketsForPeer(endpointId)
                                if (pendingPackets.isNotEmpty()) {
                                    log(
                                            "Found ${pendingPackets.size} stored packets for $endpointId. Sending..."
                                    )
                                    pendingPackets.forEach { entity ->
                                        val packet =
                                                Packet(
                                                        id = entity.id,
                                                        type = PacketType.valueOf(entity.type),
                                                        timestamp = entity.timestamp,
                                                        sourceId = entity.sourceId,
                                                        destId = entity.destId,
                                                        payload = entity.payload,
                                                        ttl = 5,
                                                        trace = ArrayList(),
                                                        sequenceNumber = 0
                                                )
                                        sendPacketReliably(packet)
                                        packetDao.deletePacket(entity.id)
                                    }
                                }
                            }
                        }
                        else -> {
                            log("Connection to $endpointId failed: ${result.status.statusCode}")
                        }
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    log("Disconnected from $endpointId")
                    neighbors.remove(endpointId)
                    _state.update {
                        it.copy(connectedEndpoints = it.connectedEndpoints - endpointId)
                    }
                }

                override fun onBandwidthChanged(endpointId: String, bandwidthInfo: BandwidthInfo) {
                    val qualityStr =
                            when (bandwidthInfo.quality) {
                                BandwidthInfo.Quality.HIGH -> "HIGH"
                                BandwidthInfo.Quality.MEDIUM -> "MEDIUM"
                                BandwidthInfo.Quality.LOW -> "LOW"
                                else -> "UNKNOWN"
                            }
                    log("Bandwidth changed for $endpointId: $qualityStr")

                    scope.launch { _bandwidthEvents.emit(bandwidthInfo) }

                    val qualityScore =
                            when (bandwidthInfo.quality) {
                                BandwidthInfo.Quality.HIGH -> 3
                                BandwidthInfo.Quality.MEDIUM -> 2
                                BandwidthInfo.Quality.LOW -> 1
                                else -> 0
                            }
                    neighbors[endpointId]?.let { it.quality = qualityScore }
                }
            }

    private val endpointDiscoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    val parts = info.endpointName.split("|")
                    val peerName = if (parts.size >= 3) parts[2] else info.endpointName
                    val flags = if (parts.size >= 3) parts[0] else "0"
                    val battery = if (parts.size >= 3) parts[1] else "0"

                    log("Found $peerName ($endpointId). Flags=$flags, Bat=$battery%")

                    // FIX: Always request connection. The API handles race conditions.
                    // We no longer wait for the "Dominant" peer to initiate.
                    log("Requesting connection to $peerName...")
                    connectionsClient.stopDiscovery()

                    connectionsClient
                            .requestConnection(
                                    "1|${getBatteryLevel()}|$localUsername",
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnSuccessListener {
                                // Store peer name for later use
                                neighbors[endpointId] =
                                        com.fyp.resilientp2p.data.Neighbor(
                                                endpointId,
                                                peerName = peerName
                                        )
                            }
                            .addOnFailureListener { e ->
                                log("Request connection failed: ${e.message}")
                                // If request fails, restart discovery to try again or find others
                                startDiscovery()
                            }
                }

                override fun onEndpointLost(endpointId: String) {
                    log("Lost endpoint: $endpointId")
                }
            }

    private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    when (payload.type) {
                        Payload.Type.BYTES -> {
                            val bytes = payload.asBytes() ?: return
                            try {
                                val packet = Packet.fromBytes(bytes)
                                handlePacket(packet, endpointId)
                            } catch (e: Exception) {
                                log(
                                        "Failed to parse packet from $endpointId: ${e.message}",
                                        "ERROR"
                                )
                            }
                        }
                        Payload.Type.FILE -> {
                            log("Received file payload from $endpointId. ID: ${payload.id}")
                            val file = payload.asFile()?.asUri()?.path?.let { java.io.File(it) }
                            if (file != null) {
                                log("File received at: ${file.absolutePath}")
                            }
                        }
                        else -> {
                            payload.close()
                            log(
                                    "Received unsupported payload type from $endpointId. Closed.",
                                    "WARN"
                            )
                        }
                    }
                }

                override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                ) {
                    neighbors[endpointId]?.lastSeen = System.currentTimeMillis()

                    if (update.status == PayloadTransferUpdate.Status.SUCCESS ||
                                    update.status == PayloadTransferUpdate.Status.FAILURE ||
                                    update.status == PayloadTransferUpdate.Status.CANCELED
                    ) {
                        activeFilePayloads.remove(update.payloadId)
                    } else if (update.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                        if (activeFilePayloads.containsKey(update.payloadId)) {
                            activeFilePayloads[update.payloadId] = System.currentTimeMillis()
                        }
                    }

                    if (update.totalBytes > 0) {
                        val progress = (update.bytesTransferred * 100 / update.totalBytes).toInt()
                        scope.launch {
                            _payloadProgressEvents.emit(
                                    PayloadProgressEvent(endpointId, progress, update.totalBytes)
                            )
                        }
                    }

                    if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                        log("Payload transfer to $endpointId failed.", "WARN")
                    }
                }
            }

    private fun handlePacket(packet: Packet, fromEndpointId: String) {
        if (messageCache.hasSeen(packet.id)) {
            return
        }
        messageCache.markSeen(packet.id)

        neighbors[fromEndpointId]?.lastSeen = System.currentTimeMillis()

        // Special handling for PING: always process AND forward for mesh-wide testing
        if (packet.type == PacketType.PING) {
            processPacket(packet)
            forwardPacket(packet, fromEndpointId)
        } else if (packet.destId == localUsername || packet.destId == "BROADCAST") {
            processPacket(packet)
        } else {
            forwardPacket(packet, fromEndpointId)
        }
    }

    private fun processPacket(packet: Packet) {
        when (packet.type) {
            PacketType.DATA -> {
                sendAck(packet.sourceId, packet.id)
                scope.launch { _payloadEvents.emit(PayloadEvent(packet.sourceId, packet)) }
            }
            PacketType.ACK -> {
                val ackedId = String(packet.payload)
                if (pendingAcks.remove(ackedId) != null) {
                    log("ACK received for $ackedId")
                }
            }
            PacketType.PING -> {
                sendPong(packet.sourceId, packet.payload)
                log("Received PING from ${packet.sourceId}. Sent PONG.")
            }
            PacketType.PONG -> {
                scope.launch { _payloadEvents.emit(PayloadEvent(packet.sourceId, packet)) }
            }
            PacketType.UWB_ADDRESS -> {
                uwbManager?.onPeerAddressReceived(packet.sourceId, packet.payload)
            }
            PacketType.GOSSIP -> {
                try {
                    val bais = java.io.ByteArrayInputStream(packet.payload)
                    val dis = java.io.DataInputStream(bais)
                    val count = dis.readInt()
                    val senderName = packet.sourceId

                    for (i in 0 until count) {
                        val nodeId = dis.readUTF()
                        val hops = dis.readInt()
                        // Don't add self or direct neighbors (unless better route?)
                        if (nodeId != localUsername) {
                            val currentRoute = routingTable[nodeId]
                            if (currentRoute == null || currentRoute.hopCount > hops) {
                                routingTable[nodeId] =
                                        RouteInfo(senderName, hops, System.currentTimeMillis())
                                log("Updated route to $nodeId via $senderName ($hops hops)")
                            }
                        }
                    }
                    // Update UI State with new routes
                    _state.update { it.copy(knownPeers = HashMap(routingTable)) }
                } catch (e: Exception) {
                    log("Failed to parse GOSSIP: ${e.message}", "ERROR")
                }
            }
        }
    }

    private fun forwardPacket(packet: Packet, fromEndpointId: String) {
        if (packet.ttl <= 0) {
            log("Packet ${packet.id} dropped (TTL expired).", "WARN")
            return
        }

        val newTrace = packet.trace + com.fyp.resilientp2p.transport.Hop(localUsername, 0)
        val forwardedPacket = packet.copy(ttl = packet.ttl - 1, trace = newTrace)

        // SMART ROUTING
        val directEndpoint =
                neighbors.entries.find { it.value.peerName == forwardedPacket.destId }?.key
        if (directEndpoint != null) {
            sendUnicastPacket(directEndpoint, forwardedPacket)
        } else {
            val route = routingTable[forwardedPacket.destId]
            if (route != null) {
                val nextHopEndpoint =
                        neighbors.entries.find { it.value.peerName == route.nextHop }?.key
                if (nextHopEndpoint != null) {
                    log("Forwarding packet for ${forwardedPacket.destId} to ${route.nextHop}")
                    sendUnicastPacket(nextHopEndpoint, forwardedPacket)
                } else {
                    log("Route broken for ${forwardedPacket.destId}. Broadcasting.", "WARN")
                    broadcastPacket(forwardedPacket, excludeEndpointId = fromEndpointId)
                }
            } else {
                broadcastPacket(forwardedPacket, excludeEndpointId = fromEndpointId)
            }
        }
    }

    private fun storePacket(packet: Packet) {
        scope.launch {
            try {
                val entity =
                        com.fyp.resilientp2p.data.PacketEntity(
                                id = packet.id,
                                destId = packet.destId,
                                type = packet.type.name,
                                payload = packet.payload,
                                timestamp = packet.timestamp,
                                expiration = System.currentTimeMillis() + (packet.ttl * 10000),
                                sourceId = packet.sourceId
                        )
                packetDao.insertPacket(entity)
            } catch (e: Exception) {
                log("Failed to store packet: ${e.message}", "ERROR")
            }
        }
    }

    fun getNeighborsSnapshot(): Map<String, com.fyp.resilientp2p.data.Neighbor> {
        return HashMap(neighbors)
    }

    fun disconnectFromEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        neighbors.remove(endpointId)
        _state.update { it.copy(connectedEndpoints = it.connectedEndpoints - endpointId) }
        log("Disconnected from $endpointId (Requested)")
    }

    fun log(msg: String, type: String = "INFO") {
        Log.d(TAG, msg)
        _state.update { it.copy(logs = it.logs + msg) }

        scope.launch { logDao.insert(LogEntry(type = type, message = msg)) }
    }
}
