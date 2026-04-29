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
import com.fyp.resilientp2p.utils.FormatUtils
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
@Suppress("LargeClass", "TooManyFunctions") // Core networking engine requires comprehensive functionality
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
    private var fileTransferCleanupJob: kotlinx.coroutines.Job? = null

    // Network Statistics (public for HeartbeatManager RTT access)
    val networkStats = NetworkStats()

    // Connection timing for duration tracking
    private val connectionTimestamps = ConcurrentHashMap<String, Long>()

    // Battery Monitoring
    private val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val temp = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
            val voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
            if (level >= 0) networkStats.batteryLevel = level * 100 / scale
            networkStats.batteryTemperature = temp
            networkStats.batteryVoltageMilliV = voltage

            // µAh from charge counter (returns -1 or 0 on unsupported OEMs)
            val chargeUah = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER).toLong()
            if (chargeUah > 0) networkStats.batteryChargeUah = chargeUah
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

    // Ping response events (emitted when PONG is received with RTT)
    data class PongReceivedEvent(val peerName: String, val rttMs: Long)
    private val _pongReceivedEvents = MutableSharedFlow<PongReceivedEvent>()
    val pongReceivedEvents: SharedFlow<PongReceivedEvent> = _pongReceivedEvents.asSharedFlow()

    // Mesh Data
    private val neighbors = ConcurrentHashMap<String, Neighbor>() // endpointId -> Neighbor
    private val routingTable = ConcurrentHashMap<String, String>() // destId -> nextHopEndpointId
    private val routingScores = ConcurrentHashMap<String, Int>() // destId -> routeScore
    private val routingLock = Any()
    private val routeLastSeen = ConcurrentHashMap<String, Long>() // destId -> timestamp
    private val messageCache = MessageCache(capacity = 2000) // 2000 items capacity
    /** Neighbor endpoints that advertise internet gateway capability via ROUTE_ANNOUNCE. */
    private val gatewayNeighbors = ConcurrentHashMap.newKeySet<String>() // endpointId

    // Configuration
    private val strategy = Strategy.P2P_CLUSTER
    private val serviceId = "com.fyp.resilientp2p"
    private val maxConnections = 4
    private val stabilityWindowMs = 15000L
    private val reconnectDelayMs = 3000L
    private val reconnectMaxAttempts = 5
    private val storeForwardTtlMs = 2 * 60 * 60 * 1000L // 2 hours

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

    // Key-exchange gating: endpoints that haven't completed ECDH yet
    private val pendingKeyExchange = ConcurrentHashMap.newKeySet<String>() // endpointIds
    private val keyExchangeTimestamps = ConcurrentHashMap<String, Long>() // peerName -> epochMs
    private val lastSecurityRecovery = ConcurrentHashMap<String, Long>() // endpointId -> epochMs

    // Track rapid consecutive transfer failures per endpoint for proactive disconnect
    private val consecutiveFailures = ConcurrentHashMap<String, Int>() // endpointId -> count
    private val lastFailureTime = ConcurrentHashMap<String, Long>() // endpointId -> timestamp

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
    /** Per-peer reputation tracker (libp2p GossipSub-inspired). */
    var peerTrustScorer: com.fyp.resilientp2p.security.PeerTrustScorer? = null

    // Phase 4 manager references
    /** RTT-based trilateration engine. Set by [P2PApplication]. */
    var locationEstimator: LocationEstimator? = null
    /** DTN encounter logger. Set by [P2PApplication]. */
    var encounterLogger: EncounterLogger? = null
    /** Content-addressable file sharing manager. Set by [P2PApplication]. */
    var fileShareManager: FileShareManager? = null
    /** Cloud log upload manager. Set by [P2PApplication]. */
    var cloudLogManager: CloudLogManager? = null

    @Volatile private var internetPeers = emptySet<String>()

    // File transfer metadata: payloadId -> FileMetadata (set by FILE_META packet, consumed on transfer complete)
    data class FileMetadata(val fileName: String, val mimeType: String, val fileSize: Long, val senderName: String, val transferId: String = "")
    private val pendingFileMetadata = ConcurrentHashMap<Long, FileMetadata>()

    // Mesh file transfer state: transferId -> FileTransferState
    @Suppress("DataClassShouldBeImmutable") // receivedChunks is mutated during transfer
    data class FileTransferState(
        val metadata: FileMetadata,
        val chunks: MutableMap<Int, ByteArray> = mutableMapOf(),
        val totalChunks: Int,
        var receivedChunks: Int = 0,
        val startTime: Long = System.currentTimeMillis()
    )
    @Suppress("UnusedPrivateProperty") // Will be used in mesh file transfer implementation
    private val activeFileTransfers = ConcurrentHashMap<String, FileTransferState>()

    // Pending FILE payloads (stored until transfer completes) - for legacy direct transfers
    private val pendingFilePayloads = ConcurrentHashMap<Long, Payload>()

    @Suppress("DataClassShouldBeImmutable") // attemptCount/lastAttemptTime are mutated during reconnection retries
    data class ReconnectionEntry(
        val peerName: String,
        val lastEndpointId: String,
        var attemptCount: Int = 0,
        var lastAttemptTime: Long = 0
    )

    // Optional Managers
    var voiceManager: VoiceManager? = null
    /** Mesh-routed audio with AAC-LC codec. Set by [P2PApplication] or init. */
    var meshAudioManager: com.fyp.resilientp2p.audio.MeshAudioManager? = null

    // Data Classes for UI
    data class PayloadEvent(val endpointId: String, val packet: Packet)
    data class PayloadProgressEvent(val endpointId: String, val peerName: String, val progress: Int)

    // --- Callbacks ---

    private val payloadCallback =
            object : PayloadCallback() {
                @Suppress("NestedBlockDepth")
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
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
                                var packet = Packet.fromBytes(bytes)

                                // --- Security checks ---
                                // All checks key on endpointId (physical transport identity,
                                // assigned by Nearby Connections) rather than packet.sourceId
                                // (self-declared, spoofable). This is the same principle as
                                // libp2p connection gating — trust the transport, not the claim.
                                //
                                // 1. Blacklist check (by physical endpoint)
                                if (peerBlacklist?.isBlacklisted(endpointId) == true) {
                                    log("BLOCKED_BLACKLISTED endpoint=$endpointId(${packet.sourceId})",
                                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                    return
                                }
                                // 2. Rate limiting (tiered, keyed on endpointId)
                                val limiter = rateLimiter
                                if (limiter != null && !limiter.allowPacket(endpointId, packet.type)) {
                                    peerBlacklist?.recordViolation(endpointId, "rate_limit")
                                    peerTrustScorer?.recordRateLimitViolation(endpointId)
                                    log("RATE_LIMITED endpoint=$endpointId type=${packet.type} src=${packet.sourceId}",
                                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                    return
                                }
                                // 3. HMAC verification (end-to-end: only at the final destination)
                                // Intermediate forwarders skip HMAC check since they don't
                                // share the source↔destination key pair.
                                val security = securityManager
                                val requiresSecurePayload =
                                    packet.type == PacketType.DATA ||
                                        packet.type == PacketType.STORE_FORWARD ||
                                        packet.type == PacketType.ACK
                                val isDestination = packet.destId == localUsername
                                val hasSharedKey = security?.hasKeyForPeer(packet.sourceId) == true
                                val shouldVerifyHmac = security != null && requiresSecurePayload && isDestination && hasSharedKey
                                if (shouldVerifyHmac) {
                                    // Last HMAC_SIZE bytes are the HMAC
                                    if (packet.payload.size > com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE) {
                                        val dataBytes = packet.payload.copyOfRange(
                                            0,
                                            packet.payload.size -
                                                com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE
                                        )
                                        val hmacBytes = packet.payload.copyOfRange(
                                            packet.payload.size -
                                                com.fyp.resilientp2p.security.SecurityManager.HMAC_SIZE,
                                            packet.payload.size
                                        )
                                        if (!security.verifyHmac(packet.sourceId, dataBytes, hmacBytes)) {
                                            // Use separate "hmac" reason; this should NOT auto-blacklist
                                            // because HMAC race conditions during key re-exchange cause
                                            // false positives. Only log and drop the packet.
                                            peerTrustScorer?.recordHmacFailure(endpointId)
                                            // Grace period: within 3s of key exchange, log at DEBUG
                                            // (expected race between key registration and first verified packet)
                                            val keyTs = keyExchangeTimestamps[packet.sourceId]
                                            val gracePeriod = keyTs != null && System.currentTimeMillis() - keyTs < 3000L
                                            val logLevel = if (gracePeriod) com.fyp.resilientp2p.data.LogLevel.DEBUG
                                                           else com.fyp.resilientp2p.data.LogLevel.WARN
                                            log("HMAC_INVALID from=${packet.sourceId}${if (gracePeriod) " (grace period)" else ""} — dropping packet",
                                                logLevel, peerId = peerName)
                                            triggerSecurityRecovery(endpointId)
                                            return
                                        }
                                        // Strip HMAC from payload — dataBytes is the (encrypted) ciphertext
                                        packet = packet.copy(payload = dataBytes)
                                    }

                                    // Decrypt: Encrypt-then-MAC receive path (mirror of forwardPacket send)
                                    //   HMAC was verified above on the ciphertext; now decrypt to plaintext.
                                    //   If decrypt fails (key race, version mismatch), fall through with
                                    //   ciphertext — handlePacket will likely discard the garbled data
                                    //   gracefully rather than crash.
                                    val decrypted = security.decrypt(packet.sourceId, packet.payload)
                                    if (decrypted != null) {
                                        packet = packet.copy(payload = decrypted)
                                    } else {
                                        log("DECRYPT_FAIL from=${packet.sourceId} — proceeding with raw payload",
                                            com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                        triggerSecurityRecovery(endpointId)
                                    }
                                }

                                try {
                                    handlePacket(packet, endpointId)
                                    // Packet passed all checks (blacklist, rate limit, HMAC, decrypt)
                                    // and was processed successfully → reward the sending peer.
                                    peerTrustScorer?.recordValidPacket(endpointId)
                                } catch (e: Exception) {
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
                    val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER

                    // Keep updating lastSeen during long transfers (like Audio Stream or Big File)
                    if (update.status == PayloadTransferUpdate.Status.IN_PROGRESS) {
                        neighbors.computeIfPresent(endpointId) { _, neighbor -> neighbor.lastSeen.set(System.currentTimeMillis()); neighbor }
                        activeTransferEndpoints[endpointId] = System.currentTimeMillis()
                    }

                    // Log transfer completion/failure
                    when (update.status) {
                        PayloadTransferUpdate.Status.SUCCESS -> {
                            activeTransferEndpoints.remove(endpointId)
                            consecutiveFailures.remove(endpointId)
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
                                        // Read received file via ParcelFileDescriptor (non-deprecated API)
                                        val pfd = filePayload.asFile()?.asParcelFileDescriptor()
                                        val targetName = meta?.fileName ?: "received_${payloadId}"
                                        val receivedDir = java.io.File(context.filesDir, "received_files")
                                        receivedDir.mkdirs()
                                        val targetFile = java.io.File(receivedDir, targetName)
                                        if (pfd != null) {
                                            android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd).use { input ->
                                                targetFile.outputStream().use { output ->
                                                    input.copyTo(output)
                                                }
                                            }
                                            val senderName = meta?.senderName ?: peerName
                                            val mimeType = meta?.mimeType ?: "application/octet-stream"
                                            log(
                                                "FILE_RECEIVED from='$senderName' " +
                                                    "name='$targetName' mime='$mimeType' " +
                                                    "size=${update.bytesTransferred}",
                                                com.fyp.resilientp2p.data.LogLevel.INFO,
                                                peerId = senderName
                                            )
                                            _receivedFileEvents.emit(
                                                ReceivedFileEvent(senderName, targetName, mimeType, targetFile)
                                            )
                                        } else {
                                            log("FILE_RECEIVE_ERROR payloadId=$payloadId reason=NO_FILE_DESCRIPTOR",
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
                                    "TRANSFER_FAILED from=$endpointId($peerName) " +
                                    "transferred=${update.bytesTransferred}/${update.totalBytes} " +
                                    "payloadId=${update.payloadId}",
                                    com.fyp.resilientp2p.data.LogLevel.WARN,
                                    peerId = peerName
                            )

                            // Track consecutive failures for cascade detection
                            val now = System.currentTimeMillis()
                            val lastFail = lastFailureTime[endpointId] ?: 0L
                            if (now - lastFail < 5000) {
                                val count = consecutiveFailures.merge(endpointId, 1) { a, b -> a + b } ?: 1
                                if (count >= 5) {
                                    log("DEAD_LINK_DETECTED endpoint=$endpointId($peerName) " +
                                        "failures=$count — disconnecting",
                                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = peerName)
                                    consecutiveFailures.remove(endpointId)
                                    lastFailureTime.remove(endpointId)
                                    disconnectFromEndpoint(endpointId)
                                    return@onPayloadTransferUpdate
                                }
                            } else {
                                consecutiveFailures[endpointId] = 1
                            }
                            lastFailureTime[endpointId] = now

                            // Retry identity if this was an identity-sized packet that failed completely
                            if (update.bytesTransferred == 0L && update.totalBytes in 200..300 &&
                                neighbors.containsKey(endpointId)) {
                                log("IDENTITY_RETRY_SCHEDULED endpoint=$endpointId reason=TRANSFER_FAILED_0/${update.totalBytes}",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG)
                                scope.launch {
                                    delay(1000)
                                    if (neighbors.containsKey(endpointId)) {
                                        sendIdentityPacket(endpointId)
                                    }
                                }
                            }
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
                                        peerName,
                                        if (update.totalBytes > 0)
                                                (update.bytesTransferred * 100 /
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
                            "neighbors=$neighborCount/$maxConnections connecting=$connectingCount"
                    )

                    // Limit Connections (synchronized to prevent TOCTOU race)
                    synchronized(routingLock) {
                        if (neighbors.size >= maxConnections) {
                            log(
                                    "CONNECTION_REJECTED endpoint=$endpointId reason=maxConnections " +
                                    "current=$neighborCount max=$maxConnections " +
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
                        val isAlive = lastSeenMs < stabilityWindowMs

                        // Tie-Breaker Logic
                        if (isAlive && existingId > endpointId) {
                            log(
                                    "CONNECTION_REJECTED endpoint=$endpointId reason=DUPLICATE_ALIVE " +
                                    "peerName='${info.endpointName}' existingEp=$existingId " +
                                    "lastSeen=${lastSeenMs}ms window=${stabilityWindowMs}ms " +
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
                        pendingKeyExchange.remove(existingId)
                        securityManager?.removePeerKeys(info.endpointName)
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
                                Neighbor(endpointId, UNKNOWN_PEER, 0, System.currentTimeMillis())
                            }
                        }

                        val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
                        networkStats.recordPeerConnected(peerName)

                        // Clean slate: reset rate-limit violations and per-peer
                        // rate-limit windows on reconnect so the peer isn't penalised
                        // for stale counters from a previous session.
                        // Key on endpointId (physical identity) to match the security pipeline.
                        peerBlacklist?.resetViolations(endpointId)
                        rateLimiter?.resetPeer(endpointId)
                        peerTrustScorer?.resetPeer(endpointId)

                        log(
                                "CONNECTION_ESTABLISHED endpoint=$endpointId peerName='$peerName' " +
                                "status=$statusCode($statusName) " +
                                "totalNeighbors=${neighbors.size}/$maxConnections " +
                                "lifetimeConnections=${networkStats.totalConnectionsEstablished.get()} " +
                                "peers=[${neighbors.values.joinToString { "${it.peerName}(${it.peerId})" }}]",
                                com.fyp.resilientp2p.data.LogLevel.INFO,
                                peerId = peerName
                        )

                        updateConnectedEndpoints()

                        // Delay identity send to let transport settle (WiFi Direct/BLE handoff).
                        // Immediate sends frequently fail with TRANSFER_FAILED 0/241.
                        // Increased from 500ms to 1500ms to allow transport upgrade to complete.
                        pendingKeyExchange.add(endpointId)
                        scope.launch {
                            delay(1500)
                            if (neighbors.containsKey(endpointId)) {
                                sendIdentityPacket(endpointId)
                            } else {
                                pendingKeyExchange.remove(endpointId)
                                log("IDENTITY_SKIPPED endpoint=$endpointId reason=DISCONNECTED_DURING_DELAY",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG)
                            }
                        }

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
                    val peerName = disconnectedPeer?.peerName ?: UNKNOWN_PEER
                    val connectedAtMs = connectionTimestamps.remove(endpointId)
                    val durationMs = if (connectedAtMs != null) System.currentTimeMillis() - connectedAtMs else -1L
                    val durationStr = if (durationMs > 0) FormatUtils.formatDuration(durationMs) else UNKNOWN_DURATION

                    networkStats.recordPeerDisconnected(peerName)

                    neighbors.remove(endpointId)
                    activeTransferEndpoints.remove(endpointId)
                    pendingKeyExchange.remove(endpointId)
                    keyExchangeTimestamps.remove(peerName)
                    gatewayNeighbors.remove(endpointId) // Clear gateway flag on disconnect

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
                    if (peerName != UNKNOWN_PEER && peerName.isNotBlank()) {
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
                    val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
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
                            "neighbors=$neighborCount/$maxConnections connecting=$connectingCount",
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
                                "inNeighbors=${neighbors.containsKey(endpointId)} " +
                                "inConnecting=${_state.value.connectingEndpoints.contains(endpointId)}",
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

                    // Deterministic tiebreaker: only one side initiates to prevent
                    // bidirectional race conditions (both devices connecting simultaneously).
                    // Higher-name device initiates; lower-name device waits for incoming.
                    if (localUsername < info.endpointName) {
                        log(
                                "CONNECTION_DEFERRED endpoint=$endpointId name='${info.endpointName}' " +
                                "reason=LEXICOGRAPHIC_TIEBREAK local='$localUsername' < remote='${info.endpointName}'",
                                com.fyp.resilientp2p.data.LogLevel.DEBUG
                        )
                        return
                    }

                    log(
                            "CONNECTION_REQUESTING endpoint=$endpointId name='${info.endpointName}' " +
                            "currentNeighbors=$neighborCount/$maxConnections",
                            com.fyp.resilientp2p.data.LogLevel.INFO
                    )
                    connectionsClient
                            .requestConnection(
                                    localUsername,
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnSuccessListener {
                                log(
                                    "CONNECTION_REQUESTED endpoint=$endpointId " +
                                        "name='${info.endpointName}'",
                                    com.fyp.resilientp2p.data.LogLevel.DEBUG
                                )
                            }
                            .addOnFailureListener { e ->
                                val errorMsg = e.message ?: UNKNOWN_DURATION
                                log("CONNECTION_REQUEST_FAILED endpoint=$endpointId " +
                                        "name='${info.endpointName}' error='$errorMsg'",
                                        com.fyp.resilientp2p.data.LogLevel.WARN)
                                // Handle specific Nearby Connections error codes
                                when {
                                    errorMsg.contains("8007") -> {
                                        log("RADIO_ERROR endpoint=$endpointId — scheduling retry",
                                            com.fyp.resilientp2p.data.LogLevel.WARN)
                                        scope.launch {
                                            delay(2000)
                                            if (!neighbors.containsKey(endpointId)) {
                                                scheduleReconnection(info.endpointName, endpointId)
                                            }
                                        }
                                    }
                                    errorMsg.contains("8011") -> {
                                        log("ENDPOINT_UNKNOWN endpoint=$endpointId — peer vanished",
                                            com.fyp.resilientp2p.data.LogLevel.WARN)
                                    }
                                    errorMsg.contains("8012") -> {
                                        log("ENDPOINT_IO_ERROR endpoint=$endpointId — transport failure",
                                            com.fyp.resilientp2p.data.LogLevel.WARN)
                                    }
                                }
                            }
                }

                override fun onEndpointLost(endpointId: String) {
                    val wasNeighbor = neighbors.containsKey(endpointId)
                    val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
                    log(
                            "ENDPOINT_LOST endpoint=$endpointId peerName='$peerName' wasNeighbor=$wasNeighbor",
                            if (wasNeighbor) com.fyp.resilientp2p.data.LogLevel.INFO else com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )
                }
            }

    // --- Initialization ---

    private val appVersion: String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: UNKNOWN_DURATION
    } catch (_: Exception) { UNKNOWN_DURATION }

    init {
        log(
            "P2PManager INITIALIZED appVersion='$appVersion' " +
                "localIdentity='$localUsername' maxConnections=$maxConnections " +
                "strategy=P2P_CLUSTER serviceId='$serviceId'"
        )

        voiceManager = VoiceManager(context) { msg, level -> log(msg, level) }

        // Initialize mesh audio manager (AAC-encoded multi-hop audio)
        meshAudioManager = com.fyp.resilientp2p.audio.MeshAudioManager(
            context, localUsername
        ) { msg, level -> log(msg, level) }
        meshAudioManager?.sendPacket = { packet -> injectPacket(packet) }
        meshAudioManager?.getPeerRttMs = { peerId -> networkStats.peerRtt[peerId] ?: -1L }

        // Register battery monitor
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
            log("Battery monitor registered", com.fyp.resilientp2p.data.LogLevel.DEBUG)
        } catch (e: Exception) {
            log("BATTERY_MONITOR_FAILED error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN)
        }

        // Read battery design capacity once (from hidden PowerProfile or sysfs)
        try {
            val designCapMah = readBatteryDesignCapacityMah()
            if (designCapMah > 0) {
                networkStats.batteryDesignCapacityMah = designCapMah
                log("Battery design capacity: ${designCapMah} mAh", com.fyp.resilientp2p.data.LogLevel.INFO)
            } else {
                log("Battery design capacity unavailable (OEM does not expose it)", com.fyp.resilientp2p.data.LogLevel.WARN)
            }
        } catch (e: Exception) {
            log("BATTERY_CAPACITY_READ_FAILED error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.WARN)
        }
    }

    /**
     * Read battery design capacity in mAh from Android's hidden `PowerProfile` class
     * (reflection) or the `/sys/class/power_supply/battery/charge_full_design` sysfs node.
     * Returns 0 if neither source is available.
     */
    private fun readBatteryDesignCapacityMah(): Int {
        // Attempt 1: PowerProfile reflection (works on most Samsung, Pixel, Lenovo)
        try {
            val powerProfileClass = Class.forName("com.android.internal.os.PowerProfile")
            val constructor = powerProfileClass.getConstructor(Context::class.java)
            val profile = constructor.newInstance(context)
            val method = powerProfileClass.getMethod("getBatteryCapacity")
            val capacity = (method.invoke(profile) as Double).toInt()
            if (capacity > 0) return capacity
        } catch (_: Exception) { /* Not available */ }

        // Attempt 2: sysfs (µAh or mAh depending on kernel)
        try {
            val sysfsFile = java.io.File("/sys/class/power_supply/battery/charge_full_design")
            if (sysfsFile.exists()) {
                val raw = sysfsFile.readText().trim().toLongOrNull() ?: 0L
                // Kernel reports in µAh on most devices, mAh on some
                return if (raw > 100_000) (raw / 1000).toInt() else raw.toInt()
            }
        } catch (_: Exception) { /* Not available */ }

        return 0
    }

    private fun startRoutingUpdates() {
        if (routingJob?.isActive == true) return
        routingJob =
        scope.launch {
            var consecutiveErrors = 0
            while (isActive) {
                delay(15000L + (0..5000).random().toLong())

                try {
                    log(
                            "Broadcasting Periodic Routing Update neighbors=${neighbors.size} routes=${routingTable.size}",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )
                    HashMap(neighbors).keys.forEach { endpointId -> sendIdentityPacket(endpointId) }

                    // Broadcast route announcements to neighbors
                    broadcastRouteAnnouncement()

                    // Maintenance
                    pruneRoutes()
                    consecutiveErrors = 0 // Reset backoff on success
                } catch (e: Exception) {
                    consecutiveErrors++
                    // Exponential backoff: 5s, 10s, 20s, 40s, capped at 60s
                    val backoffMs = minOf(5000L * (1L shl (consecutiveErrors - 1).coerceAtMost(3)), 60_000L)
                    log(
                            "Error in Routing Update Loop (attempt $consecutiveErrors): ${e.message} " +
                            "— backing off ${backoffMs}ms",
                            com.fyp.resilientp2p.data.LogLevel.ERROR
                    )
                    delay(backoffMs)
                }
            }
        }
    }


    private fun pruneRoutes() {
        synchronized(routingLock) {
        val now = System.currentTimeMillis()
        val staleThreshold = 65000L // 65 seconds (route announcements every 15-20s, ~3 cycles + buffer)
        // CRITICAL FIX: Operate atomically per key to avoid TOCTOU
        routeLastSeen.entries.toList().forEach { (key, lastSeenTime) ->
            if (now - lastSeenTime > staleThreshold && routeLastSeen.remove(key, lastSeenTime)) {
                routingTable.remove(key)
                routingScores.remove(key)
                log("Pruned stale route: $key", com.fyp.resilientp2p.data.LogLevel.DEBUG)
            }
        }
        } // synchronized(routingLock)
        updateConnectedEndpoints()
    }

    // --- Reconnection Logic ---

    private fun scheduleReconnection(peerName: String, lastEndpointId: String) {
        val existing = reconnectionQueue[peerName]
        if (existing != null && existing.attemptCount >= reconnectMaxAttempts) {
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
        log(
            "Queued reconnection for $peerName " +
                "(attempt ${reconnectionQueue[peerName]?.attemptCount}/$reconnectMaxAttempts)",
            com.fyp.resilientp2p.data.LogLevel.DEBUG
        )

        ensureReconnectionLoopRunning()
    }

    private fun ensureReconnectionLoopRunning() {
        if (reconnectionJob?.isActive == true) return
        reconnectionJob = scope.launch {
            while (isActive && reconnectionQueue.isNotEmpty()) {
                delay(reconnectDelayMs)

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
                    val backoff = reconnectDelayMs * (1L shl minOf(entry.attemptCount, 4))
                    if (now - entry.lastAttemptTime > backoff) {
                        if (entry.attemptCount >= reconnectMaxAttempts) {
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

                        log(
                            "Store-Forward: Found ${queued.size} queued packets " +
                                "for $destId, attempting delivery...",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG
                        )

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
                                            log(
                                                "STORE_FORWARD_DELIVERED " +
                                                    "id=${entity.id.take(8)} dest=$destId via=$nextHop",
                                                com.fyp.resilientp2p.data.LogLevel.DEBUG
                                            )
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

                        for (packet in packets) {
                            try {
                                val bytes = packet.toBytes()
                                connectionsClient.sendPayload(nextHop, Payload.fromBytes(bytes))
                                    .addOnSuccessListener {
                                        networkStats.storeForwardDelivered.incrementAndGet()
                                        packets.remove(packet)
                                        // Atomic conditional removal to prevent race with queueForStoreForward
                                        pendingMessages.compute(destId) { _, list ->
                                            if (list == null || list.isEmpty()) null else list
                                        }
                                        log("STORE_FORWARD_DELIVERED_MEM dest=$destId", com.fyp.resilientp2p.data.LogLevel.DEBUG)
                                    }
                                    .addOnFailureListener { e ->
                                        log("Store-Forward: Send failed for dest=$destId: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                                    }
                            } catch (e: Exception) {
                                log("Store-Forward: Error sending in-memory packet: ${e.message}", com.fyp.resilientp2p.data.LogLevel.WARN)
                            }
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
                        expiration = System.currentTimeMillis() + storeForwardTtlMs,
                        sourceId = packet.sourceId
                    )
                    dao.insertPacket(entity)
                    log(
                        "Store-Forward: Queued packet ${packet.id} " +
                            "for ${packet.destId} (TTL: ${storeForwardTtlMs / 3600000}h)",
                        com.fyp.resilientp2p.data.LogLevel.DEBUG
                    )
                } catch (e: Exception) {
                    Log.e("P2PManager", "Failed to queue packet to DB: ${e.message}")
                }
            }
        }
    }

    private fun triggerStoreForwardDelivery(newEndpointId: String) {
        scope.launch {
            // Wait for ECDH key exchange to complete (poll up to 5s)
            var waited = 0L
            while (pendingKeyExchange.contains(newEndpointId) && waited < 5000L) {
                delay(250)
                waited += 250
            }
            if (pendingKeyExchange.contains(newEndpointId)) {
                log("Store-Forward: key exchange still pending for $newEndpointId after ${waited}ms — skipping delivery",
                    com.fyp.resilientp2p.data.LogLevel.WARN)
                return@launch
            }
            val peerName = neighbors[newEndpointId]?.peerName ?: return@launch

            // Check in-memory pending messages
            val pending = pendingMessages[peerName]
            if (pending != null && pending.isNotEmpty()) {
                log("Store-Forward: New neighbor $peerName — delivering ${pending.size} queued messages", com.fyp.resilientp2p.data.LogLevel.INFO)
                val delivered = mutableListOf<Packet>()
                for (packet in pending) {
                    try {
                        forwardPacket(packet, newEndpointId)
                        networkStats.storeForwardDelivered.incrementAndGet()
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
                                forwardPacket(packet, newEndpointId)
                                scope.launch { dao.deletePacket(entity.id) }
                                networkStats.storeForwardDelivered.incrementAndGet()
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
        // Re-register battery receiver (unregistered by stopAll)
        try {
            context.registerReceiver(batteryReceiver, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {} // May already be registered on first start
        startAdvertising()
        startDiscovery()
        startRoutingUpdates()
        startStoreForwardLoop()
        startStatsDump()
        startFileTransferCleanup()
    }

    fun stop() {
        stopAll()
    }

    fun stopAll() {
        val snap = networkStats.snapshot(neighbors.size, routingTable.size)
        log(
                "STOP_ALL initiated neighbors=${neighbors.size} routes=${routingTable.size} " +
                "totalPackets=↑${snap.totalPacketsSent}↓${snap.totalPacketsReceived} " +
                "uptime=${FormatUtils.formatDuration(snap.uptimeMs)}"
        )
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {}

        // Stop active voice streams so recorder/player file descriptors are released
        // before endpoint teardown.
        voiceManager?.stopRecording()
        voiceManager?.stopPlaying()

        // 1. Null out all job references BEFORE cancelling the scope so that
        //    any concurrent startXxx() calls don't see "isActive == true" and skip
        //    re-launching after the new scope is created.
        routingJob = null
        statsDumpJob = null
        fileTransferCleanupJob = null
        storeForwardJob = null
        reconnectionJob = null

        // 2. Cancel the supervisorJob — this immediately propagates CancellationException
        //    to ALL child coroutines atomically. No child can launch new work after this.
        supervisorJob.cancel()

        // 3. Clear all data structures AFTER cancellation is initiated.
        //    Children are now in a cancelled state and will not access maps again.
        synchronized(routingLock) {
            neighbors.clear()
            routingTable.clear()
            routingScores.clear()
            routeLastSeen.clear()
        }
        reconnectionQueue.clear()
        activeTransferEndpoints.clear()
        pendingMessages.clear()
        connectionTimestamps.clear()
        messageCache.clear()
        gatewayNeighbors.clear()

        // Clear file transfer state
        activeFileTransfers.clear()
        pendingFileMetadata.clear()
        pendingFilePayloads.clear()

        locationEstimator?.reset()
        securityManager?.destroy()

        // 4. Stop Nearby Connections (safe to do after map clear; uses its own internal state).
        connectionsClient.stopAllEndpoints()

        // 5. Recreate scope so the manager can be restarted (start() → stopAll() → start()).
        supervisorJob = SupervisorJob()
        scope = CoroutineScope(Dispatchers.IO + supervisorJob)

        // 6. Reset state atomically on the new scope.
        _state.update { P2PState(localDeviceName = localUsername) }
        log("STOP_ALL completed. All state cleared.")
    }


    // --- Nearby Connections Actions ---

    fun startAdvertising() {
        // Apply LowPower and Hybrid Logic
        val advertisingOptions =
                AdvertisingOptions.Builder()
                        .setStrategy(strategy)
                        .setLowPower(_state.value.isLowPower)
                        .build()

        connectionsClient
                .startAdvertising(
                        localUsername,
                        serviceId,
                        connectionLifecycleCallback,
                        advertisingOptions
                )
                .addOnSuccessListener {
                    log("ADVERTISING_STARTED lowPower=${_state.value.isLowPower} serviceId='$serviceId'")
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
                        .setStrategy(strategy)
                        .setLowPower(_state.value.isLowPower)
                        .build()
        connectionsClient
                .startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
                .addOnSuccessListener {
                    log("DISCOVERY_STARTED lowPower=${_state.value.isLowPower} serviceId='$serviceId'")
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

    @Suppress("CyclomaticComplexMethod")
    private fun handlePacket(packet: Packet, sourceEndpointId: String) {
        // Skip message-cache dedup for AUDIO_DATA — they arrive at high frequency
        // (10+ per second) and use their own sequence numbers for ordering.
        // Caching each UUID would thrash the 2000-entry message cache.
        if (packet.type != PacketType.AUDIO_DATA && !messageCache.tryMarkSeen(packet.id)) {
            log("PACKET_DEDUP id=${packet.id.take(8)} type=${packet.type} src='${packet.sourceId}'",
                    com.fyp.resilientp2p.data.LogLevel.TRACE)
            if (packet.type != PacketType.IDENTITY && packet.type != PacketType.ROUTE_ANNOUNCE) {
                networkStats.totalPacketsDropped.incrementAndGet()
            }
            return
        }

        val peerName = if (sourceEndpointId == LOCAL_SOURCE) LOCAL_SOURCE else neighbors[sourceEndpointId]?.peerName ?: UNKNOWN_PEER
        log(
                "PACKET_IN id=${packet.id.take(8)} type=${packet.type} " +
                "src='${packet.sourceId}' dst='${packet.destId}' " +
                "via=$sourceEndpointId($peerName) ttl=${packet.ttl} payloadSize=${packet.payload.size}B",
                com.fyp.resilientp2p.data.LogLevel.DEBUG,
                peerId = packet.sourceId,
                payloadSizeBytes = packet.payload.size
        )

        // Stats tracking for received packets (not LOCAL)
        if (sourceEndpointId != LOCAL_SOURCE) {
            networkStats.recordPacketReceived(peerName, packet.payload.size)
        }

        if (sourceEndpointId != LOCAL_SOURCE) {
            // Update lastSeen for the direct neighbor to prevent zombie disconnection
            neighbors[sourceEndpointId]?.let { it.lastSeen.set(System.currentTimeMillis()) }

            // CRITICAL FIX: Do not learn route to Self via neighbor (Self-Poisoning)
            if (packet.sourceId != localUsername) {
                // Don't add routing entries for direct neighbors — they're already in neighbors map.
                // Keeping them out of routingTable avoids showing duplicate entries (direct + routed).
                val isDirectNeighbor = neighbors.values.any { it.peerName == packet.sourceId }
                // Defensive: also check if the source endpoint IS the direct link endpoint
                // (catches case where identity hasn't resolved yet so peerName is still "Unknown")
                val sourceIsDirectLink = sourceEndpointId == neighbors.entries
                    .find { it.value.peerName == packet.sourceId }
                    ?.key
                    || sourceEndpointId == routingTable[packet.sourceId]?.let { nextHop ->
                        // If next hop for this source IS the sending endpoint, it's direct
                        nextHop.takeIf { it == sourceEndpointId }
                    }
                if (!isDirectNeighbor && !sourceIsDirectLink) {
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

                        @Suppress("ComplexCondition")
                        if (newScore > currentScore || currentHopDead || isStale && newScore >= currentScore - 50) {
                            val oldRoute = routingTable[packet.sourceId]
                            routingScores[packet.sourceId] = newScore
                            routingTable[packet.sourceId] = sourceEndpointId
                            routeLastSeen[packet.sourceId] = System.currentTimeMillis()
                            if (oldRoute != sourceEndpointId) {
                                log(
                                        "RoutingTable Updated: ${packet.sourceId} " +
                                            "-> $sourceEndpointId (Score: $newScore, " +
                                            "Prev: $currentScore, Hops: $hopCount, " +
                                            "DeadHop: $currentHopDead)",
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

        // IDENTITY and ROUTE_ANNOUNCE are always point-to-point (TTL=0, sent directly
        // via sendPayload, never forwarded). Process them regardless of destId because
        // the sender may not know our name yet (identity not yet exchanged).
        val isControlPacket = packet.type == PacketType.IDENTITY ||
                packet.type == PacketType.ROUTE_ANNOUNCE

        // HeartbeatManager PINGs arrive with destId="Unknown" before identity resolves.
        // Process these directly — they're from an adjacent neighbor, not multi-hop.
        val isDirectPingForUs = packet.type == PacketType.PING &&
                sourceEndpointId != LOCAL_SOURCE &&
                (packet.destId == localUsername || packet.destId == UNKNOWN_PEER)

        val isForLocalProcessing = packet.destId == localUsername ||
                packet.destId == BROADCAST_DEST ||
                isControlPacket ||
                isDirectPingForUs
        if (isForLocalProcessing) {
            processPacket(packet, sourceEndpointId)
        }

        // EMERGENCY packets: always process locally AND flood to all neighbors
        if (packet.type == PacketType.EMERGENCY) {
            if (packet.destId != localUsername && packet.destId != BROADCAST_DEST) {
                processPacket(packet, sourceEndpointId)
            }
            // Always flood emergency regardless of destination (entire mesh must see it)
            // Forwarding budget still applies to prevent one peer from amplifying
            // emergency floods (BATMAN-adv uses similar OGM forwarding limits).
            if (packet.ttl > 0 && sourceEndpointId != LOCAL_SOURCE) {
                val fwdAllowed = rateLimiter?.allowForward(sourceEndpointId) ?: true
                if (fwdAllowed) {
                    forwardPacket(packet, sourceEndpointId)
                } else {
                    log("FORWARD_BUDGET_EXCEEDED_EMERGENCY src=${packet.sourceId} via=$sourceEndpointId",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
                    peerTrustScorer?.recordForwardBudgetExceeded(sourceEndpointId)
                    networkStats.totalPacketsDropped.incrementAndGet()
                }
            } else if (packet.ttl > 0) {
                forwardPacket(packet, sourceEndpointId)
            }
            return
        }

        if (packet.destId != localUsername && !isControlPacket && !isDirectPingForUs) {
            if (packet.ttl > 0) {
                // Forwarding budget: limit how many packets any single peer can cause
                // us to relay, preventing amplification attacks (libp2p GossipSub principle).
                val fwdAllowed = if (sourceEndpointId != LOCAL_SOURCE) {
                    rateLimiter?.allowForward(sourceEndpointId) ?: true
                } else {
                    true  // locally originated — no budget limit
                }
                if (fwdAllowed) {
                    forwardPacket(packet, sourceEndpointId)
                } else {
                    log("FORWARD_BUDGET_EXCEEDED id=${packet.id.take(8)} src=${packet.sourceId} via=$sourceEndpointId",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
                    peerTrustScorer?.recordForwardBudgetExceeded(sourceEndpointId)
                    networkStats.totalPacketsDropped.incrementAndGet()
                }
            } else {
                // IDENTITY and ROUTE_ANNOUNCE always have TTL=0 by design — don't count as drops
                // BROADCAST packets naturally expire after flood fanout and are expected.
                if (packet.type != PacketType.IDENTITY &&
                    packet.type != PacketType.ROUTE_ANNOUNCE &&
                    packet.destId != BROADCAST_DEST) {
                    log("PACKET_TTL_EXCEEDED id=${packet.id.take(8)} dest='${packet.destId}' src='${packet.sourceId}'",
                            com.fyp.resilientp2p.data.LogLevel.WARN)
                    networkStats.totalPacketsDropped.incrementAndGet()
                }
            }
        }
    }

    @Suppress("CyclomaticComplexMethod", "LongMethod", "NestedBlockDepth") // Complex packet routing logic
    private fun processPacket(packet: Packet, sourceEndpointId: String) {
        scope.launch { _payloadEvents.emit(PayloadEvent(sourceEndpointId, packet)) }

        when (packet.type) {
            PacketType.DATA -> {
                // CRITICAL FIX: Validate payload is valid UTF-8 text before processing as chat message
                // This prevents binary data (corrupted AUDIO_DATA packets) from being logged as garbled text
                val textContent = try {
                    val decoded = String(packet.payload, StandardCharsets.UTF_8)
                    // Validate UTF-8: reject binary data that contains control characters
                    val hasControlChars = decoded.any { char ->
                        char.code < 32 && char != '\n' && char != '\r' && char != '\t'
                    }
                    if (hasControlChars) {
                        log("DATA_PACKET_INVALID_UTF8 from='${packet.sourceId}' size=${packet.payload.size}B — dropping binary payload",
                            com.fyp.resilientp2p.data.LogLevel.WARN, peerId = packet.sourceId)
                        return
                    }
                    decoded
                } catch (e: Exception) {
                    log("DATA_PACKET_DECODE_ERROR from='${packet.sourceId}' error='${e.message}' — dropping malformed payload",
                        com.fyp.resilientp2p.data.LogLevel.WARN, peerId = packet.sourceId)
                    return
                }

                // Intercept log relay packets — gateways upload on behalf of offline peers
                if (textContent.startsWith(CloudLogManager.LOG_RELAY_PREFIX)) {
                    cloudLogManager?.handleRelayedLogs(
                        textContent.removePrefix(CloudLogManager.LOG_RELAY_PREFIX)
                    )
                    return
                }

                // Intercept proxy relay requests — gateways relay to Firestore on behalf of
                // non-gateway mesh peers that want to reach internet-only peers.
                if (textContent.startsWith(InternetGatewayManager.PROXY_RELAY_PREFIX)) {
                    internetGatewayManager?.handleProxyRelayRequest(packet)
                    return
                }

                log(
                        "Message: $textContent",
                        com.fyp.resilientp2p.data.LogLevel.INFO,
                        com.fyp.resilientp2p.data.LogType.CHAT,
                        packet.sourceId
                )
                // Send ACK back to sender for end-to-end delivery confirmation.
                // Payload = original packet ID so sender can correlate.
                if (packet.destId == localUsername) {
                    sendAck(packet.sourceId, packet.id)
                }
            }
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
                if (existingName != null && existingName != UNKNOWN_PEER && existingName != packet.sourceId) {
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
                        pendingKeyExchange.remove(sourceEndpointId)
                        keyExchangeTimestamps[packet.sourceId] = System.currentTimeMillis()
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
                            // Clean up stale keys under old name (e.g. "Unknown" → real name)
                            if (oldName != UNKNOWN_PEER) {
                                securityManager?.removePeerKeys(oldName)
                            }
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
                // Send PONG directly back to the physical endpoint that sent the PING.
                // This avoids routing-pipeline issues and guarantees the PONG returns
                // to the adjacent neighbor (HeartbeatManager PINGs are always 1-hop).
                if (sourceEndpointId != LOCAL_SOURCE && neighbors.containsKey(sourceEndpointId)) {
                    try {
                        val bytes = pongPacket.toBytes()
                        val peerName = neighbors[sourceEndpointId]?.peerName ?: UNKNOWN_PEER
                        networkStats.recordPacketSent(peerName, bytes.size)
                        connectionsClient.sendPayload(sourceEndpointId, Payload.fromBytes(bytes))
                    } catch (e: Exception) {
                        log("PONG_SEND_ERROR endpoint=$sourceEndpointId error='${e.message}'",
                                com.fyp.resilientp2p.data.LogLevel.ERROR)
                    }
                } else {
                    // User-initiated ping (LOCAL_SOURCE) or source left — use mesh routing
                    handlePacket(pongPacket, LOCAL_SOURCE)
                }
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
                        // Emit event for UI notification
                        scope.launch {
                            _pongReceivedEvents.emit(PongReceivedEvent(packet.sourceId, rtt))
                        }
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
                    log("ROUTE_ANNOUNCE_RECEIVED from='${packet.sourceId}' via=$sourceEndpointId data='$routeData'",
                        com.fyp.resilientp2p.data.LogLevel.DEBUG, peerId = packet.sourceId)

                    if (routeData.isBlank()) return

                    val entries = routeData.split(",")
                    val neighborNames = neighbors.values.map { it.peerName }.toSet()
                    var updated = false
                    var routesAdded = 0
                    var routesSkipped = 0

                    synchronized(routingLock) {
                        // Collect advertised destinations for implicit withdrawal
                        val advertisedDests = mutableSetOf<String>()

                        @Suppress("LoopWithTooManyJumpStatements")
                        for (entry in entries) {
                            val parts = entry.split(":")
                            if (parts.size != 2) continue
                            val destName = parts[0].trim()
                            val advertScore = parts[1].trim().toIntOrNull() ?: continue
                            advertisedDests.add(destName)

                            // Don't add route to self
                            if (destName == localUsername) {
                                routesSkipped++
                                continue
                            }
                            // Don't add routes for direct neighbors (they're in neighbors map)
                            if (destName in neighborNames) {
                                routesSkipped++
                                continue
                            }

                            // Score degrades by 100 per hop (indirect route penalty)
                            val adjustedScore = advertScore - 100
                            if (adjustedScore <= 0) {
                                routesSkipped++
                                continue
                            }

                            val currentScore = routingScores[destName] ?: Int.MIN_VALUE
                            val currentNextHop = routingTable[destName]
                            val currentHopDead = currentNextHop != null && !neighbors.containsKey(currentNextHop)

                            if (adjustedScore > currentScore || currentHopDead) {
                                routingTable[destName] = sourceEndpointId
                                routingScores[destName] = adjustedScore
                                routeLastSeen[destName] = System.currentTimeMillis()
                                updated = true
                                routesAdded++
                                log("ROUTE_ADDED dest='$destName' via='${packet.sourceId}' score=$adjustedScore (was=$currentScore)",
                                    com.fyp.resilientp2p.data.LogLevel.INFO)
                            } else {
                                routesSkipped++
                            }
                        }

                        // Track gateway flag: if __GATEWAY__:1 appears in this announcement,
                        // mark the sending endpoint as a gateway neighbor so we can proxy
                        // cloud relay through it when we ourselves lack internet access.
                        if (InternetGatewayManager.GATEWAY_FLAG in advertisedDests) {
                            gatewayNeighbors.add(sourceEndpointId)
                        } else {
                            gatewayNeighbors.remove(sourceEndpointId)
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

                    log(
                        "ROUTE_ANNOUNCE_PROCESSED from='${packet.sourceId}' " +
                        "added=$routesAdded skipped=$routesSkipped totalRoutes=${routingTable.size}",
                        com.fyp.resilientp2p.data.LogLevel.INFO,
                        peerId = packet.sourceId
                    )

                    if (updated) {
                        updateConnectedEndpoints()
                    }
                } catch (e: Exception) {
                    log("Error processing ROUTE_ANNOUNCE: ${e.message}", com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
            PacketType.FILE_META -> {
                try {
                    val json = org.json.JSONObject(String(packet.payload, StandardCharsets.UTF_8))

                    // Check if this is a mesh transfer (has transferId) or legacy direct transfer (has payloadId)
                    if (json.has(TRANSFER_ID_FIELD)) {
                        // Mesh file transfer metadata
                        val transferId = json.getString(TRANSFER_ID_FIELD)
                        val fileName = json.getString("fileName")
                        val mimeType = json.getString("mimeType")
                        val fileSize = json.getLong("fileSize")
                        val totalChunks = json.getInt("totalChunks")

                        val metadata = FileMetadata(fileName, mimeType, fileSize, packet.sourceId, transferId)
                        val transferState = FileTransferState(metadata, mutableMapOf(), totalChunks)
                        activeFileTransfers[transferId] = transferState

                        log(
                            "FILE_META_MESH_RECEIVED from='${packet.sourceId}' transferId=${transferId.take(8)} " +
                            "name='$fileName' mime='$mimeType' size=$fileSize chunks=$totalChunks",
                            com.fyp.resilientp2p.data.LogLevel.INFO,
                            peerId = packet.sourceId
                        )
                    } else {
                        // Legacy direct transfer metadata
                        val payloadId = json.getLong("payloadId")
                        val fileName = json.getString("fileName")
                        val mimeType = json.getString("mimeType")
                        val fileSize = json.optLong("fileSize", -1)
                        pendingFileMetadata[payloadId] = FileMetadata(fileName, mimeType, fileSize, packet.sourceId)

                        log(
                            "FILE_META_DIRECT_RECEIVED from='${packet.sourceId}' payloadId=$payloadId " +
                            "name='$fileName' mime='$mimeType' size=$fileSize",
                            com.fyp.resilientp2p.data.LogLevel.INFO,
                            peerId = packet.sourceId
                        )
                    }
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
            PacketType.FILE_ANNOUNCE -> {
                fileShareManager?.handleFileAnnounce(packet)
            }
            PacketType.FILE_REQUEST -> {
                fileShareManager?.handleFileRequest(packet)
            }
            PacketType.FILE_CHUNK -> {
                // Handle both mesh file transfer chunks and content-addressed file sharing chunks
                try {
                    val json = org.json.JSONObject(String(packet.payload, StandardCharsets.UTF_8))

                    if (json.has(TRANSFER_ID_FIELD)) {
                        // Mesh file transfer chunk
                        val transferId = json.getString(TRANSFER_ID_FIELD)
                        val chunkIndex = json.getInt("chunkIndex")
                        val totalChunks = json.getInt("totalChunks")
                        val chunkData = android.util.Base64.decode(json.getString("data"), android.util.Base64.NO_WRAP)

                        val transferState = activeFileTransfers[transferId]
                        if (transferState != null) {
                            transferState.chunks[chunkIndex] = chunkData
                            transferState.receivedChunks++

                            log(
                                "FILE_CHUNK_RECEIVED from='${packet.sourceId}' transferId=${transferId.take(8)} " +
                                "chunk=$chunkIndex/$totalChunks progress=${transferState.receivedChunks}/$totalChunks",
                                com.fyp.resilientp2p.data.LogLevel.DEBUG,
                                peerId = packet.sourceId
                            )

                            // Check if transfer is complete
                            if (transferState.receivedChunks >= transferState.totalChunks) {
                                scope.launch {
                                    completeFileTransfer(transferId, transferState)
                                }
                            }
                        } else {
                            log("FILE_CHUNK_ORPHANED from='${packet.sourceId}' transferId=${transferId.take(8)} " +
                                "chunk=$chunkIndex — no active transfer",
                                com.fyp.resilientp2p.data.LogLevel.WARN, peerId = packet.sourceId)
                        }
                    } else {
                        // Content-addressed file sharing chunk - delegate to FileShareManager
                        fileShareManager?.handleFileChunk(packet)
                    }
                } catch (e: Exception) {
                    log("FILE_CHUNK_PARSE_ERROR from='${packet.sourceId}' error='${e.message}'",
                        com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = packet.sourceId)
                }
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
            PacketType.AUDIO_DATA -> {
                // Mesh-routed audio: AAC-encoded frames → MeshAudioManager → AudioPlayer
                meshAudioManager?.handleAudioData(packet)
            }
            PacketType.AUDIO_CONTROL -> {
                // Audio session start/stop signals
                meshAudioManager?.handleAudioControl(packet)
            }
            PacketType.ACK -> {
                // Delivery receipt: payload = the original packet ID that was delivered.
                // 1. Log confirmation   2. Remove from store-forward queue   3. Track in stats.
                val ackedId = String(packet.payload, StandardCharsets.UTF_8)
                log("ACK_RECEIVED from='${packet.sourceId}' ackedId=${ackedId.take(8)}",
                    com.fyp.resilientp2p.data.LogLevel.DEBUG, peerId = packet.sourceId)
                networkStats.deliveryConfirmed.incrementAndGet()
                peerTrustScorer?.recordDeliveryConfirmed(packet.sourceId)

                // Clear the ACKed packet from in-memory store-forward queue (if still pending)
                pendingMessages[packet.sourceId]?.removeAll { it.id == ackedId }

                // Clear from Room DB store-forward table (async, best-effort)
                scope.launch {
                    try {
                        packetDao?.deletePacket(ackedId)
                    } catch (_: Exception) { /* best effort */ }
                }
            }
            PacketType.STORE_FORWARD -> {
                // Store-forward delivery attempt — treat as regular data
                log("STORE_FORWARD from='${packet.sourceId}': ${String(packet.payload, StandardCharsets.UTF_8).take(100)}",
                    com.fyp.resilientp2p.data.LogLevel.INFO, peerId = packet.sourceId)
                if (packet.destId == localUsername) {
                    sendAck(packet.sourceId, packet.id)
                }
            }
            else -> {}
        }
    }

    /**
     * Send a delivery receipt (ACK) back to the original sender.
     * Payload = the original packet ID being acknowledged.
     * TTL = DEFAULT_TTL for multi-hop return. ACKs are CONTROL-category
     * rate-limited (50/sec) to prevent ACK storms.
     */
    private fun sendAck(destId: String, originalPacketId: String) {
        val ackPacket = Packet(
            id = java.util.UUID.randomUUID().toString(),
            type = PacketType.ACK,
            sourceId = localUsername,
            destId = destId,
            payload = originalPacketId.toByteArray(StandardCharsets.UTF_8),
            timestamp = System.currentTimeMillis(),
            ttl = Packet.DEFAULT_TTL
        )
        handlePacket(ackPacket, LOCAL_SOURCE)
    }

    private fun triggerSecurityRecovery(endpointId: String) {
        val now = System.currentTimeMillis()
        val previous = lastSecurityRecovery[endpointId] ?: 0L
        if (now - previous < 5000L) return
        lastSecurityRecovery[endpointId] = now
        if (neighbors.containsKey(endpointId)) {
            log(
                "SECURITY_RECOVERY endpoint=$endpointId action=RESEND_IDENTITY",
                com.fyp.resilientp2p.data.LogLevel.DEBUG
            )
            sendIdentityPacket(endpointId)
        }
    }

    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth")
    private fun forwardPacket(packet: Packet, excludeEndpointId: String? = null) {
        // Check if destination is a direct neighbor first (they're not in routing table)
        val directNeighborEndpoint = neighbors.entries.find { it.value.peerName == packet.destId }?.key
        val nextHop = directNeighborEndpoint ?: synchronized(routingLock) { routingTable[packet.destId] }
        var newPacket = packet.copy(ttl = packet.ttl - 1)

        // End-to-end security for locally-originated unicast packets:
        //   1. ENCRYPT (AES-256-GCM) — confidentiality + authenticity
        //   2. HMAC over ciphertext (Encrypt-then-MAC) — integrity for relays
        //
        // Relayers don't share the key, so they can't decrypt or verify HMAC.
        // Only the final destination can strip HMAC → decrypt → get plaintext.
        // This is the same Encrypt-then-MAC pattern used by Signal and Matrix.
        val shouldSecurePayload =
            packet.type == PacketType.DATA ||
                packet.type == PacketType.STORE_FORWARD ||
                packet.type == PacketType.ACK
        if (packet.sourceId == localUsername && packet.destId != BROADCAST_DEST && shouldSecurePayload) {
            val security = securityManager
            if (security != null && security.hasKeyForPeer(packet.destId)) {
                // Step 1: Encrypt payload
                val encrypted = security.encrypt(packet.destId, newPacket.payload)
                if (encrypted != null) {
                    newPacket = newPacket.copy(payload = encrypted)
                }
                // Step 2: HMAC over (possibly encrypted) payload
                val hmac = security.computeHmac(packet.destId, newPacket.payload)
                if (hmac != null) {
                    newPacket = newPacket.copy(payload = newPacket.payload + hmac)
                }
            }
        }

        val bytes =
                try {
                    newPacket.toBytes()
                } catch (e: Exception) {
                    log("FORWARD_SERIALIZE_ERROR id=${packet.id.take(8)} error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
                    return
                }
        val payload = Payload.fromBytes(bytes)

        if (nextHop != null && nextHop != excludeEndpointId && neighbors.containsKey(nextHop)) {
            val hopPeerName = neighbors[nextHop]?.peerName ?: UNKNOWN_PEER
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
        } else if (packet.destId != BROADCAST_DEST) {
            val gateway = internetGatewayManager
            val shouldRelayViaCloud =
                packet.destId in internetPeers &&
                    packet.type == PacketType.DATA &&
                    gateway?.hasInternet?.value == true

            if (shouldRelayViaCloud) {
                // Validate peer is still online before attempting relay
                val peerScore = gateway?.getPeerCapabilityScore(packet.destId) ?: -1
                val isStale = peerScore == -1 // No capability score means peer not seen recently

                if (isStale) {
                    log("RELAY_SKIPPED_STALE dest='${packet.destId}' reason=PEER_NOT_RECENTLY_SEEN",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
                    // Fall back to store-and-forward for potentially stale peers
                    queueForStoreForward(newPacket)
                } else {
                    scope.launch {
                        val relayed = gateway?.relayToCloud(packet.destId, packet.sourceId, newPacket.payload, packet.id) == true
                        if (relayed) {
                            log(
                                "PACKET_RELAYED_CLOUD id=${packet.id.take(8)} dest='${packet.destId}' reason=INTERNET_PEER",
                                com.fyp.resilientp2p.data.LogLevel.INFO
                            )
                        } else {
                            log(
                                "RELAY_FAILED_FALLBACK_SF id=${packet.id.take(8)} dest='${packet.destId}' reason=INTERNET_PEER",
                                com.fyp.resilientp2p.data.LogLevel.INFO
                            )
                            queueForStoreForward(newPacket)
                        }
                    }
                }
                return
            }

            // Unicast with no valid route — try flood to all neighbors, trust-sorted
            val scorer = peerTrustScorer
            val availableNeighbors = neighbors.keys
                .filter { it != excludeEndpointId }
                .let { list ->
                    if (scorer != null) {
                        // Prefer trusted peers first; skip untrusted unless they're the only option
                        val (trusted, untrusted) = list.partition { !scorer.isUntrusted(it) }
                        trusted.sortedByDescending { scorer.getScore(it) } + untrusted
                    } else {
                        list
                    }
                }
            if (availableNeighbors.isNotEmpty()) {
                log(
                        "PACKET_FLOOD id=${packet.id.take(8)} dest='${packet.destId}' " +
                        "noRoute=true flooding=${availableNeighbors.size}peers",
                        com.fyp.resilientp2p.data.LogLevel.DEBUG
                )
                availableNeighbors.forEach { endpointId ->
                    val floodPeerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
                    networkStats.totalPacketsForwarded.incrementAndGet()
                    networkStats.recordPacketSent(floodPeerName, bytes.size)
                    connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes)).addOnFailureListener { e ->
                        if (e.message?.contains("8012") == true) {
                            log("DEAD_ENDPOINT_8012 endpoint=$endpointId", com.fyp.resilientp2p.data.LogLevel.WARN)
                            disconnectFromEndpoint(endpointId)
                        }
                    }
                }
            } else {
                // No neighbors at all — queue for store-and-forward or try cloud relay
                if (newPacket.type == PacketType.DATA || newPacket.type == PacketType.STORE_FORWARD) {
                    when {
                        // Case 1: We are a gateway — relay directly to Firestore
                        gateway != null && gateway.hasInternet.value && newPacket.type == PacketType.DATA -> {
                            scope.launch {
                                val relayed = gateway.relayToCloud(newPacket.destId, newPacket.sourceId, newPacket.payload, newPacket.id)
                                if (relayed) {
                                    log("PACKET_RELAYED_CLOUD id=${packet.id.take(8)} dest='${packet.destId}'",
                                        com.fyp.resilientp2p.data.LogLevel.INFO)
                                } else {
                                    log("RELAY_FAILED_FALLBACK_SF id=${packet.id.take(8)} dest='${packet.destId}'",
                                        com.fyp.resilientp2p.data.LogLevel.INFO)
                                    queueForStoreForward(newPacket)
                                }
                            }
                        }
                        // Case 2: We have no internet but a neighbor is a gateway — proxy through them
                        newPacket.type == PacketType.DATA -> {
                            val proxyEndpoint = gatewayNeighbors.firstOrNull { neighbors.containsKey(it) }
                            if (proxyEndpoint != null) {
                                val proxyName = neighbors[proxyEndpoint]?.peerName ?: UNKNOWN_PEER
                                log("PACKET_PROXY_GATEWAY id=${packet.id.take(8)} dest='${packet.destId}' " +
                                    "via=$proxyEndpoint($proxyName)",
                                    com.fyp.resilientp2p.data.LogLevel.INFO)
                                // Wrap as a proxy relay request so the gateway knows to relay
                                // to Firestore rather than trying to route locally (which would fail).
                                // Format: __PROXY_RELAY__:{"destId":"...","payload":"<base64>","packetId":"..."}
                                val proxyJson = org.json.JSONObject().apply {
                                    put("destId", newPacket.destId)
                                    put("payload", android.util.Base64.encodeToString(newPacket.payload, android.util.Base64.NO_WRAP))
                                    put("packetId", newPacket.id)
                                }
                                val proxyPayload = (InternetGatewayManager.PROXY_RELAY_PREFIX + proxyJson.toString())
                                    .toByteArray(StandardCharsets.UTF_8)
                                val proxyPacket = newPacket.copy(
                                    id = java.util.UUID.randomUUID().toString(),
                                    destId = proxyName, // addressed to the gateway peer
                                    payload = proxyPayload
                                )
                                val proxyBytes = proxyPacket.toBytes()
                                networkStats.totalPacketsForwarded.incrementAndGet()
                                networkStats.recordPacketSent(proxyName, proxyBytes.size)
                                connectionsClient.sendPayload(proxyEndpoint, Payload.fromBytes(proxyBytes))
                                    .addOnFailureListener { e ->
                                        log("PROXY_FORWARD_FAILED to=$proxyEndpoint error='${e.message}' — queuing S&F",
                                            com.fyp.resilientp2p.data.LogLevel.WARN)
                                        queueForStoreForward(newPacket)
                                    }
                            } else {
                                log("PACKET_QUEUED_SF id=${packet.id.take(8)} dest='${packet.destId}' " +
                                    "reason=NO_ROUTE_NO_NEIGHBORS_NO_GATEWAY type=${newPacket.type}",
                                    com.fyp.resilientp2p.data.LogLevel.INFO)
                                queueForStoreForward(newPacket)
                            }
                        }
                        // Case 3: Not DATA — just queue S&F
                        else -> {
                            log("PACKET_QUEUED_SF id=${packet.id.take(8)} dest='${packet.destId}' " +
                                "reason=NO_ROUTE_NO_NEIGHBORS type=${newPacket.type}",
                                com.fyp.resilientp2p.data.LogLevel.INFO)
                            queueForStoreForward(newPacket)
                        }
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
                val bcPeerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
                networkStats.totalPacketsForwarded.incrementAndGet()
                networkStats.recordPacketSent(bcPeerName, bytes.size)
                connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes)).addOnFailureListener { e ->
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
        handlePacket(packet, LOCAL_SOURCE)
    }

    fun broadcastMessage(message: String) {
        sendData(BROADCAST_DEST, message)
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
        handlePacket(packet, LOCAL_SOURCE)
    }

    // New Direct Ping for Heartbeats (Bypasses Mesh Routing, works even if Name is Unknown)
    fun sendDirectPing(endpointId: String, data: ByteArray) {
        // destId targets the specific peer to avoid flooding PINGs to all neighbors
        val targetPeerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.PING,
                        sourceId = localUsername,
                        destId = targetPeerName,
                        payload = data,
                        timestamp = System.currentTimeMillis()
                )
        // Send directly to the physical endpoint
        try {
            val bytes = packet.toBytes()
            val peerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
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
        handlePacket(packet, LOCAL_SOURCE)
    }

    fun sendFile(peerName: String, uri: android.net.Uri) {
        scope.launch {
            try {
                sendFileAsync(peerName, uri)
            } catch (e: Exception) {
                log("FILE_SEND_ERROR peer='$peerName' error='${e.message}'",
                    com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
            }
        }
    }

    private suspend fun sendFileAsync(peerName: String, uri: android.net.Uri) {
        // CRITICAL FIX: Improved peer connectivity validation
        // Check multiple connection types: direct, routed, and internet
        val state = state.value
        val hasDirectConnection = neighbors.values.any { it.peerName == peerName }
        val hasRouteConnection = state.knownPeers.containsKey(peerName)
        val hasInternetConnection = state.internetPeers.contains(peerName)

        // ADDITIONAL FIX: Check if peer is reachable via gateway
        val hasGatewayRoute = internetGatewayManager?.let { gateway ->
            gateway.hasInternet.value && gateway.cloudPeers.value.contains(peerName)
        } ?: false

        @Suppress("ComplexCondition") // Multiple connectivity checks are necessary for comprehensive validation
        if (!hasDirectConnection && !hasRouteConnection && !hasInternetConnection && !hasGatewayRoute) {
            log("FILE_SEND_ERROR peer='$peerName' reason=NO_ENDPOINT " +
                "direct=$hasDirectConnection routed=$hasRouteConnection " +
                "internet=$hasInternetConnection gateway=$hasGatewayRoute",
                com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
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
            } else {
                null
            }
        } ?: Pair(null, -1L)

        val resolvedFileName = fileName ?: uri.lastPathSegment ?: "file"
        val mimeType = context.contentResolver.getType(uri) ?: "application/octet-stream"

        if (fileSize > MAX_FILE_SIZE) {
            log("FILE_SEND_ERROR peer='$peerName' reason=FILE_TOO_LARGE size=$fileSize max=$MAX_FILE_SIZE",
                com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
            return
        }

        // Check if this is a direct neighbor (use legacy FILE payload method)
        val endpointId = neighbors.entries.find { it.value.peerName == peerName }?.key
        if (endpointId != null) {
            // Direct neighbor - use legacy Nearby Connections FILE payload for efficiency
            sendFileDirect(peerName, endpointId, uri, resolvedFileName, mimeType, fileSize)
            return
        }

        // Not a direct neighbor - use mesh-compatible chunked transfer
        sendFileMesh(peerName, uri, resolvedFileName, mimeType, fileSize)
    }

    private fun sendFileDirect(peerName: String, endpointId: String, uri: android.net.Uri,
                              fileName: String, mimeType: String, fileSize: Long) {
        // Legacy direct transfer using Nearby Connections FILE payload
        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        if (pfd == null) {
            log("FILE_SEND_ERROR peer='$peerName' reason=NULL_PFD", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }

        val payload = try {
            Payload.fromFile(pfd)
        } catch (e: Exception) {
            pfd.close()
            log("FILE_SEND_ERROR peer='$peerName' error='${e.message}'", com.fyp.resilientp2p.data.LogLevel.ERROR)
            return
        }

        // Send metadata first
        val payloadId = payload.id
        val metaJson = org.json.JSONObject().apply {
            put("payloadId", payloadId)
            put("fileName", fileName)
            put("mimeType", mimeType)
            put("fileSize", fileSize)
        }.toString()
        val metaPacket = com.fyp.resilientp2p.transport.Packet(
            type = com.fyp.resilientp2p.transport.PacketType.FILE_META,
            sourceId = localUsername,
            destId = peerName,
            payload = metaJson.toByteArray(),
            ttl = 0  // Direct only
        )
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(metaPacket.toBytes()))
            .addOnSuccessListener {
                connectionsClient.sendPayload(endpointId, payload)
                    .addOnSuccessListener {
                        log("FILE_SENT_DIRECT peer='$peerName' name='$fileName' mime='$mimeType' payloadId=$payloadId",
                            com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)
                    }
                    .addOnFailureListener { e ->
                        log("FILE_SEND_FAILED peer='$peerName' name='$fileName' error='${e.message}'",
                            com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
                    }
            }
            .addOnFailureListener { e ->
                log("FILE_META_SEND_FAILED peer='$peerName' name='$fileName' error='${e.message}'",
                    com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = peerName)
            }
    }

    private suspend fun sendFileMesh(peerName: String, uri: android.net.Uri,
                                   fileName: String, mimeType: String, fileSize: Long) {
        // Mesh-compatible chunked transfer
        val transferId = java.util.UUID.randomUUID().toString()
        val totalChunks = ((fileSize + FILE_CHUNK_SIZE - 1) / FILE_CHUNK_SIZE).toInt()

        log("FILE_SEND_MESH_START peer='$peerName' name='$fileName' size=$fileSize chunks=$totalChunks transferId=${transferId.take(8)}",
            com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)

        // Send metadata first
        val metaJson = org.json.JSONObject().apply {
            put(TRANSFER_ID_FIELD, transferId)
            put("fileName", fileName)
            put("mimeType", mimeType)
            put("fileSize", fileSize)
            put("totalChunks", totalChunks)
            put("chunkSize", FILE_CHUNK_SIZE)
        }.toString()

        val metaPacket = com.fyp.resilientp2p.transport.Packet(
            type = com.fyp.resilientp2p.transport.PacketType.FILE_META,
            sourceId = localUsername,
            destId = peerName,
            payload = metaJson.toByteArray(),
            ttl = Packet.DEFAULT_TTL  // Allow routing
        )
        forwardPacket(metaPacket)

        // Read file and send chunks
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val buffer = ByteArray(FILE_CHUNK_SIZE)
            var chunkIndex = 0

            while (chunkIndex < totalChunks) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                val chunkData = if (bytesRead == FILE_CHUNK_SIZE) buffer else buffer.copyOf(bytesRead)

                // Create chunk packet
                val chunkJson = org.json.JSONObject().apply {
                    put(TRANSFER_ID_FIELD, transferId)
                    put("chunkIndex", chunkIndex)
                    put("totalChunks", totalChunks)
                    put("data", android.util.Base64.encodeToString(chunkData, android.util.Base64.NO_WRAP))
                }.toString()

                val chunkPacket = com.fyp.resilientp2p.transport.Packet(
                    type = com.fyp.resilientp2p.transport.PacketType.FILE_CHUNK,
                    sourceId = localUsername,
                    destId = peerName,
                    payload = chunkJson.toByteArray(),
                    ttl = Packet.DEFAULT_TTL
                )

                forwardPacket(chunkPacket)
                chunkIndex++

                // Small delay to avoid overwhelming the mesh
                kotlinx.coroutines.delay(10)
            }

            log("FILE_SEND_MESH_COMPLETE peer='$peerName' name='$fileName' chunks=$chunkIndex transferId=${transferId.take(8)}",
                com.fyp.resilientp2p.data.LogLevel.INFO, peerId = peerName)
        }
    }

    /**
     * Complete a mesh file transfer by reassembling chunks into the final file.
     * Called when all chunks have been received for a transfer.
     */
    private suspend fun completeFileTransfer(transferId: String, transferState: FileTransferState) {
        try {
            val metadata = transferState.metadata
            val chunks = transferState.chunks

            // Verify all chunks are present
            val missingChunks = (0 until transferState.totalChunks).filter { !chunks.containsKey(it) }
            if (missingChunks.isNotEmpty()) {
                log("FILE_TRANSFER_INCOMPLETE transferId=${transferId.take(8)} " +
                    "missing=${missingChunks.size} chunks=$missingChunks",
                    com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = metadata.senderName)
                activeFileTransfers.remove(transferId)
                return
            }

            // Create received files directory
            val receivedDir = java.io.File(context.filesDir, "received_files")
            receivedDir.mkdirs()

            // Generate unique filename to avoid conflicts
            val targetFile = java.io.File(receivedDir, metadata.fileName)
            var finalFile = targetFile
            var counter = 1
            while (finalFile.exists()) {
                val nameWithoutExt = metadata.fileName.substringBeforeLast(".")
                val extension = if (metadata.fileName.contains("."))
                    ".${metadata.fileName.substringAfterLast(".")}" else ""
                finalFile = java.io.File(receivedDir, "${nameWithoutExt}_$counter$extension")
                counter++
            }

            // Reassemble file from chunks
            finalFile.outputStream().use { output ->
                for (i in 0 until transferState.totalChunks) {
                    val chunkData = chunks[i] ?: error("Missing chunk $i")
                    output.write(chunkData)
                }
            }

            val transferDurationMs = System.currentTimeMillis() - transferState.startTime
            val totalBytes = finalFile.length()

            log(
                "FILE_TRANSFER_COMPLETE from='${metadata.senderName}' " +
                "name='${finalFile.name}' mime='${metadata.mimeType}' " +
                "size=$totalBytes chunks=${transferState.totalChunks} " +
                "duration=${FormatUtils.formatDuration(transferDurationMs)} " +
                "transferId=${transferId.take(8)}",
                com.fyp.resilientp2p.data.LogLevel.INFO,
                peerId = metadata.senderName
            )

            // Clean up transfer state
            activeFileTransfers.remove(transferId)

            // Emit file received event
            _receivedFileEvents.emit(
                ReceivedFileEvent(metadata.senderName, finalFile.name, metadata.mimeType, finalFile)
            )

        } catch (e: Exception) {
            log("FILE_TRANSFER_ASSEMBLY_ERROR transferId=${transferId.take(8)} error='${e.message}'",
                com.fyp.resilientp2p.data.LogLevel.ERROR, peerId = transferState.metadata.senderName)
            activeFileTransfers.remove(transferId)
        }
    }

    /**
     * Start the file transfer cleanup job that handles timeouts and orphaned transfers.
     */
    private fun startFileTransferCleanup() {
        if (fileTransferCleanupJob?.isActive == true) return
        fileTransferCleanupJob = scope.launch {
            while (isActive) {
                delay(30_000L) // Check every 30 seconds

                try {
                    val now = System.currentTimeMillis()
                    val timedOutTransfers = activeFileTransfers.entries.filter { (_, state) ->
                        now - state.startTime > FILE_TRANSFER_TIMEOUT_MS
                    }

                    timedOutTransfers.forEach { (transferId, state) ->
                        log(
                            "FILE_TRANSFER_TIMEOUT transferId=${transferId.take(8)} " +
                            "from='${state.metadata.senderName}' name='${state.metadata.fileName}' " +
                            "received=${state.receivedChunks}/${state.totalChunks} " +
                            "elapsed=${FormatUtils.formatDuration(now - state.startTime)}",
                            com.fyp.resilientp2p.data.LogLevel.WARN,
                            peerId = state.metadata.senderName
                        )
                        activeFileTransfers.remove(transferId)
                    }

                    if (timedOutTransfers.isNotEmpty()) {
                        log("FILE_TRANSFER_CLEANUP removed ${timedOutTransfers.size} timed-out transfers",
                            com.fyp.resilientp2p.data.LogLevel.DEBUG)
                    }
                } catch (e: Exception) {
                    log("FILE_TRANSFER_CLEANUP_ERROR error='${e.message}'",
                        com.fyp.resilientp2p.data.LogLevel.ERROR)
                }
            }
        }
    }

    fun startAudioStreaming(peerName: String) {
        val target = if (peerName == BROADCAST_DEST) BROADCAST_DEST else peerName
        log("Starting mesh audio stream to '$target'")

        val mam = meshAudioManager
        if (mam != null) {
            val started = mam.startStreaming(target)
            if (!started) {
                log("Mesh audio failed to start, falling back to direct stream", com.fyp.resilientp2p.data.LogLevel.WARN)
                startDirectAudioStreaming(peerName)
            }
        } else {
            startDirectAudioStreaming(peerName)
        }
    }

    /**
     * Legacy direct audio streaming via Payload.fromStream (single hop only).
     * Used as fallback when MeshAudioManager is unavailable.
     */
    private fun startDirectAudioStreaming(peerName: String) {
        val targetEndpointIds =
                if (peerName == BROADCAST_DEST) {
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
                "Starting direct audio stream to ${if(peerName == BROADCAST_DEST) "ALL (${targetEndpointIds.size})" else peerName}"
        )

        val pfd = voiceManager?.startRecording()
        if (pfd != null) {
            log("Audio recording started, sending payload...")
            val payload = Payload.fromStream(pfd)

            connectionsClient
                    .sendPayload(targetEndpointIds, payload)
                    .addOnSuccessListener { log("Audio payload sent to $targetEndpointIds") }
                    .addOnFailureListener { e ->
                        log("Failed to send audio payload: ${e.message}")
                        voiceManager?.stopRecording()
                    }
        } else {
            log("Failed to start audio recording (returned null)")
        }
    }

    fun stopAudioStreaming() {
        log("Audio Streaming stopped")
        meshAudioManager?.stopStreaming()
        voiceManager?.stopRecording()
    }

    fun getLocalDeviceName(): String = localUsername

    /**
     * Get the safety number for verifying the ECDH key exchange with [peerId].
     * Both peers will compute the same number; compare out-of-band (e.g. read aloud)
     * to confirm no MITM attack. Returns null if no key is exchanged with this peer.
     *
     * @see SecurityManager.computeSafetyNumber
     */
    fun getSafetyNumber(peerId: String): String? = securityManager?.computeSafetyNumber(peerId)

    fun disconnectFromEndpoint(endpointId: String) {
        val peer = neighbors[endpointId]
        val peerName = peer?.peerName ?: UNKNOWN_PEER
        val connectedAtMs = connectionTimestamps.remove(endpointId)
        val durationMs = if (connectedAtMs != null) System.currentTimeMillis() - connectedAtMs else -1L
        val durationStr = if (durationMs > 0) FormatUtils.formatDuration(durationMs) else UNKNOWN_DURATION

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
        encounterLogger?.onPeerDisconnected(peerName)
        neighbors.remove(endpointId)
        activeTransferEndpoints.remove(endpointId)
        consecutiveFailures.remove(endpointId)
        lastFailureTime.remove(endpointId)
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

    /**
     * Toggle the internet gateway on or off.
     * When disabled, this device will not relay for the mesh even if it has internet.
     */
    fun setGatewayEnabled(enabled: Boolean) {
        internetGatewayManager?.setGatewayEnabled(enabled)
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

    fun getNeighborsSnapshot(): Map<String, Neighbor> = HashMap(neighbors)

    private fun calculateRouteScore(packet: Packet): Int {
        // Higher score = better route. Base: TTL * 100 (more hops = lower TTL = worse)
        // Cap TTL to prevent artificially boosted routes from malicious local sources
        var score = minOf(packet.ttl, 10) * 100

        // Add RSSI bonus if available from trace (only if valid negative value)
        packet.trace.lastOrNull()?.rssi?.let { rssi ->
            if (rssi < 0) {
                // RSSI is typically negative (e.g. -50 dBm is better than -80 dBm)
                score += rssi / 10
            }
        }

        return score
    }

    private fun sendIdentityPacket(endpointId: String) {
        // Embed our ECDH public key in the IDENTITY payload for key exchange
        val pubKeyPayload = securityManager?.getPublicKeyBase64()
            ?.toByteArray(java.nio.charset.StandardCharsets.UTF_8)
            ?: ByteArray(0)

        // destId targets the specific peer (not BROADCAST) to avoid false TTL_EXCEEDED logs
        val targetPeerName = neighbors[endpointId]?.peerName ?: UNKNOWN_PEER
        val packet =
                Packet(
                        id = java.util.UUID.randomUUID().toString(),
                        type = PacketType.IDENTITY,
                        sourceId = localUsername,
                        destId = targetPeerName,
                        payload = pubKeyPayload,
                        timestamp = System.currentTimeMillis(),
                        ttl = 0 // CRITICAL: Never forward — IDENTITY is point-to-point only
                )
        val bytes = packet.toBytes()
        log("IDENTITY_SENDING endpoint=$endpointId target='$targetPeerName' payloadSize=${bytes.size}",
            com.fyp.resilientp2p.data.LogLevel.DEBUG)
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
            .addOnSuccessListener {
                log("IDENTITY_SENT endpoint=$endpointId target='$targetPeerName'",
                    com.fyp.resilientp2p.data.LogLevel.DEBUG)
            }
            .addOnFailureListener { e ->
                log("IDENTITY_SEND_FAILED endpoint=$endpointId target='$targetPeerName' error='${e.message}'",
                    com.fyp.resilientp2p.data.LogLevel.WARN)
            }
    }

    private fun broadcastRouteAnnouncement() {
        // Poison Reverse: send per-neighbor route tables where routes learned
        // FROM a neighbor are advertised back to that neighbor with score=0 (unreachable).
        // This prevents count-to-infinity and speeds convergence on link failure.
        val neighborsSnapshot = HashMap(neighbors)

        // Check if this device is an internet gateway
        val isGateway = internetGatewayManager?.shouldAdvertiseGateway() == true
        // Update state for UI
        updateState {
            it.copy(
                isGateway = isGateway,
                hasInternet = internetGatewayManager?.hasInternet?.value == true,
                gatewayEnabled = internetGatewayManager?.gatewayEnabled?.value != false
            )
        }

        neighborsSnapshot.keys.forEach { endpointId ->
            // Build route table: include self with max score so neighbors know we exist
            val selfEntry = "$localUsername:${Packet.DEFAULT_TTL * 100}"

            // CRITICAL FIX: Include direct neighbors in announcements so multi-hop works
            // Build list of all reachable destinations: self + direct neighbors + routed peers
            val allDestinations = mutableListOf<String>()

            synchronized(routingLock) {
                // Add all routed destinations (non-direct peers)
                routingScores.entries
                    .filter { it.key != localUsername } // Don't duplicate self
                    .forEach { (dest, score) ->
                        val nextHop = routingTable[dest]
                        if (nextHop == endpointId) {
                            // Poison Reverse: advertise 0 score for routes learned from this neighbor
                            allDestinations.add("$dest:0")
                        } else {
                            allDestinations.add("$dest:$score")
                        }
                    }

                // Add direct neighbors (excluding the one we're sending to)
                // Score = DEFAULT_TTL * 100 (same as self) since they're 1 hop away
                neighborsSnapshot.entries
                    .filter { it.key != endpointId } // Don't announce neighbor to itself
                    .forEach { (neighborEndpoint, neighborInfo) ->
                        val neighborName = neighborInfo.peerName
                        if (neighborName != UNKNOWN_PEER &&
                            neighborName != localUsername &&
                            allDestinations.none { it.startsWith("$neighborName:") }) {
                            // Only add if not already in routed destinations
                            allDestinations.add("$neighborName:${Packet.DEFAULT_TTL * 100}")
                        }
                    }
            }

            // Combine self + all destinations
            var routeData = if (allDestinations.isEmpty()) {
                selfEntry
            } else {
                "$selfEntry,${allDestinations.joinToString(",")}"
            }

            // Append __GATEWAY__ flag if this device has internet access
            if (isGateway) {
                routeData = "$routeData,${InternetGatewayManager.GATEWAY_FLAG}:1"
            }

            // destId targets specific neighbor (not BROADCAST) to avoid false TTL_EXCEEDED
            val targetPeerName = neighborsSnapshot[endpointId]?.peerName ?: UNKNOWN_PEER
            val packet = Packet(
                id = java.util.UUID.randomUUID().toString(),
                type = PacketType.ROUTE_ANNOUNCE,
                sourceId = localUsername,
                destId = targetPeerName,
                payload = routeData.toByteArray(StandardCharsets.UTF_8),
                timestamp = System.currentTimeMillis(),
                ttl = 0 // Route announcements are direct-only (0 = no further forwarding)
            )
            val bytes = packet.toBytes()
            connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes))
                .addOnSuccessListener {
                    log("ROUTE_ANNOUNCE_SENT to=$targetPeerName($endpointId) destinations=${allDestinations.size + 1}",
                        com.fyp.resilientp2p.data.LogLevel.DEBUG)
                }
                .addOnFailureListener { e ->
                    log("ROUTE_ANNOUNCE_FAILED to=$endpointId error='${e.message}'",
                        com.fyp.resilientp2p.data.LogLevel.WARN)
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
                val estimatedHops = maxOf(1, Packet.DEFAULT_TTL - score / 100)
                dest to RouteInfo(nextHop, estimatedHops)
            }
        }
        val internetPeerRoutes = internetPeers
            .asSequence()
            .filter { it != localUsername }
            .filterNot { it in neighborNames }
            .filterNot { it in knownPeersMap.keys }
            .associateWith { RouteInfo(INTERNET_RELAY_HOP, 0) }
        val snap = networkStats.snapshot(neighbors.size, routingTable.size)
        updateState {
            it.copy(
                connectedEndpoints = neighborList,
                knownPeers = knownPeersMap + internetPeerRoutes,
                internetPeers = internetPeerRoutes.keys,
                stats = snap
            )
        }
    }

    fun updateInternetPeers(peers: Set<String>) {
        internetPeers = peers
        updateConnectedEndpoints()
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
                    val mAhStr = if (snap.batteryChargeUah > 0)
                        "${snap.batteryChargeUah / 1000}mAh"
                    else
                        "~${snap.estimatedRemainingMah().toInt()}mAh(est)"
                    log(
                            "STATS_DUMP v=$appVersion uptime=${FormatUtils.formatDuration(snap.uptimeMs)} " +
                            "battery=${snap.batteryLevel}%(${snap.batteryTemperature}\u00B0C) ${mAhStr} ${snap.batteryVoltageMilliV}mV " +
                            "neighbors=${snap.currentNeighborCount}[$neighborList] " +
                            "routes=${snap.currentRouteCount}[$routeList] " +
                            "packets=\u2191${snap.totalPacketsSent}" +
                            "\u2193${snap.totalPacketsReceived}" +
                            "\u21BB${snap.totalPacketsForwarded}" +
                            "\u2717${snap.totalPacketsDropped} " +
                            "bytes=\u2191${FormatUtils.formatBytes(snap.totalBytesSent)}\u2193${FormatUtils.formatBytes(snap.totalBytesReceived)} " +
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

        // 2. Persist to Room DB only at INFO level and above (ERROR, WARN, METRIC, INFO).
        //    DEBUG and TRACE are high-frequency and excluded from DB/CSV to keep volume manageable.
        if (level.ordinal <= com.fyp.resilientp2p.data.LogLevel.INFO.ordinal) {
            logDao?.let { dao ->
                scope.launch {
                    try {
                        dao.insert(entry)
                    } catch (e: Exception) {
                        Log.e("P2PManager", "Failed to persist log to DB: ${e.message}")
                    }
                }
            }
        }

        // 3. Filter for UI display (respects user log level setting, independent of DB persistence)
        val currentLevel = _state.value.logLevel
        if (level.ordinal <= currentLevel.ordinal) {
            updateState { state ->
                state.copy(logs = (state.logs + entry).takeLast(100))
            }
        }
    }

    companion object {
        /** Fallback name for peers not yet identified */
        const val UNKNOWN_PEER = "Unknown"
        /** Fallback for unknown durations/versions */
        private const val UNKNOWN_DURATION = "unknown"
        /** Sentinel endpoint ID for locally-originated packets */
        const val LOCAL_SOURCE = "LOCAL"
        /** Sentinel destination for broadcast/flood packets */
        const val BROADCAST_DEST = "BROADCAST"
        /** Synthetic next hop label used for internet-relayed peers in the UI. */
        private const val INTERNET_RELAY_HOP = "Internet Relay"

        // File transfer constants
        /** Maximum size of each file chunk in bytes (64KB - safe for mesh routing) */
        const val FILE_CHUNK_SIZE = 64 * 1024
        /** Maximum file size for mesh transfer (10MB) */
        const val MAX_FILE_SIZE = 10 * 1024 * 1024
        /** File transfer timeout in milliseconds (5 minutes) */
        const val FILE_TRANSFER_TIMEOUT_MS = 5 * 60 * 1000L
        /** Transfer ID field name for JSON packets */
        private const val TRANSFER_ID_FIELD = "transferId"
    }
}
