package com.fyp.resilientp2p

import android.app.Application
import com.fyp.resilientp2p.data.AppDatabase
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager

class P2PApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }

    lateinit var p2pManager: P2PManager
        private set
    lateinit var heartbeatManager: HeartbeatManager
        private set
    lateinit var uwbManager: com.fyp.resilientp2p.managers.UwbManager
        private set

    override fun onCreate() {
        super.onCreate()
        uwbManager = com.fyp.resilientp2p.managers.UwbManager(this)
        p2pManager = P2PManager(this, database.logDao(), database.packetDao())

        // Wire up circular dependency
        p2pManager.uwbManager = uwbManager
        uwbManager.p2pManager = p2pManager

        heartbeatManager = HeartbeatManager(p2pManager)
    }
}
