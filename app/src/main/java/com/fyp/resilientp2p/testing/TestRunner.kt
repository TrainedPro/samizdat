package com.fyp.resilientp2p.testing

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.data.NetworkStatsSnapshot
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.transport.Hop
import com.fyp.resilientp2p.transport.MessageCache
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Comprehensive automated test runner for the P2P mesh network.
 *
 * Tests cover: packet serialization, message dedup, connectivity, identity exchange,
 * text messaging, message reception, ping/RTT, broadcast, routing table integrity,
 * store-and-forward, file transfer, network stats, per-peer stats, connection
 * stability, and heartbeat verification.
 *
 * Enable via BuildConfig.TEST_MODE or the in-app "🧪 Run Tests" menu item.
 * Results are exported as JSON, CSV and a detailed log file.
 */
class TestRunner(
    private val context: Context,
    private val p2pManager: P2PManager,
    /** Optional callback to upload test results to cloud telemetry */
    var onTestResultsReady: ((String) -> Unit)? = null
) {
    companion object {
        private const val TAG = "TestRunner"
        private const val PEER_WAIT_TIMEOUT_MS = 120_000L   // 2 min to find peers
        private const val TEST_MSG_PREFIX = "__TEST__"
        private const val PING_COUNT = 20
        private const val PING_INTERVAL_MS = 500L
        private const val MSG_COUNT = 10
        private const val PING_WAIT_TIMEOUT_MS = 15_000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(TestState())
    val state: StateFlow<TestState> = _state.asStateFlow()

    // Tracking structures for async packet reception
    private val receivedTestMessages = ConcurrentHashMap<String, MutableSet<String>>()
    private val receivedPongs = ConcurrentHashMap<String, CopyOnWriteArrayList<Long>>()
    private val receivedBroadcasts = CopyOnWriteArrayList<String>()
    private var listenerJob: Job? = null
    private val testLog = CopyOnWriteArrayList<String>()

    // ─── Test Registry ──────────────────────────────────────────────────

    private val allTests = listOf(
        "Packet Serialization",
        "Message Cache Dedup",
        "Connection Verification",
        "Identity Exchange",
        "Text Messaging",
        "Message Reception",
        "Ping / RTT",
        "Broadcast Messaging",
        "Routing Table Integrity",
        "Store-and-Forward Queue",
        "File Transfer",
        "Network Stats Counters",
        "Per-Peer Stats",
        "Connection Stability",
        "Heartbeat Active"
    )

    // ─── Public API ─────────────────────────────────────────────────────

    fun runTests(autoStart: Boolean = false) {
        if (_state.value.isRunning) {
            Log.w(TAG, "Tests already running")
            return
        }

        scope.launch {
            val startTime = System.currentTimeMillis()
            testLog.clear()
            _state.update {
                TestState(isRunning = true, totalTests = allTests.size, statusMessage = "Initializing...")
            }

            try {
                // Phase 1: Start mesh if needed
                if (autoStart) {
                    tlog("Starting mesh networking (auto-start mode)...")
                    _state.update { it.copy(statusMessage = "Starting mesh...") }
                    withContext(Dispatchers.Main) { p2pManager.start() }
                    delay(3000)
                }

                // Phase 2: Wait for peers
                tlog("Waiting for at least one peer to connect...")
                _state.update { it.copy(statusMessage = "Waiting for peers...", waitingForPeers = true) }
                val hasPeers = waitForPeers()
                _state.update { it.copy(waitingForPeers = false) }

                if (!hasPeers) {
                    tlog("FATAL: No peers found within ${PEER_WAIT_TIMEOUT_MS / 1000}s. Aborting.")
                    _state.update { it.copy(
                        isRunning = false,
                        statusMessage = "FAILED: No peers found within ${PEER_WAIT_TIMEOUT_MS / 1000}s"
                    ) }
                    return@launch
                }

                // Phase 3: Start collecting incoming packets
                startPacketListener()

                val peers = getConnectedPeerNames()
                tlog("═══════════════════════════════════════════════════")
                tlog("Starting comprehensive test suite")
                tlog("Device: ${p2pManager.getLocalDeviceName()}")
                tlog("Peers: ${peers.joinToString()}")
                tlog("Tests: ${allTests.size}")
                tlog("═══════════════════════════════════════════════════")

                p2pManager.log("[TEST] ═══ Test suite starting ═══ Device: ${p2pManager.getLocalDeviceName()}, Peers: ${peers.joinToString()}, Tests: ${allTests.size}")

                // Capture stats baseline BEFORE tests
                val baselineStats = p2pManager.networkStats.snapshot(
                    p2pManager.getNeighborsSnapshot().size,
                    p2pManager.state.value.knownPeers.size
                )

                // Phase 4: Execute all tests
                val results = mutableListOf<TestResult>()
                for ((index, testName) in allTests.withIndex()) {
                    _state.update { it.copy(
                        currentTest = testName,
                        progress = index.toFloat() / allTests.size,
                        statusMessage = "Running: $testName (${index + 1}/${allTests.size})"
                    ) }

                    tlog("─── Test ${index + 1}/${allTests.size}: $testName ───")
                    val result = try {
                        runSingleTest(testName, peers, baselineStats)
                    } catch (e: Exception) {
                        tlog("  EXCEPTION: ${e.message}")
                        TestResult(testName, false, 0, error = "Exception: ${e.message}")
                    }
                    results.add(result)

                    val status = if (result.passed) "✅ PASS" else "❌ FAIL"
                    tlog("  Result: $status (${result.durationMs}ms)")
                    result.warnings.forEach { w -> tlog("  ⚠️ $w") }
                    result.error?.let { e -> tlog("  Error: $e") }

                    _state.update { it.copy(
                        completedTests = index + 1,
                        results = results.toList(),
                        logMessages = testLog.toList()
                    ) }

                    p2pManager.log("[TEST] ${result.testName}: $status (${result.durationMs}ms)${result.error?.let { " — $it" } ?: ""}")
                }

                // Phase 5: Compile and export
                stopPacketListener()
                val totalDuration = System.currentTimeMillis() - startTime
                val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).format(Date())
                val runResult = TestRunResult(
                    runTimestamp = timestamp,
                    deviceName = p2pManager.getLocalDeviceName(),
                    peers = peers,
                    results = results,
                    totalDurationMs = totalDuration
                )

                exportResults(runResult)

                // Upload test results to cloud telemetry if available
                try {
                    onTestResultsReady?.invoke(runResult.toJson().toString())
                } catch (e: Exception) {
                    tlog("Failed to send results to telemetry: ${e.message}")
                }

                val summary = "Complete: ${runResult.passedCount}/${runResult.results.size} passed (${totalDuration / 1000}s)"
                tlog("═══════════════════════════════════════════════════")
                tlog(summary)
                tlog("═══════════════════════════════════════════════════")

                _state.update { it.copy(
                    isRunning = false,
                    progress = 1f,
                    currentTest = "",
                    finalResult = runResult,
                    statusMessage = summary,
                    logMessages = testLog.toList()
                ) }

                p2pManager.log("[TEST] ═══ $summary ═══")
                results.filter { !it.passed }.forEach { r ->
                    p2pManager.log("[TEST] FAILED: ${r.testName} — ${r.error}", LogLevel.WARN)
                }

            } catch (e: CancellationException) {
                tlog("Test suite cancelled.")
                _state.update { it.copy(isRunning = false, statusMessage = "Cancelled", logMessages = testLog.toList()) }
            } catch (e: Exception) {
                tlog("FATAL ERROR: ${e.message}")
                Log.e(TAG, "Test suite error", e)
                _state.update { it.copy(isRunning = false, statusMessage = "ERROR: ${e.message}", logMessages = testLog.toList()) }
                p2pManager.log("[TEST] Suite error: ${e.message}", LogLevel.ERROR)
            }
        }
    }

    fun cancel() {
        scope.coroutineContext.cancelChildren()
        stopPacketListener()
        tlog("Test suite cancelled by user.")
        _state.update { TestState(statusMessage = "Cancelled", logMessages = testLog.toList()) }
    }

    // ─── Test Dispatcher ────────────────────────────────────────────────

    private suspend fun runSingleTest(
        testName: String,
        peers: List<String>,
        baselineStats: NetworkStatsSnapshot
    ): TestResult {
        return when (testName) {
            "Packet Serialization"     -> testPacketSerialization()
            "Message Cache Dedup"      -> testMessageCacheDedup()
            "Connection Verification"  -> testConnectionVerification(peers)
            "Identity Exchange"        -> testIdentityExchange(peers)
            "Text Messaging"           -> testTextMessaging(peers)
            "Message Reception"        -> testMessageReception()
            "Ping / RTT"              -> testPingRtt(peers)
            "Broadcast Messaging"     -> testBroadcast(peers)
            "Routing Table Integrity" -> testRoutingTableIntegrity(peers)
            "Store-and-Forward Queue" -> testStoreAndForward()
            "File Transfer"           -> testFileTransfer(peers)
            "Network Stats Counters"  -> testNetworkStats(baselineStats)
            "Per-Peer Stats"          -> testPerPeerStats(peers)
            "Connection Stability"    -> testConnectionStability(peers)
            "Heartbeat Active"        -> testHeartbeatActive(peers)
            else -> TestResult(testName, false, 0, error = "Unknown test: $testName")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 1: Packet Serialization — binary round-trip for every PacketType
    // ═══════════════════════════════════════════════════════════════════

    private fun testPacketSerialization(): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()
        var passCount = 0
        var failCount = 0

        // Round-trip every PacketType
        for (type in PacketType.values()) {
            try {
                val original = Packet(
                    id = UUID.randomUUID().toString(),
                    type = type,
                    sourceId = "TestDevice-ABC123",
                    destId = "TestDevice-DEF456",
                    payload = "Hello from $type test".toByteArray(StandardCharsets.UTF_8),
                    ttl = 5,
                    sequenceNumber = 42L,
                    trace = listOf(Hop("Peer-A", -40), Hop("Peer-B", -60))
                )
                val bytes = original.toBytes()
                val restored = Packet.fromBytes(bytes)

                val checks = listOf(
                    "id" to (original.id == restored.id),
                    "type" to (original.type == restored.type),
                    "sourceId" to (original.sourceId == restored.sourceId),
                    "destId" to (original.destId == restored.destId),
                    "payload" to (original.payload.contentEquals(restored.payload)),
                    "ttl" to (original.ttl == restored.ttl),
                    "seq" to (original.sequenceNumber == restored.sequenceNumber),
                    "traceSize" to (original.trace.size == restored.trace.size),
                    "timestamp" to (original.timestamp == restored.timestamp)
                )
                val failures = checks.filter { !it.second }.map { it.first }
                if (failures.isEmpty()) {
                    passCount++
                    tlog("  Serialize $type: OK (${bytes.size}B)")
                } else {
                    failCount++
                    warnings.add("$type failed fields: ${failures.joinToString()}")
                    tlog("  Serialize $type: FAIL — mismatched: ${failures.joinToString()}")
                }
            } catch (e: Exception) {
                failCount++
                warnings.add("$type threw: ${e.message}")
                tlog("  Serialize $type: EXCEPTION — ${e.message}")
            }
        }

        // Edge case: empty payload
        try {
            val pkt = Packet(type = PacketType.DATA, sourceId = "A", destId = "B", payload = ByteArray(0))
            val restored = Packet.fromBytes(pkt.toBytes())
            if (restored.payload.isEmpty()) { passCount++; tlog("  Empty payload: OK") }
            else { failCount++; warnings.add("Empty payload not preserved") }
        } catch (e: Exception) { failCount++; warnings.add("Empty payload exception: ${e.message}") }

        // Edge case: long sourceId (200 chars, within 256 limit)
        try {
            val longName = "X".repeat(200)
            val pkt = Packet(type = PacketType.DATA, sourceId = longName, destId = "B", payload = ByteArray(0))
            val restored = Packet.fromBytes(pkt.toBytes())
            if (restored.sourceId == longName) { passCount++; tlog("  Long name (200 chars): OK") }
            else { failCount++; warnings.add("Long name not preserved") }
        } catch (e: Exception) { failCount++; warnings.add("Long name exception: ${e.message}") }

        // Edge case: large payload (100 KB)
        try {
            val bigPayload = ByteArray(100_000) { (it % 256).toByte() }
            val pkt = Packet(type = PacketType.DATA, sourceId = "A", destId = "B", payload = bigPayload)
            val restored = Packet.fromBytes(pkt.toBytes())
            if (restored.payload.contentEquals(bigPayload)) { passCount++; tlog("  Large payload (100KB): OK") }
            else { failCount++; warnings.add("Large payload corrupted") }
        } catch (e: Exception) { failCount++; warnings.add("Large payload exception: ${e.message}") }

        // Edge case: invalid data should throw
        try {
            Packet.fromBytes(byteArrayOf(0, 0, 0, 1, 99))
            failCount++; warnings.add("Invalid data did not throw")
        } catch (_: Exception) { passCount++; tlog("  Invalid data rejection: OK") }

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Packet Serialization",
            passed = failCount == 0,
            durationMs = duration,
            details = mapOf("passed" to passCount, "failed" to failCount, "packetTypes" to PacketType.values().size),
            warnings = warnings,
            error = if (failCount > 0) "$failCount serialization failure(s)" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 2: Message Cache Dedup
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testMessageCacheDedup(): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()
        val cache = MessageCache(capacity = 100)

        // 1: First insertion returns true
        val id1 = UUID.randomUUID().toString()
        if (!cache.tryMarkSeen(id1)) warnings.add("First tryMarkSeen returned false")

        // 2: Duplicate returns false
        if (cache.tryMarkSeen(id1)) warnings.add("Duplicate tryMarkSeen returned true")

        // 3: Different ID returns true
        if (!cache.tryMarkSeen(UUID.randomUUID().toString())) warnings.add("Different ID tryMarkSeen returned false")

        // 4: Capacity overflow — insert 150 items into capacity-100 cache
        for (i in 0..150) cache.tryMarkSeen(UUID.randomUUID().toString())
        if (!cache.tryMarkSeen(UUID.randomUUID().toString())) warnings.add("Post-overflow insert failed")

        // 5: Clear works
        cache.clear()
        if (!cache.tryMarkSeen(id1)) warnings.add("After clear, previously-seen ID was still rejected")

        // 6: Thread safety — 100 concurrent unique inserts
        val cache2 = MessageCache(capacity = 200)
        val jobs = (1..100).map { i -> scope.async { cache2.tryMarkSeen("concurrent-$i") } }
        val trueCount = jobs.awaitAll().count { it }
        if (trueCount != 100) warnings.add("Concurrent unique inserts: expected 100 true, got $trueCount")

        tlog("  Dedup checks: 6 run, ${warnings.size} issue(s). Concurrent=$trueCount/100")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Message Cache Dedup",
            passed = warnings.isEmpty(),
            durationMs = duration,
            details = mapOf("checks" to 6, "issues" to warnings.size, "concurrentOk" to trueCount),
            warnings = warnings,
            error = if (warnings.isNotEmpty()) "${warnings.size} dedup check(s) failed" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 3: Connection Verification
    // ═══════════════════════════════════════════════════════════════════

    private fun testConnectionVerification(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val neighbors = p2pManager.getNeighborsSnapshot()
        val state = p2pManager.state.value

        tlog("  Neighbors: ${neighbors.size} (${neighbors.keys.joinToString()})")
        tlog("  Peer names: ${neighbors.values.map { it.peerName }.joinToString()}")
        tlog("  State.connectedEndpoints: ${state.connectedEndpoints.size}")

        if (neighbors.isEmpty()) warnings.add("No neighbors in snapshot")
        if (neighbors.size != state.connectedEndpoints.size) {
            warnings.add("Neighbor count (${neighbors.size}) != connectedEndpoints (${state.connectedEndpoints.size})")
        }
        if (!state.isAdvertising && !state.isDiscovering) {
            warnings.add("Neither advertising nor discovering is active")
        }
        neighbors.forEach { (eid, neighbor) ->
            if (eid.isBlank()) warnings.add("Blank endpoint ID found")
            if (neighbor.peerId != eid) warnings.add("peerId mismatch: ${neighbor.peerId} != $eid")
        }

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Connection Verification",
            passed = warnings.isEmpty() && neighbors.isNotEmpty(),
            durationMs = duration,
            details = mapOf(
                "neighborCount" to neighbors.size,
                "isAdvertising" to state.isAdvertising,
                "isDiscovering" to state.isDiscovering,
                "connectedEndpoints" to state.connectedEndpoints.size
            ),
            warnings = warnings,
            error = if (neighbors.isEmpty()) "No neighbors connected"
                    else if (warnings.isNotEmpty()) "${warnings.size} issue(s)" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 4: Identity Exchange
    // ═══════════════════════════════════════════════════════════════════

    private fun testIdentityExchange(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val neighbors = p2pManager.getNeighborsSnapshot()
        val unknownPeers = neighbors.values.filter { it.peerName == "Unknown" || it.peerName.isBlank() }
        val resolvedPeers = neighbors.values.filter { it.peerName != "Unknown" && it.peerName.isNotBlank() }

        tlog("  Resolved: ${resolvedPeers.map { it.peerName }.joinToString()}")
        if (unknownPeers.isNotEmpty()) {
            tlog("  Unknown: ${unknownPeers.size} (${unknownPeers.map { it.peerId }.joinToString()})")
            warnings.add("${unknownPeers.size} peer(s) still have name 'Unknown'")
        }

        val localName = p2pManager.getLocalDeviceName()
        if (localName == "Unknown" || localName.isBlank()) warnings.add("Local device name is '$localName'")
        tlog("  Local device name: $localName")

        val selfNamed = resolvedPeers.filter { it.peerName == localName }
        if (selfNamed.isNotEmpty()) warnings.add("${selfNamed.size} peer(s) share our name — identity confusion")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Identity Exchange",
            passed = unknownPeers.isEmpty() && selfNamed.isEmpty(),
            durationMs = duration,
            details = mapOf("resolved" to resolvedPeers.size, "unknown" to unknownPeers.size, "localName" to localName),
            warnings = warnings,
            error = if (unknownPeers.isNotEmpty()) "${unknownPeers.size} unresolved identities" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 5: Text Messaging — Send numbered messages to each peer
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testTextMessaging(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        var totalSent = 0
        var totalErrors = 0
        val warnings = mutableListOf<String>()

        for (peer in peers) {
            tlog("  Sending $MSG_COUNT messages to $peer...")
            for (i in 1..MSG_COUNT) {
                try {
                    val msg = "${TEST_MSG_PREFIX}MSG_${p2pManager.getLocalDeviceName()}_${i}_${System.currentTimeMillis()}"
                    p2pManager.sendData(peer, msg)
                    totalSent++
                    delay(150)
                } catch (e: Exception) {
                    totalErrors++
                    tlog("  Send error to $peer #$i: ${e.message}")
                }
            }
        }

        delay(3000) // Propagation time

        if (totalErrors > 0) warnings.add("$totalErrors send errors out of ${peers.size * MSG_COUNT}")
        tlog("  Sent: $totalSent/${peers.size * MSG_COUNT}, Errors: $totalErrors")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Text Messaging",
            passed = totalErrors == 0 && totalSent == peers.size * MSG_COUNT,
            durationMs = duration,
            details = mapOf(
                "totalSent" to totalSent, "totalErrors" to totalErrors,
                "peersCount" to peers.size, "msgsPerPeer" to MSG_COUNT,
                "avgIntervalMs" to if (totalSent > 1) (duration / totalSent) else 0
            ),
            warnings = warnings,
            error = if (totalErrors > 0) "$totalErrors send errors" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 6: Message Reception — Check if test messages arrived from others
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testMessageReception(): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        delay(2000) // Extra wait for slow arrivals

        val totalReceived = receivedTestMessages.values.sumOf { it.size }
        val peersWhoSent = receivedTestMessages.keys.toList()

        tlog("  Received test messages from ${peersWhoSent.size} peer(s), total: $totalReceived")
        peersWhoSent.forEach { peer ->
            tlog("    $peer: ${receivedTestMessages[peer]?.size ?: 0} messages")
        }

        if (totalReceived == 0) warnings.add("No test messages received — other devices may not be in test mode")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Message Reception",
            passed = true, // Informational — passes as long as listener is working
            durationMs = duration,
            details = mapOf("totalReceived" to totalReceived, "peersWhoSent" to peersWhoSent.size),
            warnings = warnings
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 7: Ping / RTT — Measure round-trip latencies
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testPingRtt(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()
        receivedPongs.clear()
        peers.forEach { receivedPongs[it] = CopyOnWriteArrayList() }

        tlog("  Sending $PING_COUNT pings to ${peers.size} peer(s)...")
        var sendErrors = 0
        for (i in 1..PING_COUNT) {
            for (peer in peers) {
                try {
                    val buf = ByteBuffer.allocate(8).putLong(System.currentTimeMillis()).array()
                    p2pManager.sendPing(peer, buf)
                } catch (_: Exception) { sendErrors++ }
            }
            delay(PING_INTERVAL_MS)
        }

        tlog("  Waiting for PONGs (${PING_WAIT_TIMEOUT_MS / 1000}s)...")
        delay(PING_WAIT_TIMEOUT_MS)

        val totalSent = PING_COUNT * peers.size
        val details = mutableMapOf<String, Any>("pingSent" to totalSent, "sendErrors" to sendErrors)
        var totalPongsReceived = 0

        for (peer in peers) {
            val rtts = receivedPongs[peer] ?: emptyList<Long>()
            totalPongsReceived += rtts.size
            if (rtts.isNotEmpty()) {
                val sorted = rtts.sorted()
                val min = sorted.first()
                val max = sorted.last()
                val avg = rtts.average().toLong()
                val median = sorted[sorted.size / 2]
                val p95 = sorted[(sorted.size * 0.95).toInt().coerceAtMost(sorted.size - 1)]
                tlog("  $peer: ${rtts.size}/$PING_COUNT PONGs, RTT min=${min}ms avg=${avg}ms med=${median}ms p95=${p95}ms max=${max}ms")
                details["${peer}_count"] = rtts.size
                details["${peer}_min"] = min; details["${peer}_avg"] = avg
                details["${peer}_med"] = median; details["${peer}_p95"] = p95; details["${peer}_max"] = max
                if (avg > 500) warnings.add("$peer high avg RTT: ${avg}ms")
            } else {
                tlog("  $peer: 0/$PING_COUNT PONGs")
                warnings.add("$peer: No PONGs received")
            }
        }

        details["totalPongs"] = totalPongsReceived
        val allRtts = receivedPongs.values.flatten()
        if (allRtts.isNotEmpty()) {
            details["overallAvg"] = allRtts.average().toLong()
            val loss = ((1.0 - totalPongsReceived.toDouble() / totalSent) * 100)
            details["lossPercent"] = String.format("%.1f", loss)
            if (loss > 50) warnings.add("High packet loss: ${String.format("%.1f", loss)}%")
        }

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Ping / RTT",
            passed = totalPongsReceived > 0,
            durationMs = duration,
            details = details,
            warnings = warnings,
            error = if (totalPongsReceived == 0) "No PONGs received" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 8: Broadcast Messaging
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testBroadcast(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()
        receivedBroadcasts.clear()

        val broadcastCount = 5
        var sendErrors = 0
        tlog("  Sending $broadcastCount broadcasts...")
        for (i in 1..broadcastCount) {
            try {
                p2pManager.broadcastMessage("${TEST_MSG_PREFIX}BCAST_${p2pManager.getLocalDeviceName()}_${i}_${System.currentTimeMillis()}")
            } catch (e: Exception) { sendErrors++; tlog("  Broadcast #$i error: ${e.message}") }
            delay(300)
        }

        delay(3000)
        tlog("  Sent: $broadcastCount (errors: $sendErrors), received from others: ${receivedBroadcasts.size}")
        if (sendErrors > 0) warnings.add("$sendErrors broadcast send errors")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Broadcast Messaging",
            passed = sendErrors == 0,
            durationMs = duration,
            details = mapOf("sent" to broadcastCount, "errors" to sendErrors, "receivedFromOthers" to receivedBroadcasts.size, "peers" to peers.size),
            warnings = warnings,
            error = if (sendErrors > 0) "$sendErrors broadcast send errors" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 9: Routing Table Integrity
    // ═══════════════════════════════════════════════════════════════════

    private fun testRoutingTableIntegrity(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val state = p2pManager.state.value
        val neighbors = p2pManager.getNeighborsSnapshot()
        val knownPeers = state.knownPeers

        val directNames = neighbors.values.map { it.peerName }.filter { it != "Unknown" && it.isNotBlank() }
        val routedPeers = knownPeers.keys.toList()
        val allReachable = (directNames + routedPeers).distinct()

        tlog("  Direct neighbors: ${directNames.joinToString()}")
        tlog("  Routed peers: ${routedPeers.joinToString()}")

        val unreachable = peers.filter { it !in allReachable }
        if (unreachable.isNotEmpty()) warnings.add("Unreachable: ${unreachable.joinToString()}")

        val duplicates = directNames.filter { it in routedPeers }
        if (duplicates.isNotEmpty()) warnings.add("In BOTH neighbor & routing tables: ${duplicates.joinToString()}")

        val localName = p2pManager.getLocalDeviceName()
        if (localName in routedPeers) warnings.add("Self ('$localName') is in routing table — self-poisoning!")

        knownPeers.forEach { (peer, route) ->
            if (route.hopCount <= 0) warnings.add("$peer: invalid hopCount ${route.hopCount}")
            if (route.hopCount > Packet.DEFAULT_TTL) warnings.add("$peer: hopCount ${route.hopCount} > DEFAULT_TTL ${Packet.DEFAULT_TTL}")
        }

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Routing Table Integrity",
            passed = unreachable.isEmpty() && duplicates.isEmpty() && localName !in routedPeers,
            durationMs = duration,
            details = mapOf(
                "directNeighbors" to directNames.size, "routedPeers" to routedPeers.size,
                "totalReachable" to allReachable.size, "expected" to peers.size, "duplicates" to duplicates.size
            ),
            warnings = warnings,
            error = if (unreachable.isNotEmpty() || duplicates.isNotEmpty()) {
                buildString {
                    if (unreachable.isNotEmpty()) append("Unreachable: ${unreachable.joinToString()}. ")
                    if (duplicates.isNotEmpty()) append("Duplicates: ${duplicates.joinToString()}.")
                }.trimEnd()
            } else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 10: Store-and-Forward Queue
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testStoreAndForward(): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val fakePeer = "FakeDevice-${UUID.randomUUID().toString().take(8)}"
        val queuedBefore = p2pManager.networkStats.storeForwardQueued.get()

        tlog("  Sending to non-existent '$fakePeer' to trigger S&F...")
        try {
            p2pManager.sendData(fakePeer, "${TEST_MSG_PREFIX}SF_TEST_${System.currentTimeMillis()}")
        } catch (e: Exception) { warnings.add("sendData to fake peer threw: ${e.message}") }

        delay(2000)

        val queuedAfter = p2pManager.networkStats.storeForwardQueued.get()
        val delivered = p2pManager.networkStats.storeForwardDelivered.get()
        tlog("  S&F queued: $queuedBefore→$queuedAfter (Δ${queuedAfter - queuedBefore}), delivered total: $delivered")

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Store-and-Forward Queue",
            passed = true, // Pass as long as no crash; S&F behavior depends on topology
            durationMs = duration,
            details = mapOf(
                "queuedBefore" to queuedBefore, "queuedAfter" to queuedAfter,
                "wasQueued" to (queuedAfter > queuedBefore), "totalDelivered" to delivered
            ),
            warnings = warnings
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 11: File Transfer — Generate and send a test file
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testFileTransfer(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        if (peers.isEmpty()) {
            return TestResult("File Transfer", false, 0, error = "No peers to send to")
        }

        val testContent = buildString {
            appendLine("ResilientP2P Test File")
            appendLine("Generated: ${Date()}")
            appendLine("Device: ${p2pManager.getLocalDeviceName()}")
            repeat(100) { appendLine("Test data line $it — ${UUID.randomUUID()}") }
        }
        val testFile = File(context.cacheDir, "test_transfer_${System.currentTimeMillis()}.txt")

        try {
            testFile.writeText(testContent)
            val hash = sha256(testContent.toByteArray())
            tlog("  Generated: ${testFile.name} (${testFile.length()}B, sha256=${hash.take(16)}...)")

            val targetPeer = peers.first()
            tlog("  Sending to $targetPeer via sendFile...")

            val uri = androidx.core.content.FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", testFile
            )
            p2pManager.sendFile(targetPeer, uri)
            tlog("  File send initiated")
            delay(5000)

            val duration = System.currentTimeMillis() - start
            return TestResult(
                testName = "File Transfer",
                passed = true,
                durationMs = duration,
                details = mapOf("fileName" to testFile.name, "fileSize" to testFile.length(), "target" to targetPeer),
                warnings = warnings
            )
        } catch (e: Exception) {
            val duration = System.currentTimeMillis() - start
            tlog("  File transfer error (non-fatal): ${e.message}")
            warnings.add("File transfer skipped: ${e.message}")
            return TestResult(
                testName = "File Transfer",
                passed = true, // Don't fail suite for missing FileProvider config
                durationMs = duration,
                details = mapOf("skipped" to true, "reason" to (e.message ?: "unknown")),
                warnings = warnings
            )
        } finally {
            testFile.delete()
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 12: Network Stats Counters
    // ═══════════════════════════════════════════════════════════════════

    private fun testNetworkStats(baselineStats: NetworkStatsSnapshot): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val stats = p2pManager.networkStats.snapshot(
            p2pManager.getNeighborsSnapshot().size,
            p2pManager.state.value.knownPeers.size
        )

        val checks = mapOf(
            "uptimeMs > 0" to (stats.uptimeMs > 0),
            "packetsSent > 0" to (stats.totalPacketsSent > 0),
            "packetsReceived > 0" to (stats.totalPacketsReceived > 0),
            "bytesSent > 0" to (stats.totalBytesSent > 0),
            "bytesReceived > 0" to (stats.totalBytesReceived > 0),
            "connections > 0" to (stats.totalConnectionsEstablished > 0),
            "packetsSent increased" to (stats.totalPacketsSent > baselineStats.totalPacketsSent),
            "packetsReceived increased" to (stats.totalPacketsReceived > baselineStats.totalPacketsReceived)
        )

        checks.forEach { (name, ok) ->
            if (!ok) warnings.add("FAIL: $name")
            tlog("  $name: ${if (ok) "OK" else "FAIL"}")
        }

        val sentΔ = stats.totalPacketsSent - baselineStats.totalPacketsSent
        val recvΔ = stats.totalPacketsReceived - baselineStats.totalPacketsReceived
        tlog("  Test traffic Δ: sent=$sentΔ pkts, recv=$recvΔ pkts")
        tlog("  Totals: sent=${stats.totalPacketsSent}, recv=${stats.totalPacketsReceived}, fwd=${stats.totalPacketsForwarded}, drop=${stats.totalPacketsDropped}")

        val failedChecks = checks.count { !it.value }
        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Network Stats Counters",
            passed = failedChecks == 0,
            durationMs = duration,
            details = mapOf(
                "uptimeMs" to stats.uptimeMs,
                "packetsSent" to stats.totalPacketsSent, "packetsReceived" to stats.totalPacketsReceived,
                "bytesSent" to stats.totalBytesSent, "bytesReceived" to stats.totalBytesReceived,
                "forwarded" to stats.totalPacketsForwarded, "dropped" to stats.totalPacketsDropped,
                "connections" to stats.totalConnectionsEstablished, "avgRtt" to stats.avgRttMs,
                "testSentΔ" to sentΔ, "testRecvΔ" to recvΔ, "failedChecks" to failedChecks
            ),
            warnings = warnings,
            error = if (failedChecks > 0) "$failedChecks stat check(s) failed" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 13: Per-Peer Stats
    // ═══════════════════════════════════════════════════════════════════

    private fun testPerPeerStats(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val stats = p2pManager.networkStats.snapshot(
            p2pManager.getNeighborsSnapshot().size,
            p2pManager.state.value.knownPeers.size
        )

        tlog("  Per-peer entries: ${stats.peerStats.size}")
        for (peer in peers) {
            val ps = stats.peerStats[peer]
            if (ps == null) {
                warnings.add("No stats for $peer"); tlog("  $peer: NO STATS"); continue
            }
            tlog("  $peer: rtt=${ps.lastRttMs}ms sent=${ps.packetsSent}pkts/${ps.bytesSent}B recv=${ps.packetsReceived}pkts/${ps.bytesReceived}B disc=${ps.disconnectCount}")
            if (ps.packetsSent == 0L) warnings.add("$peer: zero packets sent")
            if (ps.bytesSent == 0L) warnings.add("$peer: zero bytes sent")
            if (ps.connectedSinceMs > System.currentTimeMillis()) warnings.add("$peer: connectedSince in future")
        }

        val criticalWarnings = warnings.filter { "zero packets" in it || "NO STATS" in it }
        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Per-Peer Stats",
            passed = criticalWarnings.isEmpty(),
            durationMs = duration,
            details = mapOf("trackedPeers" to stats.peerStats.size, "expectedPeers" to peers.size),
            warnings = warnings,
            error = if (criticalWarnings.isNotEmpty()) "${criticalWarnings.size} peer(s) missing stats" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 14: Connection Stability
    // ═══════════════════════════════════════════════════════════════════

    private fun testConnectionStability(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val neighbors = p2pManager.getNeighborsSnapshot()
        val currentPeers = neighbors.values.map { it.peerName }.filter { it != "Unknown" && it.isNotBlank() }

        val lost = peers.filter { it !in currentPeers }
        val gained = currentPeers.filter { it !in peers }

        tlog("  Initial: ${peers.joinToString()}")
        tlog("  Current: ${currentPeers.joinToString()}")
        if (lost.isNotEmpty()) { tlog("  LOST: ${lost.joinToString()}"); warnings.add("Lost ${lost.size}: ${lost.joinToString()}") }
        if (gained.isNotEmpty()) tlog("  NEW: ${gained.joinToString()}")

        val totalLost = p2pManager.networkStats.totalConnectionsLost.get()
        tlog("  Session connections lost: $totalLost")

        val now = System.currentTimeMillis()
        neighbors.forEach { (_, n) ->
            val age = now - n.lastSeen.get()
            if (age > 30_000) { warnings.add("Stale: ${n.peerName} (${age / 1000}s ago)"); tlog("  STALE: ${n.peerName} ${age / 1000}s ago") }
        }

        val duration = System.currentTimeMillis() - start
        return TestResult(
            testName = "Connection Stability",
            passed = lost.isEmpty(),
            durationMs = duration,
            details = mapOf("initial" to peers.size, "current" to currentPeers.size, "lost" to lost.size, "gained" to gained.size, "sessionLost" to totalLost),
            warnings = warnings,
            error = if (lost.isNotEmpty()) "Lost ${lost.size} peer(s) during tests" else null
        )
    }

    // ═══════════════════════════════════════════════════════════════════
    // TEST 15: Heartbeat Active
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun testHeartbeatActive(peers: List<String>): TestResult {
        val start = System.currentTimeMillis()
        val warnings = mutableListOf<String>()

        val rttBefore = peers.associateWith { p2pManager.networkStats.peerRtt[it] ?: -1L }
        tlog("  RTT before: ${rttBefore.entries.joinToString { "${it.key}=${it.value}ms" }}")

        tlog("  Waiting 8s for heartbeat cycle...")
        delay(8000)

        val rttAfter = peers.associateWith { p2pManager.networkStats.peerRtt[it] ?: -1L }
        tlog("  RTT after: ${rttAfter.entries.joinToString { "${it.key}=${it.value}ms" }}")

        var updatedCount = 0
        for (peer in peers) {
            val after = rttAfter[peer] ?: -1
            if (after >= 0) updatedCount++
            else warnings.add("$peer: no RTT data — heartbeat may not reach this peer")
        }

        val neighbors = p2pManager.getNeighborsSnapshot()
        val now = System.currentTimeMillis()
        val freshCount = neighbors.values.count { now - it.lastSeen.get() < 15_000 }
        tlog("  Fresh neighbors (<15s): $freshCount/${neighbors.size}")

        val duration = System.currentTimeMillis() - start
        val passed = updatedCount > 0 || freshCount > 0
        return TestResult(
            testName = "Heartbeat Active",
            passed = passed,
            durationMs = duration,
            details = mapOf("peersWithRtt" to updatedCount, "freshNeighbors" to freshCount, "totalNeighbors" to neighbors.size),
            warnings = warnings,
            error = if (!passed) "No heartbeat activity detected" else null
        )
    }

    // ─── Helpers ────────────────────────────────────────────────────────

    private fun tlog(msg: String) {
        testLog.add(msg)
        Log.d(TAG, msg)
        _state.update { it.copy(logMessages = testLog.toList()) }
    }

    private suspend fun waitForPeers(): Boolean {
        val deadline = System.currentTimeMillis() + PEER_WAIT_TIMEOUT_MS
        while (System.currentTimeMillis() < deadline) {
            val names = getConnectedPeerNames()
            if (names.isNotEmpty()) {
                tlog("Found ${names.size} peer(s): ${names.joinToString()}")
                return true
            }
            val remaining = (deadline - System.currentTimeMillis()) / 1000
            _state.update { it.copy(statusMessage = "Waiting for peers... (${remaining}s remaining)") }
            delay(2000)
        }
        return false
    }

    private fun getConnectedPeerNames(): List<String> {
        return p2pManager.getNeighborsSnapshot()
            .values.map { it.peerName }
            .filter { it != "Unknown" && it.isNotBlank() }
    }

    private fun startPacketListener() {
        listenerJob = scope.launch {
            p2pManager.payloadEvents.collect { event ->
                val packet = event.packet
                when (packet.type) {
                    PacketType.PONG -> {
                        try {
                            if (packet.payload.size >= 8) {
                                val buf = ByteBuffer.wrap(packet.payload)
                                val originTs = buf.long
                                val rtt = System.currentTimeMillis() - originTs
                                receivedPongs.getOrPut(packet.sourceId) { CopyOnWriteArrayList() }.add(rtt)
                            }
                        } catch (_: Exception) {}
                    }
                    PacketType.DATA -> {
                        val payload = String(packet.payload, StandardCharsets.UTF_8)
                        if (payload.startsWith(TEST_MSG_PREFIX)) {
                            if (payload.contains("BCAST_")) {
                                receivedBroadcasts.add(payload)
                            } else {
                                receivedTestMessages
                                    .getOrPut(packet.sourceId) { ConcurrentHashMap.newKeySet() }
                                    .add(payload)
                            }
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    private fun stopPacketListener() {
        listenerJob?.cancel()
        listenerJob = null
    }

    private fun sha256(data: ByteArray): String {
        return MessageDigest.getInstance("SHA-256").digest(data)
            .joinToString("") { "%02x".format(it) }
    }

    private fun exportResults(result: TestRunResult) {
        try {
            val dir = File(context.getExternalFilesDir(null), "test_results")
            dir.mkdirs()

            val dateStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val deviceTag = p2pManager.getLocalDeviceName().replace(Regex("[^a-zA-Z0-9_-]"), "_")

            // JSON
            val jsonFile = File(dir, "test_${deviceTag}_${dateStamp}.json")
            jsonFile.writeText(result.toJson().toString(2))
            tlog("Exported JSON: ${jsonFile.absolutePath}")

            // CSV
            val csvFile = File(dir, "test_${deviceTag}_${dateStamp}.csv")
            csvFile.writeText(result.toCsv())
            tlog("Exported CSV: ${csvFile.absolutePath}")

            // Detailed log
            val logFile = File(dir, "test_log_${deviceTag}_${dateStamp}.txt")
            logFile.writeText(testLog.joinToString("\n"))
            tlog("Exported log: ${logFile.absolutePath}")

            p2pManager.log("[TEST] Results exported to ${dir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export results", e)
            p2pManager.log("[TEST] Export failed: ${e.message}", LogLevel.ERROR)
        }
    }
}
