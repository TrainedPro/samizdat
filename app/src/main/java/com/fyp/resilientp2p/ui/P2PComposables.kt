package com.fyp.resilientp2p.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.UwbManager
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ResilientP2PApp(p2pManager: P2PManager, uwbManager: UwbManager, onExportLogs: () -> Unit) {
        val state by p2pManager.state.collectAsState()
        val uwbState by uwbManager.state.collectAsState()

        // Derived State
        val transferProgressEvent by p2pManager.payloadProgressEvents.collectAsState(initial = null)
        val bandwidthInfo by p2pManager.bandwidthEvents.collectAsState(initial = null)
        val latestPayload by p2pManager.payloadEvents.collectAsState(initial = null)

        // Trace logic
        var tracePath by remember { mutableStateOf("") }
        LaunchedEffect(latestPayload) {
                latestPayload?.let { event ->
                        if (event.packet.trace.isNotEmpty()) {
                                tracePath = event.packet.trace.joinToString(" -> ") { it.peerId }
                        }
                }
        }

        // Chat State
        var showChatDialog by remember { mutableStateOf(false) }
        var chatTargetId by remember { mutableStateOf<String?>(null) }

        val isConnected = state.connectedEndpoints.isNotEmpty()
        val scrollState = rememberScrollState()

        // UWB Data Extraction
        val activePeerId = state.connectedEndpoints.firstOrNull()
        val realAzimuth = activePeerId?.let { uwbState.azimuths[it] }
        val realDistance = activePeerId?.let { uwbState.peers[it] }

        Column(
                modifier =
                        Modifier.fillMaxSize()
                                .background(colorScheme.background)
                                .statusBarsPadding()
                                .padding(16.dp)
                                .verticalScroll(scrollState)
        ) {
                // --- Header Section ---
                DeviceStatusCard(
                        deviceName = p2pManager.getLocalDeviceName(),
                        connectionQuality = if (isConnected) (bandwidthInfo?.quality ?: 0) else 0
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Controls Section ---
                Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                ) {
                        // Master Mesh Button
                        Button(
                                onClick = {
                                        if (state.isDutyCycleActive) {
                                                p2pManager.setDutyCycle(false)
                                        } else {
                                                p2pManager.setDutyCycle(true)
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor =
                                                        if (state.isDutyCycleActive)
                                                                colorScheme.error
                                                        else colorScheme.primary
                                        ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                                Text(
                                        text =
                                                if (state.isDutyCycleActive) "STOP MESH NETWORK"
                                                else "START MESH NETWORK",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                )
                        }
                }

                // Stop All Button (Conditional)
                val isBusy =
                        state.isAdvertising ||
                                state.isDiscovering ||
                                state.connectedEndpoints.isNotEmpty()
                AnimatedVisibility(visible = isBusy) {
                        Button(
                                onClick = { p2pManager.stopAll() },
                                modifier =
                                        Modifier.fillMaxWidth().padding(top = 12.dp).height(40.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor = colorScheme.errorContainer
                                        ),
                                shape = RoundedCornerShape(8.dp)
                        ) {
                                Text(
                                        "STOP ALL ACTIVITY",
                                        fontSize = 12.sp,
                                        color = colorScheme.onErrorContainer
                                )
                        }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- Dashboard & Logs ---
                DashboardContent(
                        transferProgress = transferProgressEvent?.progress ?: 0,
                        tracePath = tracePath,
                        radarAzimuth = realAzimuth,
                        radarDistance = realDistance,
                        logs = state.logs,
                        onExportLogs = onExportLogs,
                        onClearLogs = { p2pManager.clearLogs() },
                        isUwbSupported = state.isUwbSupported, // Pass UWB support flag
                        isUwbPermissionGranted =
                                state.isUwbPermissionGranted, // Pass UWB permission flag
                        knownPeers = state.knownPeers,
                        connectedEndpoints = state.connectedEndpoints,
                        onPeerClick = { peerId ->
                                chatTargetId = peerId
                                showChatDialog = true
                        }
                )
        }

        if (showChatDialog && chatTargetId != null) {
                ChatDialog(
                        peerId = chatTargetId!!,
                        onDismiss = { showChatDialog = false },
                        onSend = { msg -> p2pManager.sendData(chatTargetId!!, msg.toByteArray()) },
                        onPing = {
                                val timestamp = System.currentTimeMillis()
                                val buffer = java.nio.ByteBuffer.allocate(8)
                                buffer.putLong(timestamp)
                                p2pManager.sendPing(chatTargetId!!, buffer.array())
                        }
                )
        }
}

@Composable
fun DeviceStatusCard(deviceName: String, connectionQuality: Int) {
        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Device Name",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary
                        )
                        Text(
                                text = deviceName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                        text = "Signal Quality",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(end = 12.dp)
                                )
                                // Signal Meter
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        repeat(3) { index ->
                                                val active = index < connectionQuality
                                                val color =
                                                        when {
                                                                !active ->
                                                                        colorScheme.outlineVariant
                                                                connectionQuality == 3 ->
                                                                        colorScheme.secondary
                                                                connectionQuality == 2 ->
                                                                        Color.Yellow
                                                                else -> colorScheme.tertiary
                                                        }
                                                Box(
                                                        modifier =
                                                                Modifier.width(20.dp)
                                                                        .height(8.dp)
                                                                        .clip(
                                                                                RoundedCornerShape(
                                                                                        4.dp
                                                                                )
                                                                        )
                                                                        .background(color)
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun DashboardContent(
        transferProgress: Int,
        tracePath: String,
        radarAzimuth: Float?,
        radarDistance: Float?,
        logs: List<String>,
        onExportLogs: () -> Unit,
        onClearLogs: () -> Unit,
        isUwbSupported: Boolean,
        isUwbPermissionGranted: Boolean,
        knownPeers: Map<String, P2PManager.RouteInfo>,
        connectedEndpoints: Set<String>,
        onPeerClick: (String) -> Unit
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                // --- Radar Section ---
                P2PDashboardRadar(
                        transferProgress = transferProgress,
                        tracePath = tracePath,
                        radarAzimuth = radarAzimuth,
                        radarDistance = radarDistance,
                        isUwbSupported = isUwbSupported,
                        isUwbPermissionGranted = isUwbPermissionGranted
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Mesh Contacts Section ---
                MeshContactsSection(
                        knownPeers = knownPeers,
                        connectedEndpoints = connectedEndpoints,
                        onPeerClick = onPeerClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Logs Section ---
                LogsSection(logs, onExportLogs, onClearLogs)

                // Extra space at bottom to ensure easy scrolling
                Spacer(modifier = Modifier.height(32.dp))
        }
}

@Composable
fun MeshContactsSection(
        knownPeers: Map<String, P2PManager.RouteInfo>,
        connectedEndpoints: Set<String>,
        onPeerClick: (String) -> Unit
) {
        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                                text = "Mesh Contacts",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (knownPeers.isEmpty() && connectedEndpoints.isEmpty()) {
                                Text(
                                        text = "No peers found yet. Waiting for mesh...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                        } else {
                                // Combine direct and routed peers
                                val allPeers = (connectedEndpoints + knownPeers.keys).toSet()

                                allPeers.forEach { peerId ->
                                        val isDirect = connectedEndpoints.contains(peerId)
                                        val route = knownPeers[peerId]

                                        Row(
                                                modifier =
                                                        Modifier.fillMaxWidth()
                                                                .clickable { onPeerClick(peerId) }
                                                                .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                // Status Dot
                                                Box(
                                                        modifier =
                                                                Modifier.size(10.dp)
                                                                        .clip(
                                                                                androidx.compose
                                                                                        .foundation
                                                                                        .shape
                                                                                        .CircleShape
                                                                        )
                                                                        .background(
                                                                                if (isDirect)
                                                                                        Color.Green
                                                                                else Color.Cyan
                                                                        )
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))

                                                Column {
                                                        Text(
                                                                text = peerId,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colorScheme.onSurface
                                                        )
                                                        val status =
                                                                if (isDirect) "Direct Connection"
                                                                else
                                                                        "Via ${route?.nextHop} (${route?.hopCount} hops)"
                                                        Text(
                                                                text = status,
                                                                style =
                                                                        MaterialTheme.typography
                                                                                .bodySmall,
                                                                color = colorScheme.onSurfaceVariant
                                                        )
                                                }
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                        "Chat",
                                                        color = colorScheme.primary,
                                                        fontWeight = FontWeight.Bold
                                                )
                                        }
                                        Divider(
                                                color =
                                                        colorScheme.outlineVariant.copy(
                                                                alpha = 0.5f
                                                        )
                                        )
                                }
                        }
                }
        }
}

@Composable
fun ChatDialog(
        peerId: String,
        onDismiss: () -> Unit,
        onSend: (String) -> Unit,
        onPing: () -> Unit
) {
        var message by remember { mutableStateOf("") }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text(text = "Chat with $peerId") },
                text = {
                        Column {
                                OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        label = { Text("Message") },
                                        modifier = Modifier.fillMaxWidth()
                                )
                        }
                },
                confirmButton = {
                        Row {
                                TextButton(onClick = onPing) { Text("Ping") }
                                Spacer(modifier = Modifier.width(8.dp))
                                Button(
                                        onClick = {
                                                if (message.isNotBlank()) {
                                                        onSend(message)
                                                        onDismiss()
                                                }
                                        }
                                ) { Text("Send") }
                        }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
        )
}

@Composable
fun P2PDashboardRadar(
        transferProgress: Int,
        tracePath: String,
        radarAzimuth: Float?,
        radarDistance: Float?,
        isUwbSupported: Boolean,
        isUwbPermissionGranted: Boolean
) {
        val showRadar = isUwbSupported && isUwbPermissionGranted
        val showTransfer = transferProgress > 0 && transferProgress < 100

        // If neither Radar nor File Transfer is active, hide the entire card
        if (!showRadar && !showTransfer) return

        var isRadarExpanded by remember { mutableStateOf(false) }
        val radarHeight by
                animateDpAsState(
                        targetValue = if (isRadarExpanded) 350.dp else 220.dp,
                        label = "radarHeight"
                )

        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text = "Live Dashboard",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        // Pulse
                                        val infiniteTransition =
                                                rememberInfiniteTransition(label = "pulse")
                                        val alpha by
                                                infiniteTransition.animateFloat(
                                                        initialValue = 0.2f,
                                                        targetValue = 1f,
                                                        animationSpec =
                                                                infiniteRepeatable(
                                                                        animation = tween(1000),
                                                                        repeatMode =
                                                                                RepeatMode.Reverse
                                                                ),
                                                        label = "alpha"
                                                )
                                        Box(
                                                modifier =
                                                        Modifier.size(8.dp)
                                                                .background(
                                                                        colorScheme.secondary.copy(
                                                                                alpha
                                                                        ),
                                                                        androidx.compose.foundation
                                                                                .shape.CircleShape
                                                                )
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // File Transfer Indicator
                        if (showTransfer) {
                                Text(
                                        "Transferring File: $transferProgress%",
                                        color = colorScheme.onSurface,
                                        fontSize = 12.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                        progress = { transferProgress / 100f },
                                        modifier = Modifier.fillMaxWidth().height(4.dp),
                                        color = colorScheme.primary,
                                        trackColor = colorScheme.outlineVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Radar Section (Only show if UWB is supported AND granted)
                        if (showRadar) {
                                // Radar Header
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        Text(
                                                "PROXIMITY RADAR",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                                text =
                                                        if (isRadarExpanded) "COLLAPSE"
                                                        else "EXPAND",
                                                color = colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier =
                                                        Modifier.clickable {
                                                                isRadarExpanded = !isRadarExpanded
                                                        }
                                        )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Radar View
                                Box(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(radarHeight)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                                colorScheme.onSurfaceVariant.copy(
                                                                        alpha = 0.1f
                                                                )
                                                        )
                                                        .clickable {
                                                                isRadarExpanded = !isRadarExpanded
                                                        },
                                        contentAlignment = Alignment.Center
                                ) { RadarView(azimuth = radarAzimuth, distance = radarDistance) }
                        }
                }
        }
}

@Composable
fun LogsSection(logs: List<String>, onExportLogs: () -> Unit, onClearLogs: () -> Unit) {
        var isLogsExpanded by remember { mutableStateOf(false) }

        val logsHeight by
                animateDpAsState(
                        targetValue = if (isLogsExpanded) 500.dp else 150.dp,
                        label = "logsHeight"
                )

        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Text(
                                        text = "Live Logs",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text = "EXPORT",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier =
                                                        Modifier.clickable { onExportLogs() }
                                                                .padding(end = 12.dp)
                                        )
                                        Text(
                                                text = "CLEAR",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier =
                                                        Modifier.clickable { onClearLogs() }
                                                                .padding(end = 12.dp)
                                        )
                                        Text(
                                                text = if (isLogsExpanded) "COLLAPSE" else "EXPAND",
                                                color = colorScheme.primary,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier =
                                                        Modifier.clickable {
                                                                isLogsExpanded = !isLogsExpanded
                                                        }
                                        )
                                }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Log Console
                        Box(
                                modifier =
                                        Modifier.fillMaxWidth()
                                                .height(logsHeight)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(
                                                        colorScheme.onSurfaceVariant.copy(
                                                                alpha = 0.1f
                                                        )
                                                )
                                                .padding(8.dp)
                        ) {
                                val listState = rememberLazyListState()

                                LaunchedEffect(logs.size) {
                                        if (logs.isNotEmpty()) {
                                                listState.animateScrollToItem(logs.size - 1)
                                        }
                                }

                                LazyColumn(state = listState) {
                                        items(logs) { log ->
                                                Text(
                                                        text = "> $log",
                                                        color = colorScheme.secondary,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        lineHeight = 14.sp
                                                )
                                        }
                                }
                        }
                }
        }
}

@Composable
fun RadarView(azimuth: Float?, distance: Float?) {
        // Scanning Animation
        val infiniteTransition = rememberInfiniteTransition(label = "radarScan")
        val rotation by
                infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec =
                                infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
                        label = "rotation"
                )

        // Capture colors from Composable scope
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val maxRadius = size.minDimension / 2 - 20f

                // Draw Radar Grid Circles
                val circleColor = secondaryColor.copy(alpha = 0.15f)
                drawCircle(
                        color = circleColor,
                        radius = maxRadius,
                        center = center,
                        style = Stroke(width = 2f)
                )
                drawCircle(
                        color = circleColor,
                        radius = maxRadius * 0.66f,
                        center = center,
                        style = Stroke(width = 1f)
                )
                drawCircle(
                        color = circleColor,
                        radius = maxRadius * 0.33f,
                        center = center,
                        style = Stroke(width = 1f)
                )

                // Draw Crosshairs
                drawLine(
                        color = circleColor,
                        start = Offset(center.x, center.y - maxRadius),
                        end = Offset(center.x, center.y + maxRadius),
                        strokeWidth = 1f
                )
                drawLine(
                        color = circleColor,
                        start = Offset(center.x - maxRadius, center.y),
                        end = Offset(center.x + maxRadius, center.y),
                        strokeWidth = 1f
                )

                // Draw Scanning Sweep
                rotate(rotation, center) {
                        drawCircle(
                                brush =
                                        Brush.sweepGradient(
                                                colors =
                                                        listOf(
                                                                Color.Transparent,
                                                                secondaryColor.copy(alpha = 0.05f),
                                                                secondaryColor.copy(alpha = 0.2f)
                                                        ),
                                                center = center
                                        ),
                                radius = maxRadius,
                                center = center
                        )
                }

                // Draw Peer (if available)
                if (azimuth != null && distance != null) {
                        val angleRad = Math.toRadians((azimuth - 90).toDouble())

                        val maxRange = 20f
                        val normalizedDist = (distance / maxRange).coerceIn(0f, 1f)
                        val radius = normalizedDist * maxRadius

                        val peerX = center.x + (radius * cos(angleRad)).toFloat()
                        val peerY = center.y + (radius * sin(angleRad)).toFloat()

                        // Draw target glow
                        drawCircle(
                                color = tertiaryColor.copy(alpha = 0.3f),
                                radius = 15f,
                                center = Offset(peerX, peerY)
                        )
                        drawCircle(
                                color = tertiaryColor,
                                radius = 6f,
                                center = Offset(peerX, peerY)
                        )

                        // Draw line to peer
                        drawLine(
                                color = tertiaryColor.copy(alpha = 0.4f),
                                start = center,
                                end = Offset(peerX, peerY),
                                strokeWidth = 2f
                        )
                }
        }
}
