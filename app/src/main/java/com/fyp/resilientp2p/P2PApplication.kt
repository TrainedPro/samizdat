package com.fyp.resilientp2p

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.testing.TestRunner

class P2PApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    lateinit var p2pManager: P2PManager
        private set
    lateinit var heartbeatManager: HeartbeatManager
        private set
    lateinit var testRunner: TestRunner
        private set

    val isTestMode: Boolean
        get() = BuildConfig.TEST_MODE

    override fun onCreate() {
        super.onCreate()
        p2pManager = P2PManager(this, database.logDao(), database.packetDao())
        heartbeatManager = HeartbeatManager(p2pManager)
        testRunner = TestRunner(this, p2pManager)

        // AUDIT FIX: onTerminate() is never called on real devices.
        // Use ProcessLifecycleOwner for reliable cleanup when the app process stops.
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // App went to background — managers stay alive (foreground service).
                // Cleanup only on actual destroy.
            }
            override fun onDestroy(owner: LifecycleOwner) {
                heartbeatManager.destroy()
                p2pManager.stopAll()
            }
        })
    }
}
