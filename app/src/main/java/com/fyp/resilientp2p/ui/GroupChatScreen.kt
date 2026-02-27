package com.fyp.resilientp2p.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fyp.resilientp2p.data.ChatGroup
import com.fyp.resilientp2p.data.ChatGroupDao
import com.fyp.resilientp2p.data.GroupMessage
import com.fyp.resilientp2p.data.GroupMessageDao
import com.fyp.resilientp2p.managers.P2PManager
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import kotlinx.coroutines.launch
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen group chat UI with channel list and message view.
 *
 * Left panel (in portrait: full-screen list) shows all groups. Selecting a group
 * opens the message view for that channel.
 *
 * @param p2pManager Core mesh manager for sending packets.
 * @param chatGroupDao Room DAO for groups.
 * @param groupMessageDao Room DAO for group messages.
 * @param localUsername This device's peer name.
 * @param onBack Navigation callback.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupChatScreen(
    p2pManager: P2PManager,
    chatGroupDao: ChatGroupDao,
    groupMessageDao: GroupMessageDao,
    localUsername: String,
    onBack: () -> Unit
) {
    val groups by chatGroupDao.getAllGroups().collectAsState(initial = emptyList())
    var selectedGroup by remember { mutableStateOf<ChatGroup?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selectedGroup?.name ?: "Group Channels") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedGroup != null) selectedGroup = null
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (selectedGroup == null) {
                        IconButton(onClick = { showCreateDialog = true }) {
                            Icon(Icons.Default.Add, "Create Group")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (selectedGroup == null) {
                // Group list
                GroupListPanel(
                    groups = groups,
                    localUsername = localUsername,
                    onGroupSelect = { selectedGroup = it },
                    onJoin = { group ->
                        scope.launch {
                            chatGroupDao.upsert(group.withMember(localUsername))
                        }
                    }
                )
            } else {
                // Message view for selected group
                GroupMessagePanel(
                    group = selectedGroup!!,
                    groupMessageDao = groupMessageDao,
                    p2pManager = p2pManager,
                    localUsername = localUsername
                )
            }
        }

        // Create group dialog
        if (showCreateDialog) {
            CreateGroupDialog(
                onDismiss = { showCreateDialog = false },
                onCreate = { name ->
                    scope.launch {
                        val groupId = UUID.randomUUID().toString()
                        val group = ChatGroup(
                            groupId = groupId,
                            name = name,
                            createdBy = localUsername,
                            members = localUsername
                        )
                        chatGroupDao.upsert(group)

                        // Announce group creation to mesh
                        val payload = "CREATE|$groupId|$name|$localUsername"
                        val packet = Packet(
                            id = UUID.randomUUID().toString(),
                            type = PacketType.GROUP_MESSAGE,
                            sourceId = localUsername,
                            destId = "BROADCAST",
                            payload = payload.toByteArray(StandardCharsets.UTF_8),
                            timestamp = System.currentTimeMillis()
                        )
                        p2pManager.injectPacket(packet)
                    }
                    showCreateDialog = false
                }
            )
        }
    }
}

@Composable
private fun GroupListPanel(
    groups: List<ChatGroup>,
    localUsername: String,
    onGroupSelect: (ChatGroup) -> Unit,
    onJoin: (ChatGroup) -> Unit
) {
    if (groups.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No groups yet. Tap + to create one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(groups) { group ->
                GroupCard(
                    group = group,
                    isMember = group.memberSet().contains(localUsername),
                    onClick = { onGroupSelect(group) },
                    onJoin = { onJoin(group) }
                )
            }
        }
    }
}

@Composable
private fun GroupCard(
    group: ChatGroup,
    isMember: Boolean,
    onClick: () -> Unit,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group avatar
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    group.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(group.name, fontWeight = FontWeight.Bold)
                Text(
                    "${group.memberSet().size} members • by ${group.createdBy}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!isMember) {
                TextButton(onClick = onJoin) { Text("Join") }
            }
        }
    }
}

@Composable
private fun GroupMessagePanel(
    group: ChatGroup,
    groupMessageDao: GroupMessageDao,
    p2pManager: P2PManager,
    localUsername: String
) {
    val messages by groupMessageDao.getMessagesForGroup(group.groupId).collectAsState(initial = emptyList())
    var input by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val dateFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Auto-scroll to bottom
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Messages
        LazyColumn(
            modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages) { msg ->
                val isLocal = msg.senderName == localUsername
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isLocal) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        modifier = Modifier.widthIn(max = 280.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isLocal) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            if (!isLocal) {
                                Text(
                                    msg.senderName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(msg.text, fontSize = 14.sp)
                            Text(
                                dateFormat.format(Date(msg.timestamp)),
                                fontSize = 10.sp,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                }
            }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Message #${group.name}") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.isBlank()) return@IconButton
                    val msg = input.trim()
                    input = ""
                    scope.launch {
                        // Persist locally
                        val packetId = UUID.randomUUID().toString()
                        groupMessageDao.insert(
                            GroupMessage(
                                groupId = group.groupId,
                                senderName = localUsername,
                                text = msg,
                                packetId = packetId
                            )
                        )
                        // Send to mesh: MSG|groupId|senderName|text
                        val payload = "MSG|${group.groupId}|$localUsername|$msg"
                        val packet = Packet(
                            id = packetId,
                            type = PacketType.GROUP_MESSAGE,
                            sourceId = localUsername,
                            destId = "BROADCAST",
                            payload = payload.toByteArray(StandardCharsets.UTF_8),
                            timestamp = System.currentTimeMillis()
                        )
                        p2pManager.injectPacket(packet)
                    }
                },
                enabled = input.isNotBlank()
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun CreateGroupDialog(
    onDismiss: () -> Unit,
    onCreate: (String) -> Unit
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Group") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onCreate(name.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
