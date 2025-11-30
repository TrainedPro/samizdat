package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.LogDao
import com.fyp.resilientp2p.data.LogEntry
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Manages all interactions with the Nearby Connections API. Follows the Singleton pattern to ensure
 * a single point of truth for network state.
 */
class P2PManager(private val context: Context, private val logDao: LogDao) {

    companion object {
        private const val TAG = "P2PManager"
        private const val SERVICE_ID = "com.fyp.resilientp2p.p2p"
        private val STRATEGY = Strategy.P2P_CLUSTER
    }

    private val connectionsClient = Nearby.getConnectionsClient(context)
    private val localUsername = "P2P-Node-${(100..999).random()}"
    private val scope = CoroutineScope(Dispatchers.IO)

    // State Management
    data class P2PState(
            val isAdvertising: Boolean = false,
            val isDiscovering: Boolean = false,
            val connectedEndpoints: Set<String> = emptySet(),
            val logs: List<String> = emptyList()
    )

    private val _state = MutableStateFlow(P2PState())
    val state: StateFlow<P2PState> = _state.asStateFlow()

    // Payload Event Stream
    data class PayloadEvent(val endpointId: String, val payload: Payload)
    private val _payloadEvents = MutableSharedFlow<PayloadEvent>()
    val payloadEvents: SharedFlow<PayloadEvent> = _payloadEvents.asSharedFlow()

    fun getLocalDeviceName(): String = localUsername

    fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient
                .startAdvertising(localUsername, SERVICE_ID, connectionLifecycleCallback, options)
                .addOnSuccessListener {
                    log("Advertising started.")
                    _state.update { it.copy(isAdvertising = true) }
                }
                .addOnFailureListener { e -> log("Advertising failed: ${e.message}") }
    }

    fun startDiscovery() {
        val options = DiscoveryOptions.Builder().setStrategy(STRATEGY).build()
        connectionsClient
                .startDiscovery(SERVICE_ID, endpointDiscoveryCallback, options)
                .addOnSuccessListener {
                    log("Discovery started.")
                    _state.update { it.copy(isDiscovering = true) }
                }
                .addOnFailureListener { e -> log("Discovery failed: ${e.message}") }
    }
    fun stopAdvertising() {
        connectionsClient.stopAdvertising()
        _state.update { it.copy(isAdvertising = false) }
        log("Advertising stopped.")
    }

    fun stopDiscovery() {
        connectionsClient.stopDiscovery()
        _state.update { it.copy(isDiscovering = false) }
        log("Discovery stopped.")
    }

    fun stopAll() {
        connectionsClient.stopAllEndpoints()
        _state.update {
            it.copy(isAdvertising = false, isDiscovering = false, connectedEndpoints = emptySet())
        }
        log("Stopped all connections.")
    }

    fun sendPayload(endpointId: String, bytes: ByteArray) {
        connectionsClient.sendPayload(endpointId, Payload.fromBytes(bytes)).addOnFailureListener { e
            ->
            log("Failed to send payload to $endpointId: ${e.message}", "ERROR")
        }
    }

    private val connectionLifecycleCallback =
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    log("Connection initiated by ${info.endpointName}. Accepting.")
                    connectionsClient.acceptConnection(endpointId, payloadCallback)
                }

                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    when (result.status.statusCode) {
                        ConnectionsStatusCodes.STATUS_OK -> {
                            log("Connected to $endpointId")
                            _state.update {
                                it.copy(connectedEndpoints = it.connectedEndpoints + endpointId)
                            }
                        }
                        else -> {
                            log("Connection to $endpointId failed: ${result.status.statusCode}")
                        }
                    }
                }

                override fun onDisconnected(endpointId: String) {
                    log("Disconnected from $endpointId")
                    _state.update {
                        it.copy(connectedEndpoints = it.connectedEndpoints - endpointId)
                    }
                }
            }

    private val endpointDiscoveryCallback =
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    log("Found endpoint: ${info.endpointName}. Requesting connection.")
                    connectionsClient.requestConnection(
                                    localUsername,
                                    endpointId,
                                    connectionLifecycleCallback
                            )
                            .addOnFailureListener { e ->
                                log("Request connection failed: ${e.message}")
                            }
                }

                override fun onEndpointLost(endpointId: String) {
                    log("Lost endpoint: $endpointId")
                }
            }

    private val payloadCallback =
            object : PayloadCallback() {
                override fun onPayloadReceived(endpointId: String, payload: Payload) {
                    scope.launch { _payloadEvents.emit(PayloadEvent(endpointId, payload)) }
                }

                override fun onPayloadTransferUpdate(
                        endpointId: String,
                        update: PayloadTransferUpdate
                ) {
                    // TODO: Track transfer progress if needed
                }
            }

    fun log(msg: String, type: String = "INFO") {
        Log.d(TAG, msg)
        _state.update { it.copy(logs = it.logs + msg) }

        scope.launch { logDao.insert(LogEntry(type = type, message = msg)) }
    }
}
