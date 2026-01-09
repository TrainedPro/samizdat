package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.Neighbor
import com.fyp.resilientp2p.data.P2PState
import com.fyp.resilientp2p.data.RouteInfo
import com.fyp.resilientp2p.transport.MessageCache
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
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

class P2PManager(private val context: Context) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private var supervisorJob = SupervisorJob()
    private var scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private var routingJob: kotlinx.coroutines.Job? = null

    // Local Info
    // Use a unique name to prevent self-connection and manage duplicate links
    private val localUsername: String by lazy {
        val prefs = context.getSharedPreferences("p2p_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: run {
            val newId = "${android.os.Build.MODEL}-${UUID.randomUUID().toString().take(8)}"
            prefs.edit().putString("device_id", newId).apply()
            newId
        }
    }

    // State
    private val _state = MutableStateFlow(P2PState(localDeviceName = localUsername))
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
    private val routingScores = ConcurrentHashMap<String, Int>() // destId -> routeScore
    private val routingLock = Any()
    private val routeLastSeen = ConcurrentHashMap<String, Long>() // destId -> timestamp
    private val messageCache = MessageCache(capacity = 2000) // 2000 items capacity

    // Configuration
    private val STRATEGY = Strategy.P2P_CLUSTER
    private val SERVICE_ID = "com.fyp.resilientp2p"
    private val MAX_CONNECTIONS = 4
    private val STABILITY_WINDOW_MS = 15000L

    // Track restartRadio job to prevent overlapping restarts
    private var radioRestartJob: kotlinx.coroutines.Job? = null
    // Jobs

    // Optional Managers
    var voiceManager: VoiceManager? = null

    // Data Classes for UI
    data class PayloadEvent(val endpointId: String, val packet: Packet)
    data class PayloadProgressEvent(val endpointId: String, val progress: Int)

    // --- Callbacks ---

    private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    log(
                            "onPayloadReceived from $endpointId. In neighbors? ${neighbors.containsKey(endpointId)}",
                            com.fyp.resilientp2p.data.LogLevel.TRACE
                    )

                    // Update lastSeen for ANY activity (Bytes, Stream, File) to prevent Zombie
                    // detection during calls
                    neighbors.computeIfPresent(endpointId) { _, neighbor -> neighbor.lastSeen.set(System.currentTimeMillis()); neighbor }

                    if (payload.type == Payload.Type.BYTES) {
                        try {
                            payload.asBytes()?.let { bytes ->
                                val packet = Packet.fromBytes(bytes)
                                try { handlePacket(packet, endpointId) } catch (e: Exception) { log("Error handling packet: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR) }
                            }
                        } catch (e: Exception) {
                            log("Error parsing packet from $endpointId: ${e.message}")
                        }
                    } else if (payload.type == Payload.Type.STREAM) {
                        log("Receiving audio stream from $endpointId")
                        payload.asStream()?.asInputStream()?.let { inputStream ->
                            // TOCTOU FIX: Capture voiceManager reference locally
                            val vm = voiceManager
                            if (vm != null) {
                                try {
                                    vm.startPlaying(inputStream)
                                } catch (e: Exception) {
                                    inputStream.close()
                                    log("Failed to start audio playback: ${e.message}")
                                }
                            } else {
                                inputStream.close()  // Close if not used to prevent leak
                            }
                        }
                    } else {
                        log("Unhandled Payload Type from $endpointId: ${payload.type}")
                    }
                }

                override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                ) {
                    // Keep updating lastSeen during long transfers (like Audio Stream or Big File)
                    if (update.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                        neighbors.computeIfPresent(endpointId) { _, neighbor -> neighbor.lastSeen.set(System.currentTimeMillis()); neighbor }
                    }

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

                    // Limit Connections (synchronized to prevent TOCTOU race)
                    synchronized(routingLock) {
                        if (neighbors.size >= MAX_CONNECTIONS) {
                            log("Max connections ($MAX_CONNECTIONS) reached. Rejecting $endpointId")
                            connectionsClient.rejectConnection(endpointId)
                            return
                        }
                    }

                    // Critical Self-Check
                    if (info.endpointName == localUsername) {
                        log("Refusing self-connection from $endpointId")
                        connectionsClient.rejectConnection(endpointId)
                        return
                    }

                    // duplicate check logic
                    // If we see a duplicate name, we assume the NEW one is the valid replacement
                    // (e.g. the old one timed out but we didn't get onDisconnected yet).
                    // We disconnect the OLD one and accept the NEW one.
                    val existingId =
                            neighbors.entries.find { it.value.peerName == info.endpointName }?.key
                    if (existingId != null) {
                        val existingNeighbor = neighbors[existingId]
                        val isAlive =
                                existingNeighbor != null &&
                                        (System.currentTimeMillis() - existingNeighbor.lastSeen.get() <
                                                STABILITY_WINDOW_MS)

                        // Tie-Breaker Logic:
                        // If the existing connection is "Alive", we only replace it if the NEW
                        // endpointId is "Greater" than the OLD one.
                        // This prevents "Death Spirals" where two devices constantly disconnect
                        // each other.
                        // If it's Stale, we always replace it.
                        if (isAlive && existingId > endpointId) {
                            log(
                                    "Refusing duplicate connection to ${info.endpointName} (Existing $existingId is ALIVE & > New $endpointId). Keeping stable link."
                            )
                            connectionsClient.rejectConnection(endpointId)
                            return
                        }

                        log(
                                "Duplicate Connection from '${info.endpointName}'. Replacing Old($existingId) with New($endpointId). (Alive=$isAlive, Replace Strategy)",
                                com.fyp.resilientp2p.data.LogLevel.WARN
                        )
                        // Proactively cleanup the old "Zombie"
                        connectionsClient.disconnectFromEndpoint(existingId)
                        neighbors.remove(existingId)
                        // Also clean routes immediately - SYNCHRONIZED
                        synchronized(routingLock) {
                            val lostDestinations = routingTable.filterValues { it == existingId }.keys.toList()
                            lostDestinations.forEach { dest ->
                                routingTable.remove(dest)
                                routingScores.remove(dest)
                                routeLastSeen.remove(dest)
                            }
                        }
                        updateConnectedEndpoints()
                        // Proceed to accept the new one below...
                    }

                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                    // ATOMIC UPDATE: Read-modify-write inside lambda
                    updateState { it.copy(connectingEndpoints = it.connectingEndpoints + endpointId) }
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    // ATOMIC UPDATE: Read-modify-write inside lambda
                    updateState { it.copy(connectingEndpoints = it.connectingEndpoints - endpointId) }

                    if (result.status.statusCode == ConnectionsStatusCodes.STATUS_OK) {
                        log("Connection Established: $endpointId")

                        // ATOMIC: Use compute() to avoid TOCTOU race
                        // ATOMIC: Preserve existing neighbor, only create if new
                        neighbors.compute(endpointId) { _, existing ->
                            if (existing != null) {
                                existing.lastSeen.set(System.currentTimeMillis())
                                existing // Preserve existing neighbor with its name
                            } else {
                                Neighbor(endpointId, "Unknown", 0, System.currentTimeMillis())
                            }
                        }
                        updateConnectedEndpoints()
                        sendIdentityPacket(endpointId)
                    } else {
                        log("Connection Failed: $endpointId, Code: ${result.status.statusCode}")
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    log("Disconnected: $endpointId")
                    neighbors.remove(endpointId)
                    // Remove all routes that went through this neighbor - SYNCHRONIZED
                    synchronized(routingLock) {
                        val lostDestinations = routingTable.filterValues { it == endpointId }.keys.toList()
                        lostDestinations.forEach { dest ->
                            routingTable.remove(dest)
                            routingScores.remove(dest)
                            routeLastSeen.remove(dest)
                        }
                    }
                    updateConnectedEndpoints()
                }

                override fun onBandwidthChanged(endpointId: String, bandwidthInfo: BandwidthInfo) {
                    scope.launch { _bandwidthEvents.emit(bandwidthInfo) }
                }
            }

    private val endpointDiscoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    log(
                            "onEndpointFound ID=$endpointId Name='${info.endpointName}'",
                            com.fyp.resilientp2p.data.LogLevel.TRACE
                    )
                    log(
                            "Me='$localUsername'. Match? ${info.endpointName == localUsername}",
                            com.fyp.resilientp2p.data.LogLevel.TRACE
                    )

                    if (info.endpointName == localUsername) {
                        log("Ignoring self: $endpointId (Name match)")
                        return
                    }

                    if (neighbors.containsKey(endpointId) ||
                                    _state.value.connectingEndpoints.contains(endpointId)
                    ) {
                        log(
                                "Already known or connecting to $endpointId",
                                com.fyp.resilientp2p.data.LogLevel.TRACE
                        )
                        return
                    }

                    // Stabilization Logic: If we already have a route to this peer (Direct or Hop),
                    // DO NOT attempt to connect again. This prevents "Cycling" where devices
                    // constantly upgrade/downgrade between Direct and Hop connections.
                    if (routingTable.containsKey(info.endpointName)) {
                        log(
                                "Ignoring discovered endpoint $endpointId (${info.endpointName}) - Valid connection already exists via ${routingTable[info.endpointName]}.",
                                com.fyp.resilientp2p.data.LogLevel.TRACE
                        )
                        return
                    }

                    log(
                            "Requesting connection to $endpointId",
                            com.fyp.resilientp2p.data.LogLevel.TRACE
                    )
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

    // --- Initialization ---

    init {
        log("P2PManager Initialized. Local Identity: '$localUsername'")
        // localDeviceName is already set in _state constructor
        // Re-order callbacks to avoid NPE if referenced in init (already done, but good to note)


        voiceManager = VoiceManager(context) { msg, level -> log(msg, level) }
        // NOTE: startAdvertising/startDiscovery/startRoutingUpdates moved to start()
        // Called from MainActivity after permissions are granted
    }

    private fun startRoutingUpdates() {
        if (routingJob?.isActive == true) return
        routingJob =
        scope.launch {
            while (isActive) {
                delay(8000L + (0..2000).random().toLong())

                try {
                    log(
                            "Broadcasting Periodic Routing Update (Identity)...",
                            com.fyp.resilientp2p.data.LogLevel.TRACE
                    )
                    HashMap(neighbors).keys.forEach { endpointId -> sendIdentityPacket(endpointId) }

                    // Maintenance
                    pruneRoutes()
                } catch (e: Exception) {
                    log(
                            "Error in Routing Update Loop: ${e.message}",
                            com.fyp.resilientp2p.data.LogLevel.ERROR
                    )
                }
            }
        }
    }

    private fun pruneRoutes() {
        synchronized(routingLock) {
        val now = System.currentTimeMillis()
        val staleThreshold = 60000L // 1 minute
        // CRITICAL FIX: Operate atomically per key to avoid TOCTOU
        routeLastSeen.entries.toList().forEach { (key, lastSeenTime) ->
            if ((now - lastSeenTime) > staleThreshold) {
                if (routeLastSeen.remove(key, lastSeenTime)) {
                    routingTable.remove(key)
                    routingScores.remove(key)
                    log("Pruned stale route: $key", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                }
            }
        }
        } // synchronized(routingLock)
        updateConnectedEndpoints()
    }

    fun start() {
        log("Starting P2PManager...")
        startAdvertising()
        startDiscovery()
        startRoutingUpdates()
    }

    fun stop() {
        stopAll()
    }

    fun stopAll() {
        log("Stopping All Activity...")
        // 1. Cancel coroutines FIRST to stop background work
        routingJob?.cancel()
        routingJob = null
        supervisorJob.cancel()
        
        // 2. Recreate scope IMMEDIATELY to prevent old coroutines from accessing cleared data
        supervisorJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + supervisorJob)
        
        // 3. Stop connections client
        connectionsClient.stopAllEndpoints()
        
        // 4. Clear data atomically under lock
        synchronized(routingLock) {
            neighbors.clear()
            routingTable.clear()
            routingScores.clear()
            routeLastSeen.clear()
        }
        
        // 5. Reset state
        _state.value = P2PState(localDeviceName = localUsername)
    }

    // --- Nearby Connections Actions ---

    fun startAdvertising() {
        // Apply LowPower and Hybrid Logic
        val advertisingOptions =
                AdvertisingOptions.Builder()
                        .setStrategy(STRATEGY)
                        .setLowPower(_state.value.isLowPower)
                        .build()

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

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        log("Advertising Stopped")
        updateState { it.copy(isAdvertising = false) }
    }

    fun startDiscovery() {
        val discoveryOptions =
                DiscoveryOptions.Builder()
                        .setStrategy(STRATEGY)
                        .setLowPower(_state.value.isLowPower)
                        .build()
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

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        log("Discovery Stopped")
        updateState { it.copy(isDiscovering = false) }
    }

    // --- Packet Handling ---

    private fun handlePacket(packet: Packet, sourceEndpointId: String) {
        if (!messageCache.tryMarkSeen(packet.id)) return

        if (sourceEndpointId != "LOCAL") {
            // Update lastSeen for the direct neighbor to prevent zombie disconnection
            neighbors[sourceEndpointId]?.let { it.lastSeen.set(System.currentTimeMillis()) }

            // CRITICAL FIX: Do not learn route to Self via neighbor (Self-Poisoning)
            if (packet.sourceId != localUsername) {
                // Dynamic Routing Logic (Extensible Scoring)
                val newScore = calculateRouteScore(packet)
                
                // CRITICAL FIX: Use synchronized for atomic update across all 3 maps
                synchronized(routingLock) {
                    val currentScore = routingScores[packet.sourceId] ?: Int.MIN_VALUE
                    if (newScore > currentScore) {
                        val oldRoute = routingTable[packet.sourceId]
                        routingScores[packet.sourceId] = newScore
                        routingTable[packet.sourceId] = sourceEndpointId
                        routeLastSeen[packet.sourceId] = System.currentTimeMillis()
                        if (oldRoute != sourceEndpointId) {
                            log(
                                    "RoutingTable Updated: ${packet.sourceId} -> $sourceEndpointId (Score: $newScore, Prev: $currentScore)",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG
                            )
                        }
                    }
                }
                updateConnectedEndpoints()
            } else {
                log(
                        "Refusing to add Self ('${packet.sourceId}') to routing table via $sourceEndpointId",
                        com.fyp.resilientp2p.data.LogLevel.TRACE
                )
            }
        }

        if (packet.destId == localUsername || packet.destId == "BROADCAST") {
            processPacket(packet, sourceEndpointId)
        }

        if (packet.destId != localUsername) {
            if (packet.ttl > 0) {
                forwardPacket(packet, sourceEndpointId)
            } else {
                log("Packet ${packet.id} TTL exceeded, dropping. Dest: ${packet.destId}", com.fyp.resilientp2p.data.LogLevel.WARN)
            }
        }
    }

    private fun processPacket(packet: Packet, sourceEndpointId: String) {
        scope.launch { _payloadEvents.emit(PayloadEvent(sourceEndpointId, packet)) }

        when (packet.type) {
            PacketType.DATA ->
                    log(
                            "Message: ${String(packet.payload, StandardCharsets.UTF_8)}",
                            com.fyp.resilientp2p.data.LogLevel.INFO,
                            com.fyp.resilientp2p.data.LogType.CHAT,
                            packet.sourceId
                    )
            PacketType.IDENTITY -> {
                // Prevent Self-Poisoning, but DO NOT disconnect the neighbor.
                // If a neighbor echoes our identity back (e.g. routing loop), they are still a
                // valid neighbor.
                // We just shouldn't rename them to "Me".
                if (packet.sourceId == localUsername) {
                    log(
                            "WARN: Received own IDENTITY from $sourceEndpointId. Dropping packet to avoid self-poisoning."
                    )
                    return
                }

                // ATOMIC IN-PLACE UPDATE: Mutate existing neighbor instead of replacing
                neighbors.compute(sourceEndpointId) { _, existing ->
                    if (existing != null) {
                        existing.peerName = packet.sourceId
                        existing.lastSeen.set(System.currentTimeMillis())
                        existing
                    } else {
                        Neighbor(sourceEndpointId, packet.sourceId, 0, System.currentTimeMillis())
                    }
                }
                updateConnectedEndpoints()
            }
            PacketType.PING -> {
                // Reply with PONG
                val pongPacket =
                        packet.copy(
                                id = java.util.UUID.randomUUID().toString(),
                                type = PacketType.PONG,
                                destId = packet.sourceId,
                                sourceId = localUsername,
                                // Payload contains timestamp, we just echo it back
                                payload = packet.payload,
                                timestamp = System.currentTimeMillis()
                        )
                // Use handlePacket("LOCAL") to route it correctly (direct send)
                handlePacket(pongPacket, "LOCAL")
            }
            PacketType.PONG -> {
                // HeartbeatManager listens to payloadEvents, so this is handled via emit above
                log(
                        "Received PONG from ${packet.sourceId}",
                        com.fyp.resilientp2p.data.LogLevel.TRACE
                )
            }
            else -> {}
        }
    }

    private fun forwardPacket(packet: Packet, excludeEndpointId: String? = null) {
        val nextHop = synchronized(routingLock) { routingTable[packet.destId] }
        val newPacket = packet.copy(ttl = packet.ttl - 1)
        val bytes =
                try {
                    newPacket.toBytes()
                } catch (e: Exception) {
                    log("Error serializing packet for forward: ${e.message}")
                    return
                }
        val payload = Payload.fromBytes(bytes)

        if (nextHop != null && nextHop != excludeEndpointId && neighbors.containsKey(nextHop)) {
            connectionsClient.sendPayload(nextHop, payload).addOnFailureListener { e ->
                log("Forward Packet Failed: ${e.message}")
                if (e.message?.contains("8012") == true) {
                    log(
                            "Detected Dead Endpoint 8012: $nextHop. Disconnecting.",
                            com.fyp.resilientp2p.data.LogLevel.WARN
                    )
                    disconnectFromEndpoint(nextHop)
                }
            }
        } else {
            // Split Horizon: Do not send back to the node we received it from (excludeEndpointId)
            neighbors.keys.filter { it != excludeEndpointId }.forEach { endpointId ->
                connectionsClient.sendPayload(endpointId, payload).addOnFailureListener { e ->
                    // Silent fail for broadcast or check for 8012
                    if (e.message?.contains("8012") == true) {
                        // Don't spam disconnects here, let heartbeat handle it or map logic
                        // But for routing robustness:
                        log(
                                "Detected Dead Broadcast Endpoint 8012: $endpointId",
                                com.fyp.resilientp2p.data.LogLevel.WARN
                        )
                        disconnectFromEndpoint(endpointId)
                    }
                }
            }
        }
    }

    // --- Public Methods ---

    fun sendData(destId: String, message: String) {
        log(
                "sendData requested. Dest='$destId' Msg='$message'",
                com.fyp.resilientp2p.data.LogLevel.TRACE
        )
        // Log the SENT message so it appears in the UI
        log(
                "[SENT] $message",
                com.fyp.resilientp2p.data.LogLevel.INFO,
                com.fyp.resilientp2p.data.LogType.CHAT,
                localUsername
        )
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
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.PING,
                        sourceId = localUsername,
                        destId = peerId,
                        payload = data,
                        timestamp = System.currentTimeMillis()
                )
        handlePacket(packet, "LOCAL")
    }

    // New Direct Ping for Heartbeats (Bypasses Mesh Routing, works even if Name is Unknown)
    fun sendDirectPing(endpointId: String, data: ByteArray) {
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.PING,
                        sourceId = localUsername,
                        destId = "BROADCAST", // Use BROADCAST/Wildcard to ensure receiver processes
                        // it
                        payload = data,
                        timestamp = System.currentTimeMillis()
                )
        // Send directly to the physical endpoint
        try {
            val bytes = packet.toBytes()
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e -> log("Failed to send IDENTITY to $endpointId: ${e.message}") }
        } catch (e: Exception) {
            log("Error sending Direct PING to $endpointId: ${e.message}")
        }
    }

    fun sendFile(peerId: String, uri: android.net.Uri) {
        // Nearby Connections API takes ownership of PFD for FILE payloads
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            log("Could not open file descriptor", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }
        val payload = try {
            Payload.fromFile(pfd)
        } catch (e: Exception) {
            pfd.close()  // Safe - payload creation failed, API never got it
            log("Error creating file payload: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }
        // After this point, Nearby API owns PFD - do NOT close in callbacks
        connectionsClient.sendPayload(peerId, payload)
            .addOnSuccessListener { log("File sent to $peerId") }
            .addOnFailureListener { e -> log("File send failed: ${e.message}") }
    }

    fun startAudioStreaming(peerName: String) {
        val targetEndpointIds =
                if (peerName == "BROADCAST") {
                    neighbors.keys.toList()
                } else {
                    val id = neighbors.entries.find { it.value.peerName == peerName }?.key
                    if (id != null) listOf(id) else emptyList()
                }

        if (targetEndpointIds.isEmpty()) {
            log(
                    "No valid endpoints found for audio streaming to '$peerName'",
                    com.fyp.resilientp2p.data.LogLevel.ERROR
            )
            return
        }

        log(
                "Starting audio stream to ${if(peerName == "BROADCAST") "ALL (${targetEndpointIds.size})" else peerName}"
        )

        val pfd = voiceManager?.startRecording()
        if (pfd != null) {
            log("Audio recording started, sending payload...")
            val payload = Payload.fromStream(pfd)

            // Nearby Connections sendPayload accepts a list of endpoints
            connectionsClient
                    .sendPayload(targetEndpointIds, payload)
                    .addOnSuccessListener { log("Audio payload sent to $targetEndpointIds") }
                    .addOnFailureListener { e ->
                        log("Failed to send audio payload: ${e.message}")
                        voiceManager?.stopRecording()
                        // Do NOT close pfd - Nearby API owns it after Payload.fromStream()
                    }
        } else {
            log("Failed to start audio recording (returned null)")
        }
    }

    fun stopAudioStreaming() {
        log("Audio Streaming stopped")
        voiceManager?.stopRecording()
    }

    fun getLocalDeviceName(): String = localUsername

    fun disconnectFromEndpoint(endpointId: String) {
        connectionsClient.disconnectFromEndpoint(endpointId)
        // Note: Let the API callback handle cleanup - dont manually invoke
    }

    fun acceptConnection(endpointId: String) {
        connectionsClient.acceptConnection(endpointId, payloadCallback)
    }

    fun rejectConnection(endpointId: String) {
        connectionsClient.rejectConnection(endpointId)
    }

    fun setHybridMode(enabled: Boolean) {
        if (_state.value.isHybridMode == enabled) return
        updateState { it.copy(isHybridMode = enabled) }
        restartRadio()
    }

    fun setManualConnection(enabled: Boolean) {
        updateState { it.copy(isManualConnectionEnabled = enabled) }
    }

    fun setLowPower(enabled: Boolean) {
        if (_state.value.isLowPower == enabled) return
        updateState { it.copy(isLowPower = enabled) }
        restartRadio()
    }

    private fun restartRadio() {
        val wasAdvertising = _state.value.isAdvertising
        val wasDiscovering = _state.value.isDiscovering

        if (wasAdvertising) stopAdvertising()
        if (wasDiscovering) stopDiscovery()

        // Cancel any pending restart job and start new one
        radioRestartJob?.cancel()
        radioRestartJob = scope.launch {
            if (wasAdvertising) {
                delay(100)
                startAdvertising()
            }
            if (wasDiscovering) {
                delay(100)
                startDiscovery()
            }
        }
    }

    fun clearLogs() {
        updateState { it.copy(logs = emptyList()) }
    }

    // --- Helpers ---

    fun getNeighborsSnapshot(): Map<String, Neighbor> {
        return HashMap(neighbors)
    }

    private fun dumpRoutingTable() {
        val sb = StringBuilder("Routing Table Dump:\n")
        routingTable.forEach { (dest, hop) -> sb.append("  $dest -> $hop\n") }
        log(sb.toString(), com.fyp.resilientp2p.data.LogLevel.TRACE)
    }

    private fun calculateRouteScore(packet: Packet): Int {
        // Higher score = better route. Base: TTL * 100 (more hops = lower TTL = worse)
        // Cap TTL to prevent artificially boosted routes from malicious local sources
        var score = minOf(packet.ttl, 10) * 100
        
        // Add RSSI bonus if available from trace (only if valid negative value)
        packet.trace.lastOrNull()?.rssi?.let { rssi ->
            if (rssi < 0) {
                // RSSI is typically negative (e.g. -50 dBm is better than -80 dBm)
                score += (rssi / 10)
            }
        }
        
        return score
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
            .addOnFailureListener { e -> log("Failed to send IDENTITY to $endpointId: ${e.message}") }
    }

    private fun updateState(update: (P2PState) -> P2PState) {
        _state.update(update)
    }

    private fun updateConnectedEndpoints() {
        val neighborList = neighbors.values.map { it.peerName }.toList()
        val knownPeersMap = synchronized(routingLock) {
            routingTable.entries.associate { (dest, nextHop) -> dest to RouteInfo(nextHop, 1) }
        }
        updateState { it.copy(connectedEndpoints = neighborList, knownPeers = knownPeersMap) }
    }

    fun setLogLevel(level: com.fyp.resilientp2p.data.LogLevel) {
        updateState { it.copy(logLevel = level) }
    }

    private fun getTimestamp(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date())
    }

    fun log(
            msg: String,
            level: com.fyp.resilientp2p.data.LogLevel = com.fyp.resilientp2p.data.LogLevel.INFO,
            type: com.fyp.resilientp2p.data.LogType = com.fyp.resilientp2p.data.LogType.SYSTEM,
            peerId: String? = null
    ) {
        val taggedMsg = "[com.fyp.resilientp2p] [$level] $msg"

        // 1. Always output to Logcat/Stdout
        Log.d("P2PManager", taggedMsg)

        // 2. Filter for UI Storage
        val currentLevel = _state.value.logLevel
        if (level.ordinal <= currentLevel.ordinal) {
            val entry =
                    com.fyp.resilientp2p.data.LogEntry(
                            timestamp = System.currentTimeMillis(),
                            message = msg,
                            level = level,
                            logType = type,
                            peerId = peerId
                    )
            updateState { state -> 
                state.copy(logs = (state.logs + entry).takeLast(100)) 
            }
        }
    }
}
