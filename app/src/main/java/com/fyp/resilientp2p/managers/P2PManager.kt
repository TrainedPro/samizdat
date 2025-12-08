package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.Neighbor
import com.fyp.resilientp2p.data.P2PState
import com.fyp.resilientp2p.data.RouteInfo
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.BandwidthInfo
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class P2PManager(private val context: Context) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // State
    private val _state = MutableStateFlow(P2PState())
    val state: StateFlow<P2PState> = _state.asStateFlow()

    // Events
    private val _payloadEvents = MutableSharedFlow<PayloadEvent>()
    val payloadEvents: SharedFlow<PayloadEvent> = _payloadEvents.asSharedFlow()

    private val _payloadProgressEvents = MutableSharedFlow<PayloadProgressEvent>()
    val payloadProgressEvents: SharedFlow<PayloadProgressEvent> =
            _payloadProgressEvents.asSharedFlow()

    private val _bandwidthEvents = MutableSharedFlow<BandwidthInfo>()
    val bandwidthEvents: SharedFlow<BandwidthInfo> = _bandwidthEvents.asSharedFlow()

    // Mesh Data
    private val neighbors = ConcurrentHashMap<String, Neighbor>() // endpointId -> Neighbor
    private val routingTable = ConcurrentHashMap<String, String>() // destId -> nextHopEndpointId
    private val messageCache = ConcurrentHashMap<String, Long>() // packetId -> timestamp

    // Configuration
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.fyp.resilientp2p"
    private val MAX_CONNECTIONS = 4
    private val STABILITY_WINDOW_MS = 15000L

    // Jobs

    // Optional Managers
    var uwbManager: UwbManager? = null
    var voiceManager: VoiceManager? = null

    // Local Info
    private val localUsername = android.os.Build.MODEL

    // Data Classes for UI
    data class PayloadEvent(val endpointId: String, val packet: Packet)
    data class PayloadProgressEvent(val endpointId: String, val progress: Int)

    // --- Initialization ---

    init {
        voiceManager = VoiceManager(context)
        if (_state.value.isMeshMaintenanceActive) {
            startMeshMaintenance()
        }
    }

    fun start() {
        log("Starting P2PManager with Automated Pulse Mesh...")
        startMeshMaintenance()
    }

    fun stop() {
        stopAll()
    }

    fun stopAll() {
        log("Stopping All Activity...")
        stopMeshMaintenance()
        connectionsClient.stopAllEndpoints()
        neighbors.clear()
        routingTable.clear()
        _state.value = P2PState()
    }

    // --- Mesh Maintenance (The "Pulse") ---

    fun setDutyCycle(enabled: Boolean) {
        if (enabled) {
            startMeshMaintenance()
        } else {
            stopMeshMaintenance()
        }
        updateState { it.copy(isMeshMaintenanceActive = enabled) }
    }

    private fun startMeshMaintenance() {
        stopMeshMaintenance()
        log("Starting Always-On Mesh (Advertising + Discovery)")
        startAdvertising()
        startDiscovery()
    }

    private fun stopMeshMaintenance() {
        stopRadioActivity()
    }

    private fun stopRadioActivity() {
        connectionsClient.stopDiscovery()
        connectionsClient.stopAdvertising()
        updateState { it.copy(isDiscovering = false, isAdvertising = false) }
    }

    // --- Nearby Connections Actions ---

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient
                .startAdvertising(
                        localUsername,
                        SERVICE_ID,
                        connectionLifecycleCallback,
                        advertisingOptions
                )
                .addOnSuccessListener {
                    log("Advertising Started")
                    updateState { it.copy(isAdvertising = true) }
                }
                .addOnFailureListener { e ->
                    log("Advertising Failed: ${e.message}")
                    updateState { it.copy(isAdvertising = false) }
                }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener {
                    log("Discovery Started")
                    updateState { it.copy(isDiscovering = true) }
                }
                .addOnFailureListener { e ->
                    log("Discovery Failed: ${e.message}")
                    updateState { it.copy(isDiscovering = false) }
                }
    }

    // --- Callbacks ---

    private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    when (payload.type) {
                        Payload.Type.BYTES -> {
                            payload.asBytes()?.let { bytes ->
                                try {
                                    val packet = Packet.fromBytes(bytes)
                                    handlePacket(packet, endpointId)
                                } catch (e: Exception) {
                                    log("Error parsing packet: ${e.message}")
                                }
                            }
                        }
                        Payload.Type.FILE -> {
                            log("Receiving file from $endpointId: ID=${payload.id}")
                            // The file is automatically saved to the Downloads directory by Nearby
                            // Connections
                            // We just need to track it or notify the user
                        }
                        Payload.Type.STREAM -> {
                            log("Receiving audio stream from $endpointId")
                            payload.asStream()?.asInputStream()?.let { inputStream ->
                                voiceManager?.startPlaying(inputStream)
                            }
                        }
                        else -> log("Unhandled Payload Type: ${payload.type}")
                    }
                }

                override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                ) {
                    scope.launch {
                        _payloadProgressEvents.emit(
                                PayloadProgressEvent(
                                        endpointId,
                                        if (update.totalBytes > 0)
                                                ((update.bytesTransferred * 100) /
                                                                update.totalBytes)
                                                        .toInt()
                                        else 0
                                )
                        )
                    }
                }
            }

    private val connectionLifecycleCallback =
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    log("Connection Initiated: $endpointId (${info.endpointName})")
                    if (info.endpointName == localUsername || endpointId == localUsername) {
                        log("Ignoring self connection: $endpointId")
                        connectionsClient.rejectConnection(endpointId)
                        return
                    }
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                    val newConnecting = _state.value.connectingEndpoints + endpointId
                    updateState { it.copy(connectingEndpoints = newConnecting) }
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    val newConnecting = _state.value.connectingEndpoints - endpointId
                    updateState { it.copy(connectingEndpoints = newConnecting) }

                    if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                        log("Connection Established: $endpointId")
                        // stopRadioActivity() // REMOVED: Always-On Strategy

                        val neighbor =
                                Neighbor(endpointId, "Unknown", 0, System.currentTimeMillis())
                        neighbors[endpointId] = neighbor
                        updateConnectedEndpoints()
                        sendIdentityPacket(endpointId)

                        // Start UWB if available
                        uwbManager?.startRanging(endpointId)
                    } else {
                        log("Connection Failed: $endpointId, Code: ${result.status.statusCode}")
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    log("Disconnected: $endpointId")
                    neighbors.remove(endpointId)
                    routingTable.entries.removeIf { it.value == endpointId }
                    updateConnectedEndpoints()
                    uwbManager?.stopRanging()
                }

                override fun onBandwidthChanged(endpointId: String, bandwidthInfo: BandwidthInfo) {
                    scope.launch { _bandwidthEvents.emit(bandwidthInfo) }
                }
            }

    private val endpointDiscoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    log("Endpoint Found: $endpointId (${info.endpointName})")
                    if (info.endpointName == localUsername || endpointId == localUsername) {
                        log("Ignoring self: $endpointId")
                        return
                    }
                    if (neighbors.containsKey(endpointId) ||
                                    _state.value.connectingEndpoints.contains(endpointId)
                    )
                            return

                    connectionsClient
                            .requestConnection(
                                    localUsername,
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnSuccessListener { log("Connection Requested to $endpointId") }
                            .addOnFailureListener {
                                log("Request Connection Failed: ${it.message}")
                            }
                }

                override fun onEndpointLost(endpointId: String) {
                    log("Endpoint Lost: $endpointId")
                }
            }

    // --- Packet Handling ---

    private fun handlePacket(packet: Packet, sourceEndpointId: String) {
        if (messageCache.containsKey(packet.id)) return
        messageCache[packet.id] = System.currentTimeMillis()

        if (sourceEndpointId != "LOCAL") {
            routingTable[packet.sourceId] = sourceEndpointId
        }

        if (packet.destId == localUsername || packet.destId == "BROADCAST") {
            processPacket(packet, sourceEndpointId)
        }

        if (packet.destId != localUsername && packet.ttl > 0) {
            forwardPacket(packet)
        }
    }

    private fun processPacket(packet: Packet, sourceEndpointId: String) {
        scope.launch { _payloadEvents.emit(PayloadEvent(sourceEndpointId, packet)) }

        when (packet.type) {
            PacketType.DATA -> log("Message: ${String(packet.payload, StandardCharsets.UTF_8)}")
            PacketType.IDENTITY -> {
                neighbors[routingTable[packet.sourceId]]?.let {
                    val updated = it.copy(peerName = packet.sourceId)
                    neighbors[routingTable[packet.sourceId]!!] = updated
                    updateConnectedEndpoints()
                }
            }
            else -> {}
        }
    }

    private fun forwardPacket(packet: Packet) {
        val nextHop = routingTable[packet.destId]
        val newPacket = packet.copy(ttl = packet.ttl - 1)
        val bytes = newPacket.toBytes()
        val payload = Payload.fromBytes(bytes)

        if (nextHop != null && neighbors.containsKey(nextHop)) {
            connectionsClient.sendPayload(nextHop, payload)
        } else {
            neighbors.keys.forEach { endpointId ->
                connectionsClient.sendPayload(endpointId, payload)
            }
        }
    }

    // --- Public Methods ---

    fun sendData(destId: String, message: String) {
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.DATA,
                        sourceId = localUsername,
                        destId = destId,
                        payload = message.toByteArray(StandardCharsets.UTF_8),
                        timestamp = System.currentTimeMillis()
                )
        handlePacket(packet, "LOCAL")
    }

    fun broadcastMessage(message: String) {
        sendData("BROADCAST", message)
    }

    fun sendPing(peerId: String, data: ByteArray) {
        // Basic ping implementation
        sendData(peerId, "PING")
    }

    fun sendUwbAddress(peerId: String, address: ByteArray) {
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.UWB_ADDRESS,
                        sourceId = localUsername,
                        destId = peerId,
                        payload = address,
                        timestamp = System.currentTimeMillis()
                )
        handlePacket(packet, "LOCAL")
    }

    fun sendFile(peerId: String, uri: android.net.Uri) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val payload = Payload.fromFile(pfd)
                connectionsClient.sendPayload(peerId, payload)
                log("Sending file to $peerId")
            } else {
                log("Error: Could not open file descriptor")
            }
        } catch (e: Exception) {
            log("Error sending file: ${e.message}")
        }
    }

    fun startAudioStreaming(peerId: String) {
        log("Starting audio stream to $peerId")
        voiceManager?.startRecording()?.let { pfd ->
            val payload = Payload.fromStream(pfd)
            connectionsClient.sendPayload(peerId, payload)
        }
                ?: log("Failed to start audio recording")
    }

    fun stopAudioStreaming() {
        log("Audio Streaming stopped")
        voiceManager?.stopRecording()
    }

    fun getLocalDeviceName(): String = localUsername

    fun disconnectFromEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        connectionLifecycleCallback.onDisconnected(endpointId)
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    fun setHybridMode(enabled: Boolean) {
        updateState { it.copy(isHybridMode = enabled) }
    }

    fun setManualConnection(enabled: Boolean) {
        updateState { it.copy(isManualConnectionEnabled = enabled) }
    }

    fun setLowPower(enabled: Boolean) {
        updateState { it.copy(isLowPower = enabled) }
    }

    fun updateUwbPermission(granted: Boolean) {
        updateState { it.copy(isUwbPermissionGranted = granted) }
    }

    fun clearLogs() {
        updateState { it.copy(logs = emptyList()) }
    }

    // --- Helpers ---

    fun getNeighborsSnapshot(): Map<String, Neighbor> {
        return HashMap(neighbors)
    }

    private fun sendIdentityPacket(endpointId: String) {
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.IDENTITY,
                        sourceId = localUsername,
                        destId = "BROADCAST",
                        payload = ByteArray(0),
                        timestamp = System.currentTimeMillis()
                )
        val bytes = packet.toBytes()
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
    }

    private fun updateState(update: (P2PState) -> P2PState) {
        _state.value = update(_state.value)
    }

    private fun updateConnectedEndpoints() {
        val neighborList = neighbors.values.map { it.peerName }.toList()
        val knownPeersMap =
                routingTable.entries.associate { (dest, nextHop) -> dest to RouteInfo(nextHop, 1) }
        updateState { it.copy(connectedEndpoints = neighborList, knownPeers = knownPeersMap) }
    }

    fun log(msg: String) {
        Log.d("P2PManager", msg)
        val newLogs = (_state.value.logs + msg).takeLast(100)
        updateState { it.copy(logs = newLogs) }
    }
}
