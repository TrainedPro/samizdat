package com.fyp.resilientp2p.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun P2PDashboard(
        transferProgress: Int,
        tracePath: String,
        radarAzimuth: Float?,
        radarDistance: Float?
) {
    Column(
            modifier =
                    Modifier.fillMaxWidth()
                            .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
    ) {
        Text(
                text = "Live Dashboard",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
        )

        // 1. File Transfer Progress
        if (transferProgress > 0 && transferProgress < 100) {
            Text(
                    text = "Transferring File...",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
            )
            LinearProgressIndicator(
                    progress = { transferProgress / 100f },
                    modifier = Modifier.fillMaxWidth().height(8.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Trace Visualization
        if (tracePath.isNotEmpty()) {
            Text(
                    text = "Last Packet Path:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray
            )
            Text(
                    text = "$tracePath -> Me",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. UWB Radar View
        Text(
                text = "Proximity Radar",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
        )
        Box(
                modifier =
                        Modifier.fillMaxWidth()
                                .height(200.dp)
                                .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
        ) { RadarView(azimuth = radarAzimuth, distance = radarDistance) }
    }
}

@Composable
fun RadarView(azimuth: Float?, distance: Float?) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.minDimension / 2 - 20f

        // Draw Radar Circles
        drawCircle(
                color = Color.Green.copy(alpha = 0.3f),
                radius = maxRadius,
                center = center,
                style = Stroke(width = 2f)
        )
        drawCircle(
                color = Color.Green.copy(alpha = 0.2f),
                radius = maxRadius * 0.66f,
                center = center,
                style = Stroke(width = 2f)
        )
        drawCircle(
                color = Color.Green.copy(alpha = 0.1f),
                radius = maxRadius * 0.33f,
                center = center,
                style = Stroke(width = 2f)
        )

        // Draw Crosshairs
        drawLine(
                color = Color.Green.copy(alpha = 0.2f),
                start = Offset(center.x, center.y - maxRadius),
                end = Offset(center.x, center.y + maxRadius),
                strokeWidth = 1f
        )
        drawLine(
                color = Color.Green.copy(alpha = 0.2f),
                start = Offset(center.x - maxRadius, center.y),
                end = Offset(center.x + maxRadius, center.y),
                strokeWidth = 1f
        )

        // Draw Peer (if available)
        if (azimuth != null && distance != null) {
            // Convert azimuth (degrees) and distance to coordinates
            // 0 degrees is usually North (Up). In Canvas, 0 is Right.
            // Adjust: -90 degrees to make 0 be Up.
            val angleRad = Math.toRadians((azimuth - 90).toDouble())

            // Map distance to radius (e.g., max 10m)
            val maxDistance = 10f
            val normalizedDist = (distance / maxDistance).coerceIn(0f, 1f)
            val radius = normalizedDist * maxRadius

            val peerX = center.x + (radius * cos(angleRad)).toFloat()
            val peerY = center.y + (radius * sin(angleRad)).toFloat()

            drawCircle(color = Color.Red, radius = 10f, center = Offset(peerX, peerY))

            // Draw line to peer
            drawLine(
                    color = Color.Red.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(peerX, peerY),
                    strokeWidth = 2f
            )
        }
    }
}
