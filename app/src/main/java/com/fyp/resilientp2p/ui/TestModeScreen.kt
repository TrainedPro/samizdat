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
import com.fyp.resilientp2p.testing.TestResult
import com.fyp.resilientp2p.testing.TestRunner
import com.fyp.resilientp2p.testing.TestState

/**
 * Full-screen overlay for test mode. Shows test progress and results.
 */
@Composable
fun TestModeScreen(
    testRunner: TestRunner,
    onDismiss: () -> Unit
) {
    val testState by testRunner.state.collectAsState()

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
                if (!testState.isRunning) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status bar
            StatusCard(testState)

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

            // Tab row: Results vs Logs
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

            // Results list OR Log panel
            if (!showLogs && testState.results.isNotEmpty()) {
                Text(
                    "Results",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(testState.results) { result ->
                        TestResultCard(result)
                    }

                    // Summary card at the bottom
                    testState.finalResult?.let { final ->
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            SummaryCard(final.passedCount, final.failedCount, final.totalDurationMs)
                        }
                    }
                }
            } else if (showLogs) {
                // Log panel
                val logListState = rememberLazyListState()
                LaunchedEffect(testState.logMessages.size) {
                    if (testState.logMessages.isNotEmpty()) {
                        logListState.animateScrollToItem(testState.logMessages.size - 1)
                    }
                }
                LazyColumn(
                    state = logListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    items(testState.logMessages) { line ->
                        val color = when {
                            line.startsWith("  EXCEPTION") || line.startsWith("  FAIL") || "FATAL" in line -> Color(0xFFFF6B6B)
                            line.startsWith("  ⚠️") -> Color(0xFFFFD93D)
                            line.startsWith("═") || line.startsWith("───") -> Color(0xFF6BCB77)
                            line.startsWith("  Result: ✅") -> Color(0xFF4CAF50)
                            line.startsWith("  Result: ❌") -> Color(0xFFEF5350)
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
            } else if (!testState.isRunning) {
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Press \"Run Tests\" to start the automated test suite.\n" +
                        "Ensure at least one peer is connected, or use \"Auto-Start + Test\".",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: TestState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                state.waitingForPeers -> MaterialTheme.colorScheme.tertiaryContainer
                state.isRunning -> MaterialTheme.colorScheme.primaryContainer
                state.finalResult?.allPassed == true -> Color(0xFFE8F5E9)
                state.finalResult != null -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            }
            Text(
                text = state.statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (state.isRunning) FontWeight.Bold else FontWeight.Normal
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

            // Error message
            result.error?.let { err ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = err,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Warnings
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

            // Details
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Black.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    result.details.forEach { (key, value) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = key,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Test Suite Complete",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
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
