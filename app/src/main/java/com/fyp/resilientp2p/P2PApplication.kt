package com.fyp.resilientp2p

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fyp.resilientp2p.audio.AudioCodecManager
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.managers.CloudLogManager
import com.fyp.resilientp2p.managers.EmergencyManager
import com.fyp.resilientp2p.managers.EncounterLogger
import com.fyp.resilientp2p.managers.FileShareManager
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.InternetGatewayManager
import com.fyp.resilientp2p.managers.LocationEstimator
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.TelemetryManager
import com.fyp.resilientp2p.security.PeerBlacklist
import com.fyp.resilientp2p.security.PeerTrustScorer
import com.fyp.resilientp2p.security.RateLimiter
import com.fyp.resilientp2p.security.SecurityManager
import com.fyp.resilientp2p.testing.EnduranceTestRunner
import com.fyp.resilientp2p.testing.TestRunner
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * Application singleton that owns all long-lived managers.
 *
 * Lifecycle: created once by Android, outlives any Activity/Service.
 * All managers are initialized in [onCreate] and shared via lateinit properties.
 */
@Suppress("LateinitUsage") // Android lifecycle: managers initialised in onCreate()
class P2PApplication : Application() {
    /** Room database singleton (lazy to defer until first DAO access). */
    val database by lazy { AppDatabase.getDatabase(this) }

    /** Core mesh networking engine. */
    lateinit var p2pManager: P2PManager
        private set
    /** Heartbeat-based liveness detection for mesh neighbors. */
    lateinit var heartbeatManager: HeartbeatManager
        private set
    /** Automated test suite runner (15 tests covering full stack). */
    lateinit var testRunner: TestRunner
        private set
    /** Long-running endurance/soak test runner for battery & stability analysis. */
    lateinit var enduranceTestRunner: EnduranceTestRunner
        private set
    /** Cloud telemetry collection and upload via WorkManager. */
    lateinit var telemetryManager: TelemetryManager
        private set
    /** Internet gateway detection and cross-mesh cloud relay. */
    lateinit var internetGatewayManager: InternetGatewayManager
        private set
    /** Emergency broadcast and SOS beacon management. */
    lateinit var emergencyManager: EmergencyManager
        private set
    /** ECDH + AES-256-GCM end-to-end encryption and HMAC packet integrity. */
    lateinit var securityManager: SecurityManager
        private set
    /** Per-peer sliding-window rate limiter (DoS defence). */
    lateinit var rateLimiter: RateLimiter
        private set
    /** Persistent peer blacklist with auto-ban on violation threshold. */
    lateinit var peerBlacklist: PeerBlacklist
        private set
    /** Per-peer trust/reputation scoring (libp2p GossipSub-inspired). */
    lateinit var peerTrustScorer: PeerTrustScorer
        private set
    /** RTT-based trilateration engine for 2D peer positioning. */
    lateinit var locationEstimator: LocationEstimator
        private set
    /** DTN encounter logger for sneakernet analytics. */
    lateinit var encounterLogger: EncounterLogger
        private set
    /** Content-addressable distributed file sharing over the mesh. */
    lateinit var fileShareManager: FileShareManager
        private set
    /** Cloud log upload manager — batches system logs to Firestore. */
    lateinit var cloudLogManager: CloudLogManager
        private set

    /** Application-wide coroutine scope for non-UI async work. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Whether this build was compiled with test mode enabled. */
    val isTestMode: Boolean
        get() = BuildConfig.TEST_MODE

    override fun onCreate() {
        super.onCreate()
        p2pManager = P2PManager(this, database.logDao(), database.packetDao())
        heartbeatManager = HeartbeatManager(p2pManager)
        testRunner = TestRunner(this, p2pManager)
        enduranceTestRunner = EnduranceTestRunner(this, p2pManager)
        telemetryManager = TelemetryManager(this, database.telemetryDao(), database.logDao(), p2pManager)

        // Phase 3 managers
        internetGatewayManager = InternetGatewayManager(this, p2pManager)
        emergencyManager = EmergencyManager(this, p2pManager)

        // Security managers
        securityManager = SecurityManager()
        rateLimiter = RateLimiter()
        peerBlacklist = PeerBlacklist(this)
        peerTrustScorer = PeerTrustScorer()

        // Phase 4 managers
        locationEstimator = LocationEstimator()
        encounterLogger = EncounterLogger(database.encounterDao())
        fileShareManager = FileShareManager(this, database.sharedFileDao(), p2pManager.state.value.localDeviceName, database.downloadedChunkDao())
        fileShareManager.sendPacket = { packet -> p2pManager.injectPacket(packet) }

        // Cloud log manager
        cloudLogManager = CloudLogManager(this, database.logDao(), p2pManager)

        // Wire cross-references
        p2pManager.internetGatewayManager = internetGatewayManager
        p2pManager.emergencyManager = emergencyManager
        p2pManager.securityManager = securityManager
        p2pManager.rateLimiter = rateLimiter
        p2pManager.peerBlacklist = peerBlacklist
        p2pManager.peerTrustScorer = peerTrustScorer
        p2pManager.locationEstimator = locationEstimator
        p2pManager.encounterLogger = encounterLogger
        p2pManager.fileShareManager = fileShareManager
        p2pManager.cloudLogManager = cloudLogManager

        // Wire log callbacks so all managers route through p2pManager.log()
        val logFn: (String, LogLevel) -> Unit = { msg, level -> p2pManager.log(msg, level) }
        securityManager.logFn = logFn
        rateLimiter.logFn = logFn
        peerBlacklist.logFn = logFn
        peerTrustScorer.logFn = logFn
        locationEstimator.logFn = logFn
        encounterLogger.logFn = logFn
        fileShareManager.logFn = logFn
        AudioCodecManager.logFn = logFn

        // Start managers
        telemetryManager.start()
        internetGatewayManager.start()
        emergencyManager.start()
        cloudLogManager.start()

        // Keep internet-discovered peers visible in UI state.
        appScope.launch(start = CoroutineStart.UNDISPATCHED) {
            internetGatewayManager.cloudPeers.collectLatest { peers ->
                p2pManager.updateInternetPeers(peers)
            }
        }

        // Wire test results to telemetry
        testRunner.onTestResultsReady = { json -> telemetryManager.recordTestResults(json) }

        // Wire endurance test results to telemetry
        enduranceTestRunner.onEnduranceReportReady = { json ->
            telemetryManager.recordEnduranceReport(json)
        }
        enduranceTestRunner.onEnduranceSnapshotReady = { json ->
            telemetryManager.recordEnduranceSnapshot(json)
        }

        // ─── Global chat message persistence ───────────────────────────
        // Collect payload events from P2PManager and persist ALL incoming
        // text messages to Room, regardless of whether the chat UI is open.
        // This prevents message loss when the user is on another screen.
        val chatDao = database.chatDao()
        val seenPayloadIds = java.util.Collections.newSetFromMap(
            java.util.concurrent.ConcurrentHashMap<String, Boolean>()
        )
        appScope.launch {
            p2pManager.payloadEvents.collect { event ->
                val pkt = event.packet
                if (pkt.type == com.fyp.resilientp2p.transport.PacketType.DATA &&
                    pkt.sourceId != p2pManager.state.value.localDeviceName) {
                    val payloadId = "${pkt.sourceId}:${pkt.id}"
                    if (seenPayloadIds.add(payloadId)) {
                        val text = String(pkt.payload, StandardCharsets.UTF_8)
                        val isTestMessage = text.startsWith("__TEST__")
                        val isEnduranceMessage = text.startsWith("__ENDURANCE__")
                        val isLogRelay = text.startsWith(com.fyp.resilientp2p.managers.CloudLogManager.LOG_RELAY_PREFIX)
                        val isProxyRelay = text.startsWith(com.fyp.resilientp2p.managers.InternetGatewayManager.PROXY_RELAY_PREFIX)
                        val shouldSaveToChat = !isTestMessage && !isEnduranceMessage && !isLogRelay && !isProxyRelay
                        if (shouldSaveToChat) {
                            val isBroadcast = pkt.destId == "BROADCAST"
                            chatDao.insert(com.fyp.resilientp2p.data.ChatMessage(
                                peerId = pkt.sourceId,
                                isOutgoing = false,
                                type = com.fyp.resilientp2p.data.MessageType.TEXT,
                                text = text,
                                isBroadcast = isBroadcast
                            ))
                        }
                        // Cap dedup set to prevent unbounded growth
                        if (seenPayloadIds.size > 5000) seenPayloadIds.clear()
                    }
                }
            }
        }
        // Persist received file events globally
        appScope.launch {
            p2pManager.receivedFileEvents.collect { event ->
                val msgType = if (event.mimeType.startsWith("image/"))
                    com.fyp.resilientp2p.data.MessageType.IMAGE
                else com.fyp.resilientp2p.data.MessageType.FILE
                chatDao.insert(com.fyp.resilientp2p.data.ChatMessage(
                    peerId = event.senderName,
                    isOutgoing = false,
                    type = msgType,
                    fileName = event.fileName,
                    filePath = event.file.absolutePath,
                    mimeType = event.mimeType,
                    fileSize = event.file.length(),
                    transferProgress = -1
                ))
            }
        }

        // Track file transfer progress and update chat messages
        appScope.launch {
            p2pManager.payloadProgressEvents.collect { event ->
                // Find the most recent outgoing file/image message for this peer with progress 0-99
                val messages = chatDao.getRecentMessages(peerId = event.peerName, limit = 50)
                val pendingMsg = messages.find { msg ->
                    msg.isOutgoing &&
                    (msg.type == com.fyp.resilientp2p.data.MessageType.FILE ||
                     msg.type == com.fyp.resilientp2p.data.MessageType.IMAGE) &&
                    msg.transferProgress in 0..99
                }
                pendingMsg?.let { msg ->
                    if (event.progress >= 100) {
                        // Transfer complete
                        chatDao.markTransferComplete(msg.id, msg.filePath ?: "")
                    } else {
                        // Update progress
                        chatDao.updateProgress(msg.id, event.progress)
                    }
                }
            }
        }

        // ProcessLifecycleOwner NEVER dispatches ON_DESTROY, so that callback was dead code.
        // Use onStop to know when the app goes to background; actual cleanup is done
        // by the foreground service's onDestroy and graceful-shutdown paths.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to background — managers stay alive (foreground service).
            }
        })
    }
}
