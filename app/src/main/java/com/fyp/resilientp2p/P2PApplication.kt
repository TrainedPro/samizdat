package com.fyp.resilientp2p

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.data.GroupMessage
import com.fyp.resilientp2p.managers.EmergencyManager
import com.fyp.resilientp2p.managers.EncounterLogger
import com.fyp.resilientp2p.managers.FileShareManager
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.InternetGatewayManager
import com.fyp.resilientp2p.managers.LocationEstimator
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.TelemetryManager
import com.fyp.resilientp2p.testing.TestRunner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets

/**
 * Application singleton that owns all long-lived managers.
 *
 * Lifecycle: created once by Android, outlives any Activity/Service.
 * All managers are initialized in [onCreate] and shared via lateinit properties.
 */
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
    /** Cloud telemetry collection and upload via WorkManager. */
    lateinit var telemetryManager: TelemetryManager
        private set
    /** Internet gateway detection and cross-mesh cloud relay. */
    lateinit var internetGatewayManager: InternetGatewayManager
        private set
    /** Emergency broadcast and SOS beacon management. */
    lateinit var emergencyManager: EmergencyManager
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
        telemetryManager = TelemetryManager(this, database.telemetryDao(), database.logDao(), p2pManager)

        // Phase 3 managers
        internetGatewayManager = InternetGatewayManager(this, p2pManager)
        emergencyManager = EmergencyManager(this, p2pManager)

        // Phase 4 managers
        locationEstimator = LocationEstimator()
        encounterLogger = EncounterLogger(database.encounterDao())
        fileShareManager = FileShareManager(this, database.sharedFileDao(), p2pManager.state.value.localDeviceName)
        fileShareManager.sendPacket = { packet -> p2pManager.injectPacket(packet) }

        // Wire cross-references
        p2pManager.internetGatewayManager = internetGatewayManager
        p2pManager.emergencyManager = emergencyManager
        p2pManager.locationEstimator = locationEstimator
        p2pManager.encounterLogger = encounterLogger
        p2pManager.fileShareManager = fileShareManager

        // Group message handler: persist + auto-join unknown groups
        val groupMessageDao = database.groupMessageDao()
        val chatGroupDao = database.chatGroupDao()
        p2pManager.groupMessageHandler = { packet ->
            appScope.launch {
                val payloadStr = String(packet.payload, StandardCharsets.UTF_8)
                val parts = payloadStr.split("|", limit = 4)
                when (parts.getOrNull(0)) {
                    "MSG" -> {
                        if (parts.size >= 4) {
                            val groupId = parts[1]
                            val sender = parts[2]
                            val text = parts[3]
                            // Auto-register group if unknown
                            if (chatGroupDao.exists(groupId) == 0) {
                                chatGroupDao.upsert(
                                    com.fyp.resilientp2p.data.ChatGroup(
                                        groupId = groupId,
                                        name = "Group-${groupId.take(8)}",
                                        createdBy = sender
                                    )
                                )
                            }
                            if (groupMessageDao.existsByPacketId(packet.id) == 0) {
                                groupMessageDao.insert(
                                    GroupMessage(
                                        groupId = groupId,
                                        senderName = sender,
                                        text = text,
                                        packetId = packet.id
                                    )
                                )
                            }
                        }
                    }
                    "CREATE" -> {
                        if (parts.size >= 4) {
                            val groupId = parts[1]
                            val name = parts[2]
                            val creator = parts[3]
                            if (chatGroupDao.exists(groupId) == 0) {
                                chatGroupDao.upsert(
                                    com.fyp.resilientp2p.data.ChatGroup(
                                        groupId = groupId,
                                        name = name,
                                        createdBy = creator
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Start managers
        telemetryManager.start()
        internetGatewayManager.start()
        emergencyManager.start()

        // Wire test results to telemetry
        testRunner.onTestResultsReady = { json -> telemetryManager.recordTestResults(json) }

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
