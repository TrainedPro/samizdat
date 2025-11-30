package com.fyp.resilientp2p // Your correct package name

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fyp.resilientp2p.R
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class MainActivity : AppCompatActivity() {

    private val serviceId = "com.fyp.resilientp2p.p2p"
    private val strategy = Strategy.P2P_CLUSTER
    private val localUsername = "P2P-Node-${(100..999).random()}"

    private lateinit var connectionsClient: ConnectionsClient
    private lateinit var deviceNameText: TextView
    private lateinit var advertiseButton: Button
    private lateinit var discoverButton: Button
    private lateinit var stopButton: Button // New UI element
    private lateinit var statusLogText: TextView

    // A set to keep track of our connected endpoints
    private val connectedEndpoints = mutableSetOf<String>()

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
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { it }) {
                log("All permissions granted.")
            } else {
                log("ERROR: Some permissions were denied.")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        connectionsClient = Nearby.getConnectionsClient(this)

        deviceNameText = findViewById(R.id.deviceName)
        advertiseButton = findViewById(R.id.advertiseButton)
        discoverButton = findViewById(R.id.discoverButton)
        stopButton = findViewById(R.id.stopButton) // Find the new button
        statusLogText = findViewById(R.id.statusLog)

        deviceNameText.text = localUsername

        advertiseButton.setOnClickListener { startAdvertising() }
        discoverButton.setOnClickListener { startDiscovery() }
        stopButton.setOnClickListener { resetState() } // Set its click listener

        log("App started. Device name: $localUsername")
    }

    override fun onStart() {
        super.onStart()
        if (!hasPermissions()) {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }

    override fun onStop() {
        super.onStop()
        // Ensure we reset state and disconnect when the app is stopped.
        resetState()
    }

    /**
     * The new central cleanup function. This is the key to fixing the reset bug.
     * It stops all Nearby Connections activities and resets the UI to its initial state.
     */
    private fun resetState() {
        log("Resetting state and stopping all connections.")
        connectionsClient.stopAllEndpoints()
        connectedEndpoints.clear()
        updateUiState(isConnecting = false)
    }

    /** A new helper function to manage the visibility and state of our buttons. */
    private fun updateUiState(isConnecting: Boolean) {
        if (isConnecting) {
            advertiseButton.visibility = View.GONE
            discoverButton.visibility = View.GONE
            stopButton.visibility = View.VISIBLE
        } else {
            advertiseButton.visibility = View.VISIBLE
            discoverButton.visibility = View.VISIBLE
            stopButton.visibility = View.GONE
        }
    }

    private fun startAdvertising() {
        updateUiState(isConnecting = true)
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            localUsername, serviceId, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            log("Advertising started successfully.")
        }.addOnFailureListener { e ->
            log("ERROR: Advertising failed: $e")
            resetState() // If advertising fails, reset to the initial state
        }
    }

    private fun startDiscovery() {
        updateUiState(isConnecting = true)
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            log("Discovery started successfully.")
        }.addOnFailureListener { e ->
            log("ERROR: Discovery failed: $e")
            resetState() // If discovery fails, reset to the initial state
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            log("Endpoint found: ${info.endpointName} (ID: $endpointId)")
            connectionsClient.requestConnection(localUsername, endpointId, connectionLifecycleCallback)
                .addOnSuccessListener { log("Connection request sent to ${info.endpointName}") }
                .addOnFailureListener { e -> log("ERROR: Failed to send connection request: $e") }
        }
        override fun onEndpointLost(endpointId: String) {
            log("Endpoint lost: $endpointId")
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            log("Connection initiated by ${connectionInfo.endpointName}. Accepting.")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    log("✅ CONNECTION ESTABLISHED with $endpointId!")
                    connectedEndpoints.add(endpointId) // Track the new connection
                    // The UI is already in the "connecting" state (Stop button visible), which is correct.
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    log("Connection rejected by $endpointId.")
                    // If we were trying to connect and got rejected, we should go back to idle.
                    if (connectedEndpoints.isEmpty()) {
                        resetState()
                    }
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    log("ERROR: Connection error with $endpointId.")
                    if (connectedEndpoints.isEmpty()) {
                        resetState()
                    }
                }
                else -> log("Unknown connection status: ${result.status.statusCode}")
            }
        }
        override fun onDisconnected(endpointId: String) {
            log("Disconnected from $endpointId.")
            connectedEndpoints.remove(endpointId) // Stop tracking the connection
            // If this was our last connection, reset the state completely.
            if (connectedEndpoints.isEmpty()) {
                resetState()
            }
        }
    }

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) { /* Not used */ }
        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) { /* Not used */ }
    }

    private fun hasPermissions(): Boolean {
        return requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun log(message: String) {
        Log.d("P2PTestbed", message)
        runOnUiThread { statusLogText.append("\n> $message") }
    }
}