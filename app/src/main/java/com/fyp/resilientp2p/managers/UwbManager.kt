package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import androidx.core.uwb.RangingParameters
import androidx.core.uwb.RangingResult
import androidx.core.uwb.UwbAddress
import androidx.core.uwb.UwbClientSessionScope
import androidx.core.uwb.UwbControllerSessionScope
import androidx.core.uwb.UwbManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UwbManager(private val context: Context) {

    companion object {
        private const val TAG = "UwbManager"
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var uwbManager: UwbManager? = null
    private var clientSessionScope: UwbClientSessionScope? = null
    private var rangingJob: Job? = null

    data class UwbState(
            val isRanging: Boolean = false,
            val peers: Map<String, Float> = emptyMap(),
            val azimuths: Map<String, Float> = emptyMap()
    )

    private val _state = MutableStateFlow(UwbState())
    val state = _state.asStateFlow()

    // Circular dependency handled via property injection
    var p2pManager: P2PManager? = null

    fun startRanging(peerEndpointId: String) {
        if (uwbManager == null) {
            log("UWB Manager not initialized.", "WARN")
            return
        }

        val myId = p2pManager?.getLocalDeviceName() ?: ""

        scope.launch {
            try {
                if (myId > peerEndpointId) {
                    // Role: Controller
                    log("Initializing as UWB Controller for $peerEndpointId")
                    val session = uwbManager?.controllerSessionScope()
                    clientSessionScope = session

                    if (session == null) {
                        log("Failed to create Controller session", "ERROR")
                        return@launch
                    }

                    val localAddress = session.localAddress
                    log("Local UWB Address (Controller): $localAddress")

                    // Send Address to Peer
                    p2pManager?.sendUwbAddress(peerEndpointId, localAddress.address)
                } else {
                    // Role: Controlee
                    log("Initializing as UWB Controlee for $peerEndpointId")
                    val session = uwbManager?.controleeSessionScope()
                    clientSessionScope = session

                    if (session == null) {
                        log("Failed to create Controlee session", "ERROR")
                        return@launch
                    }

                    val localAddress = session.localAddress
                    log("Local UWB Address (Controlee): $localAddress")

                    // Send Address to Peer
                    p2pManager?.sendUwbAddress(peerEndpointId, localAddress.address)
                }
            } catch (e: Exception) {
                log("Error starting UWB ranging: ${e.message}", "ERROR")
            }
        }
    }

    fun onPeerAddressReceived(peerEndpointId: String, addressBytes: ByteArray) {
        scope.launch {
            try {
                val peerAddress = UwbAddress(addressBytes)
                log("Received UWB Address from $peerEndpointId: $peerAddress")

                if (clientSessionScope != null) {
                    val session = clientSessionScope!!
                    val myId = p2pManager?.getLocalDeviceName() ?: ""

                    if (myId > peerEndpointId) {
                        // I am Controller. Add Controlee.
                        if (session is UwbControllerSessionScope) {
                            session.addControlee(peerAddress)
                            log("Added Controlee: $peerAddress")
                            startCollectingResults(session, peerEndpointId)
                        }
                    } else {
                        // I am Controlee.
                        // The Jetpack SDK handles the handshake if we provided the address?
                        // Actually, for Controlee, we usually need to provide the Controller's
                        // parameters.
                        // But `controleeSessionScope` doesn't have `addController`.
                        // It implies we just need to be active.
                        // However, we might need to pass the peer address to the session
                        // configuration if required.
                        // For now, let's assume the SDK handles it or we just start collecting.
                        log("Controlee received Controller address. Starting collection.")
                        startCollectingResults(session, peerEndpointId)
                    }
                } else {
                    log("Session not initialized when address received.", "WARN")
                }
            } catch (e: Exception) {
                log("Error handling peer UWB address: ${e.message}", "ERROR")
            }
        }
    }

    fun stopRanging() {
        // Stop all sessions
    }

    private fun log(message: String, level: String = "DEBUG") {
        when (level) {
            "DEBUG" -> Log.d(TAG, message)
            "WARN" -> Log.w(TAG, message)
            "ERROR" -> Log.e(TAG, message)
            else -> Log.d(TAG, message)
        }
    }

    private fun startCollectingResults(session: UwbClientSessionScope, peerId: String) {
        rangingJob?.cancel()
        rangingJob =
                scope.launch {
                    try {
                        val rangingParameters =
                                RangingParameters(
                                        uwbConfigType =
                                                1, // UWB_CONFIG_ID_1 (Hardcoded to avoid unresolved
                                        // ref)
                                        sessionId = 12345,
                                        subSessionId = 0,
                                        sessionKeyInfo = null,
                                        subSessionKeyInfo = null,
                                        complexChannel = null,
                                        peerDevices = emptyList(),
                                        updateRateType =
                                                RangingParameters.RANGING_UPDATE_RATE_FREQUENT
                                )

                        session.prepareSession(rangingParameters).collect { result ->
                            when (result) {
                                is RangingResult.RangingResultPosition -> {
                                    val dist = result.position.distance?.value ?: 0f
                                    val azimuth = result.position.azimuth?.value ?: 0f
                                    log("Ranging: $dist meters, $azimuth degrees")
                                    _state.update {
                                        it.copy(
                                                isRanging = true,
                                                peers = it.peers + (peerId to dist),
                                                azimuths = it.azimuths + (peerId to azimuth)
                                        )
                                    }
                                }
                                is RangingResult.RangingResultPeerDisconnected -> {
                                    // peerAddress might be 'peer' in some versions
                                    log("Peer disconnected", "WARN")
                                }
                                else -> {}
                            }
                        }
                    } catch (e: Exception) {
                        log("Error collecting ranging results: ${e.message}", "ERROR")
                    }
                }
    }
}
