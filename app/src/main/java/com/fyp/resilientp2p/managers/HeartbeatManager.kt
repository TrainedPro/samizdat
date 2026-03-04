package com.fyp.resilientp2p.managers

import com.fyp.resilientp2p.data.LogLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Manages periodic heartbeat (PING/PONG) exchanges with all direct mesh neighbors.
 *
 * Heartbeats serve three purposes:
 * 1. **Liveness detection** — peers that stop responding are disconnected ("zombie cleanup").
 * 2. **RTT measurement** — round-trip times are fed into [NetworkStats] for the dashboard.
 * 3. **Adaptive interval** — [BandwidthInfo] events adjust heartbeat frequency
 *    (2 s on HIGH bandwidth, 10 s on LOW, default 5 s).
 *
 * Zombie threshold: `max(interval * 6, 45 000 ms)`. The 45 s floor prevents false
 * positives caused by normal Bluetooth stalls under contested bandwidth.
 *
 * @param p2pManager The core mesh engine used to send PINGs and read neighbor state.
 * @see HeartbeatConfig
 */
class HeartbeatManager(private val p2pManager: P2PManager) {

    companion object {
        /** Default heartbeat interval in milliseconds. */
        private const val DEFAULT_INTERVAL_MS = 5000L
        /** Default PING payload size in bytes (includes 8-byte timestamp). */
        private const val DEFAULT_PAYLOAD_SIZE = 64
    }

    private var heartbeatJob: Job? = null
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + supervisorJob)

    /**
     * Configuration for the heartbeat subsystem.
     *
     * @property intervalMs Milliseconds between PING rounds.
     * @property payloadSizeBytes PING payload size (min 8 bytes for embedded timestamp).
     * @property isEnabled Master switch — heartbeat auto-starts when enabled.
     */
    data class HeartbeatConfig(
            val intervalMs: Long = DEFAULT_INTERVAL_MS,
            val payloadSizeBytes: Int = DEFAULT_PAYLOAD_SIZE,
            val isEnabled: Boolean = true // ENABLED BY DEFAULT
    )

    private val _config = MutableStateFlow(HeartbeatConfig())
    val config = _config.asStateFlow()

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
                                    10000L // Slow down to save bandwidth
                            com.google.android.gms.nearby.connection.BandwidthInfo.Quality.HIGH ->
                                    2000L // Speed up for responsiveness
                            else -> currentInterval
                        }

                if (newInterval != currentInterval) {
                    log(
                            "Adapting heartbeat interval to ${newInterval}ms due to bandwidth change.",
                            LogLevel.DEBUG
                    )
                    updateConfig(intervalMs = newInterval)
                }
            }
        }

        // Zombie Cleanup Task
        scope.launch {
            while (isActive) {
                delay(10000) // Check every 10 seconds
                cleanupZombies()
            }
        }

        // Auto-start heartbeat if enabled by default
        if (_config.value.isEnabled) {
            startHeartbeat(_config.value)
        }
    }

    // Config and state moved to top to support init access

    @Synchronized
    fun updateConfig(intervalMs: Long? = null, payloadSize: Int? = null, enabled: Boolean? = null) {
        val current = _config.value
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
        _config.value = newConfig
    }

    fun setHeartbeatEnabled(enabled: Boolean) {
        updateConfig(enabled = enabled)
    }

    fun setHeartbeatInterval(intervalMs: Long) {
        updateConfig(intervalMs = intervalMs)
    }

    private fun startHeartbeat(config: HeartbeatConfig) {
        log(
                "Starting heartbeat: ${config.intervalMs}ms, ${config.payloadSizeBytes}B",
                LogLevel.DEBUG
        )
        heartbeatJob =
                scope.launch {
                    while (isActive) {
                        try {
                            sendPing(config.payloadSizeBytes)
                        } catch (e: Exception) {
                            log("Error in Heartbeat Loop: ${e.message}", LogLevel.ERROR)
                        }
                        delay(config.intervalMs)
                    }
                }
    }

    fun destroy() {
        log("Destroying HeartbeatManager", LogLevel.DEBUG)
        heartbeatJob?.cancel()
        heartbeatJob = null
        supervisorJob.cancel()
    }

    private fun stopHeartbeat() {
        log("Stopping heartbeat", LogLevel.DEBUG)
        heartbeatJob?.cancel()
        heartbeatJob = null
    }

    private fun sendPing(size: Int) {
        // Iterate physical neighbors (EndpointIDs), NOT names (connectedEndpoints)
        // This ensures we ping everyone, even if they are unnamed "Unknown"
        val neighbors = p2pManager.getNeighborsSnapshot()

        if (neighbors.isEmpty()) {
            return
        }

        val timestamp = System.currentTimeMillis()
        val buffer = java.nio.ByteBuffer.allocate(size.coerceAtLeast(8))
        buffer.putLong(timestamp)
        // Remaining bytes are 0-padding by default

        // Log sparingly (DEBUG level, implicitly handled by log wrapper usually being INFO/DEBUG)
        // Reverting "TRACE" to normal debug log or removing if too noisy.
        // User asked to keep it "working", but "TRACE" is usually for dev.
        // I will keep a single line log but less verbose than the trace dump.
        // log("Sending Heartbeat to ${neighbors.size} peers", "DEBUG")

        neighbors.keys.forEach { endpointId ->
            p2pManager.sendDirectPing(endpointId, buffer.array())
        }
    }

    private fun cleanupZombies() {
        val now = System.currentTimeMillis()
        val neighbors = p2pManager.getNeighborsSnapshot()
        val currentConfig = _config.value
        // Tolerated silence = 6 * interval, with a MINIMUM of 45 seconds.
        // Previous threshold (3x = 15s) was far too aggressive — Bluetooth can stall
        // for 10-20s under normal conditions when bandwidth is contested.
        val threshold = maxOf(currentConfig.intervalMs * 6, 45_000L)

        log(
                "ZOMBIE_CHECK neighbors=${neighbors.size} threshold=${threshold}ms interval=${currentConfig.intervalMs}ms",
                LogLevel.TRACE
        )

        neighbors.forEach { (id, neighbor) ->
            val diff = now - neighbor.lastSeen.get()

            // Exempt endpoints with active transfers (audio streaming, file transfer)
            if (p2pManager.activeTransferEndpoints.containsKey(id)) {
                log("Peer $id exempt from zombie check (active transfer).", LogLevel.TRACE)
                return@forEach
            }

            if (diff > threshold) {
                // Double-check with live data to avoid false positives
                val liveNeighbor = p2pManager.getNeighborsSnapshot()[id]
                if (liveNeighbor != null && now - liveNeighbor.lastSeen.get() > threshold) {
                    log(
                            "ZOMBIE_DETECTED endpoint=$id peerName='${neighbor.peerName}' " +
                            "lastSeen=${diff}ms threshold=${threshold}ms. Disconnecting...",
                            LogLevel.WARN
                    )
                    p2pManager.disconnectFromEndpoint(id)
                }
            } else {
                log(
                        "Peer $id alive. Last seen ${diff}ms ago.",
                        LogLevel.TRACE
                )
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
                    val peerName = p2pManager.getNeighborsSnapshot()[endpointId]?.peerName ?: endpointId
                    // Feed RTT to NetworkStats for metrics tracking
                    p2pManager.networkStats.recordRtt(peerName, rtt)
                    log("RTT endpoint=$endpointId peerName='$peerName' rtt=${rtt}ms", LogLevel.TRACE)
                }
            } catch (e: Exception) {
                log("PONG_PARSE_ERROR endpoint=$endpointId error='${e.message}'", LogLevel.ERROR)
            }
        }
    }

    private fun log(msg: String, level: LogLevel = LogLevel.INFO) {
        p2pManager.log(msg, level)
    }
}
