package com.fyp.resilientp2p.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.managers.LocationEstimator
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Radar-style composable that visualises nearby mesh peers on a 2D plane.
 *
 * Uses [LocationEstimator] RTT-derived positions. Each peer is drawn as a dot
 * with a label; the local device sits at the centre. Concentric range rings
 * show distance gradients.
 *
 * @param locationEstimator The trilateration engine providing peer positions and RTTs.
 * @param localPeerName The name of this device (shown at centre).
 * @param modifier Standard Compose modifier.
 */
@Composable
fun RadarView(
    locationEstimator: LocationEstimator,
    localPeerName: String,
    modifier: Modifier = Modifier
) {
    val positions = remember { mutableStateOf(emptyMap<String, LocationEstimator.Position>()) }
    val rtts = remember { mutableStateOf(emptyMap<String, Double>()) }
    val localPos = remember { mutableStateOf<LocationEstimator.Position?>(null) }

    // Refresh positions every second
    LaunchedEffect(Unit) {
        while (true) {
            positions.value = locationEstimator.getAllPositions()
            rtts.value = locationEstimator.getAllRtts()
            localPos.value = locationEstimator.localPosition
            kotlinx.coroutines.delay(1000)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Mesh Radar",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            // RTT legend
            val rttMap = rtts.value
            if (rttMap.isNotEmpty()) {
                Text(
                    text = "Peers: ${rttMap.size} | Avg RTT: ${"%.1f".format(rttMap.values.average())} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
            }

            // Radar canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color(0xFF1A1A2E), RoundedCornerShape(8.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    drawRadar(
                        positions = positions.value,
                        localPosition = localPos.value,
                        localPeerName = localPeerName,
                        rttMap = rttMap
                    )
                }
            }

            // Peer list with RTTs
            if (rttMap.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                rttMap.forEach { (peer, rtt) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = peer,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${"%.1f".format(rtt)} ms",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF4FC3F7)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draw the radar: concentric rings, centre dot, peer dots with labels.
 */
private fun DrawScope.drawRadar(
    positions: Map<String, LocationEstimator.Position>,
    localPosition: LocationEstimator.Position?,
    localPeerName: String,
    rttMap: Map<String, Double>
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val maxRadius = min(cx, cy) - 20f

    val ringColor = Color(0xFF2E7D32).copy(alpha = 0.3f)
    val ringStroke = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f)))

    // Draw 4 concentric range rings
    for (i in 1..4) {
        val r = maxRadius * i / 4f
        drawCircle(
            color = ringColor,
            radius = r,
            center = Offset(cx, cy),
            style = ringStroke
        )
    }

    // Draw cross-hair
    drawLine(ringColor, Offset(cx - maxRadius, cy), Offset(cx + maxRadius, cy), strokeWidth = 0.5f)
    drawLine(ringColor, Offset(cx, cy - maxRadius), Offset(cx, cy + maxRadius), strokeWidth = 0.5f)

    // Draw local device at centre
    drawCircle(Color(0xFF4CAF50), radius = 8f, center = Offset(cx, cy))

    // Draw label for local device
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 28f
            textAlign = android.graphics.Paint.Align.CENTER
        }
        drawText(localPeerName, cx, cy - 15f, paint)
    }

    // If we have trilaterated positions, plot them relative to local pos
    val lp = localPosition
    if (lp != null && positions.isNotEmpty()) {
        // Find max distance to scale the radar
        val maxDist = positions.values.maxOfOrNull { lp.distanceTo(it) }?.coerceAtLeast(1.0) ?: 1.0

        positions.forEach { (peer, pos) ->
            val dx = (pos.x - lp.x) / maxDist
            val dy = (pos.y - lp.y) / maxDist
            val screenX = cx + (dx * maxRadius).toFloat()
            val screenY = cy + (dy * maxRadius).toFloat()

            // Peer dot
            drawCircle(Color(0xFF2196F3), radius = 6f, center = Offset(screenX, screenY))

            // Peer label
            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#64B5F6")
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                val rtt = rttMap[peer]
                val label = if (rtt != null) "$peer (${"%.0f".format(rtt)}ms)" else peer
                drawText(label, screenX, screenY - 12f, paint)
            }
        }
    } else if (rttMap.isNotEmpty()) {
        // No trilateration yet — place peers at angular intervals based on RTT (as distance proxy)
        val maxRtt = rttMap.values.maxOrNull()?.coerceAtLeast(1.0) ?: 1.0
        val angleDelta = (2 * Math.PI) / rttMap.size

        rttMap.entries.forEachIndexed { index, (peer, rtt) ->
            val angle = angleDelta * index - Math.PI / 2
            val normalizedDist = (rtt / maxRtt).coerceIn(0.1, 0.95)
            val screenX = cx + (normalizedDist * maxRadius * Math.cos(angle)).toFloat()
            val screenY = cy + (normalizedDist * maxRadius * Math.sin(angle)).toFloat()

            drawCircle(Color(0xFF2196F3), radius = 6f, center = Offset(screenX, screenY))

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#64B5F6")
                    textSize = 24f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
                drawText("$peer (${"%.0f".format(rtt)}ms)", screenX, screenY - 12f, paint)
            }
        }
    }
}
