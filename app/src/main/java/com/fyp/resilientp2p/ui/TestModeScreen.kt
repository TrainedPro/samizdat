package com.fyp.resilientp2p.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.testing.EnduranceDuration
import com.fyp.resilientp2p.testing.EnduranceTestRunner
import com.fyp.resilientp2p.testing.EnduranceTestState
import com.fyp.resilientp2p.testing.TestResult
import com.fyp.resilientp2p.testing.TestRunner
import com.fyp.resilientp2p.testing.TestState

/**
 * Full-screen overlay for test mode.
 * Contains two tabs: **Functional Tests** (existing 15-test suite) and
 * **Endurance Test** (long-running soak test with battery/mAh tracking).
 */
@Composable
fun TestModeScreen(
    testRunner: TestRunner,
    enduranceTestRunner: EnduranceTestRunner? = null,
    onDismiss: () -> Unit
) {
    val testState by testRunner.state.collectAsState()
    val enduranceState = enduranceTestRunner?.state?.collectAsState()?.value
        ?: EnduranceTestState()

    // Top-level mode selector: Functional vs Endurance
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val anyRunning = testState.isRunning || enduranceState.isRunning

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Test Mode",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                if (!anyRunning) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mode tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { if (!anyRunning) selectedTab = 0 },
                    text = { Text("Functional Tests") }
                )
                if (enduranceTestRunner != null) {
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { if (!anyRunning) selectedTab = 1 },
                        text = { Text("Endurance Test") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (selectedTab) {
                0 -> FunctionalTestTab(testRunner, testState)
                1 -> if (enduranceTestRunner != null) {
                    EnduranceTestTab(enduranceTestRunner, enduranceState)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Functional Tests Tab (existing)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ColumnScope.FunctionalTestTab(testRunner: TestRunner, testState: TestState) {
    // Status bar
    StatusCard(testState.statusMessage, testState.isRunning, testState.waitingForPeers,
        testState.finalResult?.allPassed)

    Spacer(modifier = Modifier.height(12.dp))

    // Progress
    if (testState.isRunning) {
        LinearProgressIndicator(
            progress = { testState.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "${testState.completedTests} / ${testState.totalTests} tests",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!testState.isRunning && testState.finalResult == null) {
            Button(
                onClick = { testRunner.runTests(autoStart = false) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Run Tests")
            }
            OutlinedButton(
                onClick = { testRunner.runTests(autoStart = true) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Auto-Start + Test")
            }
        }
        if (testState.isRunning) {
            OutlinedButton(
                onClick = { testRunner.cancel() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Cancel")
            }
        }
        if (testState.finalResult != null && !testState.isRunning) {
            Button(
                onClick = { testRunner.runTests(autoStart = false) },
                modifier = Modifier.weight(1f)
            ) {
                Text("Re-run Tests")
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Sub-tab: Results vs Logs
    var showLogs by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = !showLogs,
            onClick = { showLogs = false },
            label = { Text("Results (${testState.results.size})") }
        )
        FilterChip(
            selected = showLogs,
            onClick = { showLogs = true },
            label = { Text("Log (${testState.logMessages.size})") }
        )
    }
    Spacer(modifier = Modifier.height(8.dp))

    if (!showLogs && testState.results.isNotEmpty()) {
        Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(testState.results) { result -> TestResultCard(result) }
            testState.finalResult?.let { final ->
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SummaryCard(final.passedCount, final.failedCount, final.totalDurationMs)
                }
            }
        }
    } else if (showLogs) {
        LogPanel(testState.logMessages, Modifier.weight(1f))
    } else if (!testState.isRunning) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Text(
                "Press \"Run Tests\" to start the automated test suite.\n" +
                "Ensure at least one peer is connected, or use \"Auto-Start + Test\".",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Endurance Test Tab (new)
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun ColumnScope.EnduranceTestTab(runner: EnduranceTestRunner, state: EnduranceTestState) {
    var selectedDuration by remember { mutableStateOf(EnduranceDuration.TWO_HOURS) }

    // Duration picker (disabled while running)
    if (!state.isRunning && state.finalReport == null) {
        Text("Duration", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            EnduranceDuration.entries.forEach { dur ->
                FilterChip(
                    selected = selectedDuration == dur,
                    onClick = { selectedDuration = dur },
                    label = { Text(dur.label, fontSize = 12.sp) }
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Live stats card
    if (state.isRunning || state.finalReport != null) {
        EnduranceStatsCard(state)
        Spacer(modifier = Modifier.height(12.dp))
    }

    // Progress bar (only for timed runs)
    if (state.isRunning && state.duration != EnduranceDuration.INDEFINITE) {
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth().height(6.dp),
            color = MaterialTheme.colorScheme.tertiary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!state.isRunning && state.finalReport == null) {
            Button(
                onClick = { runner.start(selectedDuration) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Start Endurance Test")
            }
        }
        if (state.isRunning) {
            OutlinedButton(
                onClick = { runner.stop() },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Stop Test")
            }
        }
        if (state.finalReport != null && !state.isRunning) {
            Button(
                onClick = { runner.start(selectedDuration) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Re-run Endurance")
            }
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Final report or log
    if (state.finalReport != null && !state.isRunning) {
        EnduranceReportCard(state)
        Spacer(modifier = Modifier.height(8.dp))
    }

    // Log panel
    LogPanel(state.logMessages, Modifier.weight(1f))
}

@Composable
private fun EnduranceStatsCard(state: EnduranceTestState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (state.isRunning) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    if (state.isRunning) "⏱ Running — ${state.duration.label}" else "Complete",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    formatDuration(state.elapsedMs),
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Battery row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Battery", "${state.currentBatteryLevel}%")
                StatColumn("Drain", if (state.mahDrained > 0) "%.1f mAh".format(state.mahDrained) else "—")
                StatColumn("Msgs Sent", "${state.messagesSent}")
                StatColumn("Msgs Recv", "${state.messagesReceived}")
            }
        }
    }
}

@Composable
private fun EnduranceReportCard(state: EnduranceTestState) {
    val report = state.finalReport ?: return
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Endurance Report", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            // ── Battery ──
            Text("Battery", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ReportRow("Duration", formatDuration(report.actualDurationMs))
            ReportRow("Battery Drain", "${report.startBatteryLevel}% → ${report.endBatteryLevel}% (Δ${report.batteryDrainPercent}%)")
            ReportRow("mAh Drained", "%.1f mAh".format(report.mahDrained))
            ReportRow("Drain Rate", "%.1f mAh/hr".format(report.mahPerHour))
            ReportRow("mAh Source", report.mAhSource)

            Spacer(modifier = Modifier.height(8.dp))

            // ── Traffic ──
            Text("Traffic", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ReportRow("Packets Sent", "${report.totalPacketsSent}")
            ReportRow("Packets Received", "${report.totalPacketsReceived}")
            ReportRow("Packets Forwarded", "${report.totalPacketsForwarded}")
            ReportRow("Packets Dropped", "${report.totalPacketsDropped}")
            ReportRow("Bytes", "↑${formatBytes(report.totalBytesSent)} ↓${formatBytes(report.totalBytesReceived)}")

            Spacer(modifier = Modifier.height(8.dp))

            // ── Latency ──
            Text("Latency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ReportRow("Avg RTT", "${report.avgRttMs} ms")
            ReportRow("Min RTT", "${report.minRttMs} ms")
            ReportRow("Max RTT", "${report.maxRttMs} ms")
            ReportRow("Mean Jitter", "%.1f ms".format(report.meanJitterMs))

            Spacer(modifier = Modifier.height(8.dp))

            // ── Reliability ──
            Text("Reliability", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ReportRow("Packet Loss", "%.2f%%".format(report.overallPacketLossRate * 100))
            ReportRow("Avg Throughput", "%.1f KB/s".format(report.avgThroughputBytesPerSec / 1024))

            Spacer(modifier = Modifier.height(8.dp))

            // ── Connectivity ──
            Text("Connectivity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            ReportRow("Peak Neighbors", "${report.peakNeighborCount}")
            ReportRow("Connections Up", "${report.totalConnectionsEstablished}")
            ReportRow("Connections Lost", "${report.totalConnectionsLost}")
            ReportRow("S&F Queued", "${report.storeForwardQueued}")
            ReportRow("S&F Delivered", "${report.storeForwardDelivered}")
            ReportRow("Snapshots", "${report.snapshots.size}")

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Results exported to endurance_results/ and queued for cloud upload",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 16.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ═══════════════════════════════════════════════════════════════════════
// Shared Components
// ═══════════════════════════════════════════════════════════════════════

@Composable
private fun StatusCard(
    statusMessage: String,
    isRunning: Boolean,
    waitingForPeers: Boolean,
    allPassed: Boolean?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                waitingForPeers -> MaterialTheme.colorScheme.tertiaryContainer
                isRunning -> MaterialTheme.colorScheme.primaryContainer
                allPassed == true -> Color(0xFFE8F5E9)
                allPassed == false -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRunning) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isRunning) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun LogPanel(messages: List<String>, modifier: Modifier) {
    val logListState = rememberLazyListState()
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            logListState.animateScrollToItem(messages.size - 1)
        }
    }
    LazyColumn(
        state = logListState,
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        items(messages) { line ->
            val color = when {
                "EXCEPTION" in line || "FAIL" in line || "FATAL" in line || "ERROR" in line -> Color(0xFFFF6B6B)
                "⚠" in line || "WARN" in line -> Color(0xFFFFD93D)
                line.contains("═") || line.contains("───") -> Color(0xFF6BCB77)
                "✅" in line || "COMPLETE" in line -> Color(0xFF4CAF50)
                "❌" in line -> Color(0xFFEF5350)
                "SNAPSHOT" in line -> Color(0xFF64B5F6)
                else -> Color(0xFFCCCCCC)
            }
            Text(
                text = line,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = color,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                modifier = Modifier.padding(vertical = 1.dp)
            )
        }
    }
}

@Composable
private fun TestResultCard(result: TestResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.passed) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (result.passed) "✅" else "❌",
                        fontSize = 18.sp
                    )
                    Text(
                        text = result.testName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${result.durationMs}ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            result.error?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }

            if (result.warnings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                result.warnings.forEach { warn ->
                    Text(
                        text = "⚠️ $warn",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFE65100),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.05f), shape = RoundedCornerShape(4.dp))
                        .padding(8.dp)
                ) {
                    result.details.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = key, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(text = value.toString(), style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(passed: Int, failed: Int, durationMs: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Test Suite Complete", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$passed", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    Text("Passed", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$failed", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = if (failed > 0) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Failed", style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${durationMs / 1000}s", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Duration", style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Results exported to test_results/ directory",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ─── Utilities ──────────────────────────────────────────────────────────

private fun formatDuration(ms: Long): String {
    val secs = ms / 1000
    val mins = secs / 60
    val hrs = mins / 60
    return when {
        hrs > 0 -> "%dh %02dm %02ds".format(hrs, mins % 60, secs % 60)
        mins > 0 -> "%dm %02ds".format(mins, secs % 60)
        else -> "${secs}s"
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "${bytes}B"
    bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
    else -> "%.1fMB".format(bytes / (1024.0 * 1024.0))
}
