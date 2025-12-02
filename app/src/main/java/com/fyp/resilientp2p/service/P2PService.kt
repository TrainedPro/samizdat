package com.fyp.resilientp2p.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.fyp.resilientp2p.MainActivity
import com.fyp.resilientp2p.P2PApplication
import com.fyp.resilientp2p.R
import com.fyp.resilientp2p.managers.P2PManager

class P2PService : Service() {

    private val binder = LocalBinder()
    lateinit var p2pManager: P2PManager

    inner class LocalBinder : Binder() {
        fun getService(): P2PService = this@P2PService
    }

    override fun onCreate() {
        super.onCreate()
        p2pManager = (application as P2PApplication).p2pManager
        startForegroundService()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // If the service is killed, restart it
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private fun startForegroundService() {
        val channelId = "P2P_SERVICE_CHANNEL"
        val channelName = "P2P Background Service"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                    NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification: Notification =
                NotificationCompat.Builder(this, channelId)
                        .setContentTitle("Resilient P2P Node Active")
                        .setContentText("Maintaining mesh network connection...")
                        .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                        .setContentIntent(pendingIntent)
                        .setOngoing(true)
                        .build()

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    1,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
            )
        } else {
            startForeground(1, notification)
        }
    }
}
