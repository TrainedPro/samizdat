package com.fyp.resilientp2p.testing

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.data.NetworkStatsSnapshot
import com.fyp.resilientp2p.managers.P2PManager
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Duration presets for the endurance test. [INDEFINITE] runs until manually stopped.
 */
enum class EnduranceDuration(val label: String, val ms: Long) {
    THIRTY_MIN("30 min", 30 * 60_000L),
    ONE_HOUR("1 hour", 60 * 60_000L),
    TWO_HOURS("2 hours", 120 * 60_000L),
    INDEFINITE("Indefinite", Long.MAX_VALUE);
}

/**
 * Immutable snapshot captured at a point in time during the endurance test.
 * Covers battery, traffic, latency, reliability, and mesh topology.
 */
data class BatterySnapshot(
    val timestampMs: Long,
    val elapsedMs: Long,
    // ── Battery ──
    val batteryLevel: Int,
    val batteryTemperature: Float,
    val batteryChargeUah: Long,
    val batteryVoltageMilliV: Int,
    val estimatedRemainingMah: Double,
    // ── Traffic ──
    val totalPacketsSent: Long,
    val totalPacketsReceived: Long,
    val totalPacketsForwarded: Long,
    val totalPacketsDropped: Long,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    // ── Latency ──
    val avgRttMs: Long,
    val minRttMs: Long,
    val maxRttMs: Long,
    /** Jitter = standard deviation of per-peer RTTs at this instant (ms). */
    val jitterMs: Double,
    // ── Reliability ──
    /** Packet loss ratio (dropped / sent), 0.0–1.0. */
    val packetLossRate: Double,
    /** Throughput in bytes/sec since test start. */
    val throughputBytesPerSec: Double,
    // ── Topology ──
    val neighborCount: Int,
    val routeCount: Int,
    val totalConnectionsEstablished: Long,
    val totalConnectionsLost: Long,
    // ── Store-and-Forward ──
    val storeForwardQueued: Long,
    val storeForwardDelivered: Long,
    // ── Per-peer RTT ──
    val perPeerRttMs: Map<String, Long>
)

/**
 * UI-observable state for the endurance test.
 */
data class EnduranceTestState(
    val isRunning: Boolean = false,
    val duration: EnduranceDuration = EnduranceDuration.TWO_HOURS,
    val elapsedMs: Long = 0,
    val durationMs: Long = 0,
    val startBatteryLevel: Int = -1,
    val currentBatteryLevel: Int = -1,
    val startMah: Double = -1.0,
    val currentMah: Double = -1.0,
    val mahDrained: Double = 0.0,
    val messagesSent: Long = 0,
    val messagesReceived: Long = 0,
    val snapshots: List<BatterySnapshot> = emptyList(),
    val statusMessage: String = "Idle",
    val logMessages: List<String> = emptyList(),
    val finalReport: EnduranceReport? = null
) {
    val progress: Float
        get() = if (durationMs <= 0 || durationMs == Long.MAX_VALUE) 0f
                else (elapsedMs.toFloat() / durationMs).coerceIn(0f, 1f)
}

/**
 * Final report generated when the endurance test ends.
 */
data class EnduranceReport(
    val deviceName: String,
    val duration: String,
    val actualDurationMs: Long,
    // ── Battery ──
    val startBatteryLevel: Int,
    val endBatteryLevel: Int,
    val batteryDrainPercent: Int,
    val startMah: Double,
    val endMah: Double,
    val mahDrained: Double,
    val mahPerHour: Double,
    val mAhSource: String,
    // ── Traffic ──
    val totalPacketsSent: Long,
    val totalPacketsReceived: Long,
    val totalPacketsForwarded: Long,
    val totalPacketsDropped: Long,
    val totalBytesSent: Long,
    val totalBytesReceived: Long,
    // ── Latency ──
    val avgRttMs: Long,
    val minRttMs: Long,
    val maxRttMs: Long,
    /** Mean jitter (avg of per-snapshot jitter values) in ms. */
    val meanJitterMs: Double,
    // ── Reliability ──
    val overallPacketLossRate: Double,
    val avgThroughputBytesPerSec: Double,
    // ── Topology ──
    val peakNeighborCount: Int,
    val totalConnectionsEstablished: Long,
    val totalConnectionsLost: Long,
    // ── Store-and-Forward ──
    val storeForwardQueued: Long,
    val storeForwardDelivered: Long,
    // ── Timeline ──
    val snapshots: List<BatterySnapshot>
)

/**
 * Long-running endurance/soak test for the P2P mesh.
 *
 * Generates steady mesh traffic (one broadcast every [TRAFFIC_INTERVAL_MS]),
 * records battery snapshots every [SNAPSHOT_INTERVAL_MS], and produces a final
 * report with mAh drain, traffic stats, and per-minute battery history.
 *
 * Supports configurable duration including an indefinite mode that runs until
 * manually stopped via [stop].
 */
class EnduranceTestRunner(
    private val context: Context,
    private val p2pManager: P2PManager
) {
    companion object {
        private const val TAG = "EnduranceTest"
        /** Interval between broadcast messages for generating steady traffic. */
        private const val TRAFFIC_INTERVAL_MS = 30_000L       // 30 seconds
        /** Interval between battery/stats snapshots. */
        private const val SNAPSHOT_INTERVAL_MS = 60_000L       // 1 minute
        /** Prefix for endurance test messages to distinguish from user traffic. */
        private const val ENDURANCE_MSG_PREFIX = "__ENDURANCE__"
        /** UI tick interval for elapsed time updates. */
        private const val UI_TICK_MS = 1_000L                  // 1 second
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var testJob: Job? = null

    private val _state = MutableStateFlow(EnduranceTestState())
    val state: StateFlow<EnduranceTestState> = _state.asStateFlow()

    private val logLines = CopyOnWriteArrayList<String>()
    private val snapshotList = CopyOnWriteArrayList<BatterySnapshot>()
    private var peakNeighborCount = 0

    /**
     * Callback invoked with each periodic snapshot's JSON for cloud upload.
     * Set by the application layer (e.g. P2PApplication) to wire into TelemetryManager.
     */
    var onEnduranceSnapshotReady: ((String) -> Unit)? = null

    /**
     * Callback invoked with the final endurance report JSON for cloud upload.
     * Set by the application layer (e.g. P2PApplication) to wire into TelemetryManager.
     */
    var onEnduranceReportReady: ((String) -> Unit)? = null

    // ─── Public API ─────────────────────────────────────────────────────

    /**
     * Start the endurance test with the given [duration].
     * Mesh must already be running (call `p2pManager.start()` first).
     */
    fun start(duration: EnduranceDuration) {
        if (_state.value.isRunning) {
            Log.w(TAG, "Endurance test already running")
            return
        }

        testJob = scope.launch {
            runEnduranceTest(duration)
        }
    }

    /** Stop the endurance test early. Produces a partial report. */
    fun stop() {
        if (!_state.value.isRunning) return
        testJob?.cancel()
        // The finally block in runEnduranceTest handles report generation
    }

    // ─── Core loop ──────────────────────────────────────────────────────

    private suspend fun runEnduranceTest(duration: EnduranceDuration) {
        logLines.clear()
        snapshotList.clear()
        peakNeighborCount = 0

        val startTime = System.currentTimeMillis()
        val startSnap = currentBatterySnapshot(startTime, 0L)

        _state.update {
            EnduranceTestState(
                isRunning = true,
                duration = duration,
                durationMs = if (duration == EnduranceDuration.INDEFINITE) Long.MAX_VALUE else duration.ms,
                startBatteryLevel = startSnap.batteryLevel,
                currentBatteryLevel = startSnap.batteryLevel,
                startMah = startSnap.estimatedRemainingMah,
                currentMah = startSnap.estimatedRemainingMah,
                statusMessage = "Starting endurance test (${duration.label})...",
                logMessages = emptyList(),
                finalReport = null
            )
        }

        tlog("═══════════════════════════════════════════════════════")
        tlog("  ENDURANCE TEST — ${duration.label}")
        tlog("  Start Battery: ${startSnap.batteryLevel}%  " +
             "${formatMah(startSnap.estimatedRemainingMah)}  " +
             "${startSnap.batteryTemperature}°C  ${startSnap.batteryVoltageMilliV}mV")
        tlog("  Charge counter available: ${startSnap.batteryChargeUah > 0}")
        tlog("  Design capacity: ${p2pManager.networkStats.batteryDesignCapacityMah} mAh")
        tlog("═══════════════════════════════════════════════════════")

        snapshotList.add(startSnap)
        var msgCounter = 0L

        try {
            // Launch parallel child coroutines
            coroutineScope {
                // 1. UI ticker — updates elapsed time every second
                launch {
                    while (isActive) {
                        delay(UI_TICK_MS)
                        val elapsed = System.currentTimeMillis() - startTime
                        val snap = currentBatterySnapshot(System.currentTimeMillis(), elapsed)
                        val drain = computeDrain(_state.value.startMah, snap.estimatedRemainingMah)
                        _state.update {
                            it.copy(
                                elapsedMs = elapsed,
                                currentBatteryLevel = snap.batteryLevel,
                                currentMah = snap.estimatedRemainingMah,
                                mahDrained = drain,
                                messagesSent = msgCounter,
                                messagesReceived = snap.totalPacketsReceived
                            )
                        }

                        // Check duration limit
                        if (duration != EnduranceDuration.INDEFINITE && elapsed >= duration.ms) {
                            tlog("Duration reached (${duration.label}). Stopping...")
                            cancel()
                        }
                    }
                }

                // 2. Traffic generator — sends a small broadcast every 30s
                launch {
                    while (isActive) {
                        delay(TRAFFIC_INTERVAL_MS)
                        try {
                            val seq = ++msgCounter
                            val msg = "${ENDURANCE_MSG_PREFIX}seq=$seq t=${System.currentTimeMillis()}"
                            p2pManager.broadcastMessage(msg)
                            if (seq % 10 == 0L) {
                                tlog("Traffic: sent $seq messages so far")
                            }
                        } catch (e: Exception) {
                            tlog("Traffic send error: ${e.message}")
                        }
                    }
                }

                // 3. Battery snapshot collector — every 60s
                launch {
                    while (isActive) {
                        delay(SNAPSHOT_INTERVAL_MS)
                        val elapsed = System.currentTimeMillis() - startTime
                        val snap = currentBatterySnapshot(System.currentTimeMillis(), elapsed)
                        snapshotList.add(snap)
                        peakNeighborCount = maxOf(peakNeighborCount, snap.neighborCount)

                        val drain = computeDrain(_state.value.startMah, snap.estimatedRemainingMah)
                        _state.update { it.copy(snapshots = snapshotList.toList()) }

                        tlog(
                            "SNAPSHOT [${formatDuration(elapsed)}] " +
                            "bat=${snap.batteryLevel}% ${formatMah(snap.estimatedRemainingMah)} " +
                            "${snap.batteryTemperature}°C ${snap.batteryVoltageMilliV}mV " +
                            "drain=${formatMah(drain)} " +
                            "pkt↑${snap.totalPacketsSent}↓${snap.totalPacketsReceived}↻${snap.totalPacketsForwarded}✗${snap.totalPacketsDropped} " +
                            "rtt=${snap.avgRttMs}ms(min=${snap.minRttMs},max=${snap.maxRttMs}) jitter=${"%.1f".format(snap.jitterMs)}ms " +
                            "loss=${"%.2f".format(snap.packetLossRate * 100)}% " +
                            "throughput=${"%.1f".format(snap.throughputBytesPerSec / 1024)}KB/s " +
                            "neighbors=${snap.neighborCount} routes=${snap.routeCount} " +
                            "conn↑${snap.totalConnectionsEstablished}↓${snap.totalConnectionsLost} " +
                            "sf=Q${snap.storeForwardQueued}D${snap.storeForwardDelivered}"
                        )

                        // Upload snapshot to cloud telemetry
                        try {
                            onEnduranceSnapshotReady?.invoke(snapshotToJson(snap).toString())
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to queue snapshot for cloud upload", e)
                        }
                    }
                }
            }
        } catch (_: CancellationException) {
            // Normal stop — either duration expired or user pressed stop
        } catch (e: Exception) {
            tlog("ENDURANCE_ERROR: ${e.message}")
            Log.e(TAG, "Endurance test error", e)
        } finally {
            // Always generate report
            val endTime = System.currentTimeMillis()
            val actualDuration = endTime - startTime
            val endSnap = currentBatterySnapshot(endTime, actualDuration)
            snapshotList.add(endSnap)

            val drain = computeDrain(startSnap.estimatedRemainingMah, endSnap.estimatedRemainingMah)
            val mahPerHour = if (actualDuration > 60_000) {
                drain / (actualDuration / 3_600_000.0)
            } else drain

            val mAhSource = when {
                startSnap.batteryChargeUah > 0 && endSnap.batteryChargeUah > 0 -> "CHARGE_COUNTER (direct)"
                p2pManager.networkStats.batteryDesignCapacityMah > 0 -> "INTERPOLATED (${p2pManager.networkStats.batteryDesignCapacityMah}mAh design × %)"
                else -> "UNAVAILABLE"
            }

            // Compute aggregate latency & reliability from all snapshots
            val allRtts = snapshotList.filter { it.avgRttMs > 0 }
            val globalMinRtt = allRtts.minOfOrNull { it.minRttMs } ?: 0L
            val globalMaxRtt = allRtts.maxOfOrNull { it.maxRttMs } ?: 0L
            val meanJitter = if (allRtts.isNotEmpty()) allRtts.map { it.jitterMs }.average() else 0.0
            val avgThroughput = if (actualDuration > 0)
                (endSnap.totalBytesSent + endSnap.totalBytesReceived).toDouble() / (actualDuration / 1000.0) else 0.0

            val report = EnduranceReport(
                deviceName = p2pManager.getLocalDeviceName(),
                duration = duration.label,
                actualDurationMs = actualDuration,
                startBatteryLevel = startSnap.batteryLevel,
                endBatteryLevel = endSnap.batteryLevel,
                batteryDrainPercent = startSnap.batteryLevel - endSnap.batteryLevel,
                startMah = startSnap.estimatedRemainingMah,
                endMah = endSnap.estimatedRemainingMah,
                mahDrained = drain,
                mahPerHour = mahPerHour,
                mAhSource = mAhSource,
                totalPacketsSent = endSnap.totalPacketsSent,
                totalPacketsReceived = endSnap.totalPacketsReceived,
                totalPacketsForwarded = endSnap.totalPacketsForwarded,
                totalPacketsDropped = endSnap.totalPacketsDropped,
                totalBytesSent = endSnap.totalBytesSent,
                totalBytesReceived = endSnap.totalBytesReceived,
                avgRttMs = endSnap.avgRttMs,
                minRttMs = globalMinRtt,
                maxRttMs = globalMaxRtt,
                meanJitterMs = meanJitter,
                overallPacketLossRate = endSnap.packetLossRate,
                avgThroughputBytesPerSec = avgThroughput,
                peakNeighborCount = peakNeighborCount,
                totalConnectionsEstablished = endSnap.totalConnectionsEstablished,
                totalConnectionsLost = endSnap.totalConnectionsLost,
                storeForwardQueued = endSnap.storeForwardQueued,
                storeForwardDelivered = endSnap.storeForwardDelivered,
                snapshots = snapshotList.toList()
            )

            tlog("═══════════════════════════════════════════════════════")
            tlog("  ENDURANCE TEST COMPLETE — ${formatDuration(actualDuration)}")
            tlog("  Battery: ${startSnap.batteryLevel}% → ${endSnap.batteryLevel}% (Δ${report.batteryDrainPercent}%)")
            tlog("  mAh: ${formatMah(startSnap.estimatedRemainingMah)} → ${formatMah(endSnap.estimatedRemainingMah)} (Δ${formatMah(drain)})")
            tlog("  Drain rate: ${formatMah(mahPerHour)}/hour")
            tlog("  Source: $mAhSource")
            tlog("  RTT: avg=${endSnap.avgRttMs}ms min=${globalMinRtt}ms max=${globalMaxRtt}ms jitter=${"%.1f".format(meanJitter)}ms")
            tlog("  Packet loss: ${"%.2f".format(endSnap.packetLossRate * 100)}%")
            tlog("  Throughput: ${"%.1f".format(avgThroughput / 1024)}KB/s")
            tlog("  Packets: ↑${report.totalPacketsSent} ↓${report.totalPacketsReceived} ↻${report.totalPacketsForwarded} ✗${report.totalPacketsDropped}")
            tlog("  Connections: ↑${report.totalConnectionsEstablished} ↓${report.totalConnectionsLost}")
            tlog("  Store-Forward: Q${report.storeForwardQueued} D${report.storeForwardDelivered}")
            tlog("  Snapshots: ${snapshotList.size}")
            tlog("═══════════════════════════════════════════════════════")

            exportReport(report)

            // Upload final report to cloud telemetry
            try {
                onEnduranceReportReady?.invoke(reportToJson(report).toString())
                tlog("Endurance report queued for cloud upload")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to queue endurance report for cloud upload", e)
                tlog("Cloud upload queue failed: ${e.message}")
            }

            _state.update {
                it.copy(
                    isRunning = false,
                    elapsedMs = actualDuration,
                    mahDrained = drain,
                    currentBatteryLevel = endSnap.batteryLevel,
                    currentMah = endSnap.estimatedRemainingMah,
                    snapshots = snapshotList.toList(),
                    statusMessage = "Complete — Δ${report.batteryDrainPercent}%, ${formatMah(drain)} drained",
                    finalReport = report,
                    logMessages = logLines.toList()
                )
            }
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private fun currentBatterySnapshot(now: Long, elapsed: Long): BatterySnapshot {
        val stats = p2pManager.state.value.stats
        val peerRtts = stats.peerStats.values.map { it.lastRttMs }.filter { it > 0 }
        val minRtt = peerRtts.minOrNull() ?: 0L
        val maxRtt = peerRtts.maxOrNull() ?: 0L
        val jitter = if (peerRtts.size >= 2) {
            val mean = peerRtts.average()
            kotlin.math.sqrt(peerRtts.map { (it - mean) * (it - mean) }.average())
        } else 0.0
        val lossRate = if (stats.totalPacketsSent > 0)
            stats.totalPacketsDropped.toDouble() / stats.totalPacketsSent else 0.0
        val throughput = if (elapsed > 0)
            (stats.totalBytesSent + stats.totalBytesReceived).toDouble() / (elapsed / 1000.0) else 0.0

        return BatterySnapshot(
            timestampMs = now,
            elapsedMs = elapsed,
            batteryLevel = stats.batteryLevel,
            batteryTemperature = stats.batteryTemperature,
            batteryChargeUah = stats.batteryChargeUah,
            batteryVoltageMilliV = stats.batteryVoltageMilliV,
            estimatedRemainingMah = stats.estimatedRemainingMah(),
            totalPacketsSent = stats.totalPacketsSent,
            totalPacketsReceived = stats.totalPacketsReceived,
            totalPacketsForwarded = stats.totalPacketsForwarded,
            totalPacketsDropped = stats.totalPacketsDropped,
            totalBytesSent = stats.totalBytesSent,
            totalBytesReceived = stats.totalBytesReceived,
            avgRttMs = stats.avgRttMs,
            minRttMs = minRtt,
            maxRttMs = maxRtt,
            jitterMs = jitter,
            packetLossRate = lossRate,
            throughputBytesPerSec = throughput,
            neighborCount = stats.currentNeighborCount,
            routeCount = stats.currentRouteCount,
            totalConnectionsEstablished = stats.totalConnectionsEstablished,
            totalConnectionsLost = stats.totalConnectionsLost,
            storeForwardQueued = stats.storeForwardQueued,
            storeForwardDelivered = stats.storeForwardDelivered,
            perPeerRttMs = stats.peerStats.mapValues { it.value.lastRttMs }
        )
    }

    private fun computeDrain(startMah: Double, currentMah: Double): Double {
        if (startMah < 0 || currentMah < 0) return 0.0
        return (startMah - currentMah).coerceAtLeast(0.0)
    }

    private fun formatMah(mah: Double): String = when {
        mah < 0 -> "N/A"
        mah < 1 -> "%.3f mAh".format(mah)
        else -> "%.1f mAh".format(mah)
    }

    private fun formatDuration(ms: Long): String {
        val secs = ms / 1000
        val mins = secs / 60
        val hrs = mins / 60
        return when {
            hrs > 0 -> "%dh%02dm%02ds".format(hrs, mins % 60, secs % 60)
            mins > 0 -> "%dm%02ds".format(mins, secs % 60)
            else -> "${secs}s"
        }
    }

    private fun tlog(msg: String) {
        val ts = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val line = "[$ts] $msg"
        logLines.add(line)
        Log.d(TAG, msg)
        p2pManager.log("[ENDURANCE] $msg", LogLevel.METRIC)

        // Keep last 500 lines in UI state
        _state.update { it.copy(logMessages = logLines.takeLast(500)) }
    }

    // ─── Export ─────────────────────────────────────────────────────────

    private fun exportReport(report: EnduranceReport) {
        try {
            val dir = File(context.getExternalFilesDir(null), "endurance_results")
            dir.mkdirs()

            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val deviceTag = report.deviceName.replace(Regex("[^a-zA-Z0-9_-]"), "_")

            // JSON
            val jsonFile = File(dir, "endurance_${deviceTag}_${dateStamp}.json")
            jsonFile.writeText(reportToJson(report).toString(2))
            tlog("Exported JSON: ${jsonFile.absolutePath}")

            // CSV
            val csvFile = File(dir, "endurance_${deviceTag}_${dateStamp}.csv")
            csvFile.writeText(reportToCsv(report))
            tlog("Exported CSV: ${csvFile.absolutePath}")

            // Log
            val logFile = File(dir, "endurance_log_${deviceTag}_${dateStamp}.txt")
            logFile.writeText(logLines.joinToString("\n"))
            tlog("Exported log: ${logFile.absolutePath}")

            p2pManager.log("[ENDURANCE] Results exported to ${dir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export endurance results", e)
            tlog("Export failed: ${e.message}")
        }
    }

    private fun snapshotToJson(snap: BatterySnapshot): JSONObject = JSONObject().apply {
        put("testType", "endurance_snapshot")
        put("deviceName", p2pManager.getLocalDeviceName())
        put("timestampMs", snap.timestampMs)
        put("elapsedMs", snap.elapsedMs)
        // Battery
        put("batteryLevel", snap.batteryLevel)
        put("batteryTemperature", snap.batteryTemperature.toDouble())
        put("batteryChargeUah", snap.batteryChargeUah)
        put("batteryVoltageMilliV", snap.batteryVoltageMilliV)
        put("estimatedRemainingMah", snap.estimatedRemainingMah)
        // Traffic
        put("totalPacketsSent", snap.totalPacketsSent)
        put("totalPacketsReceived", snap.totalPacketsReceived)
        put("totalPacketsForwarded", snap.totalPacketsForwarded)
        put("totalPacketsDropped", snap.totalPacketsDropped)
        put("totalBytesSent", snap.totalBytesSent)
        put("totalBytesReceived", snap.totalBytesReceived)
        // Latency
        put("avgRttMs", snap.avgRttMs)
        put("minRttMs", snap.minRttMs)
        put("maxRttMs", snap.maxRttMs)
        put("jitterMs", snap.jitterMs)
        // Reliability
        put("packetLossRate", snap.packetLossRate)
        put("throughputBytesPerSec", snap.throughputBytesPerSec)
        // Topology
        put("neighborCount", snap.neighborCount)
        put("routeCount", snap.routeCount)
        put("totalConnectionsEstablished", snap.totalConnectionsEstablished)
        put("totalConnectionsLost", snap.totalConnectionsLost)
        // Store-and-Forward
        put("storeForwardQueued", snap.storeForwardQueued)
        put("storeForwardDelivered", snap.storeForwardDelivered)
        // Per-peer RTT
        if (snap.perPeerRttMs.isNotEmpty()) {
            put("perPeerRttMs", JSONObject().apply {
                snap.perPeerRttMs.forEach { (peer, rtt) -> put(peer, rtt) }
            })
        }
    }

    private fun reportToJson(report: EnduranceReport): JSONObject = JSONObject().apply {
        put("testType", "endurance")
        put("deviceName", report.deviceName)
        put("configuredDuration", report.duration)
        put("actualDurationMs", report.actualDurationMs)
        put("mAhSource", report.mAhSource)

        put("battery", JSONObject().apply {
            put("startLevel", report.startBatteryLevel)
            put("endLevel", report.endBatteryLevel)
            put("drainPercent", report.batteryDrainPercent)
            put("startMah", report.startMah)
            put("endMah", report.endMah)
            put("mahDrained", report.mahDrained)
            put("mahPerHour", report.mahPerHour)
        })

        put("traffic", JSONObject().apply {
            put("totalPacketsSent", report.totalPacketsSent)
            put("totalPacketsReceived", report.totalPacketsReceived)
            put("totalPacketsForwarded", report.totalPacketsForwarded)
            put("totalPacketsDropped", report.totalPacketsDropped)
            put("totalBytesSent", report.totalBytesSent)
            put("totalBytesReceived", report.totalBytesReceived)
            put("peakNeighborCount", report.peakNeighborCount)
        })

        put("latency", JSONObject().apply {
            put("avgRttMs", report.avgRttMs)
            put("minRttMs", report.minRttMs)
            put("maxRttMs", report.maxRttMs)
            put("meanJitterMs", report.meanJitterMs)
        })

        put("reliability", JSONObject().apply {
            put("overallPacketLossRate", report.overallPacketLossRate)
            put("avgThroughputBytesPerSec", report.avgThroughputBytesPerSec)
        })

        put("connectivity", JSONObject().apply {
            put("totalConnectionsEstablished", report.totalConnectionsEstablished)
            put("totalConnectionsLost", report.totalConnectionsLost)
            put("storeForwardQueued", report.storeForwardQueued)
            put("storeForwardDelivered", report.storeForwardDelivered)
        })

        put("snapshots", JSONArray().apply {
            report.snapshots.forEach { snap ->
                put(JSONObject().apply {
                    put("timestampMs", snap.timestampMs)
                    put("elapsedMs", snap.elapsedMs)
                    put("batteryLevel", snap.batteryLevel)
                    put("batteryTemperature", snap.batteryTemperature.toDouble())
                    put("batteryChargeUah", snap.batteryChargeUah)
                    put("batteryVoltageMilliV", snap.batteryVoltageMilliV)
                    put("estimatedRemainingMah", snap.estimatedRemainingMah)
                    put("totalPacketsSent", snap.totalPacketsSent)
                    put("totalPacketsReceived", snap.totalPacketsReceived)
                    put("totalPacketsForwarded", snap.totalPacketsForwarded)
                    put("totalPacketsDropped", snap.totalPacketsDropped)
                    put("totalBytesSent", snap.totalBytesSent)
                    put("totalBytesReceived", snap.totalBytesReceived)
                    put("avgRttMs", snap.avgRttMs)
                    put("minRttMs", snap.minRttMs)
                    put("maxRttMs", snap.maxRttMs)
                    put("jitterMs", snap.jitterMs)
                    put("packetLossRate", snap.packetLossRate)
                    put("throughputBytesPerSec", snap.throughputBytesPerSec)
                    put("neighborCount", snap.neighborCount)
                    put("routeCount", snap.routeCount)
                    put("totalConnectionsEstablished", snap.totalConnectionsEstablished)
                    put("totalConnectionsLost", snap.totalConnectionsLost)
                    put("storeForwardQueued", snap.storeForwardQueued)
                    put("storeForwardDelivered", snap.storeForwardDelivered)
                    if (snap.perPeerRttMs.isNotEmpty()) {
                        put("perPeerRttMs", JSONObject().apply {
                            snap.perPeerRttMs.forEach { (peer, rtt) -> put(peer, rtt) }
                        })
                    }
                })
            }
        })
    }

    private fun reportToCsv(report: EnduranceReport): String = buildString {
        appendLine("# Samizdat Endurance Test Report")
        appendLine("# Device: ${report.deviceName}")
        appendLine("# Duration: ${report.duration} (actual: ${report.actualDurationMs}ms)")
        appendLine("# Battery: ${report.startBatteryLevel}% -> ${report.endBatteryLevel}% (drain: ${report.batteryDrainPercent}%)")
        appendLine("# mAh: ${report.startMah} -> ${report.endMah} (drain: ${report.mahDrained})")
        appendLine("# Drain rate: ${report.mahPerHour} mAh/hour")
        appendLine("# Source: ${report.mAhSource}")
        appendLine("# Packets: sent=${report.totalPacketsSent} received=${report.totalPacketsReceived} forwarded=${report.totalPacketsForwarded} dropped=${report.totalPacketsDropped}")
        appendLine("# Bytes: sent=${report.totalBytesSent} received=${report.totalBytesReceived}")
        appendLine("# RTT: avg=${report.avgRttMs}ms min=${report.minRttMs}ms max=${report.maxRttMs}ms jitter=${"%.1f".format(report.meanJitterMs)}ms")
        appendLine("# Loss: ${"%.2f".format(report.overallPacketLossRate * 100)}%  Throughput: ${"%.1f".format(report.avgThroughputBytesPerSec / 1024)}KB/s")
        appendLine("# Connections: established=${report.totalConnectionsEstablished} lost=${report.totalConnectionsLost}")
        appendLine("# Store-Forward: queued=${report.storeForwardQueued} delivered=${report.storeForwardDelivered}")
        appendLine("#")
        appendLine("ElapsedMs,BatteryLevel,BatteryTempC,ChargeUah,VoltageMv,EstMah,PacketsSent,PacketsRecv,PacketsFwd,PacketsDrop,BytesSent,BytesRecv,AvgRttMs,MinRttMs,MaxRttMs,JitterMs,LossRate,ThroughputBps,Neighbors,Routes,ConnEstab,ConnLost,SfQueued,SfDelivered")
        report.snapshots.forEach { s ->
            appendLine("${s.elapsedMs},${s.batteryLevel},${s.batteryTemperature}," +
                       "${s.batteryChargeUah},${s.batteryVoltageMilliV},${s.estimatedRemainingMah}," +
                       "${s.totalPacketsSent},${s.totalPacketsReceived}," +
                       "${s.totalPacketsForwarded},${s.totalPacketsDropped}," +
                       "${s.totalBytesSent},${s.totalBytesReceived}," +
                       "${s.avgRttMs},${s.minRttMs},${s.maxRttMs}," +
                       "${"%.2f".format(s.jitterMs)},${"%.4f".format(s.packetLossRate)}," +
                       "${"%.1f".format(s.throughputBytesPerSec)}," +
                       "${s.neighborCount},${s.routeCount}," +
                       "${s.totalConnectionsEstablished},${s.totalConnectionsLost}," +
                       "${s.storeForwardQueued},${s.storeForwardDelivered}")
        }
    }
}
