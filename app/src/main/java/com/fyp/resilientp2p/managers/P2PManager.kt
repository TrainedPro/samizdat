package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.LogDao
import com.fyp.resilientp2p.data.Neighbor
import com.fyp.resilientp2p.data.P2PState
import com.fyp.resilientp2p.data.PacketDao
import com.fyp.resilientp2p.data.PacketEntity
import com.fyp.resilientp2p.data.RouteInfo
import com.fyp.resilientp2p.transport.MessageCache
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import androidx.core.content.edit
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
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.fyp.resilientp2p.data.NetworkStats

/**
 * Core mesh networking engine managing peer discovery, connection lifecycle,
 * packet routing, store-and-forward, and file/audio transfers via Google Nearby Connections.
 *
 * Uses P2P_CLUSTER strategy with max 4 simultaneous connections. Implements distance-vector
 * routing with poison reverse, automatic reconnection, and zombie endpoint detection.
 *
 * @param context Application context for Nearby Connections and system services
 * @param logDao Optional Room DAO for persistent structured logging
 * @param packetDao Optional Room DAO for store-and-forward packet persistence
 */
class P2PManager(
    private val context: Context,
    private val logDao: LogDao? = null,
    private val packetDao: PacketDao? = null
) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    @Volatile private var supervisorJob = SupervisorJob()
    @Volatile private var scope = CoroutineScope(Dispatchers.IO + supervisorJob)
    private var routingJob: kotlinx.coroutines.Job? = null
    private var statsDumpJob: kotlinx.coroutines.Job? = null

    // Network Statistics (public for HeartbeatManager RTT access)
    val networkStats = NetworkStats()

    // Connection timing for duration tracking
    private val connectionTimestamps = ConcurrentHashMap<String, Long>()

    // Battery Monitoring
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            if (level >= 0) networkStats.batteryLevel = (level * 100) / scale
            networkStats.batteryTemperature = temp
        }
    }

    // Local Info
    // Use a unique name to prevent self-connection and manage duplicate links
    private val localUsername: String by lazy {
        val prefs = context.getSharedPreferences("p2p_prefs", android.content.Context.MODE_PRIVATE)
        prefs.getString("device_id", null) ?: run {
            val newId = "${android.os.Build.MODEL}-${UUID.randomUUID().toString().take(12)}"
            prefs.edit { putString("device_id", newId) }
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

    // Received file events (emitted when a file transfer completes)
    data class ReceivedFileEvent(val senderName: String, val fileName: String, val mimeType: String, val file: java.io.File)
    private val _receivedFileEvents = MutableSharedFlow<ReceivedFileEvent>()
    val receivedFileEvents: SharedFlow<ReceivedFileEvent> = _receivedFileEvents.asSharedFlow()

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
    private val RECONNECT_DELAY_MS = 3000L
    private val RECONNECT_MAX_ATTEMPTS = 5
    private val STORE_FORWARD_TTL_MS = 2 * 60 * 60 * 1000L // 2 hours

    // Track restartRadio job to prevent overlapping restarts
    private var radioRestartJob: kotlinx.coroutines.Job? = null
    // Jobs

    // Reconnection Queue: peerName -> (lastEndpointId, attemptCount)
    private val reconnectionQueue = ConcurrentHashMap<String, ReconnectionEntry>()
    private var reconnectionJob: kotlinx.coroutines.Job? = null

    // Store-and-Forward Queue (in-memory, backed by Room DB)
    private val pendingMessages = ConcurrentHashMap<String, java.util.concurrent.CopyOnWriteArrayList<Packet>>() // destId -> packets
    private var storeForwardJob: kotlinx.coroutines.Job? = null

    // Endpoints with active transfers (exempt from zombie detection)
    val activeTransferEndpoints = ConcurrentHashMap<String, Long>() // endpointId -> startTime

    // External manager references (set by P2PApplication after construction)
    /** Internet gateway manager for cross-mesh relay. Set by [P2PApplication]. */
    var internetGatewayManager: InternetGatewayManager? = null
    /** Emergency broadcast manager. Set by [P2PApplication]. */
    var emergencyManager: EmergencyManager? = null

    // Security (optional — enabled on feature/security branch)
    /** End-to-end encryption and HMAC integrity manager. Set by [P2PApplication]. */
    var securityManager: com.fyp.resilientp2p.security.SecurityManager? = null
    /** Per-peer rate limiter. Set by [P2PApplication]. */
    var rateLimiter: com.fyp.resilientp2p.security.RateLimiter? = null
    /** Persistent peer blacklist. Set by [P2PApplication]. */
    var peerBlacklist: com.fyp.resilientp2p.security.PeerBlacklist? = null

    // Phase 4 manager references
    /** RTT-based trilateration engine. Set by [P2PApplication]. */
    var locationEstimator: LocationEstimator? = null
    /** DTN encounter logger. Set by [P2PApplication]. */
    var encounterLogger: EncounterLogger? = null
    /** Content-addressable file sharing manager. Set by [P2PApplication]. */
    var fileShareManager: FileShareManager? = null
    /** Group message handler callback — set to a lambda that persists and distributes. */
    var groupMessageHandler: ((com.fyp.resilientp2p.transport.Packet) -> Unit)? = null

    // File transfer metadata: payloadId -> FileMetadata (set by FILE_META packet, consumed on transfer complete)
    data class FileMetadata(val fileName: String, val mimeType: String, val fileSize: Long, val senderName: String)
    private val pendingFileMetadata = ConcurrentHashMap<Long, FileMetadata>()
    // Pending FILE payloads (stored until transfer completes)
    private val pendingFilePayloads = ConcurrentHashMap<Long, Payload>()

    data class ReconnectionEntry(
        val peerName: String,
        val lastEndpointId: String,
        var attemptCount: Int = 0,
        var lastAttemptTime: Long = 0
    )

    // Optional Managers
    var voiceManager: VoiceManager? = null

    // Data Classes for UI
    data class PayloadEvent(val endpointId: String, val packet: Packet)
    data class PayloadProgressEvent(val endpointId: String, val progress: Int)

    // --- Callbacks ---

    private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    val peerName = neighbors[endpointId]?.peerName ?: "Unknown"
                    val payloadTypeName = when (payload.type) {
                        Payload.Type.BYTES -> "BYTES"
                        Payload.Type.STREAM -> "STREAM"
                        Payload.Type.FILE -> "FILE"
                        else -> "UNKNOWN(${payload.type})"
                    }
                    log(
                            "PAYLOAD_RECEIVED from=$endpointId($peerName) type=$payloadTypeName " +
                            "inNeighbors=${neighbors.containsKey(endpointId)}",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG,
                            peerId = peerName
                    )

                    // Update lastSeen for ANY activity (Bytes, Stream, File) to prevent Zombie
                    // detection during calls
                    neighbors.computeIfPresent(endpointId) { _, neighbor -> neighbor.lastSeen.set(System.currentTimeMillis()); neighbor }

                    if (payload.type == Payload.Type.BYTES) {
                        try {
                            payload.asBytes()?.let { bytes ->
                                val packet = Packet.fromBytes(bytes)

                                // --- Security checks ---
                                // 1. Blacklist check
                                if (peerBlacklist?.isBlacklisted(packet.sourceId) == true) {
                                    log("BLOCKED_BLACKLISTED from=${packet.sourceId}",
                                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                    return
                                }
                                // 2. Rate limiting
                                val limiter = rateLimiter
                                if (limiter != null && !limiter.allowPacket(packet.sourceId)) {
                                    peerBlacklist?.recordViolation(packet.sourceId)
                                    log("RATE_LIMITED from=${packet.sourceId}",
                                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                    return
                                }
                                // 3. HMAC verification (if security enabled and key exists)
                                val security = securityManager
                                if (security != null && security.hasKeyForPeer(packet.sourceId)) {
                                    // Last HMAC_SIZE bytes are the HMAC
                                    if (packet.payload.size > com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE) {
                                        val dataBytes = packet.payload.copyOfRange(0, packet.payload.size - com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE)
                                        val hmacBytes = packet.payload.copyOfRange(packet.payload.size - com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE, packet.payload.size)
                                        if (!security.verifyHmac(packet.sourceId, dataBytes, hmacBytes)) {
                                            log("HMAC_INVALID from=${packet.sourceId} — dropping packet",
                                                com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                            peerBlacklist?.recordViolation(packet.sourceId)
                                            return
                                        }
                                    }
                                }

                                try { handlePacket(packet, endpointId) } catch (e: Exception) {
                                    log("PACKET_HANDLE_ERROR from=$endpointId($peerName) error='${e.message}' " +
                                        "stackTrace='${e.stackTrace.take(3).joinToString(" -> ")}'",
                                        com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                                }
                            }
                        } catch (e: Exception) {
                            log("PACKET_PARSE_ERROR from=$endpointId($peerName) error='${e.message}'",
                                com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                        }
                    } else if (payload.type == Payload.Type.STREAM) {
                        log("AUDIO_STREAM_RECEIVED from=$endpointId($peerName)", com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)
                        payload.asStream()?.asInputStream()?.let { inputStream ->
                            // TOCTOU FIX: Capture voiceManager reference locally
                            val vm = voiceManager
                            if (vm != null) {
                                try {
                                    val rtt = networkStats.peerRtt[peerName] ?: -1L
                                    vm.startPlaying(inputStream, rtt)
                                } catch (e: Exception) {
                                    inputStream.close()
                                    log("AUDIO_PLAYBACK_FAILED from=$endpointId($peerName) error='${e.message}'",
                                        com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                                }
                            } else {
                                inputStream.close()  // Close if not used to prevent leak
                                log("AUDIO_PLAYBACK_SKIPPED from=$endpointId reason=NO_VOICE_MANAGER", com.fyp.resilientp2p.data.LogLevel.WARN)
                            }
                        }
                    } else if (payload.type == Payload.Type.FILE) {
                        log("FILE_PAYLOAD_RECEIVED from=$endpointId($peerName) payloadId=${payload.id}",
                            com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)
                        pendingFilePayloads[payload.id] = payload
                    } else {
                        log("PAYLOAD_UNHANDLED from=$endpointId($peerName) type=$payloadTypeName", com.fyp.resilientp2p.data.LogLevel.WARN)
                    }
                }

                override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                ) {
                    val peerName = neighbors[endpointId]?.peerName ?: "Unknown"

                    // Keep updating lastSeen during long transfers (like Audio Stream or Big File)
                    if (update.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                        neighbors.computeIfPresent(endpointId) { _, neighbor -> neighbor.lastSeen.set(System.currentTimeMillis()); neighbor }
                        activeTransferEndpoints[endpointId] = System.currentTimeMillis()
                    }

                    // Log transfer completion/failure
                    when (update.status) {
                        PayloadTransferUpdate.Status.SUCCESS -> {
                            activeTransferEndpoints.remove(endpointId)
                            log(
                                    "TRANSFER_COMPLETE from=$endpointId($peerName) bytes=${update.bytesTransferred}",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG,
                                    peerId = peerName,
                                    payloadSizeBytes = update.bytesTransferred.toInt()
                            )
                            // Check if this is a completed FILE transfer
                            val payloadId = update.payloadId
                            val filePayload = pendingFilePayloads.remove(payloadId)
                            if (filePayload != null) {
                                val meta = pendingFileMetadata.remove(payloadId)
                                scope.launch {
                                    try {
                                        // Nearby API stores received files as <payloadId> in filesDir
                                        @Suppress("DEPRECATION")
                                        val receivedFile = filePayload.asFile()?.asJavaFile()
                                            ?: java.io.File(context.filesDir, payloadId.toString())
                                        if (receivedFile.exists()) {
                                            val targetName = meta?.fileName ?: "received_${payloadId}"
                                            val receivedDir = java.io.File(context.filesDir, "received_files")
                                            receivedDir.mkdirs()
                                            val targetFile = java.io.File(receivedDir, targetName)
                                            receivedFile.renameTo(targetFile)
                                            val senderName = meta?.senderName ?: peerName
                                            val mimeType = meta?.mimeType ?: "application/octet-stream"
                                            log(
                                                "FILE_RECEIVED from='$senderName' name='$targetName' mime='$mimeType' size=${update.bytesTransferred}",
                                                com.fyp.resilientp2p.data.LogLevel.INFO,
                                                peerId = senderName
                                            )
                                            _receivedFileEvents.emit(
                                                ReceivedFileEvent(senderName, targetName, mimeType, targetFile)
                                            )
                                        } else {
                                            log("FILE_RECEIVE_ERROR payloadId=$payloadId reason=FILE_NOT_FOUND",
                                                com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                                        }
                                    } catch (e: Exception) {
                                        log("FILE_RECEIVE_ERROR payloadId=$payloadId error='${e.message}'",
                                            com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                                    }
                                }
                            }
                        }
                        PayloadTransferUpdate.Status.FAILURE -> {
                            activeTransferEndpoints.remove(endpointId)
                            log(
                                    "TRANSFER_FAILED from=$endpointId($peerName) transferred=${update.bytesTransferred}/${update.totalBytes}",
                                    com.fyp.resilientp2p.data.LogLevel.WARN,
                                    peerId = peerName
                            )
                        }
                        PayloadTransferUpdate.Status.CANCELED -> {
                            activeTransferEndpoints.remove(endpointId)
                            log("TRANSFER_CANCELED from=$endpointId($peerName)", com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                        }
                        else -> {} // IN_PROGRESS handled above
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
                    val neighborCount = neighbors.size
                    val connectingCount = _state.value.connectingEndpoints.size
                    log(
                            "CONNECTION_INITIATED endpoint=$endpointId name='${info.endpointName}' " +
                            "isIncoming=${info.isIncomingConnection} " +
                            "neighbors=$neighborCount/$MAX_CONNECTIONS connecting=$connectingCount"
                    )

                    // Limit Connections (synchronized to prevent TOCTOU race)
                    synchronized(routingLock) {
                        if (neighbors.size >= MAX_CONNECTIONS) {
                            log(
                                    "CONNECTION_REJECTED endpoint=$endpointId reason=MAX_CONNECTIONS " +
                                    "current=$neighborCount max=$MAX_CONNECTIONS " +
                                    "peers=[${neighbors.values.joinToString { "${it.peerName}(${it.peerId})" }}]",
                                    com.fyp.resilientp2p.data.LogLevel.WARN
                            )
                            connectionsClient.rejectConnection(endpointId)
                            return
                        }
                    }

                    // Critical Self-Check
                    if (info.endpointName == localUsername) {
                        log("CONNECTION_REJECTED endpoint=$endpointId reason=SELF_CONNECTION name='${info.endpointName}'",
                                com.fyp.resilientp2p.data.LogLevel.WARN)
                        connectionsClient.rejectConnection(endpointId)
                        return
                    }

                    // duplicate check logic
                    // If we see a duplicate name, we assume the NEW one is the valid replacement
                    val existingId =
                            neighbors.entries.find { it.value.peerName == info.endpointName }?.key
                    if (existingId != null) {
                        val existingNeighbor = neighbors[existingId]
                        val lastSeenMs = existingNeighbor?.let { System.currentTimeMillis() - it.lastSeen.get() } ?: Long.MAX_VALUE
                        val isAlive = lastSeenMs < STABILITY_WINDOW_MS

                        // Tie-Breaker Logic
                        if (isAlive && existingId > endpointId) {
                            log(
                                    "CONNECTION_REJECTED endpoint=$endpointId reason=DUPLICATE_ALIVE " +
                                    "peerName='${info.endpointName}' existingEp=$existingId " +
                                    "lastSeen=${lastSeenMs}ms window=${STABILITY_WINDOW_MS}ms " +
                                    "tieBreaker=existing>new",
                                    com.fyp.resilientp2p.data.LogLevel.WARN
                            )
                            connectionsClient.rejectConnection(endpointId)
                            return
                        }

                        log(
                                "DUPLICATE_REPLACEMENT endpoint=$endpointId replacing=$existingId " +
                                "peerName='${info.endpointName}' alive=$isAlive lastSeen=${lastSeenMs}ms",
                                com.fyp.resilientp2p.data.LogLevel.WARN
                        )
                        // Proactively cleanup the old "Zombie"
                        connectionsClient.disconnectFromEndpoint(existingId)
                        networkStats.recordPeerDisconnected(info.endpointName)
                        val lostRoutes = synchronized(routingLock) {
                            val lost = routingTable.filterValues { it == existingId }.keys.toList()
                            lost.forEach { dest ->
                                routingTable.remove(dest)
                                routingScores.remove(dest)
                                routeLastSeen.remove(dest)
                            }
                            lost
                        }
                        neighbors.remove(existingId)
                        connectionTimestamps.remove(existingId)
                        if (lostRoutes.isNotEmpty()) {
                            log("  ROUTES_LOST from replacement: [${lostRoutes.joinToString()}]", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                        }
                        updateConnectedEndpoints()
                        // Proceed to accept the new one below...
                    }

                    log(
                            "CONNECTION_ACCEPTED endpoint=$endpointId name='${info.endpointName}' " +
                            "postAcceptNeighbors=${neighbors.size}",
                            com.fyp.resilientp2p.data.LogLevel.INFO,
                            peerId = info.endpointName
                    )
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                    // ATOMIC UPDATE: Read-modify-write inside lambda
                    updateState { it.copy(connectingEndpoints = it.connectingEndpoints + endpointId) }
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    // ATOMIC UPDATE: Read-modify-write inside lambda
                    updateState { it.copy(connectingEndpoints = it.connectingEndpoints - endpointId) }

                    val statusCode = result.status.statusCode
                    val statusName = ConnectionsStatusCodes.getStatusCodeString(statusCode)

                    if (statusCode == ConnectionsStatusCodes.STATUS_OK) {
                        connectionTimestamps[endpointId] = System.currentTimeMillis()

                        // ATOMIC: Preserve existing neighbor, only create if new
                        neighbors.compute(endpointId) { _, existing ->
                            if (existing != null) {
                                existing.lastSeen.set(System.currentTimeMillis())
                                existing // Preserve existing neighbor with its name
                            } else {
                                Neighbor(endpointId, "Unknown", 0, System.currentTimeMillis())
                            }
                        }

                        val peerName = neighbors[endpointId]?.peerName ?: "Unknown"
                        networkStats.recordPeerConnected(peerName)

                        log(
                                "CONNECTION_ESTABLISHED endpoint=$endpointId peerName='$peerName' " +
                                "status=$statusCode($statusName) " +
                                "totalNeighbors=${neighbors.size}/$MAX_CONNECTIONS " +
                                "lifetimeConnections=${networkStats.totalConnectionsEstablished.get()} " +
                                "peers=[${neighbors.values.joinToString { "${it.peerName}(${it.peerId})" }}]",
                                com.fyp.resilientp2p.data.LogLevel.INFO,
                                peerId = peerName
                        )

                        updateConnectedEndpoints()
                        sendIdentityPacket(endpointId)

                        // DTN encounter logging
                        encounterLogger?.onPeerConnected(localUsername, peerName)

                        // Trigger store-forward delivery attempt for newly connected peer
                        triggerStoreForwardDelivery(endpointId)
                    } else {
                        log(
                                "CONNECTION_FAILED endpoint=$endpointId " +
                                "status=$statusCode($statusName) " +
                                "message='${result.status.statusMessage}'",
                                com.fyp.resilientp2p.data.LogLevel.WARN
                        )
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    val disconnectedPeer = neighbors[endpointId]
                    val peerName = disconnectedPeer?.peerName ?: "Unknown"
                    val connectedAtMs = connectionTimestamps.remove(endpointId)
                    val durationMs = if (connectedAtMs != null) System.currentTimeMillis() - connectedAtMs else -1L
                    val durationStr = if (durationMs > 0) formatDuration(durationMs) else "unknown"

                    networkStats.recordPeerDisconnected(peerName)

                    neighbors.remove(endpointId)
                    activeTransferEndpoints.remove(endpointId)
                    
                    // Remove all routes that went through this neighbor - SYNCHRONIZED
                    val lostRoutes = synchronized(routingLock) {
                        val lost = routingTable.filterValues { it == endpointId }.keys.toList()
                        lost.forEach { dest ->
                            routingTable.remove(dest)
                            routingScores.remove(dest)
                            routeLastSeen.remove(dest)
                        }
                        lost
                    }

                    log(
                            "DISCONNECTED endpoint=$endpointId peerName='$peerName' " +
                            "duration=$durationStr(${durationMs}ms) " +
                            "routesLost=${lostRoutes.size}[${lostRoutes.joinToString()}] " +
                            "remainingNeighbors=${neighbors.size} " +
                            "lifetimeDisconnections=${networkStats.totalConnectionsLost.get()} " +
                            "remainingPeers=[${neighbors.values.joinToString { "${it.peerName}(${it.peerId})" }}]",
                            com.fyp.resilientp2p.data.LogLevel.WARN,
                            peerId = peerName
                    )

                    // Purge security keys for the disconnected peer
                    securityManager?.removePeerKeys(peerName)

                    updateConnectedEndpoints()

                    // DTN encounter logging  
                    encounterLogger?.onPeerDisconnected(peerName)
                    
                    // Queue for reconnection if they had a valid name
                    if (peerName != "Unknown" && peerName.isNotBlank()) {
                        scheduleReconnection(peerName, endpointId)
                    }
                }

                override fun onBandwidthChanged(endpointId: String, bandwidthInfo: BandwidthInfo) {
                    val qualityName = when (bandwidthInfo.quality) {
                        BandwidthInfo.Quality.LOW -> "LOW"
                        BandwidthInfo.Quality.MEDIUM -> "MEDIUM"
                        BandwidthInfo.Quality.HIGH -> "HIGH"
                        BandwidthInfo.Quality.UNKNOWN -> "UNKNOWN"
                        else -> "UNRECOGNIZED(${bandwidthInfo.quality})"
                    }
                    val peerName = neighbors[endpointId]?.peerName ?: "Unknown"
                    log(
                            "BANDWIDTH_CHANGED endpoint=$endpointId peerName='$peerName' quality=$qualityName",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG,
                            peerId = peerName
                    )
                    scope.launch { _bandwidthEvents.emit(bandwidthInfo) }
                }
            }

    private val endpointDiscoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    val neighborCount = neighbors.size
                    val connectingCount = _state.value.connectingEndpoints.size
                    log(
                            "ENDPOINT_FOUND endpoint=$endpointId name='${info.endpointName}' " +
                            "serviceId='${info.serviceId}' " +
                            "neighbors=$neighborCount/$MAX_CONNECTIONS connecting=$connectingCount",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )

                    if (info.endpointName == localUsername) {
                        log("ENDPOINT_IGNORED endpoint=$endpointId reason=SELF_DISCOVERY", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                        return
                    }

                    if (neighbors.containsKey(endpointId) ||
                                    _state.value.connectingEndpoints.contains(endpointId)
                    ) {
                        log(
                                "ENDPOINT_IGNORED endpoint=$endpointId reason=ALREADY_KNOWN " +
                                "inNeighbors=${neighbors.containsKey(endpointId)} inConnecting=${_state.value.connectingEndpoints.contains(endpointId)}",
                                com.fyp.resilientp2p.data.LogLevel.DEBUG
                        )
                        return
                    }

                    // Connection policy: Only skip if we have a DIRECT (1-hop) live connection.
                    val existingRoute = routingTable[info.endpointName]
                    if (existingRoute != null && neighbors.containsKey(existingRoute)) {
                        val isDirectConnection = neighbors[existingRoute]?.peerName == info.endpointName
                        if (isDirectConnection) {
                            log(
                                    "ENDPOINT_IGNORED endpoint=$endpointId reason=DIRECT_LINK_EXISTS " +
                                    "peerName='${info.endpointName}' via=$existingRoute",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG
                            )
                            return
                        }
                        log(
                                "ENDPOINT_UPGRADE endpoint=$endpointId peerName='${info.endpointName}' " +
                                "currentRoute=MULTI_HOP via=$existingRoute upgrading=DIRECT",
                                com.fyp.resilientp2p.data.LogLevel.INFO
                        )
                    }
                    
                    // Remove stale route if next-hop is dead
                    if (existingRoute != null && !neighbors.containsKey(existingRoute)) {
                        synchronized(routingLock) {
                            routingTable.remove(info.endpointName)
                            routingScores.remove(info.endpointName)
                            routeLastSeen.remove(info.endpointName)
                        }
                        log("STALE_ROUTE_CLEARED dest='${info.endpointName}' deadHop=$existingRoute", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                    }
                    
                    // Remove from reconnection queue since we found them
                    reconnectionQueue.remove(info.endpointName)

                    log(
                            "CONNECTION_REQUESTING endpoint=$endpointId name='${info.endpointName}' " +
                            "currentNeighbors=$neighborCount/$MAX_CONNECTIONS",
                            com.fyp.resilientp2p.data.LogLevel.INFO
                    )
                    connectionsClient
                            .requestConnection(
                                    localUsername,
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnSuccessListener { log("CONNECTION_REQUESTED endpoint=$endpointId name='${info.endpointName}'", com.fyp.resilientp2p.data.LogLevel.DEBUG) }
                            .addOnFailureListener {
                                log("CONNECTION_REQUEST_FAILED endpoint=$endpointId name='${info.endpointName}' error='${it.message}'",
                                        com.fyp.resilientp2p.data.LogLevel.WARN)
                            }
                }

                override fun onEndpointLost(endpointId: String) {
                    val wasNeighbor = neighbors.containsKey(endpointId)
                    val peerName = neighbors[endpointId]?.peerName ?: "Unknown"
                    log(
                            "ENDPOINT_LOST endpoint=$endpointId peerName='$peerName' wasNeighbor=$wasNeighbor",
                            if (wasNeighbor) com.fyp.resilientp2p.data.LogLevel.INFO else com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )
                }
            }

    // --- Initialization ---

    private val appVersion: String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }

    init {
        log("P2PManager INITIALIZED appVersion='$appVersion' localIdentity='$localUsername' maxConnections=$MAX_CONNECTIONS strategy=P2P_CLUSTER serviceId='$SERVICE_ID'")

        voiceManager = VoiceManager(context) { msg, level -> log(msg, level) }

        // Register battery monitor
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            log("Battery monitor registered", com.fyp.resilientp2p.data.LogLevel.DEBUG)
        } catch (e: Exception) {
            log("BATTERY_MONITOR_FAILED error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN)
        }
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

                    // Broadcast route announcements to neighbors
                    broadcastRouteAnnouncement()

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
        val staleThreshold = 30000L // 30 seconds (route announcements are every 8-10s)
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

    // --- Reconnection Logic ---
    
    private fun scheduleReconnection(peerName: String, lastEndpointId: String) {
        val existing = reconnectionQueue[peerName]
        if (existing != null && existing.attemptCount >= RECONNECT_MAX_ATTEMPTS) {
            log("Max reconnection attempts reached for $peerName. Giving up.", com.fyp.resilientp2p.data.LogLevel.WARN)
            reconnectionQueue.remove(peerName)
            return
        }
        
        reconnectionQueue[peerName] = ReconnectionEntry(
            peerName = peerName,
            lastEndpointId = lastEndpointId,
            attemptCount = (existing?.attemptCount ?: 0) + 1,
            lastAttemptTime = System.currentTimeMillis()
        )
        log("Queued reconnection for $peerName (attempt ${reconnectionQueue[peerName]?.attemptCount}/$RECONNECT_MAX_ATTEMPTS)", com.fyp.resilientp2p.data.LogLevel.DEBUG)
        
        ensureReconnectionLoopRunning()
    }
    
    private fun ensureReconnectionLoopRunning() {
        if (reconnectionJob?.isActive == true) return
        reconnectionJob = scope.launch {
            while (isActive && reconnectionQueue.isNotEmpty()) {
                delay(RECONNECT_DELAY_MS)
                
                // Discovery is the mechanism for reconnection in Nearby Connections.
                // We just need to make sure discovery is running and stale routes are cleared
                // so that when the endpoint is re-discovered, we connect to it.
                val now = System.currentTimeMillis()
                reconnectionQueue.entries.toList().forEach { (name, entry) ->
                    // If we've reconnected (peer is in neighbors), remove from queue
                    val reconnected = neighbors.values.any { it.peerName == name }
                    if (reconnected) {
                        reconnectionQueue.remove(name)
                        log("Reconnected to $name! Removing from queue.", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                        return@forEach
                    }
                    
                    // Give up after max attempts with exponential backoff (3s, 6s, 12s, 24s, 48s)
                    val backoff = RECONNECT_DELAY_MS * (1L shl minOf(entry.attemptCount, 4))
                    if (now - entry.lastAttemptTime > backoff) {
                        if (entry.attemptCount >= RECONNECT_MAX_ATTEMPTS) {
                            log("Reconnection to $name timed out after ${entry.attemptCount} attempts.", com.fyp.resilientp2p.data.LogLevel.WARN)
                            reconnectionQueue.remove(name)
                        } else {
                            // Clear any stale routing state that might block rediscovery
                            synchronized(routingLock) {
                                routingTable.remove(name)
                                routingScores.remove(name)
                                routeLastSeen.remove(name)
                            }
                            entry.attemptCount++
                            entry.lastAttemptTime = now
                            log("Reconnect attempt ${entry.attemptCount} for $name (backoff ${backoff}ms)", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                            
                            // Restart discovery if not running to find the lost peer
                            if (!_state.value.isDiscovering) {
                                startDiscovery()
                            }
                        }
                    }
                }
            }
            log("Reconnection loop ended. Queue empty.", com.fyp.resilientp2p.data.LogLevel.DEBUG)
        }
    }

    // --- Store-and-Forward Engine ---

    private fun startStoreForwardLoop() {
        if (packetDao == null) return
        if (storeForwardJob?.isActive == true) return

        storeForwardJob = scope.launch {
            while (isActive) {
                delay(15_000L) // Check every 15 seconds
                try {
                    // 1. Clean up expired packets
                    packetDao.cleanupExpired(System.currentTimeMillis())

                    // 2. For each known route destination, try to deliver queued packets
                    val currentRoutes = synchronized(routingLock) { HashMap(routingTable) }
                    for ((destId, nextHop) in currentRoutes) {
                        if (!neighbors.containsKey(nextHop)) continue // next hop is dead

                        val queued = packetDao.getPacketsForPeer(destId)
                        if (queued.isEmpty()) continue

                        log("Store-Forward: Found ${queued.size} queued packets for $destId, attempting delivery...", com.fyp.resilientp2p.data.LogLevel.DEBUG)

                        for (entity in queued) {
                            try {
                                val packet = Packet(
                                    id = entity.id,
                                    type = PacketType.valueOf(entity.type),
                                    sourceId = entity.sourceId,
                                    destId = entity.destId,
                                    payload = entity.payload,
                                    timestamp = entity.timestamp
                                )
                                val bytes = packet.toBytes()
                                connectionsClient.sendPayload(nextHop, Payload.fromBytes(bytes))
                                    .addOnSuccessListener {
                                        scope.launch {
                                            packetDao.deletePacket(entity.id)
                                            networkStats.storeForwardDelivered.incrementAndGet()
                                            log("STORE_FORWARD_DELIVERED id=${entity.id.take(8)} dest=$destId via=$nextHop", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        log("Store-Forward: Failed to deliver ${entity.id}: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                                    }
                            } catch (e: Exception) {
                                log("Store-Forward: Error reconstructing packet ${entity.id}: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
                                // Delete corrupted packets
                                packetDao.deletePacket(entity.id)
                            }
                        }
                    }

                    // 3. Also try in-memory pending messages
                    val pendingCopy = HashMap(pendingMessages)
                    for ((destId, packets) in pendingCopy) {
                        val nextHop = synchronized(routingLock) { routingTable[destId] }
                        if (nextHop == null || !neighbors.containsKey(nextHop)) continue

                        val delivered = mutableListOf<Packet>()
                        for (packet in packets) {
                            try {
                                val bytes = packet.toBytes()
                                connectionsClient.sendPayload(nextHop, Payload.fromBytes(bytes))
                                    .addOnSuccessListener {
                                        networkStats.storeForwardDelivered.incrementAndGet()
                                        log("STORE_FORWARD_DELIVERED_MEM dest=$destId", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                                    }
                                delivered.add(packet)
                            } catch (e: Exception) {
                                log("Store-Forward: Error sending in-memory packet: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                            }
                        }
                        packets.removeAll(delivered.toSet())
                        if (packets.isEmpty()) {
                            pendingMessages.remove(destId)
                        }
                    }
                } catch (e: Exception) {
                    log("Store-Forward loop error: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
        }
    }

    private fun queueForStoreForward(packet: Packet) {
        // Queue to in-memory map
        pendingMessages.computeIfAbsent(packet.destId) { java.util.concurrent.CopyOnWriteArrayList() }.add(packet)
        networkStats.storeForwardQueued.incrementAndGet()

        // Also persist to Room DB if available
        packetDao?.let { dao ->
            scope.launch {
                try {
                    val entity = PacketEntity(
                        id = packet.id,
                        destId = packet.destId,
                        type = packet.type.name,
                        payload = packet.payload,
                        timestamp = packet.timestamp,
                        expiration = System.currentTimeMillis() + STORE_FORWARD_TTL_MS,
                        sourceId = packet.sourceId
                    )
                    dao.insertPacket(entity)
                    log("Store-Forward: Queued packet ${packet.id} for ${packet.destId} (TTL: ${STORE_FORWARD_TTL_MS / 3600000}h)", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                } catch (e: Exception) {
                    Log.e("P2PManager", "Failed to queue packet to DB: ${e.message}")
                }
            }
        }
    }

    private fun triggerStoreForwardDelivery(newEndpointId: String) {
        scope.launch {
            // Small delay to allow IDENTITY exchange to complete
            delay(2000L)
            val peerName = neighbors[newEndpointId]?.peerName ?: return@launch

            // Check in-memory pending messages
            val pending = pendingMessages[peerName]
            if (pending != null && pending.isNotEmpty()) {
                log("Store-Forward: New neighbor $peerName — delivering ${pending.size} queued messages", com.fyp.resilientp2p.data.LogLevel.INFO)
                val delivered = mutableListOf<Packet>()
                for (packet in pending) {
                    try {
                        val bytes = packet.toBytes()
                        connectionsClient.sendPayload(newEndpointId, Payload.fromBytes(bytes))
                            .addOnSuccessListener {
                                networkStats.storeForwardDelivered.incrementAndGet()
                            }
                        delivered.add(packet)
                    } catch (e: Exception) {
                        log("Store-Forward: Delivery failed: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                    }
                }
                pending.removeAll(delivered.toSet())
                if (pending.isEmpty()) pendingMessages.remove(peerName)
            }

            // Check DB for queued packets
            packetDao?.let { dao ->
                try {
                    val dbPackets = dao.getPacketsForPeer(peerName)
                    if (dbPackets.isNotEmpty()) {
                        log("Store-Forward: Delivering ${dbPackets.size} DB-queued packets to $peerName", com.fyp.resilientp2p.data.LogLevel.INFO)
                        for (entity in dbPackets) {
                            try {
                                val packet = Packet(
                                    id = entity.id,
                                    type = PacketType.valueOf(entity.type),
                                    sourceId = entity.sourceId,
                                    destId = entity.destId,
                                    payload = entity.payload,
                                    timestamp = entity.timestamp
                                )
                                val bytes = packet.toBytes()
                                connectionsClient.sendPayload(newEndpointId, Payload.fromBytes(bytes))
                                    .addOnSuccessListener {
                                        scope.launch { dao.deletePacket(entity.id) }
                                        networkStats.storeForwardDelivered.incrementAndGet()
                                    }
                            } catch (e: Exception) {
                                log("Store-Forward: DB packet delivery error: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                                dao.deletePacket(entity.id) // Remove corrupted
                            }
                        }
                    }
                } catch (e: Exception) {
                    log("Store-Forward: DB query error: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
        }
    }

    fun start() {
        log("P2PManager START localName='$localUsername' battery=${networkStats.batteryLevel}%")
        startAdvertising()
        startDiscovery()
        startRoutingUpdates()
        startStoreForwardLoop()
        startStatsDump()
    }

    fun stop() {
        stopAll()
    }

    fun stopAll() {
        val snap = networkStats.snapshot(neighbors.size, routingTable.size)
        log(
                "STOP_ALL initiated neighbors=${neighbors.size} routes=${routingTable.size} " +
                "totalPackets=↑${snap.totalPacketsSent}↓${snap.totalPacketsReceived} " +
                "uptime=${formatDuration(snap.uptimeMs)}"
        )
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}
        // 1. Cancel coroutines FIRST to stop background work
        routingJob?.cancel()
        routingJob = null
        statsDumpJob?.cancel()
        statsDumpJob = null
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
        
        // 5. Clear all auxiliary state
        reconnectionQueue.clear()
        reconnectionJob?.cancel()
        reconnectionJob = null
        activeTransferEndpoints.clear()
        pendingMessages.clear()
        connectionTimestamps.clear()
        storeForwardJob?.cancel()
        storeForwardJob = null
        messageCache.clear()
        
        // 6. Reset state
        _state.value = P2PState(localDeviceName = localUsername)
        log("STOP_ALL completed. All state cleared.")
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
                    log("ADVERTISING_STARTED lowPower=${_state.value.isLowPower} serviceId='$SERVICE_ID'")
                    updateState { it.copy(isAdvertising = true) }
                    // Ensure routing and store-forward loops are running
                    startRoutingUpdates()
                    startStoreForwardLoop()
                }
                .addOnFailureListener { e ->
                    log("ADVERTISING_FAILED error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN)
                    updateState { it.copy(isAdvertising = false) }
                }
    }

    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        log("ADVERTISING_STOPPED")
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
                    log("DISCOVERY_STARTED lowPower=${_state.value.isLowPower} serviceId='$SERVICE_ID'")
                    updateState { it.copy(isDiscovering = true) }
                }
                .addOnFailureListener { e ->
                    log("DISCOVERY_FAILED error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN)
                    updateState { it.copy(isDiscovering = false) }
                }
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        log("DISCOVERY_STOPPED")
        updateState { it.copy(isDiscovering = false) }
    }

    // --- Packet Handling ---

    private fun handlePacket(packet: Packet, sourceEndpointId: String) {
        if (!messageCache.tryMarkSeen(packet.id)) {
            log("PACKET_DEDUP id=${packet.id.take(8)} type=${packet.type} src='${packet.sourceId}'",
                    com.fyp.resilientp2p.data.LogLevel.TRACE)
            if (packet.type != PacketType.IDENTITY && packet.type != PacketType.ROUTE_ANNOUNCE) {
                networkStats.totalPacketsDropped.incrementAndGet()
            }
            return
        }

        val peerName = if (sourceEndpointId == "LOCAL") "LOCAL" else (neighbors[sourceEndpointId]?.peerName ?: "Unknown")
        log(
                "PACKET_IN id=${packet.id.take(8)} type=${packet.type} " +
                "src='${packet.sourceId}' dst='${packet.destId}' " +
                "via=$sourceEndpointId($peerName) ttl=${packet.ttl} payloadSize=${packet.payload.size}B",
                com.fyp.resilientp2p.data.LogLevel.DEBUG,
                peerId = packet.sourceId,
                payloadSizeBytes = packet.payload.size
        )

        // Stats tracking for received packets (not LOCAL)
        if (sourceEndpointId != "LOCAL") {
            networkStats.recordPacketReceived(peerName, packet.payload.size)
        }

        if (sourceEndpointId != "LOCAL") {
            // Update lastSeen for the direct neighbor to prevent zombie disconnection
            neighbors[sourceEndpointId]?.let { it.lastSeen.set(System.currentTimeMillis()) }

            // CRITICAL FIX: Do not learn route to Self via neighbor (Self-Poisoning)
            if (packet.sourceId != localUsername) {
                // Don't add routing entries for direct neighbors — they're already in neighbors map.
                // Keeping them out of routingTable avoids showing duplicate entries (direct + routed).
                val isDirectNeighbor = neighbors.values.any { it.peerName == packet.sourceId }
                if (!isDirectNeighbor) {
                    // Dynamic Routing Logic (Extensible Scoring)
                    val newScore = calculateRouteScore(packet)
                    val hopCount = Packet.DEFAULT_TTL - packet.ttl + 1
                    
                    // CRITICAL FIX: Use synchronized for atomic update across all 3 maps
                    synchronized(routingLock) {
                        val currentScore = routingScores[packet.sourceId] ?: Int.MIN_VALUE
                        val lastUpdate = routeLastSeen[packet.sourceId] ?: 0L
                        val routeAge = System.currentTimeMillis() - lastUpdate
                        
                        // Accept new route if:
                        // 1. Score is better (shorter/stronger path), OR
                        // 2. Current route is stale (>20s old) and this is a valid alternative, OR
                        // 3. Current next-hop is dead (not in neighbors anymore)
                        val currentNextHop = routingTable[packet.sourceId]
                        val currentHopDead = currentNextHop != null && !neighbors.containsKey(currentNextHop)
                        val isStale = routeAge > 20000L
                        
                        if (newScore > currentScore || currentHopDead || (isStale && newScore >= currentScore - 50)) {
                            val oldRoute = routingTable[packet.sourceId]
                            routingScores[packet.sourceId] = newScore
                            routingTable[packet.sourceId] = sourceEndpointId
                            routeLastSeen[packet.sourceId] = System.currentTimeMillis()
                            if (oldRoute != sourceEndpointId) {
                                log(
                                        "RoutingTable Updated: ${packet.sourceId} -> $sourceEndpointId (Score: $newScore, Prev: $currentScore, Hops: $hopCount, DeadHop: $currentHopDead)",
                                        com.fyp.resilientp2p.data.LogLevel.DEBUG
                                )
                            }
                        } else {
                            // Still refresh timestamp if same route (keep alive)
                            if (routingTable[packet.sourceId] == sourceEndpointId) {
                                routeLastSeen[packet.sourceId] = System.currentTimeMillis()
                            }
                        }
                    }
                    updateConnectedEndpoints()
                }
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

        // EMERGENCY packets: always process locally AND flood to all neighbors
        if (packet.type == PacketType.EMERGENCY) {
            if (packet.destId != localUsername && packet.destId != "BROADCAST") {
                processPacket(packet, sourceEndpointId)
            }
            // Always flood emergency regardless of destination (entire mesh must see it)
            if (packet.ttl > 0) {
                forwardPacket(packet, sourceEndpointId)
            }
            return
        }

        if (packet.destId != localUsername) {
            if (packet.ttl > 0) {
                forwardPacket(packet, sourceEndpointId)
            } else {
                log("PACKET_TTL_EXCEEDED id=${packet.id.take(8)} dest='${packet.destId}' src='${packet.sourceId}'",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
                networkStats.totalPacketsDropped.incrementAndGet()
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

                // CRITICAL: Only accept IDENTITY from the direct physical sender.
                // Forwarded IDENTITY packets corrupt neighbor names (e.g. relay "Tablet"
                // gets renamed to "Phone" when Phone's IDENTITY is forwarded through Tablet).
                // With ttl=0, forwarding is blocked. This is a belt-and-suspenders check:
                // reject if the sourceId doesn't match the endpoint's current known name
                // AND the endpoint already has a real name (not "Unknown").
                val existingName = neighbors[sourceEndpointId]?.peerName
                if (existingName != null && existingName != "Unknown" && existingName != packet.sourceId) {
                    log(
                            "IDENTITY_IGNORED src='${packet.sourceId}' via=$sourceEndpointId " +
                            "existingName='$existingName' reason=NAME_MISMATCH_FORWARDED",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )
                    return
                }

                // --- ECDH key exchange: register peer public key if present ---
                if (packet.payload.isNotEmpty()) {
                    try {
                        val peerPubKeyBase64 = String(packet.payload, java.nio.charset.StandardCharsets.UTF_8)
                        securityManager?.registerPeerKey(packet.sourceId, peerPubKeyBase64)
                        log("SECURITY_KEY_EXCHANGED peer='${packet.sourceId}'",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG, peerId = packet.sourceId)
                    } catch (e: Exception) {
                        log("SECURITY_KEY_EXCHANGE_FAILED peer='${packet.sourceId}' error='${e.message}'",
                            com.fyp.resilientp2p.data.LogLevel.WARN, peerId = packet.sourceId)
                    }
                }

                // ATOMIC IN-PLACE UPDATE: Mutate existing neighbor instead of replacing
                neighbors.compute(sourceEndpointId) { _, existing ->
                    if (existing != null) {
                        val oldName = existing.peerName
                        if (oldName != packet.sourceId) {
                            existing.peerName = packet.sourceId
                            // Transfer per-peer stats from old name to real name
                            networkStats.renamePeer(oldName, packet.sourceId)
                        }
                        // Guarantee stats entry exists even if rename found nothing
                        // (happens when multiple peers connect as "Unknown" simultaneously —
                        // the first rename consumes the shared "Unknown" key)
                        networkStats.ensurePeerTracked(packet.sourceId)
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
                                payload = packet.payload,
                                timestamp = System.currentTimeMillis(),
                                ttl = Packet.DEFAULT_TTL // Reset TTL for return trip
                        )
                // Use handlePacket("LOCAL") to route it correctly (direct send)
                handlePacket(pongPacket, "LOCAL")
            }
            PacketType.PONG -> {
                // HeartbeatManager listens to payloadEvents for RTT tracking
                // Also compute and display RTT here for user-initiated pings
                try {
                    if (packet.payload.size >= 8) {
                        val buffer = java.nio.ByteBuffer.wrap(packet.payload)
                        val originTimestamp = buffer.long
                        val rtt = System.currentTimeMillis() - originTimestamp
                        // Feed RTT to location estimator for trilateration
                        locationEstimator?.recordRtt(packet.sourceId, rtt.toDouble())
                        log(
                                "PONG_RECEIVED from='${packet.sourceId}' rtt=${rtt}ms",
                                com.fyp.resilientp2p.data.LogLevel.DEBUG,
                                peerId = packet.sourceId,
                                latencyMs = rtt
                        )
                    } else {
                        log("PONG_RECEIVED from='${packet.sourceId}'", com.fyp.resilientp2p.data.LogLevel.DEBUG, peerId = packet.sourceId)
                    }
                } catch (_: Exception) {
                    log("PONG_RECEIVED from='${packet.sourceId}'", com.fyp.resilientp2p.data.LogLevel.DEBUG, peerId = packet.sourceId)
                }
            }
            PacketType.ROUTE_ANNOUNCE -> {
                // Process route table advertisement from neighbor.
                // Payload format: "destName1:score1,destName2:score2,..."
                try {
                    val routeData = String(packet.payload, StandardCharsets.UTF_8)
                    if (routeData.isBlank()) return
                    
                    val entries = routeData.split(",")
                    val neighborNames = neighbors.values.map { it.peerName }.toSet()
                    var updated = false
                    synchronized(routingLock) {
                        // Collect advertised destinations for implicit withdrawal
                        val advertisedDests = mutableSetOf<String>()

                        for (entry in entries) {
                            val parts = entry.split(":")
                            if (parts.size != 2) continue
                            val destName = parts[0].trim()
                            val advertScore = parts[1].trim().toIntOrNull() ?: continue
                            advertisedDests.add(destName)

                            // Don't add route to self
                            if (destName == localUsername) continue
                            // Don't add routes for direct neighbors (they're in neighbors map)
                            if (destName in neighborNames) continue

                            // Score degrades by 100 per hop (indirect route penalty)
                            val adjustedScore = advertScore - 100
                            if (adjustedScore <= 0) continue

                            val currentScore = routingScores[destName] ?: Int.MIN_VALUE
                            val currentNextHop = routingTable[destName]
                            val currentHopDead = currentNextHop != null && !neighbors.containsKey(currentNextHop)

                            if (adjustedScore > currentScore || currentHopDead) {
                                routingTable[destName] = sourceEndpointId
                                routingScores[destName] = adjustedScore
                                routeLastSeen[destName] = System.currentTimeMillis()
                                updated = true
                            }
                        }

                        // Implicit route withdrawal: remove routes through this neighbor
                        // that are NO LONGER advertised. This clears ghost routes immediately
                        // instead of waiting for the 30s stale timeout.
                        val withdrawn = mutableListOf<String>()
                        routingTable.entries.toList().forEach { (dest, nextHop) ->
                            if (nextHop == sourceEndpointId && dest !in advertisedDests && dest !in neighborNames) {
                                routingTable.remove(dest)
                                routingScores.remove(dest)
                                routeLastSeen.remove(dest)
                                withdrawn.add(dest)
                                updated = true
                            }
                        }
                        if (withdrawn.isNotEmpty()) {
                            log("ROUTES_WITHDRAWN via=${packet.sourceId} removed=[${withdrawn.joinToString()}]",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG)
                        }
                    }
                    if (updated) {
                        updateConnectedEndpoints()
                        log("Route table updated from ${packet.sourceId} announcement", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                    }
                } catch (e: Exception) {
                    log("Error processing ROUTE_ANNOUNCE: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
            PacketType.FILE_META -> {
                // Store file metadata for correlating with incoming FILE payload
                try {
                    val json = org.json.JSONObject(String(packet.payload, StandardCharsets.UTF_8))
                    val payloadId = json.getLong("payloadId")
                    val fileName = json.getString("fileName")
                    val mimeType = json.getString("mimeType")
                    val fileSize = json.optLong("fileSize", -1)
                    pendingFileMetadata[payloadId] = FileMetadata(fileName, mimeType, fileSize, packet.sourceId)
                    log(
                        "FILE_META_RECEIVED from='${packet.sourceId}' payloadId=$payloadId name='$fileName' mime='$mimeType' size=$fileSize",
                        com.fyp.resilientp2p.data.LogLevel.INFO,
                        peerId = packet.sourceId
                    )
                } catch (e: Exception) {
                    log("FILE_META_PARSE_ERROR from='${packet.sourceId}' error='${e.message}'",
                        com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = packet.sourceId)
                }
            }
            PacketType.EMERGENCY -> {
                // Emergency broadcast — forward to EmergencyManager for UI alert
                log(
                    "\u26a0\ufe0f EMERGENCY from='${packet.sourceId}': ${String(packet.payload, StandardCharsets.UTF_8).take(100)}",
                    com.fyp.resilientp2p.data.LogLevel.WARN,
                    peerId = packet.sourceId
                )
                emergencyManager?.handleEmergencyPacket(packet)
                updateState { it.copy(emergencyCount = it.emergencyCount + 1) }
            }

            // --- Phase 4 packet types ---

            PacketType.GROUP_MESSAGE -> {
                // Route to group chat handler
                groupMessageHandler?.invoke(packet)
            }
            PacketType.FILE_ANNOUNCE -> {
                fileShareManager?.handleFileAnnounce(packet)
            }
            PacketType.FILE_REQUEST -> {
                fileShareManager?.handleFileRequest(packet)
            }
            PacketType.FILE_CHUNK -> {
                fileShareManager?.handleFileChunk(packet)
            }
            PacketType.ENCOUNTER_LOG -> {
                // Log receipt of DTN encounter broadcast — informational
                log(
                    "ENCOUNTER_LOG from='${packet.sourceId}': ${String(packet.payload, StandardCharsets.UTF_8).take(100)}",
                    com.fyp.resilientp2p.data.LogLevel.DEBUG,
                    peerId = packet.sourceId
                )
            }
            PacketType.LOCATION_PING -> {
                // Trilateration timing — handled via PONG RTT measurement
                locationEstimator?.recordRtt(
                    packet.sourceId,
                    (System.currentTimeMillis() - packet.timestamp).toDouble()
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
                    log("FORWARD_SERIALIZE_ERROR id=${packet.id.take(8)} error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
                    return
                }
        val payload = Payload.fromBytes(bytes)

        if (nextHop != null && nextHop != excludeEndpointId && neighbors.containsKey(nextHop)) {
            val hopPeerName = neighbors[nextHop]?.peerName ?: "Unknown"
            log(
                    "PACKET_FORWARD id=${packet.id.take(8)} dest='${packet.destId}' " +
                    "nextHop=$nextHop($hopPeerName) ttl=${newPacket.ttl} size=${bytes.size}B",
                    com.fyp.resilientp2p.data.LogLevel.DEBUG,
                    payloadSizeBytes = bytes.size
            )
            networkStats.totalPacketsForwarded.incrementAndGet()
            networkStats.recordPacketSent(hopPeerName, bytes.size)
            connectionsClient.sendPayload(nextHop, payload).addOnFailureListener { e ->
                log("FORWARD_FAILED id=${packet.id.take(8)} nextHop=$nextHop error='${e.message}'",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
                if (e.message?.contains("8012") == true) {
                    log("DEAD_ENDPOINT_8012 endpoint=$nextHop. Disconnecting.",
                            com.fyp.resilientp2p.data.LogLevel.WARN)
                    disconnectFromEndpoint(nextHop)
                }
            }
        } else if (packet.destId != "BROADCAST") {
            // Unicast with no valid route — try flood to all neighbors first
            val availableNeighbors = neighbors.keys.filter { it != excludeEndpointId }
            if (availableNeighbors.isNotEmpty()) {
                log(
                        "PACKET_FLOOD id=${packet.id.take(8)} dest='${packet.destId}' " +
                        "noRoute=true flooding=${availableNeighbors.size}peers",
                        com.fyp.resilientp2p.data.LogLevel.DEBUG
                )
                availableNeighbors.forEach { endpointId ->
                    val floodPeerName = neighbors[endpointId]?.peerName ?: "Unknown"
                    networkStats.totalPacketsForwarded.incrementAndGet()
                    networkStats.recordPacketSent(floodPeerName, bytes.size)
                    connectionsClient.sendPayload(endpointId, payload).addOnFailureListener { e ->
                        if (e.message?.contains("8012") == true) {
                            log("DEAD_ENDPOINT_8012 endpoint=$endpointId", com.fyp.resilientp2p.data.LogLevel.WARN)
                            disconnectFromEndpoint(endpointId)
                        }
                    }
                }
            } else {
                // No neighbors at all — queue for store-and-forward or try cloud relay
                if (newPacket.type == PacketType.DATA || newPacket.type == PacketType.STORE_FORWARD) {
                    // Try internet gateway relay if available
                    val gateway = internetGatewayManager
                    if (gateway != null && gateway.hasInternet.value && newPacket.type == PacketType.DATA) {
                        scope.launch {
                            val payloadStr = String(newPacket.payload, StandardCharsets.UTF_8)
                            val relayed = gateway.relayToCloud(newPacket.destId, newPacket.sourceId, payloadStr, newPacket.id)
                            if (relayed) {
                                log("PACKET_RELAYED_CLOUD id=${packet.id.take(8)} dest='${packet.destId}'",
                                    com.fyp.resilientp2p.data.LogLevel.INFO)
                            } else {
                                log("RELAY_FAILED_FALLBACK_SF id=${packet.id.take(8)} dest='${packet.destId}'",
                                    com.fyp.resilientp2p.data.LogLevel.INFO)
                                queueForStoreForward(newPacket)
                            }
                        }
                    } else {
                        log(
                                "PACKET_QUEUED_SF id=${packet.id.take(8)} dest='${packet.destId}' " +
                                "reason=NO_ROUTE_NO_NEIGHBORS type=${newPacket.type}",
                                com.fyp.resilientp2p.data.LogLevel.INFO
                        )
                        queueForStoreForward(newPacket)
                    }
                } else {
                    log("PACKET_DROPPED id=${packet.id.take(8)} dest='${packet.destId}' reason=NO_ROUTE type=${packet.type}",
                            com.fyp.resilientp2p.data.LogLevel.WARN)
                    networkStats.totalPacketsDropped.incrementAndGet()
                }
            }
        } else {
            // BROADCAST: Split Horizon — don't send back to sender
            val broadcastTargets = neighbors.keys.filter { it != excludeEndpointId }
            log(
                    "PACKET_BROADCAST id=${packet.id.take(8)} src='${packet.sourceId}' " +
                    "targets=${broadcastTargets.size} ttl=${newPacket.ttl}",
                    com.fyp.resilientp2p.data.LogLevel.TRACE
            )
            broadcastTargets.forEach { endpointId ->
                val bcPeerName = neighbors[endpointId]?.peerName ?: "Unknown"
                networkStats.totalPacketsForwarded.incrementAndGet()
                networkStats.recordPacketSent(bcPeerName, bytes.size)
                connectionsClient.sendPayload(endpointId, payload).addOnFailureListener { e ->
                    if (e.message?.contains("8012") == true) {
                        log("DEAD_ENDPOINT_8012 endpoint=$endpointId", com.fyp.resilientp2p.data.LogLevel.WARN)
                        disconnectFromEndpoint(endpointId)
                    }
                }
            }
        }
    }

    // --- Public Methods ---

    fun sendData(destId: String, message: String) {
        val payloadBytes = message.toByteArray(StandardCharsets.UTF_8)
        log(
                "SEND_DATA dest='$destId' size=${payloadBytes.size}B preview='${message.take(50)}'",
                com.fyp.resilientp2p.data.LogLevel.INFO,
                payloadSizeBytes = payloadBytes.size
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
                        payload = payloadBytes,
                        timestamp = System.currentTimeMillis()
                )
        handlePacket(packet, "LOCAL")
    }

    fun broadcastMessage(message: String) {
        sendData("BROADCAST", message)
    }

    fun sendPing(peerId: String, data: ByteArray) {
        log("PING_SENT dest='$peerId'", com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerId)
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
                        destId = "BROADCAST",
                        payload = data,
                        timestamp = System.currentTimeMillis()
                )
        // Send directly to the physical endpoint
        try {
            val bytes = packet.toBytes()
            val peerName = neighbors[endpointId]?.peerName ?: "Unknown"
            networkStats.recordPacketSent(peerName, bytes.size)
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e -> log("PING_SEND_FAILED endpoint=$endpointId error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN) }
        } catch (e: Exception) {
            log("PING_SEND_ERROR endpoint=$endpointId error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
        }
    }

    /**
     * Inject a pre-built packet into the mesh routing pipeline.
     * Used by [EmergencyManager] and [InternetGatewayManager] to send packets
     * without going through the public sendData/sendPing APIs.
     *
     * @param packet The packet to inject (will be processed as if from LOCAL)
     */
    fun injectPacket(packet: Packet) {
        handlePacket(packet, "LOCAL")
    }

    fun sendFile(peerName: String, uri: android.net.Uri) {
        // Resolve peer name to endpoint ID
        val endpointId = neighbors.entries.find { it.value.peerName == peerName }?.key
        if (endpointId == null) {
            log("FILE_SEND_ERROR peer='$peerName' reason=NO_ENDPOINT", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }

        // Extract filename and MIME type from URI
        val (fileName, fileSize) = context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                val nameIdx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                val sizeIdx = c.getColumnIndex(android.provider.OpenableColumns.SIZE)
                val name = if (nameIdx >= 0) c.getString(nameIdx) else null
                val size = if (sizeIdx >= 0) c.getLong(sizeIdx) else -1L
                Pair(name, size)
            } else null
        } ?: Pair(null, -1L)
        val resolvedFileName = fileName ?: uri.lastPathSegment ?: "file"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        // Nearby Connections API takes ownership of PFD for FILE payloads
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            log("FILE_SEND_ERROR peer='$peerName' reason=NULL_PFD", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }
        val payload = try {
            Payload.fromFile(pfd)
        } catch (e: Exception) {
            pfd.close()  // Safe - payload creation failed, API never got it
            log("FILE_SEND_ERROR peer='$peerName' error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }

        // Send metadata first so receiver knows the filename
        val payloadId = payload.id
        val metaJson = org.json.JSONObject().apply {
            put("payloadId", payloadId)
            put("fileName", resolvedFileName)
            put("mimeType", mimeType)
            put("fileSize", fileSize)
        }.toString()
        val metaPacket = com.fyp.resilientp2p.transport.Packet(
            type = com.fyp.resilientp2p.transport.PacketType.FILE_META,
            sourceId = localUsername,
            destId = peerName,
            payload = metaJson.toByteArray(),
            ttl = 0  // Direct only — FILE payloads can't be routed
        )
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(metaPacket.toBytes()))

        // Now send the actual file payload
        // After this point, Nearby API owns PFD - do NOT close in callbacks
        connectionsClient.sendPayload(endpointId, payload)
            .addOnSuccessListener {
                log("FILE_SENT peer='$peerName' name='$resolvedFileName' mime='$mimeType' payloadId=$payloadId",
                    com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)
            }
            .addOnFailureListener { e ->
                log("FILE_SEND_FAILED peer='$peerName' name='$resolvedFileName' error='${e.message}'",
                    com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
            }
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
        val peer = neighbors[endpointId]
        val peerName = peer?.peerName ?: "Unknown"
        val connectedAtMs = connectionTimestamps.remove(endpointId)
        val durationMs = if (connectedAtMs != null) System.currentTimeMillis() - connectedAtMs else -1L
        val durationStr = if (durationMs > 0) formatDuration(durationMs) else "unknown"
        
        log(
                "DISCONNECT_MANUAL endpoint=$endpointId peerName='$peerName' " +
                "duration=$durationStr(${durationMs}ms)",
                com.fyp.resilientp2p.data.LogLevel.DEBUG
        )
        
        connectionsClient.disconnectFromEndpoint(endpointId)
        
        // CRITICAL: Nearby API does NOT fire onDisconnected() on the local side when WE 
        // initiate the disconnect. We MUST clean up here ourselves, otherwise the neighbor
        // stays in the map as a ghost forever, blocking reconnection.
        networkStats.recordPeerDisconnected(peerName)
        neighbors.remove(endpointId)
        activeTransferEndpoints.remove(endpointId)
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
        // Also clear persisted logs in the database
        logDao?.let { dao ->
            scope.launch {
                try {
                    dao.deleteAll()
                } catch (e: Exception) {
                    Log.e("P2PManager", "Failed to clear DB logs: ${e.message}")
                }
            }
        }
    }

    // --- Helpers ---

    fun getNeighborsSnapshot(): Map<String, Neighbor> {
        return HashMap(neighbors)
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
        // Embed our ECDH public key in the IDENTITY payload for key exchange
        val pubKeyPayload = securityManager?.getPublicKeyBase64()
            ?.toByteArray(java.nio.charset.StandardCharsets.UTF_8) ?: ByteArray(0)

        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.IDENTITY,
                        sourceId = localUsername,
                        destId = "BROADCAST",
                        payload = pubKeyPayload,
                        timestamp = System.currentTimeMillis(),
                        ttl = 0 // CRITICAL: Never forward — IDENTITY is point-to-point only
                )
        val bytes = packet.toBytes()
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnFailureListener { e -> log("Failed to send IDENTITY to $endpointId: ${e.message}") }
    }

    private fun broadcastRouteAnnouncement() {
        // Poison Reverse: send per-neighbor route tables where routes learned
        // FROM a neighbor are advertised back to that neighbor with score=0 (unreachable).
        // This prevents count-to-infinity and speeds convergence on link failure.
        val neighborsSnapshot = HashMap(neighbors)

        // Check if this device is an internet gateway
        val isGateway = internetGatewayManager?.shouldAdvertiseGateway() == true
        // Update state for UI
        updateState { it.copy(isGateway = isGateway, hasInternet = internetGatewayManager?.hasInternet?.value == true) }

        neighborsSnapshot.keys.forEach { endpointId ->
            var routeData = synchronized(routingLock) {
                routingScores.entries
                    .filter { it.key != localUsername } // Don't advertise self
                    .joinToString(",") { (dest, score) ->
                        val nextHop = routingTable[dest]
                        if (nextHop == endpointId) {
                            // Poison Reverse: advertise 0 score for routes learned from this neighbor
                            "$dest:0"
                        } else {
                            "$dest:$score"
                        }
                    }
            }
            // Append __GATEWAY__ flag if this device has internet access
            if (isGateway) {
                routeData = if (routeData.isBlank()) {
                    "${InternetGatewayManager.GATEWAY_FLAG}:1"
                } else {
                    "$routeData,${InternetGatewayManager.GATEWAY_FLAG}:1"
                }
            }
            if (routeData.isBlank()) return@forEach

            val packet = Packet(
                id = java.util.UUID.randomUUID().toString(),
                type = PacketType.ROUTE_ANNOUNCE,
                sourceId = localUsername,
                destId = "BROADCAST",
                payload = routeData.toByteArray(StandardCharsets.UTF_8),
                timestamp = System.currentTimeMillis(),
                ttl = 0 // Route announcements are direct-only (0 = no further forwarding)
            )
            val bytes = packet.toBytes()
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
                .addOnFailureListener { e ->
                    log("Failed to send ROUTE_ANNOUNCE to $endpointId: ${e.message}", com.fyp.resilientp2p.data.LogLevel.TRACE)
                }
        }
    }

    private fun updateState(update: (P2PState) -> P2PState) {
        _state.update(update)
    }

    private fun updateConnectedEndpoints() {
        val neighborList = neighbors.values.map { it.peerName }.toList()
        val neighborNames = neighborList.toSet()
        val knownPeersMap = synchronized(routingLock) {
            // Proactively clean routing entries for direct neighbors (belt-and-suspenders)
            neighborNames.forEach { name ->
                routingTable.remove(name)
                routingScores.remove(name)
                routeLastSeen.remove(name)
            }
            routingTable.entries.associate { (dest, nextHop) ->
                // Calculate actual hop count from TTL-based score
                val score = routingScores[dest] ?: 0
                val estimatedHops = maxOf(1, Packet.DEFAULT_TTL - (score / 100))
                dest to RouteInfo(nextHop, estimatedHops)
            }
        }
        val snap = networkStats.snapshot(neighbors.size, routingTable.size)
        updateState { it.copy(connectedEndpoints = neighborList, knownPeers = knownPeersMap, stats = snap) }
    }

    fun setLogLevel(level: com.fyp.resilientp2p.data.LogLevel) {
        updateState { it.copy(logLevel = level) }
    }

    private fun startStatsDump() {
        if (statsDumpJob?.isActive == true) return
        statsDumpJob = scope.launch {
            while (isActive) {
                delay(30_000L) // Every 30 seconds
                try {
                    val snap = networkStats.snapshot(neighbors.size, routingTable.size)
                    val routeList = synchronized(routingLock) {
                        routingTable.entries.joinToString { "${it.key}->${it.value}(${routingScores[it.key] ?: 0})" }
                    }
                    val neighborList = neighbors.values.joinToString { "${it.peerName}(${it.peerId})" }
                    log(
                            "STATS_DUMP v=$appVersion uptime=${formatDuration(snap.uptimeMs)} " +
                            "battery=${snap.batteryLevel}%(${snap.batteryTemperature}\u00B0C) " +
                            "neighbors=${snap.currentNeighborCount}[$neighborList] " +
                            "routes=${snap.currentRouteCount}[$routeList] " +
                            "packets=\u2191${snap.totalPacketsSent}\u2193${snap.totalPacketsReceived}\u21BB${snap.totalPacketsForwarded}\u2717${snap.totalPacketsDropped} " +
                            "bytes=\u2191${formatBytes(snap.totalBytesSent)}\u2193${formatBytes(snap.totalBytesReceived)} " +
                            "avgRtt=${snap.avgRttMs}ms " +
                            "storeForward=Q${snap.storeForwardQueued}D${snap.storeForwardDelivered} " +
                            "connections=\u2191${snap.totalConnectionsEstablished}\u2193${snap.totalConnectionsLost}",
                            com.fyp.resilientp2p.data.LogLevel.METRIC
                    )
                    // Update UI state with latest stats
                    updateState { it.copy(stats = snap) }
                } catch (e: Exception) {
                    log("STATS_DUMP_ERROR error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
            hours > 0 -> "${hours}h${minutes % 60}m${seconds % 60}s"
            minutes > 0 -> "${minutes}m${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1fMB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format(java.util.Locale.US, "%.1fKB", bytes / 1024.0)
            else -> "${bytes}B"
        }
    }

    fun log(
            msg: String,
            level: com.fyp.resilientp2p.data.LogLevel = com.fyp.resilientp2p.data.LogLevel.INFO,
            type: com.fyp.resilientp2p.data.LogType = com.fyp.resilientp2p.data.LogType.SYSTEM,
            peerId: String? = null,
            latencyMs: Long? = null,
            payloadSizeBytes: Int? = null,
            rssi: Int? = null
    ) {
        val taggedMsg = "[com.fyp.resilientp2p] [$level] $msg"

        // 1. Always output to Logcat/Stdout (all levels)
        Log.d("P2PManager", taggedMsg)

        // 2. Persist to Room DB only at INFO level and above (ERROR, WARN, METRIC, INFO).
        //    TRACE and DEBUG are high-frequency protocol chatter (~100/sec with 3 peers).
        //    They remain available via logcat for live debugging but are excluded from
        //    DB persistence and CSV export to keep log volume manageable.
        //    Industry standard: ~1-5 persistent log entries/sec for production telemetry.
        if (level.ordinal <= com.fyp.resilientp2p.data.LogLevel.INFO.ordinal) {
            val entry =
                    com.fyp.resilientp2p.data.LogEntry(
                            timestamp = System.currentTimeMillis(),
                            message = msg,
                            level = level,
                            logType = type,
                            peerId = peerId,
                            rssi = rssi,
                            latencyMs = latencyMs,
                            payloadSizeBytes = payloadSizeBytes
                    )

            logDao?.let { dao ->
                scope.launch {
                    try {
                        dao.insert(entry)
                    } catch (e: Exception) {
                        Log.e("P2PManager", "Failed to persist log to DB: ${e.message}")
                    }
                }
            }

            // 3. Filter for UI Storage (respects user log level setting)
            val currentLevel = _state.value.logLevel
            if (level.ordinal <= currentLevel.ordinal) {
                updateState { state -> 
                    state.copy(logs = (state.logs + entry).takeLast(100)) 
                }
            }
        }
    }
}
