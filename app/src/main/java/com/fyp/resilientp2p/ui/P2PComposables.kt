package com.fyp.resilientp2p.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.data.RouteInfo
import com.fyp.resilientp2p.managers.P2PManager

@Composable
fun ResilientP2PApp(p2pManager: P2PManager, onExportLogs: () -> Unit) {
        val state by p2pManager.state.collectAsState()

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

        val activePeerId = state.connectedEndpoints.firstOrNull()

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
                        // Advertising Button
                        Button(
                                onClick = {
                                        if (state.isAdvertising) {
                                                p2pManager.stopAdvertising()
                                        } else {
                                                p2pManager.startAdvertising()
                                        }
                                },
                                modifier = Modifier.weight(1f).height(56.dp).padding(end = 8.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor =
                                                        if (state.isAdvertising) colorScheme.error
                                                        else colorScheme.primary
                                        ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                                Text(
                                        text =
                                                if (state.isAdvertising) "Stop Adv."
                                                else "Start Adv.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                )
                        }

                        // Discovery Button
                        Button(
                                onClick = {
                                        if (state.isDiscovering) {
                                                p2pManager.stopDiscovery()
                                        } else {
                                                p2pManager.startDiscovery()
                                        }
                                },
                                modifier = Modifier.weight(1f).height(56.dp).padding(start = 8.dp),
                                colors =
                                        ButtonDefaults.buttonColors(
                                                containerColor =
                                                        if (state.isDiscovering) colorScheme.error
                                                        else colorScheme.secondary
                                        ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                                Text(
                                        text =
                                                if (state.isDiscovering) "Stop Disc."
                                                else "Start Disc.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
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
                        logs = state.logs,
                        onExportLogs = onExportLogs,
                        onClearLogs = { p2pManager.clearLogs() },
                        knownPeers = state.knownPeers,
                        connectedEndpoints = state.connectedEndpoints.toSet(),
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
                        onSend = { msg ->
                                android.util.Log.d(
                                        "P2PUI",
                                        "ChatDialog: onSend clicked with message: $msg, target: $chatTargetId"
                                )
                                if (chatTargetId == "BROADCAST") {
                                        p2pManager.broadcastMessage(msg)
                                } else {
                                        p2pManager.sendData(chatTargetId!!, msg)
                                }
                        },
                        onPing = {
                                val timestamp = System.currentTimeMillis()
                                val buffer = java.nio.ByteBuffer.allocate(8)
                                buffer.putLong(timestamp)
                                p2pManager.sendPing(chatTargetId!!, buffer.array())
                        },
                        onStartAudio = { p2pManager.startAudioStreaming(chatTargetId!!) },
                        onStopAudio = { p2pManager.stopAudioStreaming() }
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
                                text = "My Device ID",
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
        logs: List<String>,
        onExportLogs: () -> Unit,
        onClearLogs: () -> Unit,
        knownPeers: Map<String, RouteInfo>,
        connectedEndpoints: Set<String>,
        onPeerClick: (String) -> Unit
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                // --- Radar Section ---

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
        knownPeers: Map<String, RouteInfo>,
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

                        // Broadcast Button
                        if (connectedEndpoints.isNotEmpty() || knownPeers.isNotEmpty()) {
                                Button(
                                        onClick = { onPeerClick("BROADCAST") },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor = colorScheme.secondary
                                                )
                                ) { Text("BROADCAST MESSAGE", fontWeight = FontWeight.Bold) }
                        }

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
                                        HorizontalDivider(
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
        onPing: () -> Unit,
        onStartAudio: () -> Unit,
        onStopAudio: () -> Unit
) {
        var message by remember { mutableStateOf("") }
        val context = androidx.compose.ui.platform.LocalContext.current
        val isBroadcast = peerId == "BROADCAST"

        val permissionLauncher =
                rememberLauncherForActivityResult(
                        androidx.activity.result.contract.ActivityResultContracts
                                .RequestPermission()
                ) { isGranted ->
                        if (isGranted) {
                                android.widget.Toast.makeText(
                                                context,
                                                "Permission granted. Hold to talk.",
                                                android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                        } else {
                                android.widget.Toast.makeText(
                                                context,
                                                "Audio permission required",
                                                android.widget.Toast.LENGTH_SHORT
                                        )
                                        .show()
                        }
                }

        AlertDialog(
                onDismissRequest = onDismiss,
                title = {
                        Text(
                                text =
                                        if (isBroadcast) "Broadcast Message"
                                        else "Chat with $peerId",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color =
                                        if (isBroadcast) MaterialTheme.colorScheme.secondary
                                        else MaterialTheme.colorScheme.primary
                        )
                },
                text = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                                // Status Text
                                Text(
                                        text =
                                                if (isBroadcast) "Sending to ALL connected peers."
                                                else "Direct secure channel.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                )

                                OutlinedTextField(
                                        value = message,
                                        onValueChange = { message = it },
                                        label = { Text("Message") },
                                        modifier = Modifier.fillMaxWidth(),
                                        maxLines = 3
                                )
                        }
                },
                confirmButton = {
                        Button(
                                onClick = {
                                        if (message.isNotBlank()) {
                                                onSend(message)
                                                onDismiss()
                                        }
                                },
                                enabled = message.isNotBlank()
                        ) { Text("SEND") }
                },
                dismissButton = {
                        Row {
                                // Push-to-Talk Button
                                val interactionSource = remember {
                                        androidx.compose.foundation.interaction
                                                .MutableInteractionSource()
                                }
                                val isPressed by interactionSource.collectIsPressedAsState()

                                LaunchedEffect(isPressed) {
                                        if (isPressed) {
                                                if (androidx.core.content.ContextCompat
                                                                .checkSelfPermission(
                                                                        context,
                                                                        android.Manifest.permission
                                                                                .RECORD_AUDIO
                                                                ) ==
                                                                android.content.pm.PackageManager
                                                                        .PERMISSION_GRANTED
                                                ) {
                                                        onStartAudio()
                                                } else {
                                                        permissionLauncher.launch(
                                                                android.Manifest.permission
                                                                        .RECORD_AUDIO
                                                        )
                                                }
                                        } else {
                                                onStopAudio()
                                        }
                                }

                                Button(
                                        onClick = {},
                                        interactionSource = interactionSource,
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                if (isPressed) Color.Red
                                                                else
                                                                        MaterialTheme.colorScheme
                                                                                .tertiary
                                                )
                                ) { Text(if (isPressed) "🎤" else "PTT") }

                                Spacer(modifier = Modifier.width(8.dp))

                                TextButton(onClick = onPing) { Text("PING") }

                                TextButton(onClick = onDismiss) { Text("CLOSE") }
                        }
                }
        )
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
