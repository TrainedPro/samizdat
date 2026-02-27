package com.fyp.resilientp2p

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.managers.EmergencyManager
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.InternetGatewayManager
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.TelemetryManager
import com.fyp.resilientp2p.security.PeerBlacklist
import com.fyp.resilientp2p.security.RateLimiter
import com.fyp.resilientp2p.security.SecurityManager
import com.fyp.resilientp2p.testing.TestRunner

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
    /** ECDH + AES-256-GCM end-to-end encryption and HMAC packet integrity. */
    lateinit var securityManager: SecurityManager
        private set
    /** Per-peer sliding-window rate limiter (DoS defence). */
    lateinit var rateLimiter: RateLimiter
        private set
    /** Persistent peer blacklist with auto-ban on violation threshold. */
    lateinit var peerBlacklist: PeerBlacklist
        private set

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

        // Security managers
        securityManager = SecurityManager()
        rateLimiter = RateLimiter()
        peerBlacklist = PeerBlacklist(this)

        // Wire cross-references
        p2pManager.internetGatewayManager = internetGatewayManager
        p2pManager.emergencyManager = emergencyManager
        p2pManager.securityManager = securityManager
        p2pManager.rateLimiter = rateLimiter
        p2pManager.peerBlacklist = peerBlacklist

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
