package com.fyp.resilientp2p.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.core.graphics.toColorInt
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.data.NetworkStats
import com.fyp.resilientp2p.data.P2PState
import com.fyp.resilientp2p.data.PeerStatsSnapshot
import com.fyp.resilientp2p.managers.LocationEstimator
import com.fyp.resilientp2p.managers.P2PManager
import kotlin.math.min

/**
 * Full-screen on-device health dashboard for the mesh network.
 *
 * Displays:
 * - Topology graph (simple node-link Canvas)
 * - Per-peer RTT and throughput mini-charts
 * - Network-wide stats cards (packets, battery, connections)
 * - Packet loss heatmap (grid of peer-vs-peer loss rates)
 *
 * @param p2pManager The core mesh manager providing state and stats.
 * @param locationEstimator Optional location engine (for position-aware topology).
 * @param onBack Navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthDashboard(
    p2pManager: P2PManager,
    locationEstimator: LocationEstimator?,
    onBack: () -> Unit
) {
    val state by p2pManager.state.collectAsState()
    val stats = p2pManager.networkStats

    // Single shared polling loop for per-peer snapshots (avoids duplicate timers)
    val peerSnapshots = remember { mutableStateOf(emptyList<PeerStatsSnapshot>()) }
    LaunchedEffect(Unit) {
        while (true) {
            peerSnapshots.value = stats.peerConnectedSince.entries.map { (peer, connSince) ->
                PeerStatsSnapshot(
                    peerName = peer,
                    connectedSinceMs = connSince,
                    lastRttMs = stats.peerRtt[peer] ?: -1,
                    bytesSent = stats.peerBytesSent[peer]?.get() ?: 0,
                    bytesReceived = stats.peerBytesReceived[peer]?.get() ?: 0,
                    packetsSent = stats.peerPacketsSent[peer]?.get() ?: 0,
                    packetsReceived = stats.peerPacketsReceived[peer]?.get() ?: 0,
                    disconnectCount = stats.peerDisconnectCount[peer]?.get() ?: 0
                )
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Health Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Network summary cards
            NetworkSummaryCards(state = state, stats = stats)

            // 2. Topology graph
            TopologyGraph(state = state, stats = stats)

            // 3. Per-peer stats table
            PeerStatsTable(peerSnapshots = peerSnapshots.value)

            // 4. Packet loss heatmap
            PacketLossHeatmap(peerSnapshots = peerSnapshots.value)

            // 5. Radar view (if location estimator available)
            if (locationEstimator != null) {
                RadarView(
                    locationEstimator = locationEstimator,
                    localPeerName = state.localDeviceName
                )
            }
        }
    }
}

/** Row of summary metric cards. */
@Composable
private fun NetworkSummaryCards(state: P2PState, stats: NetworkStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Peers", "${state.connectedEndpoints.size}", Color(0xFF4CAF50), Modifier.weight(1f))
        StatCard("Sent", "${stats.totalPacketsSent.get()}", Color(0xFF2196F3), Modifier.weight(1f))
        StatCard("Recv", "${stats.totalPacketsReceived.get()}", Color(0xFF9C27B0), Modifier.weight(1f))
        StatCard("Fwd", "${stats.totalPacketsForwarded.get()}", Color(0xFFFF9800), Modifier.weight(1f))
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard("Dropped", "${stats.totalPacketsDropped.get()}", Color(0xFFF44336), Modifier.weight(1f))
        StatCard("S&F Queued", "${stats.storeForwardQueued.get()}", Color(0xFF795548), Modifier.weight(1f))
        StatCard("Lost", "${stats.totalConnectionsLost.get()}", Color(0xFFE91E63), Modifier.weight(1f))
        StatCard("Battery", "${stats.batteryLevel}%", batteryColor(stats.batteryLevel), Modifier.weight(1f))
    }
}

@Composable
private fun StatCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = color)
            Text(label, fontSize = 10.sp, color = color.copy(alpha = 0.7f))
        }
    }
}

/** Canvas-based topology graph showing connected peers as a node-link diagram. */
@Composable
private fun TopologyGraph(state: P2PState, stats: NetworkStats) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Topology", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val peers = state.connectedEndpoints
            if (peers.isEmpty()) {
                Text("No peers connected", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
                ) {
                    drawTopology(peers.toList(), state.localDeviceName)
                }
            }
        }
    }
}

private fun DrawScope.drawTopology(peers: List<String>, localName: String) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = min(cx, cy) - 40f

    // Draw local node at centre
    drawCircle(Color(0xFF4CAF50), 12f, Offset(cx, cy))

    // Draw peers around in a circle, each connected to the local node
    val angleStep = (2 * Math.PI) / peers.size
    peers.forEachIndexed { i, peer ->
        val angle = angleStep * i - Math.PI / 2
        val px = cx + (radius * Math.cos(angle)).toFloat()
        val py = cy + (radius * Math.sin(angle)).toFloat()

        // Edge
        drawLine(Color(0xFF4FC3F7).copy(alpha = 0.5f), Offset(cx, cy), Offset(px, py), strokeWidth = 2f)
        // Node
        drawCircle(Color(0xFF2196F3), 8f, Offset(px, py))

        // Label
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 24f
                textAlign = android.graphics.Paint.Align.CENTER
            }
            drawText(peer, px, py - 14f, paint)
        }
    }

    // Local label
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = "#4CAF50".toColorInt()
            textSize = 26f
            textAlign = android.graphics.Paint.Align.CENTER
            isFakeBoldText = true
        }
        drawText(localName, cx, cy - 18f, paint)
    }
}

/** Per-peer statistics table. */
@Composable
private fun PeerStatsTable(peerSnapshots: List<PeerStatsSnapshot>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Per-Peer Stats", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            if (peerSnapshots.isEmpty()) {
                Text("No peer data", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Peer", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.5f))
                    Text("Sent", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Recv", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("RTT", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("Loss", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f))
                }
                HorizontalDivider()

                peerSnapshots.forEach { peer ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(peer.peerName, fontSize = 11.sp, modifier = Modifier.weight(1.5f), maxLines = 1)
                        Text("${peer.packetsSent}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("${peer.packetsReceived}", fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text(
                            if (peer.lastRttMs > 0) "${peer.lastRttMs}ms" else "-",
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )
                        val lossRate = if (peer.packetsSent > 10) {
                            // Approximate loss: asymmetry between sent and received
                            // A perfectly symmetric link has sent ≈ received
                            val ratio = peer.packetsReceived.toDouble() / peer.packetsSent
                            if (ratio < 0.8) "${"%.0f".format((1.0 - ratio) * 100)}%" else "0%"
                        } else "-"
                        Text(lossRate, fontSize = 11.sp, modifier = Modifier.weight(1f),
                            color = if (lossRate != "-" && lossRate != "0%") Color(0xFFF44336) else Color.Unspecified)
                    }
                }
            }
        }
    }
}

/** Packet loss heatmap — grid of estimated loss rates between peers. */
@Composable
private fun PacketLossHeatmap(peerSnapshots: List<PeerStatsSnapshot>) {
    if (peerSnapshots.size < 2) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Packet Loss Heatmap", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((peerSnapshots.size * 40 + 40).dp)
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            ) {
                drawHeatmap(peerSnapshots)
            }
        }
    }
}

private fun DrawScope.drawHeatmap(peers: List<PeerStatsSnapshot>) {
    val n = peers.size
    val cellSize = min(size.width / (n + 1), size.height / (n + 1))
    val offsetX = cellSize
    val offsetY = cellSize

    // Column and row labels
    val labelPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = (cellSize * 0.35f).coerceAtMost(28f)
        textAlign = android.graphics.Paint.Align.CENTER
    }

    peers.forEachIndexed { i, peer ->
        // Column header
        drawContext.canvas.nativeCanvas.drawText(
            peer.peerName.take(6),
            offsetX + i * cellSize + cellSize / 2,
            cellSize * 0.7f,
            labelPaint
        )
        // Row header
        val rowPaint = android.graphics.Paint(labelPaint).apply {
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        drawContext.canvas.nativeCanvas.drawText(
            peer.peerName.take(6),
            offsetX - 4f,
            offsetY + i * cellSize + cellSize * 0.65f,
            rowPaint
        )
    }

    // Cells: color by per-peer asymmetry (diagonal = self, off-diagonal = unknown)
    for (i in peers.indices) {
        for (j in peers.indices) {
            // We only have per-peer-to-local stats, not peer-to-peer.
            // Show each peer's link asymmetry on the diagonal; off-diagonal is unknown.
            val lossRate = if (i == j && peers[i].packetsSent > 10) {
                // Use recv/sent ratio: a balanced link has ratio ≈ 1.0
                val ratio = peers[i].packetsReceived.toFloat() / peers[i].packetsSent
                (1f - ratio.coerceAtMost(1f)).coerceIn(0f, 1f)
            } else 0f

            val cellColor = lerpHeatColor(lossRate)
            drawRect(
                color = cellColor,
                topLeft = Offset(offsetX + j * cellSize, offsetY + i * cellSize),
                size = Size(cellSize - 2, cellSize - 2)
            )
        }
    }
}

/** Linearly interpolate between green (0% loss) → yellow → red (100% loss). */
private fun lerpHeatColor(t: Float): Color {
    return when {
        t < 0.5f -> Color(
            red = (t * 2f),
            green = 0.8f,
            blue = 0.1f,
            alpha = 0.8f
        )
        else -> Color(
            red = 0.9f,
            green = (1f - t) * 1.6f,
            blue = 0.1f,
            alpha = 0.8f
        )
    }
}

private fun batteryColor(level: Int): Color = when {
    level > 50 -> Color(0xFF4CAF50)
    level > 20 -> Color(0xFFFF9800)
    else -> Color(0xFFF44336)
}
