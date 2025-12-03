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
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.fyp.resilientp2p.managers.HeartbeatManager
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.UwbManager
import com.fyp.resilientp2p.service.P2PService
import com.fyp.resilientp2p.ui.ResilientP2PApp
import com.fyp.resilientp2p.ui.theme.ResilientP2PTestbedTheme
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
    private lateinit var uwbManager: UwbManager

    // Dynamic permission list generation
    private fun getRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Only request UWB permission if the hardware feature is present
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        packageManager.hasSystemFeature("android.hardware.uwb")
        ) {
            permissions.add(Manifest.permission.UWB_RANGING)
        }

        return permissions.toTypedArray()
    }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                val denied = permissions.filter { !it.value }.map { it.key }

                // FIX: Filter out optional UWB permission.
                // If UWB is denied, we still want the app to run.
                val criticalDenied = denied.filter { it != Manifest.permission.UWB_RANGING }

                if (criticalDenied.isEmpty()) {
                    if (denied.contains(Manifest.permission.UWB_RANGING)) {
                        p2pManager.log(
                                "UWB Permission denied or not configured. Radar features will be disabled.",
                                "WARN"
                        )
                    } else {
                        p2pManager.log("All permissions granted.")
                    }
                    // Update UWB permission state
                    p2pManager.updateUwbPermission(
                            !denied.contains(Manifest.permission.UWB_RANGING)
                    )
                    startAndBindService()
                } else {
                    p2pManager.log("ERROR: Critical permissions denied: $criticalDenied", "ERROR")
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get Managers from Application
        val app = application as P2PApplication
        p2pManager = app.p2pManager
        heartbeatManager = app.heartbeatManager
        uwbManager = app.uwbManager

        // Set Content to Pure Compose
        val composeView =
                ComposeView(this).apply {
                    setContent {
                        ResilientP2PTestbedTheme(darkTheme = false) {
                            ResilientP2PApp(
                                    p2pManager = p2pManager,
                                    uwbManager = uwbManager,
                                    onExportLogs = { exportLogs() }
                            )
                        }
                    }
                }
        setContentView(composeView)

        // Initial Checks
        val neededPermissions = getRequiredPermissions()
        if (!hasPermissions(neededPermissions)) {
            requestPermissionLauncher.launch(neededPermissions)
        } else {
            startAndBindService()
            // Check permissions again to set initial state correctly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ContextCompat.checkSelfPermission(
                                    this,
                                    Manifest.permission.UWB_RANGING
                            ) == PackageManager.PERMISSION_GRANTED
            ) {
                p2pManager.updateUwbPermission(true)
            } else {
                p2pManager.updateUwbPermission(false)
            }
        }
        checkBatteryOptimization()

        observeAuthEvents()
    }

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            p2pManager.state.collect { state ->
                if (state.authenticationDigits != null && state.authenticatingEndpointId != null) {
                    showAuthTokenDialog(state.authenticationDigits!!)
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Advanced")
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
                )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == 1) {
            showAdvancedOptionsDialog()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showAdvancedOptionsDialog() {
        val dialogView =
                LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(60, 40, 60, 20)
                }

        // Low Power Toggle
        val lpCheck =
                android.widget.CheckBox(this).apply {
                    text = "Low Power Mode"
                    isChecked = p2pManager.state.value.isLowPower
                    setOnCheckedChangeListener { _, isChecked -> p2pManager.setLowPower(isChecked) }
                }
        dialogView.addView(lpCheck)

        // Duty Cycle Toggle
        val dcCheck =
                android.widget.CheckBox(this).apply {
                    text = "Duty Cycle (Resilience)"
                    isChecked = p2pManager.state.value.isDutyCycleActive // SYNCED STATE
                    setOnCheckedChangeListener { _, isChecked ->
                        p2pManager.setDutyCycle(isChecked)
                    }
                }
        dialogView.addView(dcCheck)

        // Hybrid Mode Toggle
        val hmCheck =
                android.widget.CheckBox(this).apply {
                    text = "Hybrid Mode (LAN/BLE)"
                    isChecked = p2pManager.state.value.isHybridMode
                    setOnCheckedChangeListener { _, isChecked ->
                        p2pManager.setHybridMode(isChecked)
                    }
                }
        dialogView.addView(hmCheck)

        // Separator
        val separator =
                android.view.View(this).apply {
                    layoutParams =
                            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 2)
                                    .apply {
                                        topMargin = 30
                                        bottomMargin = 30
                                    }
                    setBackgroundColor(android.graphics.Color.LTGRAY)
                }
        dialogView.addView(separator)

        // Heartbeat Controls
        val hbTitle =
                TextView(this).apply {
                    text = "Heartbeat Settings"
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
        dialogView.addView(hbTitle)

        val hbCheck =
                android.widget.CheckBox(this).apply {
                    text = "Enable Heartbeat"
                    isChecked = heartbeatManager.config.value.isEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        heartbeatManager.setHeartbeatEnabled(isChecked)
                    }
                }
        dialogView.addView(hbCheck)

        val sliderLabel =
                TextView(this).apply {
                    text = "Interval: ${heartbeatManager.config.value.intervalMs}ms"
                }
        dialogView.addView(sliderLabel)

        val slider =
                Slider(this).apply {
                    valueFrom = 1000f
                    valueTo = 30000f
                    stepSize = 1000f
                    value = heartbeatManager.config.value.intervalMs.toFloat()
                    // Set colors for visibility in light theme
                    thumbTintList =
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#0091EA")
                            )
                    trackActiveTintList =
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#0091EA")
                            )
                    trackInactiveTintList =
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.parseColor("#C0C0C0")
                            )
                    addOnChangeListener { _, value, _ ->
                        val interval = value.toLong()
                        sliderLabel.text = "Interval: ${interval}ms"
                        heartbeatManager.updateConfig(intervalMs = interval)
                    }
                }
        dialogView.addView(slider)

        AlertDialog.Builder(this)
                .setTitle("Advanced Options")
                .setView(dialogView)
                .setPositiveButton("Done", null)
                .show()
    }

    private fun showAuthTokenDialog(token: String) {
        AlertDialog.Builder(this)
                .setTitle("Security Check")
                .setMessage("Verify this code with the other device:\n\n$token")
                .setPositiveButton("Confirmed") { dialog, _ -> dialog.dismiss() }
                .setCancelable(false)
                .show()
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

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

    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
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
                    p2pManager.log("Logs saved locally. Attempting P2P transfer...")
                    p2pManager.sendFile(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    p2pManager.log("ERROR: Failed to export logs: ${e.message}", "ERROR")
                }
            }
        }
    }
}
