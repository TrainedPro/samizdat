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

    override fun onCreate() {
        super.onCreate()
        p2pManager = P2PManager(this, database.logDao())
        heartbeatManager = HeartbeatManager(p2pManager)
    }
}
