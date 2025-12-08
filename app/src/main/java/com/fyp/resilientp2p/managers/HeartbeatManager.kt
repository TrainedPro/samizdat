package com.fyp.resilientp2p.managers

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
        private const val DEFAULT_INTERVAL_MS = 5000L
        private const val DEFAULT_PAYLOAD_SIZE = 64
    }

    private var heartbeatJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        // Listen for incoming payloads (PING/PONG)
        scope.launch {
            p2pManager.payloadEvents.collect { event ->
                handlePayload(event.endpointId, event.packet)
            }
        }

        // Listen for bandwidth changes for Adaptive Heartbeat
        scope.launch {
            p2pManager.bandwidthEvents.collect { info ->
                val currentInterval = _config.value.intervalMs
                val newInterval =
                        when (info.quality) {
                            com.google.android.gms.nearby.connection.BandwidthInfo.Quality.LOW ->
                                    5000L // Slow down
                            com.google.android.gms.nearby.connection.BandwidthInfo.Quality.HIGH ->
                                    1000L // Speed up
                            else -> currentInterval
                        }

                if (newInterval != currentInterval) {
                    log("Adapting heartbeat interval to ${newInterval}ms due to bandwidth change.")
                    updateConfig(intervalMs = newInterval)
                }
            }
        }

        // Zombie Cleanup Task
        scope.launch {
            while (true) {
                delay(10000) // Check every 10 seconds
                cleanupZombies()
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

    fun setHeartbeatEnabled(enabled: Boolean) {
        updateConfig(enabled = enabled)
    }

    fun setHeartbeatInterval(intervalMs: Long) {
        updateConfig(intervalMs = intervalMs)
    }

    private fun startHeartbeat(config: HeartbeatConfig) {
        log("Starting heartbeat: ${config.intervalMs}ms, ${config.payloadSizeBytes}B")
        heartbeatJob =
                scope.launch {
                    while (true) {
                        // p2pManager.sendGossip() // New Mesh Heartbeat - Not implemented yet
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
        val buffer = java.nio.ByteBuffer.allocate(size.coerceAtLeast(8))
        buffer.putLong(timestamp)
        // Remaining bytes are 0-padding by default

        peers.forEach { peerId -> p2pManager.sendPing(peerId, buffer.array()) }
        log("Sent PING to ${peers.size} peers")
    }

    private fun cleanupZombies() {
        val now = System.currentTimeMillis()
        val neighbors = p2pManager.getNeighborsSnapshot()

        neighbors.forEach { (id, neighbor) ->
            // If we haven't seen them in 30 seconds (3x heartbeat), consider them dead
            if (now - neighbor.lastSeen > 30000) {
                log(
                        "Peer $id is a zombie (Last seen ${now - neighbor.lastSeen}ms ago). Disconnecting.",
                        "WARN"
                )
                p2pManager.disconnectFromEndpoint(id)
            }
        }
    }

    private fun handlePayload(endpointId: String, packet: com.fyp.resilientp2p.transport.Packet) {
        if (packet.type == com.fyp.resilientp2p.transport.PacketType.PONG) {
            try {
                val buffer = java.nio.ByteBuffer.wrap(packet.payload)
                if (buffer.remaining() >= 8) {
                    val originTimestamp = buffer.long
                    val rtt = System.currentTimeMillis() - originTimestamp
                    log("RTT to $endpointId: ${rtt}ms", "METRIC")
                }
            } catch (e: Exception) {
                log("Error parsing PONG payload: ${e.message}", "ERROR")
            }
        }
    }

    private fun log(msg: String, type: String = "INFO") {
        p2pManager.log("$type: $msg")
    }
}
