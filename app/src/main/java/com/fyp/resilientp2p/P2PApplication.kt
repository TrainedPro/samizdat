package com.fyp.resilientp2p

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.TelemetryManager
import com.fyp.resilientp2p.testing.TestRunner

class P2PApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    lateinit var p2pManager: P2PManager
        private set
    lateinit var heartbeatManager: HeartbeatManager
        private set
    lateinit var testRunner: TestRunner
        private set
    lateinit var telemetryManager: TelemetryManager
        private set

    val isTestMode: Boolean
        get() = BuildConfig.TEST_MODE

    override fun onCreate() {
        super.onCreate()
        p2pManager = P2PManager(this, database.logDao(), database.packetDao())
        heartbeatManager = HeartbeatManager(p2pManager)
        testRunner = TestRunner(this, p2pManager)
        telemetryManager = TelemetryManager(this, database.telemetryDao(), database.logDao(), p2pManager)
        telemetryManager.start()

        // Wire test results to telemetry
        testRunner.onTestResultsReady = { json -> telemetryManager.recordTestResults(json) }

        // AUDIT FIX: onTerminate() is never called on real devices.
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
