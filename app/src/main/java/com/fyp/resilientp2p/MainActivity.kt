package com.fyp.resilientp2p // CORRECTED package name

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.fyp.resilientp2p.R // CORRECTED import for the R class
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*

class MainActivity : AppCompatActivity() {

    private val serviceId = "com.fyp.resilientp2p.p2p" // CORRECTED service ID
    private val strategy = Strategy.P2P_CLUSTER
    private val localUsername = "P2P-Node-${(100..999).random()}" // CORRECTED username

    private lateinit var connectionsClient: ConnectionsClient
    private lateinit var deviceNameText: TextView
    private lateinit var advertiseButton: Button
    private lateinit var discoverButton: Button
    private lateinit var statusLogText: TextView

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
        statusLogText = findViewById(R.id.statusLog)

        deviceNameText.text = localUsername

        advertiseButton.setOnClickListener { startAdvertising() }
        discoverButton.setOnClickListener { startDiscovery() }

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
        connectionsClient.stopAllEndpoints()
        log("Stopped all endpoints.")
    }

    private fun startAdvertising() {
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startAdvertising(
            localUsername, serviceId, connectionLifecycleCallback, advertisingOptions
        ).addOnSuccessListener {
            log("Advertising started successfully.")
            advertiseButton.isEnabled = false
            discoverButton.isEnabled = false
        }.addOnFailureListener { e ->
            log("ERROR: Advertising failed: $e")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()
        connectionsClient.startDiscovery(
            serviceId, endpointDiscoveryCallback, discoveryOptions
        ).addOnSuccessListener {
            log("Discovery started successfully.")
            discoverButton.isEnabled = false
            advertiseButton.isEnabled = false
        }.addOnFailureListener { e ->
            log("ERROR: Discovery failed: $e")
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
                    advertiseButton.isEnabled = false
                    discoverButton.isEnabled = false
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> log("Connection rejected by $endpointId.")
                ConnectionsStatusCodes.STATUS_ERROR -> log("ERROR: Connection error with $endpointId.")
                else -> log("Unknown connection status: ${result.status.statusCode}")
            }
        }
        override fun onDisconnected(endpointId: String) {
            log("Disconnected from $endpointId. You can advertise/discover again.")
            advertiseButton.isEnabled = true
            discoverButton.isEnabled = true
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