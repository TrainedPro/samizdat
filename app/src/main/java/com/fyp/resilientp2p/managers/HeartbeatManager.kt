package com.fyp.resilientp2p.managers

import com.google.android.gms.nearby.connection.Payload
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HeartbeatManager(private val p2pManager: P2PManager) {

    companion object {
        private const val TAG = "HeartbeatManager"
        private const val DEFAULT_INTERVAL_MS = 1000L
        private const val DEFAULT_PAYLOAD_SIZE = 64
    }

    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Listen for incoming payloads (PING/PONG)
        scope.launch {
            p2pManager.payloadEvents.collect { event ->
                handlePayload(event.endpointId, event.payload)
            }
        }
    }

    data class HeartbeatConfig(
            val intervalMs: Long = DEFAULT_INTERVAL_MS,
            val payloadSizeBytes: Int = DEFAULT_PAYLOAD_SIZE,
            val isEnabled: Boolean = false
    )

    private val _config = MutableStateFlow(HeartbeatConfig())
    val config = _config.asStateFlow()

    fun updateConfig(intervalMs: Long? = null, payloadSize: Int? = null, enabled: Boolean? = null) {
        _config.update { current ->
            val newEnabled = enabled ?: current.isEnabled
            val newConfig =
                    current.copy(
                            intervalMs = intervalMs ?: current.intervalMs,
                            payloadSizeBytes = payloadSize ?: current.payloadSizeBytes,
                            isEnabled = newEnabled
                    )

            if (newEnabled && !current.isEnabled) {
                startHeartbeat(newConfig)
            } else if (!newEnabled && current.isEnabled) {
                stopHeartbeat()
            } else if (newEnabled) {
                // Restart if config changed while enabled
                stopHeartbeat()
                startHeartbeat(newConfig)
            }
            newConfig
        }
    }

    private fun startHeartbeat(config: HeartbeatConfig) {
        log("Starting heartbeat: ${config.intervalMs}ms, ${config.payloadSizeBytes}B")
        heartbeatJob =
                scope.launch {
                    while (true) {
                        sendPing(config.payloadSizeBytes)
                        delay(config.intervalMs)
                    }
                }
    }

    private fun stopHeartbeat() {
        log("Stopping heartbeat")
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendPing(size: Int) {
        val peers = p2pManager.state.value.connectedEndpoints
        if (peers.isEmpty()) return

        val timestamp = System.currentTimeMillis()
        // Protocol: "PING:<timestamp>:<padding>"
        val header = "PING:$timestamp:".toByteArray(StandardCharsets.UTF_8)
        val payloadData = ByteArray(size.coerceAtLeast(header.size))
        System.arraycopy(header, 0, payloadData, 0, header.size)

        peers.forEach { peerId -> p2pManager.sendPayload(peerId, payloadData) }
        log("Sent PING to ${peers.size} peers")
    }

    private fun handlePayload(endpointId: String, payload: Payload) {
        if (payload.type != Payload.Type.BYTES) return
        val bytes = payload.asBytes() ?: return
        val content = String(bytes, StandardCharsets.UTF_8)

        if (content.startsWith("PING:")) {
            // Format: PING:<timestamp>:<padding>
            val parts = content.split(":")
            if (parts.size >= 2) {
                val originTimestamp = parts[1]
                // Send PONG with same timestamp
                sendPong(endpointId, originTimestamp, bytes.size)
            }
        } else if (content.startsWith("PONG:")) {
            // Format: PONG:<timestamp>:<padding>
            val parts = content.split(":")
            if (parts.size >= 2) {
                val originTimestamp = parts[1].toLongOrNull()
                if (originTimestamp != null) {
                    val rtt = System.currentTimeMillis() - originTimestamp
                    log("RTT to $endpointId: ${rtt}ms", "METRIC")
                }
            }
        }
    }

    private fun sendPong(endpointId: String, timestamp: String, size: Int) {
        // Protocol: "PONG:<timestamp>:<padding>"
        val header = "PONG:$timestamp:".toByteArray(StandardCharsets.UTF_8)
        val payloadData = ByteArray(size.coerceAtLeast(header.size))
        System.arraycopy(header, 0, payloadData, 0, header.size)

        p2pManager.sendPayload(endpointId, payloadData)
        log("Sent PONG to $endpointId")
    }

    private fun log(msg: String, type: String = "INFO") {
        p2pManager.log(msg, type)
    }
}
