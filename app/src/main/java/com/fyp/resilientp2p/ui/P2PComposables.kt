package com.fyp.resilientp2p.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sos
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.BuildConfig
import com.fyp.resilientp2p.data.ChatDao
import com.fyp.resilientp2p.data.ChatMessage
import com.fyp.resilientp2p.data.MessageType
import com.fyp.resilientp2p.data.RouteInfo
import com.fyp.resilientp2p.data.PeerStatsSnapshot
import com.fyp.resilientp2p.data.NetworkStatsSnapshot
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.managers.TelemetryManager
import com.fyp.resilientp2p.managers.TelemetryStatus
import com.fyp.resilientp2p.testing.EnduranceTestRunner
import com.fyp.resilientp2p.testing.TestRunner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Root composable for the Samizdat meshnetwork UI.
 *
 * Renders the full-screen Scaffold including:
 * - Connection status header (advertising/discovering toggles)
 * - Gateway status card (green) when internet is available
 * - Emergency alerts banner (red) with last 3 alerts and SOS toggle
 * - Peer list with chat, file-send, and voice-call actions
 * - Network stats dashboard
 * - Log viewer with level filter and export
 * - Test mode entry point
 *
 * @param p2pManager Core mesh engine providing [P2PState] via StateFlow.
 * @param onExportLogs Callback to trigger CSV log export from the host Activity.
 * @param testRunner Optional automated test suite runner.
 * @param chatDao Optional Room DAO for chat message persistence.
 * @param telemetryManager Optional cloud telemetry manager (controls upload toggle).
 * @param emergencyManager Optional emergency broadcast manager (SOS + alerts).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Suppress("CyclomaticComplexMethod", "LongMethod")
@Composable
fun ResilientP2PApp(
    p2pManager: P2PManager,
    onExportLogs: () -> Unit,
    testRunner: TestRunner? = null,
    enduranceTestRunner: EnduranceTestRunner? = null,
    chatDao: ChatDao? = null,
    telemetryManager: TelemetryManager? = null,
    emergencyManager: com.fyp.resilientp2p.managers.EmergencyManager? = null,
    chatGroupDao: com.fyp.resilientp2p.data.ChatGroupDao? = null,
    groupMessageDao: com.fyp.resilientp2p.data.GroupMessageDao? = null,
    locationEstimator: com.fyp.resilientp2p.managers.LocationEstimator? = null
) {
        val state by p2pManager.state.collectAsState()
        val context = androidx.compose.ui.platform.LocalContext.current

        // Test mode state
        var showTestMode by rememberSaveable { mutableStateOf(false) }
        // Phase 4 screen states
        var showHealthDashboard by rememberSaveable { mutableStateOf(false) }
        var showGroupChat by rememberSaveable { mutableStateOf(false) }

        // Auto-launch test mode when compiled with TEST_MODE=true
        LaunchedEffect(Unit) {
                if (BuildConfig.TEST_MODE && testRunner != null) {
                        showTestMode = true
                        testRunner.runTests(autoStart = true)
                }
        }

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

        // Endurance test state (for persistent indicator on main screen)
        val enduranceState = enduranceTestRunner?.state?.collectAsState()
        val isEnduranceRunning = enduranceState?.value?.isRunning == true

        // Received file notifications
        val receivedFile by p2pManager.receivedFileEvents.collectAsState(initial = null)
        LaunchedEffect(receivedFile) {
                receivedFile?.let { event ->
                        android.widget.Toast.makeText(
                                context,
                                "File from ${event.senderName}: ${event.fileName}",
                                android.widget.Toast.LENGTH_LONG
                        ).show()
                }
        }

        // Incoming text message notification (only when chat is not open for that peer)
        LaunchedEffect(latestPayload) {
                latestPayload?.let { event ->
                        val pkt = event.packet
                        if (pkt.type == com.fyp.resilientp2p.transport.PacketType.DATA &&
                                pkt.sourceId != state.localDeviceName
                        ) {
                                val text = String(pkt.payload, java.nio.charset.StandardCharsets.UTF_8)
                                if (!text.startsWith("__TEST__") && !text.startsWith("__ENDURANCE__")) {
                                        val isBroadcast = pkt.destId == "BROADCAST"
                                        val chatIsOpen = showChatDialog && chatTargetId == pkt.sourceId ||
                                                showGroupChat && isBroadcast
                                        if (!chatIsOpen) {
                                                val preview = text.take(40).let {
                                                        if (text.length > 40) "$it..." else it
                                                }
                                                val label = if (isBroadcast) "[Broadcast]" else pkt.sourceId
                                                android.widget.Toast.makeText(
                                                        context,
                                                        "$label: $preview",
                                                        android.widget.Toast.LENGTH_SHORT
                                                ).show()
                                        }
                                }
                        }
                }
        }

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
                                                        leadingIcon = {
                                                                Icon(
                                                                        Icons.Default.BarChart,
                                                                        contentDescription = null,
                                                                        modifier = Modifier.size(18.dp)
                                                                )
                                                        },
                                                        text = { Text("Health Dashboard") },
                                                        onClick = {
                                                                showMenu = false
                                                                showHealthDashboard = true
                                                        }
                                                )
                                                if (chatGroupDao != null && groupMessageDao != null) {
                                                        DropdownMenuItem(
                                                                leadingIcon = {
                                                                        Icon(
                                                                                Icons.Default.Forum,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(18.dp)
                                                                        )
                                                                },
                                                                text = { Text("Group Chat") },
                                                                onClick = {
                                                                        showMenu = false
                                                                        showGroupChat = true
                                                                }
                                                        )
                                                }
                                                if (testRunner != null) {
                                                        DropdownMenuItem(
                                                                leadingIcon = {
                                                                        Icon(
                                                                                Icons.Default.Science,
                                                                                contentDescription = null,
                                                                                modifier = Modifier.size(18.dp)
                                                                        )
                                                                },
                                                                text = { Text("Run Tests") },
                                                                onClick = {
                                                                        showMenu = false
                                                                        showTestMode = true
                                                                }
                                                        )
                                                }
                                                DropdownMenuItem(
                                                        text = { Text("Exit App") },
                                                        onClick = {
                                                                showMenu = false
                                                                // Delegate to Activity's graceful shutdown
                                                                // instead of calling stop() + finishAffinity() directly
                                                                (context as? com.fyp.resilientp2p.MainActivity)
                                                                        ?.showExitConfirmationDialog()
                                                                        ?: run {
                                                                                p2pManager.stop()
                                                                                (context as? android.app.Activity)?.finishAffinity()
                                                                        }
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
                                        if (isConnected) bandwidthInfo?.quality ?: 0 else 0
                        )

                        // --- Endurance Test Running Indicator ---
                        AnimatedVisibility(visible = isEnduranceRunning) {
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(
                                                containerColor = Color(0xFF0D47A1).copy(alpha = 0.15f)
                                        ),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1565C0))
                                ) {
                                        Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                Icon(
                                                        Icons.Default.Timer,
                                                        contentDescription = "Endurance test",
                                                        modifier = Modifier.size(16.dp),
                                                        tint = Color(0xFF0D47A1)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                        "Endurance Test Running",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0D47A1)
                                                )
                                                Spacer(modifier = Modifier.weight(1f))
                                                Text(
                                                        "${enduranceState?.value?.messagesSent ?: 0} msgs",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF1565C0)
                                                )
                                        }
                                }
                        }

                        // --- Network Status Indicator (always visible) ---
                        Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                        containerColor = when {
                                                state.isGateway -> Color(0xFF1B5E20).copy(alpha = 0.15f)
                                                state.hasInternet -> colorScheme.surfaceVariant
                                                else -> Color(0xFF5D4037).copy(alpha = 0.10f)
                                        }
                                )
                        ) {
                                Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                        Icon(
                                                imageVector = when {
                                                        state.isGateway -> Icons.Default.Language
                                                        state.hasInternet -> Icons.Default.Language
                                                        else -> Icons.Default.CloudOff
                                                },
                                                contentDescription = "Network status",
                                                modifier = Modifier.size(20.dp),
                                                tint = if (state.hasInternet) Color(0xFF1B5E20) else Color(0xFF5D4037)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                when {
                                                        state.isGateway -> "Internet Gateway Active"
                                                        state.hasInternet -> "Internet Available"
                                                        else -> "Offline \u2014 mesh-only mode"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (state.hasInternet) Color(0xFF1B5E20) else Color(0xFF5D4037),
                                                modifier = Modifier.weight(1f)
                                        )
                                        // Gateway toggle (only when internet is available)
                                        if (state.hasInternet) {
                                                Text(
                                                        "Gateway",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = colorScheme.onSurfaceVariant
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Switch(
                                                        checked = state.gatewayEnabled,
                                                        onCheckedChange = { p2pManager.setGatewayEnabled(it) },
                                                        modifier = Modifier.height(24.dp)
                                                )
                                        }
                                }
                        }

                        // --- Emergency Alerts Banner ---
                        val emergencyHistory = emergencyManager?.emergencyHistory?.collectAsState()
                        val recentEmergencies = emergencyHistory?.value?.take(3) ?: emptyList()
                        if (recentEmergencies.isNotEmpty()) {
                                Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C).copy(alpha = 0.15f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB71C1C))
                                ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                                Icons.Default.Warning,
                                                                contentDescription = "Emergency",
                                                                tint = Color(0xFFB71C1C),
                                                                modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text(
                                                                "EMERGENCY ALERTS (${emergencyHistory?.value?.size ?: 0})",
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFFB71C1C),
                                                                fontSize = 14.sp
                                                        )
                                                }
                                                recentEmergencies.forEach { msg ->
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(
                                                                "${msg.sourceId}: ${msg.message.take(80)}",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = Color(0xFFB71C1C),
                                                                maxLines = 2
                                                        )
                                                        if (msg.hasLocation) {
                                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                                        Icon(
                                                                                Icons.Default.LocationOn,
                                                                                contentDescription = "Location",
                                                                                tint = Color(0xFFD32F2F),
                                                                                modifier = Modifier.size(14.dp)
                                                                        )
                                                                        Text(
                                                                                "${String.format(java.util.Locale.US, "%.5f", msg.latitude)}, " +
                                                                    String.format(java.util.Locale.US, "%.5f", msg.longitude),
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = Color(0xFFD32F2F)
                                                                        )
                                                                }
                                                        }
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Mesh On/Off Toggle ---
                        val isMeshActive = state.isAdvertising || state.isDiscovering
                        Button(
                                onClick = {
                                        if (isMeshActive) {
                                                p2pManager.stopAll()
                                        } else {
                                                p2pManager.start()
                                        }
                                },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isMeshActive) colorScheme.error else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                                Icon(
                                        imageVector = if (isMeshActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                                        contentDescription = if (isMeshActive) "Stop mesh" else "Start mesh",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                        text = if (isMeshActive) "MESH OFF" else "MESH ON",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                )
                        }

                        // Mesh status summary
                        if (isMeshActive) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.Center
                                ) {
                                        if (state.isAdvertising) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                                Icons.Default.CellTower,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = colorScheme.primary
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                                "Advertising",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colorScheme.primary
                                                        )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        if (state.isDiscovering) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                                Icons.Default.Search,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = colorScheme.secondary
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                                "Discovering",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colorScheme.secondary
                                                        )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                        }
                                        if (state.connectedEndpoints.isNotEmpty()) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                                Icons.Default.Link,
                                                                contentDescription = null,
                                                                modifier = Modifier.size(14.dp),
                                                                tint = colorScheme.tertiary
                                                        )
                                                        Spacer(modifier = Modifier.width(2.dp))
                                                        Text(
                                                                "${state.connectedEndpoints.size} peers",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = colorScheme.tertiary
                                                        )
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- SOS / Emergency Section ---
                        if (emergencyManager != null) {
                                val sosActive = emergencyManager.sosActive.collectAsState()
                                var showEmergencyInput by remember { mutableStateOf(false) }
                                var emergencyText by remember { mutableStateOf("") }

                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                        // SOS Beacon Toggle
                                        Button(
                                                onClick = { emergencyManager.toggleSOSBeacon() },
                                                modifier = Modifier.weight(1f).height(48.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (sosActive.value) Color(0xFFB71C1C) else Color(0xFFD32F2F)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                        ) {
                                                Icon(
                                                        imageVector = if (sosActive.value) Icons.Default.Stop else Icons.Default.Sos,
                                                        contentDescription = "SOS",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        if (sosActive.value) "STOP SOS" else "SOS BEACON",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                )
                                        }
                                        // One-shot emergency broadcast
                                        Button(
                                                onClick = { showEmergencyInput = !showEmergencyInput },
                                                modifier = Modifier.weight(1f).height(48.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFE65100)
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                        ) {
                                                Icon(
                                                        Icons.Default.Warning,
                                                        contentDescription = "Emergency",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                        "EMERGENCY",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = Color.White
                                                )
                                        }
                                }
                                AnimatedVisibility(visible = showEmergencyInput) {
                                        Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                        ) {
                                                OutlinedTextField(
                                                        value = emergencyText,
                                                        onValueChange = { emergencyText = it },
                                                        modifier = Modifier.weight(1f),
                                                        placeholder = { Text("Emergency message...") },
                                                        singleLine = true
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                        onClick = {
                                                                if (emergencyText.isNotBlank()) {
                                                                        emergencyManager.sendEmergencyBroadcast(emergencyText)
                                                                        emergencyText = ""
                                                                        showEmergencyInput = false
                                                                }
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                                                ) {
                                                        Text("SEND", color = Color.White, fontWeight = FontWeight.Bold)
                                                }
                                        }
                                }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- Dashboard & Logs ---
                        DashboardContent(
                                transferProgress = transferProgressEvent?.progress ?: 0,
                                tracePath = tracePath,
                                logs = state.logs,
                                stats = state.stats,
                                onExportLogs = onExportLogs,
                                onClearLogs = { p2pManager.clearLogs() },
                                knownPeers = state.knownPeers,
                                connectedEndpoints = state.connectedEndpoints.toSet(),
                                onPeerClick = { peerId ->
                                        chatTargetId = peerId
                                        showChatDialog = true
                                },
                                telemetryManager = telemetryManager
                        )
                }
        }

        // Back handler for overlay navigation — system back closes the topmost overlay
        BackHandler(
                enabled = showTestMode || showHealthDashboard || showGroupChat || showChatDialog
        ) {
                when {
                        showTestMode -> showTestMode = false
                        showHealthDashboard -> showHealthDashboard = false
                        showGroupChat -> showGroupChat = false
                        showChatDialog -> showChatDialog = false
                }
        }

        if (showChatDialog && chatTargetId != null) {
                val scope = rememberCoroutineScope()
                val isBroadcast = chatTargetId == "BROADCAST"

                // Collect chat messages from Room
                val chatMessages by remember(chatTargetId) {
                        if (chatDao != null) {
                                if (isBroadcast) chatDao.getBroadcastMessages()
                                else chatDao.getMessagesForPeer(chatTargetId!!)
                        } else {
                                kotlinx.coroutines.flow.flowOf(emptyList<ChatMessage>())
                        }
                }.collectAsState(initial = emptyList())

                // Track processed payloads (incoming persistence now handled globally in P2PApplication)

                // Received file persistence now handled globally in P2PApplication

                val targetId = chatTargetId ?: return@ResilientP2PApp

                ChatScreen(
                        peerId = targetId,
                        messages = chatMessages,
                        onSendText = { msg ->
                                if (isBroadcast) {
                                        p2pManager.broadcastMessage(msg)
                                } else {
                                        p2pManager.sendData(targetId, msg)
                                }
                                // Persist outgoing message
                                if (chatDao != null) {
                                        scope.launch {
                                                chatDao.insert(ChatMessage(
                                                        peerId = targetId,
                                                        isOutgoing = true,
                                                        type = MessageType.TEXT,
                                                        text = msg,
                                                        isBroadcast = isBroadcast
                                                ))
                                        }
                                }
                        },
                        onPing = {
                                val timestamp = System.currentTimeMillis()
                                val buffer = java.nio.ByteBuffer.allocate(8)
                                buffer.putLong(timestamp)
                                p2pManager.sendPing(targetId, buffer.array())
                        },
                        onStartAudio = { p2pManager.startAudioStreaming(targetId) },
                        onStopAudio = { p2pManager.stopAudioStreaming() },
                        onSendFile = { uri ->
                                p2pManager.sendFile(targetId, uri)
                                // Persist outgoing file message
                                if (chatDao != null) {
                                        scope.launch(Dispatchers.IO) {
                                                val cr = context.contentResolver
                                                val name = cr.query(uri, null, null, null, null)?.use { c ->
                                                        if (c.moveToFirst()) {
                                                                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                                                                if (idx >= 0) c.getString(idx) else null
                                                        } else {
                                                                null
                                                        }
                                                } ?: uri.lastPathSegment ?: "file"
                                                val mime = cr.getType(uri) ?: "application/octet-stream"
                                                val msgType = if (mime.startsWith("image/")) MessageType.IMAGE else MessageType.FILE
                                                chatDao.insert(ChatMessage(
                                                        peerId = targetId,
                                                        isOutgoing = true,
                                                        type = msgType,
                                                        fileName = name,
                                                        mimeType = mime,
                                                        transferProgress = 0,
                                                        isBroadcast = false
                                                ))
                                        }
                                }
                        },
                        onBack = { showChatDialog = false }
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

        // Test mode overlay
        if (showTestMode && testRunner != null) {
                TestModeScreen(
                        testRunner = testRunner,
                        enduranceTestRunner = enduranceTestRunner,
                        onDismiss = { showTestMode = false }
                )
        }

        // Health dashboard overlay
        if (showHealthDashboard) {
                HealthDashboard(
                        p2pManager = p2pManager,
                        locationEstimator = locationEstimator,
                        onBack = { showHealthDashboard = false }
                )
        }

        // Group chat overlay
        if (showGroupChat && chatGroupDao != null && groupMessageDao != null) {
                GroupChatScreen(
                        p2pManager = p2pManager,
                        chatGroupDao = chatGroupDao,
                        groupMessageDao = groupMessageDao,
                        localUsername = state.localDeviceName,
                        onBack = { showGroupChat = false }
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
                                                com.fyp.resilientp2p.data.LogLevel.entries
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
                                                                                .theme
                                                                                .StatusGreen
                                                                connectionQuality == 2 ->
                                                                        com.fyp.resilientp2p.ui
                                                                                .theme
                                                                                .StatusOrange
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
        stats: NetworkStatsSnapshot,
        onExportLogs: () -> Unit,
        onClearLogs: () -> Unit,
        knownPeers: Map<String, RouteInfo>,
        connectedEndpoints: Set<String>,
        onPeerClick: (String) -> Unit,
        telemetryManager: TelemetryManager? = null
) {
        Column(modifier = Modifier.fillMaxWidth()) {
                // --- Active Transfer Progress (only during active transfer, not after completion) ---
                if (transferProgress in 1..99) {
                        Card(
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text("File Transfer", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(8.dp))
                                        LinearProgressIndicator(
                                                progress = { transferProgress / 100f },
                                                modifier = Modifier.fillMaxWidth(),
                                        )
                                        Text(
                                                "${transferProgress}%",
                                                style = MaterialTheme.typography.bodySmall,
                                                modifier = Modifier.padding(top = 4.dp)
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                }

                // --- Last Trace Path ---
                if (tracePath.isNotBlank()) {
                        Card(
                                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                        ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                        Text("Route Trace", style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                                tracePath,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = colorScheme.onSurfaceVariant
                                        )
                                }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                }

                // --- Mesh Contacts Section ---
                MeshContactsSection(
                        knownPeers = knownPeers,
                        connectedEndpoints = connectedEndpoints,
                        peerStats = stats.peerStats,
                        onPeerClick = onPeerClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- Network Stats Section ---
                NetworkStatsSection(stats = stats)

                Spacer(modifier = Modifier.height(16.dp))

                // --- Telemetry Section ---
                if (telemetryManager != null) {
                        TelemetrySection(telemetryManager = telemetryManager)
                        Spacer(modifier = Modifier.height(16.dp))
                }

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
        peerStats: Map<String, PeerStatsSnapshot>,
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
                                        val stats = peerStats[peerId]

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

                                                Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                                text = peerId,
                                                                fontWeight = FontWeight.Bold,
                                                                color = colorScheme.onSurface
                                                        )
                                                        // Connection type
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
                                                        // Per-peer stats row
                                                        if (stats != null) {
                                                                val rttText = if (stats.lastRttMs >= 0) "${stats.lastRttMs}ms" else "--"
                                                                val connDuration = if (stats.connectedSinceMs > 0) {
                                                                        val dur = System.currentTimeMillis() - stats.connectedSinceMs
                                                                        when {
                                                                                dur < 60_000 -> "${dur / 1000}s"
                                                                                dur < 3_600_000 -> "${dur / 60_000}m${dur % 60_000 / 1000}s"
                                                                                else -> "${dur / 3_600_000}h${dur % 3_600_000 / 60_000}m"
                                                                        }
                                                                } else {
                                                                        "--"
                                                                }
                                                                val traffic = formatTrafficCompact(stats.bytesSent + stats.bytesReceived)
                                                                Text(
                                                                        text = "RTT: $rttText  |  Up: $connDuration  |  Traffic: $traffic",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = colorScheme.onSurfaceVariant,
                                                                        fontFamily = FontFamily.Monospace
                                                                )
                                                        }
                                                }
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

private fun formatTrafficCompact(bytes: Long): String = when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> "${"%,.1f".format(bytes / (1024.0 * 1024.0))}MB"
}
@Composable
fun NetworkStatsSection(stats: NetworkStatsSnapshot) {
        var isExpanded by remember { mutableStateOf(false) }

        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Default.BarChart,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = "Network Stats",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                        )
                                }
                                Text(
                                        text = if (isExpanded) "▲" else "▼",
                                        color = colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp
                                )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Always-visible summary row
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                StatChip(label = "Battery", value = if (stats.batteryLevel >= 0) "${stats.batteryLevel}%" else "N/A")
                                StatChip(label = "Neighbors", value = "${stats.currentNeighborCount}")
                                StatChip(label = "Routes", value = "${stats.currentRouteCount}")
                                StatChip(label = "RTT", value = if (stats.avgRttMs > 0) "${stats.avgRttMs}ms" else "-")
                        }

                        AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                        val uptimeStr = formatStatsDuration(stats.uptimeMs)
                                        StatsRow("Uptime", uptimeStr)
                                        StatsRow("Battery Temp", "${stats.batteryTemperature}\u00B0C")

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Traffic", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colorScheme.primary)
                                        StatsRow("Packets Sent", "${stats.totalPacketsSent}")
                                        StatsRow("Packets Received", "${stats.totalPacketsReceived}")
                                        StatsRow("Packets Forwarded", "${stats.totalPacketsForwarded}")
                                        StatsRow("Packets Dropped", "${stats.totalPacketsDropped}")
                                        StatsRow("Bytes Sent", formatStatsBytes(stats.totalBytesSent))
                                        StatsRow("Bytes Received", formatStatsBytes(stats.totalBytesReceived))

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Connections", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colorScheme.primary)
                                        StatsRow("Total Established", "${stats.totalConnectionsEstablished}")
                                        StatsRow("Total Lost", "${stats.totalConnectionsLost}")

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Store & Forward", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colorScheme.primary)
                                        StatsRow("Queued", "${stats.storeForwardQueued}")
                                        StatsRow("Delivered", "${stats.storeForwardDelivered}")
                                        StatsRow("ACKs Confirmed", "${stats.deliveryConfirmed}")

                                        if (stats.peerStats.isNotEmpty()) {
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Per-Peer", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = colorScheme.primary)
                                                stats.peerStats.forEach { (name, peer) ->
                                                        val rttStr = if (peer.lastRttMs >= 0) "${peer.lastRttMs}ms" else "-"
                                                        Text(
                                                                text = "$name  RTT=$rttStr  " +
                                                                    "\u2191${peer.packetsSent}" +
                                                                    "\u2193${peer.packetsReceived}" +
                                                                    "  dc=${peer.disconnectCount}",
                                                                fontSize = 11.sp,
                                                                fontFamily = FontFamily.Monospace,
                                                                color = colorScheme.onSurfaceVariant,
                                                                modifier = Modifier.padding(vertical = 1.dp)
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}

@Composable
private fun StatChip(label: String, value: String) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = colorScheme.primary)
                Text(text = label, fontSize = 10.sp, color = colorScheme.onSurfaceVariant)
        }
}

@Composable
private fun StatsRow(label: String, value: String) {
        Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
                Text(text = label, fontSize = 12.sp, color = colorScheme.onSurfaceVariant)
                Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace, color = colorScheme.onSurface)
        }
}

private fun formatStatsDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return when {
                hours > 0 -> "${hours}h ${minutes % 60}m ${seconds % 60}s"
                minutes > 0 -> "${minutes}m ${seconds % 60}s"
                else -> "${seconds}s"
        }
}

private fun formatStatsBytes(bytes: Long): String {
        return when {
                bytes >= 1_048_576 -> String.format(java.util.Locale.US, "%.1f MB", bytes / 1_048_576.0)
                bytes >= 1024 -> String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0)
                else -> "$bytes B"
        }
}

@Composable
fun LogsSection(
        logs: List<com.fyp.resilientp2p.data.LogEntry>,
        onExportLogs: () -> Unit,
        onClearLogs: () -> Unit
) {
        var isLogsExpanded by remember { mutableStateOf(false) }
        val reversedLogs = remember(logs) { logs.reversed() }

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
                                                        MaterialTheme.colorScheme.surface,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                        1.dp,
                                                        MaterialTheme.colorScheme.outlineVariant,
                                                        RoundedCornerShape(8.dp)
                                                )
                                                .padding(8.dp)
                        ) {
                                LazyColumn(
                                        reverseLayout = true,
                                        modifier = Modifier.fillMaxSize()
                                ) {
                                        items(
                                                items = reversedLogs,
                                                key = { "${it.timestamp}_${it.message.hashCode()}_${System.identityHashCode(it)}" }
                                        ) { entry ->
                                                val color =
                                                if (entry.logType == com.fyp.resilientp2p.data.LogType.CHAT) {
                                                        if (entry.message
                                                                        .startsWith(
                                                                                "[SENT]"
                                                                        )
                                                        )
                                                                com.fyp.resilientp2p
                                                                        .ui
                                                                        .theme
                                                                        .TechBluePrimary // Blue for Sent
                                                        else
                                                                com.fyp.resilientp2p
                                                                        .ui
                                                                        .theme
                                                                        .TechTealSecondary // Teal for Received
                                                } else {
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
                                                                        MaterialTheme.colorScheme.onSurface
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
                                                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                                com.fyp.resilientp2p
                                                                        .data
                                                                        .LogLevel
                                                                        .METRIC ->
                                                                        com.fyp
                                                                                .resilientp2p
                                                                                .ui
                                                                                .theme
                                                                                .TechTealSecondary
                                                                else -> Color.Gray
                                                        }
                                                }

                                                // Format: HH:MM:SS [LEVEL] Message
                                                // Chat: HH:MM:SS [CHAT] Message
                                                val tag =
                                                        if (entry.logType ==
                                                                        com.fyp.resilientp2p.data
                                                                                .LogType
                                                                                .CHAT
                                                        ) {
                                                                if (entry.peerId != null) {
                                                                        "[${entry.peerId}]"
                                                                } else {
                                                                        "[CHAT]"
                                                                }
                                                        } else {
                                                                "[${entry.level.name}]"
                                                        }

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

@Composable
fun TelemetrySection(telemetryManager: TelemetryManager) {
        var isExpanded by remember { mutableStateOf(false) }
        var status by remember { mutableStateOf<TelemetryStatus?>(null) }
        val scope = rememberCoroutineScope()

        LaunchedEffect(isExpanded) {
                status = telemetryManager.getStatus()
        }

        Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded }
        ) {
                Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                Icons.Default.Cloud,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = "Cloud Telemetry",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                        )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                                imageVector = if (telemetryManager.isEnabled) Icons.Default.Check else Icons.Default.Close,
                                                contentDescription = if (telemetryManager.isEnabled) "Enabled" else "Disabled",
                                                modifier = Modifier.size(16.dp),
                                                tint = if (telemetryManager.isEnabled) Color(0xFF2E7D32) else colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                                text = if (isExpanded) "\u25B2" else "\u25BC",
                                                color = colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                        )
                                }
                        }

                        status?.let { s ->
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                        StatChip(label = "Pending", value = "${s.pendingEvents}")
                                        StatChip(label = "Total", value = "${s.totalEvents}")
                                        StatChip(
                                                label = "Upload",
                                                value = if (telemetryManager.hasInternet()) "Online" else "Offline"
                                        )
                                }
                        }

                        AnimatedVisibility(visible = isExpanded) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                        status?.let { s ->
                                                StatsRow("Enabled", if (s.enabled) "Yes" else "No")
                                                StatsRow("WiFi Only", if (s.wifiOnly) "Yes" else "No")
                                                StatsRow("Device Registered", if (s.deviceRegistered) "Yes" else "No")
                                                StatsRow("Pending Events", "${s.pendingEvents}")
                                                StatsRow("Total Events", "${s.totalEvents}")
                                                if (s.lastSnapshotTime > 0) {
                                                        val ago = (System.currentTimeMillis() - s.lastSnapshotTime) / 1000
                                                        StatsRow("Last Snapshot", "${ago}s ago")
                                                }
                                                StatsRow(
                                                        "Firebase",
                                                        if (com.fyp.resilientp2p.managers.TelemetryUploadWorker.isFirebaseConfigured)
                                                                "Configured" else "Not configured (local export)"
                                                )
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(
                                                        onClick = {
                                                                telemetryManager.isEnabled = !telemetryManager.isEnabled
                                                                scope.launch { status = telemetryManager.getStatus() }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        Text(
                                                                if (telemetryManager.isEnabled) "Disable" else "Enable",
                                                                fontSize = 11.sp
                                                        )
                                                }
                                                OutlinedButton(
                                                        onClick = {
                                                                telemetryManager.forceSnapshot()
                                                                scope.launch {
                                                                        kotlinx.coroutines.delay(500)
                                                                        status = telemetryManager.getStatus()
                                                                }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        Text("Snapshot Now", fontSize = 11.sp)
                                                }
                                                OutlinedButton(
                                                        onClick = {
                                                                telemetryManager.wifiOnlyUpload = !telemetryManager.wifiOnlyUpload
                                                                scope.launch { status = telemetryManager.getStatus() }
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                ) {
                                                        Text(
                                                                if (telemetryManager.wifiOnlyUpload) "Any Net" else "WiFi Only",
                                                                fontSize = 11.sp
                                                        )
                                                }
                                        }
                                }
                        }
                }
        }
}
