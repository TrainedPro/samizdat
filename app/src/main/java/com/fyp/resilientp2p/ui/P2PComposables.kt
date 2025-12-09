package com.fyp.resilientp2p.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
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

@OptIn(ExperimentalMaterial3Api::class)
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

        // Dialog States
        var showChatDialog by remember { mutableStateOf(false) }
        var chatTargetId by remember { mutableStateOf<String?>(null) }
        var showAdvancedOptions by remember { mutableStateOf(false) }
        var showMenu by remember { mutableStateOf(false) }

        val isConnected = state.connectedEndpoints.isNotEmpty()
        val scrollState = rememberScrollState()

        Scaffold(
                topBar = {
                        CenterAlignedTopAppBar(
                                title = {
                                        Text(
                                                "Resilient Mesh",
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.titleLarge
                                        )
                                },
                                colors =
                                        TopAppBarDefaults.topAppBarColors(
                                                containerColor = colorScheme.primaryContainer,
                                                titleContentColor = colorScheme.onPrimaryContainer
                                        ),
                                actions = {
                                        IconButton(onClick = { showMenu = !showMenu }) {
                                                Icon(
                                                        imageVector = Icons.Default.MoreVert,
                                                        contentDescription = "Options"
                                                )
                                        }
                                        DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                        ) {
                                                DropdownMenuItem(
                                                        text = { Text("Advanced Options") },
                                                        onClick = {
                                                                showMenu = false
                                                                showAdvancedOptions = true
                                                        }
                                                )
                                                DropdownMenuItem(
                                                        text = { Text("Exit App") },
                                                        onClick = {
                                                                showMenu = false
                                                                p2pManager.stop()
                                                                kotlin.system.exitProcess(0)
                                                        }
                                                )
                                        }
                                }
                        )
                }
        ) { innerPadding ->
                Column(
                        modifier =
                                Modifier.fillMaxSize()
                                        .background(colorScheme.background)
                                        .padding(innerPadding)
                                        .padding(16.dp)
                                        .verticalScroll(scrollState)
                ) {
                        // --- Header Section ---
                        DeviceStatusCard(
                                deviceName =
                                        state.localDeviceName, // Use actual local name from state
                                connectionQuality =
                                        if (isConnected) (bandwidthInfo?.quality ?: 0) else 0
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
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(56.dp)
                                                        .padding(end = 8.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                if (state.isAdvertising)
                                                                        colorScheme.error
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
                                        modifier =
                                                Modifier.weight(1f)
                                                        .height(56.dp)
                                                        .padding(start = 8.dp),
                                        colors =
                                                ButtonDefaults.buttonColors(
                                                        containerColor =
                                                                if (state.isDiscovering)
                                                                        colorScheme.error
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
                                                Modifier.fillMaxWidth()
                                                        .padding(top = 12.dp)
                                                        .height(40.dp),
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
        }

        if (showChatDialog && chatTargetId != null) {
                ChatDialog(
                        peerId = chatTargetId!!,
                        onDismiss = { showChatDialog = false },
                        onSend = { msg ->
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

        if (showAdvancedOptions) {
                AdvancedOptionsDialog(
                        state = state,
                        onDismiss = { showAdvancedOptions = false },
                        onSetHybrid = { p2pManager.setHybridMode(it) },
                        onSetManual = { p2pManager.setManualConnection(it) },
                        onSetLowPower = { p2pManager.setLowPower(it) },
                        onSetLogLevel = { p2pManager.setLogLevel(it) }
                )
        }
}

@Composable
fun AdvancedOptionsDialog(
        state: com.fyp.resilientp2p.data.P2PState,
        onDismiss: () -> Unit,
        onSetHybrid: (Boolean) -> Unit,
        onSetManual: (Boolean) -> Unit,
        onSetLowPower: (Boolean) -> Unit,
        onSetLogLevel: (com.fyp.resilientp2p.data.LogLevel) -> Unit
) {
        AlertDialog(
                onDismissRequest = onDismiss,
                title = { Text("Advanced Options") },
                text = {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                // Log Level Dropdown
                                Text(
                                        "Log Level",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary
                                )
                                var expanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                        OutlinedButton(
                                                onClick = { expanded = true },
                                                modifier = Modifier.fillMaxWidth()
                                        ) { Text(state.logLevel.name) }
                                        DropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                        ) {
                                                com.fyp.resilientp2p.data.LogLevel.values()
                                                        .forEach { level ->
                                                                // Filter mostly useful levels for
                                                                // UI: INFO, DEBUG, TRACE
                                                                // Or show all.
                                                                if (level ==
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .INFO ||
                                                                                level ==
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .data
                                                                                                .LogLevel
                                                                                                .DEBUG ||
                                                                                level ==
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .data
                                                                                                .LogLevel
                                                                                                .TRACE
                                                                ) {
                                                                        DropdownMenuItem(
                                                                                text = {
                                                                                        Text(
                                                                                                level.name
                                                                                        )
                                                                                },
                                                                                onClick = {
                                                                                        onSetLogLevel(
                                                                                                level
                                                                                        )
                                                                                        expanded =
                                                                                                false
                                                                                }
                                                                        )
                                                                }
                                                        }
                                        }
                                }
                                Text(
                                        "Select 'TRACE' to see all traffic details.",
                                        style = MaterialTheme.typography.bodySmall
                                )

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Toggles
                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                        Text(
                                                "Hybrid Mode (WiFi+BT)",
                                                modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                                checked = state.isHybridMode,
                                                onCheckedChange = onSetHybrid
                                        )
                                }

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                        Text(
                                                "Manual Connection Confirm",
                                                modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                                checked = state.isManualConnectionEnabled,
                                                onCheckedChange = onSetManual
                                        )
                                }

                                Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                        Text("Low Power Mode", modifier = Modifier.weight(1f))
                                        Switch(
                                                checked = state.isLowPower,
                                                onCheckedChange = onSetLowPower
                                        )
                                }
                        }
                },
                confirmButton = { TextButton(onClick = onDismiss) { Text("CLOSE") } }
        )
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
                                                                        com.fyp.resilientp2p.ui
                                                                                .theme.StatusGreen
                                                                connectionQuality == 2 ->
                                                                        com.fyp.resilientp2p.ui
                                                                                .theme.StatusOrange
                                                                else -> colorScheme.error
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
        logs: List<com.fyp.resilientp2p.data.LogEntry>,
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
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .ui
                                                                                                .theme
                                                                                                .StatusGreen
                                                                                else
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .ui
                                                                                                .theme
                                                                                                .TechTealSecondary
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
fun LogsSection(
        logs: List<com.fyp.resilientp2p.data.LogEntry>,
        onExportLogs: () -> Unit,
        onClearLogs: () -> Unit
) {
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
                                                .background(
                                                        Color.White, // Explicit White as requested
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                        1.dp,
                                                        Color.LightGray,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                        ) {
                                LazyColumn(
                                        reverseLayout = true,
                                        modifier = Modifier.fillMaxSize()
                                ) {
                                        items(logs.reversed()) { entry ->
                                                val color =
                                                        when (entry.logType) {
                                                                com.fyp.resilientp2p.data.LogType
                                                                        .CHAT -> {
                                                                        if (entry.message
                                                                                        .startsWith(
                                                                                                "[SENT]"
                                                                                        )
                                                                        )
                                                                                com.fyp.resilientp2p
                                                                                        .ui.theme
                                                                                        .TechBluePrimary // Blue for Sent
                                                                        else
                                                                                com.fyp.resilientp2p
                                                                                        .ui.theme
                                                                                        .TechTealSecondary // Teal for Received
                                                                }
                                                                else ->
                                                                        when (entry.level) {
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .ERROR ->
                                                                                        colorScheme
                                                                                                .error
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .WARN ->
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .ui
                                                                                                .theme
                                                                                                .StatusOrange
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .INFO ->
                                                                                        Color.Black // Black for System Info on White BG
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .DEBUG ->
                                                                                        com.fyp
                                                                                                .resilientp2p
                                                                                                .ui
                                                                                                .theme
                                                                                                .StatusGray
                                                                                com.fyp.resilientp2p
                                                                                        .data
                                                                                        .LogLevel
                                                                                        .TRACE ->
                                                                                        Color.LightGray
                                                                                else -> Color.Gray
                                                                        }
                                                        }

                                                // Format: HH:MM:SS [LEVEL] Message
                                                // Chat: HH:MM:SS [CHAT] Message
                                                val tag =
                                                        if (entry.logType ==
                                                                        com.fyp.resilientp2p.data
                                                                                .LogType.CHAT
                                                        ) {
                                                                if (entry.peerId != null) "[${entry.peerId}]"
                                                                else "[CHAT]"
                                                        } else "[${entry.level.name}]"

                                                Text(
                                                        text =
                                                                "${entry.formattedTimestamp} $tag ${entry.message}",
                                                        color = color,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 10.sp,
                                                        lineHeight = 12.sp,
                                                        modifier = Modifier.padding(vertical = 1.dp)
                                                )
                                        }
                                }
                        }
                }
        }
}
