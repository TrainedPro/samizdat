package com.fyp.resilientp2p

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.service.P2PService
import com.fyp.resilientp2p.ui.P2PDashboard
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
    private lateinit var lowPowerCheckBox: android.widget.CheckBox
    private lateinit var dutyCycleCheckBox: android.widget.CheckBox
    private lateinit var hybridModeCheckBox: android.widget.CheckBox
    private lateinit var intervalSlider: Slider
    private lateinit var intervalValue: TextView
    private lateinit var exportLogsButton: Button
    private lateinit var signalStrengthMeter: android.widget.ProgressBar
    private lateinit var statusScrollView: android.widget.ScrollView

    // Compose State
    private var transferProgress by mutableStateOf(0)
    private var tracePath by mutableStateOf("")
    private var radarAzimuth by mutableStateOf<Float?>(null)
    private var radarDistance by mutableStateOf<Float?>(null)

    private val requiredPermissions =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.NEARBY_WIFI_DEVICES,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(
                        Manifest.permission.BLUETOOTH_SCAN,
                        Manifest.permission.BLUETOOTH_ADVERTISE,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.ACCESS_FINE_LOCATION
                )
            } else {
                // Legacy permissions for Android 7.0 (API 24) to Android 11 (API 30)
                // BLUETOOTH, BLUETOOTH_ADMIN, WIFI are "Normal" permissions and don't need runtime
                // requests.
                // Only Location needs runtime request.
                arrayOf(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
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
        lowPowerCheckBox = findViewById(R.id.lowPowerCheckBox)
        dutyCycleCheckBox = findViewById(R.id.dutyCycleCheckBox)
        hybridModeCheckBox = findViewById(R.id.hybridModeCheckBox)
        intervalSlider = findViewById(R.id.intervalSlider)
        intervalValue = findViewById(R.id.intervalValue)
        exportLogsButton = findViewById(R.id.exportLogsButton)
        signalStrengthMeter = findViewById(R.id.signalStrengthMeter)
        statusScrollView = findViewById(R.id.statusScroll)

        val composeView = findViewById<androidx.compose.ui.platform.ComposeView>(R.id.composeView)
        composeView.setContent {
            P2PDashboard(
                    transferProgress = transferProgress,
                    tracePath = tracePath,
                    radarAzimuth = radarAzimuth,
                    radarDistance = radarDistance
            )
        }

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

        // Setup Low Power UI
        lowPowerCheckBox.setOnCheckedChangeListener { _, isChecked ->
            p2pManager.setLowPower(isChecked)
        }

        // Setup Advanced Toggles
        dutyCycleCheckBox.setOnCheckedChangeListener { _, isChecked ->
            p2pManager.setDutyCycle(isChecked)
        }

        hybridModeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            p2pManager.setHybridMode(isChecked)
        }

        observeState()

        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                log("Requesting to ignore battery optimizations for mesh resilience.")
                val intent =
                        Intent(
                                android.provider.Settings
                                        .ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                        )
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }
    }

    private fun setupHeartbeatUi() {
        val heartbeatHeader = findViewById<android.widget.LinearLayout>(R.id.heartbeatHeader)
        val heartbeatContent = findViewById<android.widget.LinearLayout>(R.id.heartbeatContent)
        val expandIcon = findViewById<android.widget.ImageView>(R.id.heartbeatExpandIcon)

        heartbeatHeader.setOnClickListener {
            if (heartbeatContent.visibility == View.VISIBLE) {
                heartbeatContent.visibility = View.GONE
                expandIcon.animate().rotation(0f).start()
            } else {
                heartbeatContent.visibility = View.VISIBLE
                expandIcon.animate().rotation(180f).start()
            }
        }

        heartbeatCheckBox.setOnCheckedChangeListener { _, isChecked ->
            heartbeatManager.setHeartbeatEnabled(isChecked)
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
                        lowPowerCheckBox.isChecked = state.isLowPower

                        // Auth Token UI
                        if (state.authenticationDigits != null &&
                                        state.authenticatingEndpointId != null
                        ) {
                            showAuthTokenDialog(state.authenticationDigits)
                        }

                        // Smart Auto-scroll
                        val isAtBottom = !statusScrollView.canScrollVertically(1)
                        statusLogText.text = state.logs.joinToString("\n> ")

                        if (isAtBottom) {
                            statusScrollView.post { statusScrollView.fullScroll(View.FOCUS_DOWN) }
                        }

                        // Parse RTT from the last log entry to update meter
                        // This is a temporary hack until we expose metrics properly
                        // Removed in favor of Bandwidth Events
                    }
                }
                launch {
                    p2pManager.bandwidthEvents.collect { info ->
                        val qualityScore =
                                when (info.quality) {
                                    com.google.android.gms.nearby.connection.BandwidthInfo.Quality
                                            .HIGH -> 3
                                    com.google.android.gms.nearby.connection.BandwidthInfo.Quality
                                            .MEDIUM -> 2
                                    com.google.android.gms.nearby.connection.BandwidthInfo.Quality
                                            .LOW -> 1
                                    else -> 0
                                }
                        updateSignalMeter(qualityScore)
                    }
                }
                launch {
                    p2pManager.payloadEvents.collect { event ->
                        // Trace Visualization
                        if (event.packet.trace.isNotEmpty()) {
                            val path = event.packet.trace.joinToString(" -> ") { it.peerId }
                            tracePath = path // Update Compose State
                            log("Trace: $path -> Me")
                        }
                    }
                }
                launch {
                    p2pManager.payloadProgressEvents.collect { event ->
                        updateTransferProgress(event.progress)
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

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Service Binding
    private var p2pService: P2PService? = null
    private var isBound = false

    private val connection =
            object : ServiceConnection {
                override fun onServiceConnected(className: ComponentName, service: IBinder) {
                    val binder = service as P2PService.LocalBinder
                    p2pService = binder.getService()
                    isBound = true
                }

                override fun onServiceDisconnected(arg0: ComponentName) {
                    isBound = false
                    p2pService = null
                }
            }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions()) {
            requestPermissionLauncher.launch(requiredPermissions)
        } else {
            startAndBindService()
        }
    }

    private fun startAndBindService() {
        Intent(this, P2PService::class.java).also { intent ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
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
                                    "Logs saved to ${file.absolutePath}. Sending to peers...",
                                    Toast.LENGTH_LONG
                            )
                            .show()
                    log("Logs saved locally. Attempting P2P transfer...")
                    p2pManager.sendFile(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { log("ERROR: Failed to export logs: ${e.message}") }
            }
        }
    }

    private fun updateSignalMeter(qualityScore: Int) {
        // Map Quality (0-3) to 0-100
        val progress =
                when (qualityScore) {
                    3 -> 100
                    2 -> 66
                    1 -> 33
                    else -> 0
                }
        signalStrengthMeter.progress = progress

        val color =
                when (qualityScore) {
                    3 -> android.graphics.Color.GREEN
                    2 -> android.graphics.Color.YELLOW
                    1 -> android.graphics.Color.RED
                    else -> android.graphics.Color.GRAY
                }
        signalStrengthMeter.progressTintList = android.content.res.ColorStateList.valueOf(color)
    }

    private fun log(message: String) {
        // Local UI log for permission events not captured by P2PManager
        statusLogText.append("\n> $message")
    }

    // ============================================================================================
    // UI Enhancements
    // ============================================================================================

    private fun showAuthTokenDialog(token: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Verify this code with the other device:\n\n$token")
                .setPositiveButton("Confirmed") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false) // Force user to see it
                .show()
    }

    private fun updateTransferProgress(progress: Int) {
        transferProgress = progress
        if (progress % 25 == 0) {
            log("File Transfer: $progress%")
        }
    }

    // Mock UWB Radar View (Canvas implementation would go here in a custom View class)
    // For this testbed, we'll just log directional updates if we had them.
    // Mock UWB Radar View
    private fun updateRadar(azimuth: Float, distance: Float) {
        radarAzimuth = azimuth
        radarDistance = distance
    }
}
