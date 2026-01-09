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
import com.fyp.resilientp2p.service.P2PService
import com.fyp.resilientp2p.ui.ResilientP2PApp
import com.fyp.resilientp2p.ui.theme.ResilientP2PTestbedTheme
import com.fyp.resilientp2p.data.LogLevel
import com.google.android.material.slider.Slider
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var p2pManager: P2PManager
    private lateinit var heartbeatManager: HeartbeatManager

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

        return permissions.toTypedArray()
    }

    private val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                    permissions ->
                @android.annotation.SuppressLint("InlinedApi")
                val denied = permissions.filter { !it.value }.map { it.key }

                if (denied.isEmpty()) {
                    p2pManager.log("All permissions granted.")
                    startAndBindService()
                } else {
                    // Show user-friendly dialog for permission denial
                    androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                        .setTitle("Permissions Required")
                        .setMessage("This app requires all requested permissions to function. Please grant them to continue.")
                        .setPositiveButton("Retry") { _, _ ->
                            requestPermissions()
                        }
                        .setNegativeButton("Exit") { _, _ -> finish() }
                        .setCancelable(false)
                        .show()
                }
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get Managers from Application
        val app = application as P2PApplication
        p2pManager = app.p2pManager
        heartbeatManager = app.heartbeatManager

        // Set Content to Pure Compose
        val composeView =
                ComposeView(this).apply {
                    setContent {
                        ResilientP2PTestbedTheme(darkTheme = false) {
                            ResilientP2PApp(
                                    p2pManager = p2pManager,
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
        }
        checkBatteryOptimization()

        observeAuthEvents()
    }

    private var currentAuthDialog: AlertDialog? = null

    private fun observeAuthEvents() {
        lifecycleScope.launch {
            p2pManager
                    .state
                    .map { it.authenticationDigits to it.authenticatingEndpointId }
                    .distinctUntilChanged()
                    .collect { (digits, id) ->
                        // Dismiss existing dialog if state changes
                        currentAuthDialog?.dismiss()
                        currentAuthDialog = null

                        if (digits != null && id != null) {
                            showAuthTokenDialog(digits)
                        }
                    }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Advanced")
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
                )
        menu.add(0, 2, 0, "Exit")
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_IF_ROOM or MenuItem.SHOW_AS_ACTION_WITH_TEXT
                )
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> {
                showAdvancedOptionsDialog()
                return true
            }
            2 -> {
                showExitConfirmationDialog()
                return true
            }
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
                    text = getString(R.string.low_power_mode)
                    isChecked = p2pManager.state.value.isLowPower
                    setOnCheckedChangeListener { _, isChecked -> p2pManager.setLowPower(isChecked) }
                }
        dialogView.addView(lpCheck)

        // Hybrid Mode Toggle
        val hmCheck =
                android.widget.CheckBox(this).apply {
                    text = getString(R.string.hybrid_mode)
                    isChecked = p2pManager.state.value.isHybridMode
                    setOnCheckedChangeListener { _, isChecked ->
                        p2pManager.setHybridMode(isChecked)
                    }
                }
        dialogView.addView(hmCheck)

        // Manual Connection Toggle
        val mcCheck =
                android.widget.CheckBox(this).apply {
                    text = getString(R.string.manual_connection_mode)
                    isChecked = p2pManager.state.value.isManualConnectionEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        p2pManager.setManualConnection(isChecked)
                    }
                }
        dialogView.addView(mcCheck)

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
                    text = getString(R.string.heartbeat_settings)
                    setTypeface(null, android.graphics.Typeface.BOLD)
                }
        dialogView.addView(hbTitle)

        val hbCheck =
                android.widget.CheckBox(this).apply {
                    text = getString(R.string.enable_heartbeat)
                    isChecked = heartbeatManager.config.value.isEnabled
                    setOnCheckedChangeListener { _, isChecked ->
                        heartbeatManager.setHeartbeatEnabled(isChecked)
                    }
                }
        dialogView.addView(hbCheck)

        val sliderLabel =
                TextView(this).apply {
                    text =
                            getString(
                                    R.string.interval_format,
                                    heartbeatManager.config.value.intervalMs
                            )
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
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.BLUE)
                    trackActiveTintList =
                            android.content.res.ColorStateList.valueOf(android.graphics.Color.BLUE)
                    trackInactiveTintList =
                            android.content.res.ColorStateList.valueOf(
                                    android.graphics.Color.LTGRAY
                            )
                    addOnChangeListener { _, value, _ ->
                        val interval = value.toLong()
                        sliderLabel.text = getString(R.string.interval_format, interval)
                        heartbeatManager.updateConfig(intervalMs = interval)
                    }
                }
        dialogView.addView(slider)

        val advancedDialog =
                AlertDialog.Builder(this)
                        .setTitle(R.string.advanced_options)
                        .setView(dialogView)
                        .setPositiveButton(R.string.done, null)
                        .create()
        advancedDialog.show()
        // Set button color explicitly to make it visible
        advancedDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(android.graphics.Color.BLUE)
    }

    private fun showAuthTokenDialog(token: String) {
        // Capture endpoint ID at dialog creation time to avoid stale state
        val endpointId = p2pManager.state.value.authenticatingEndpointId
        val authDialog =
                AlertDialog.Builder(this)
                        .setTitle(R.string.security_check)
                        .setMessage(getString(R.string.verify_code_message, token))
                        .setPositiveButton(R.string.accept) {
                                dialog: android.content.DialogInterface,
                                _: Int ->
                            endpointId?.let { id -> p2pManager.acceptConnection(id) }
                            dialog.dismiss()
                        }
                        .setNegativeButton(R.string.reject) {
                                dialog: android.content.DialogInterface,
                                _: Int ->
                            endpointId?.let { id -> p2pManager.rejectConnection(id) }
                            dialog.dismiss()
                        }
                        .setCancelable(false)
                        .create()
        currentAuthDialog = authDialog
        authDialog.show()
        // Set button color explicitly to make it visible
        authDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(android.graphics.Color.GREEN) // Green
        authDialog
                .getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(android.graphics.Color.RED) // Red
    }

    private fun showExitConfirmationDialog() {
        val exitDialog =
                AlertDialog.Builder(this)
                        .setTitle(R.string.exit_application)
                        .setMessage(R.string.exit_confirmation_message)
                        .setPositiveButton(R.string.exit) { _, _ -> gracefulShutdown() }
                        .setNegativeButton(R.string.cancel, null)
                        .create()
        exitDialog.show()
        // Set button colors explicitly to make them visible
        exitDialog
                .getButton(AlertDialog.BUTTON_POSITIVE)
                ?.setTextColor(
                        android.graphics.Color.RED // Red for exit
                )
        exitDialog
                .getButton(AlertDialog.BUTTON_NEGATIVE)
                ?.setTextColor(
                        android.graphics.Color.BLUE // Blue for cancel
                )
    }

    private fun gracefulShutdown() {
        p2pManager.log("Initiating graceful shutdown...", LogLevel.INFO)

        // Stop heartbeat
        heartbeatManager.setHeartbeatEnabled(false)

        // Stop all P2P connections and advertising/discovery
        p2pManager.stopAll()

        // Unbind and stop service
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
        val serviceIntent = Intent(this, P2PService::class.java)
        stopService(serviceIntent)

        p2pManager.log("Shutdown complete. Exiting app.", LogLevel.INFO)

        // Use Handler to avoid lifecycle issues with coroutine delay
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isFinishing && !isDestroyed) {
                finishAffinity()
            }
        }, 500)
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(getRequiredPermissions())
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
        val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
            val intent =
                    Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply { data = android.net.Uri.parse("package:$packageName") }
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // CRITICAL FIX: Cancel HeartbeatManager coroutines
        if (isBound) {
            try {
                unbindService(connection)
            } catch (e: IllegalArgumentException) {
                // Service was not registered - can happen if binding not complete
            }
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
                        writer.append("${log.logType},")
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
                                    getString(R.string.logs_saved_toast, file.absolutePath),
                                    Toast.LENGTH_LONG
                            )
                            .show()
                    p2pManager.log(getString(R.string.logs_saved_log))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    p2pManager.log(getString(R.string.logs_export_error, e.message), LogLevel.ERROR)
                }
            }
        }
    }
}
