package com.fyp.resilientp2p

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var p2pManager: P2PManager
    private lateinit var heartbeatManager: HeartbeatManager
    private lateinit var deviceNameText: TextView
    private lateinit var advertiseButton: Button
    private lateinit var discoverButton: Button
    private lateinit var stopButton: Button
    private lateinit var statusLogText: TextView
    private lateinit var heartbeatCheckBox: android.widget.CheckBox
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValue: TextView
    private lateinit var exportLogsButton: Button

    private val requiredPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.NEARBY_WIFI_DEVICES
                )
            } else {
                arrayOf(
                        Manifest.permission.BLUETOOTH,
                        Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.CHANGE_WIFI_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                )
            }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                if (permissions.values.all { it }) {
                    log("All permissions granted.")
                } else {
                    log("ERROR: Some permissions were denied.")
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get Managers from Application
        val app = application as P2PApplication
        p2pManager = app.p2pManager
        heartbeatManager = app.heartbeatManager

        deviceNameText = findViewById(R.id.deviceName)
        advertiseButton = findViewById(R.id.advertiseButton)
        discoverButton = findViewById(R.id.discoverButton)
        stopButton = findViewById(R.id.stopButton)
        statusLogText = findViewById(R.id.statusLog)
        heartbeatCheckBox = findViewById(R.id.heartbeatCheckBox)
        intervalSlider = findViewById(R.id.intervalSlider)
        intervalValue = findViewById(R.id.intervalValue)
        exportLogsButton = findViewById(R.id.exportLogsButton)

        statusLogText.movementMethod = ScrollingMovementMethod()

        deviceNameText.text = p2pManager.getLocalDeviceName()

        advertiseButton.setOnClickListener {
            if (p2pManager.state.value.isAdvertising) {
                p2pManager.stopAdvertising()
            } else {
                p2pManager.startAdvertising()
            }
        }

        discoverButton.setOnClickListener {
            if (p2pManager.state.value.isDiscovering) {
                p2pManager.stopDiscovery()
            } else {
                p2pManager.startDiscovery()
            }
        }

        stopButton.setOnClickListener { p2pManager.stopAll() }
        exportLogsButton.setOnClickListener { exportLogs() }

        setupHeartbeatUi()
        observeState()
    }

    private fun setupHeartbeatUi() {
        heartbeatCheckBox.setOnCheckedChangeListener { _, isChecked ->
            heartbeatManager.updateConfig(enabled = isChecked)
        }

        intervalSlider.addOnChangeListener { _, value, _ ->
            val interval = value.toLong()
            intervalValue.text = "${interval}ms"
            // Debouncing could be added here, but for now direct update is fine
            heartbeatManager.updateConfig(intervalMs = interval)
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    p2pManager.state.collect { state ->
                        updateUiState(state)
                        statusLogText.text = state.logs.joinToString("\n> ")
                    }
                }
                launch {
                    heartbeatManager.config.collect { config ->
                        heartbeatCheckBox.isChecked = config.isEnabled
                        intervalSlider.value = config.intervalMs.toFloat()
                        intervalValue.text = "${config.intervalMs}ms"
                    }
                }
            }
        }
    }

    private fun updateUiState(state: P2PManager.P2PState) {
        // Update Advertise Button
        if (state.isAdvertising) {
            advertiseButton.text = "Stop Adv"
            advertiseButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.purple_700)
        } else {
            advertiseButton.text = "Advertise"
            advertiseButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.purple_500)
        }
        advertiseButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        (advertiseButton as com.google.android.material.button.MaterialButton).setIconTint(
                ContextCompat.getColorStateList(this, android.R.color.white)
        )

        // Update Discover Button
        if (state.isDiscovering) {
            discoverButton.text = "Stop Disc"
            discoverButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.purple_700)
        } else {
            discoverButton.text = "Discover"
            discoverButton.backgroundTintList =
                    ContextCompat.getColorStateList(this, R.color.purple_500)
        }
        discoverButton.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        (discoverButton as com.google.android.material.button.MaterialButton).setIconTint(
                ContextCompat.getColorStateList(this, android.R.color.white)
        )

        // Stop All button visibility
        val isBusy =
                state.isAdvertising || state.isDiscovering || state.connectedEndpoints.isNotEmpty()
        stopButton.visibility = if (isBusy) View.VISIBLE else View.GONE

        // Ensure buttons are always visible (unless we want to hide them when connected, but user
        // asked for simultaneous)
        advertiseButton.visibility = View.VISIBLE
        discoverButton.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions()) {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun exportLogs() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val database = (application as P2PApplication).database
                val logs = database.logDao().getLogsSnapshot()

                val fileName = "p2p_logs_${System.currentTimeMillis()}.csv"
                val file = File(getExternalFilesDir(null), fileName)

                FileWriter(file).use { writer ->
                    writer.append("ID,Timestamp,Type,PeerID,Message,RSSI,Latency,PayloadSize\n")
                    val dateFormat =
                            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

                    logs.forEach { log ->
                        val date = dateFormat.format(Date(log.timestamp))
                        writer.append("${log.id},")
                        writer.append("$date,")
                        writer.append("${log.type},")
                        writer.append("${log.peerId ?: ""},")
                        writer.append("\"${log.message.replace("\"", "\"\"")}\",")
                        writer.append("${log.rssi ?: ""},")
                        writer.append("${log.latencyMs ?: ""},")
                        writer.append("${log.payloadSizeBytes ?: ""}\n")
                    }
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                                    this@MainActivity,
                                    "Logs exported to ${file.absolutePath}",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                    log("Logs exported to ${file.absolutePath}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("ERROR: Failed to export logs: ${e.message}") }
            }
        }
    }

    private fun log(message: String) {
        // Local UI log for permission events not captured by P2PManager
        statusLogText.append("\n> $message")
    }
}
