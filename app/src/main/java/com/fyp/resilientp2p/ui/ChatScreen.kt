package com.fyp.resilientp2p.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.fyp.resilientp2p.data.ChatMessage
import com.fyp.resilientp2p.data.MessageType
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Full-screen chat interface with message bubbles, file transfer,
 * image thumbnails, and push-to-talk audio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    peerId: String,
    messages: List<ChatMessage>,
    onSendText: (String) -> Unit,
    onPing: () -> Unit,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit,
    onSendFile: (Uri) -> Unit,
    onBack: () -> Unit,
    isInternetPeer: Boolean = false
) {
    val context = LocalContext.current
    val isBroadcast = peerId == "BROADCAST"
    var messageText by remember { mutableStateOf("") }

    // Camera capture — creates a temp file and launches the system camera
    var cameraPhotoUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraPhotoUri?.let { uri ->
                onSendFile(uri)
                Toast.makeText(context, "Sending photo...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val photoFile = File(
                context.cacheDir, "camera_photos"
            ).apply { mkdirs() }.let {
                File(it, "IMG_${System.currentTimeMillis()}.jpg")
            }
            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", photoFile
            )
            cameraPhotoUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    // General file picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onSendFile(it)
            Toast.makeText(context, "Sending file...", Toast.LENGTH_SHORT).show()
        }
    }

    // Audio permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Permission granted. Hold to talk.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Audio permission required", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isBroadcast) "Broadcast" else peerId,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isBroadcast) "All connected peers" else "Direct P2P channel",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = onPing) { Text("PING") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isBroadcast) MaterialTheme.colorScheme.secondaryContainer
                                     else MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            ChatInputBar(
                messageText = messageText,
                onMessageChange = { messageText = it },
                onSend = {
                    if (messageText.isNotBlank()) {
                        onSendText(messageText)
                        messageText = ""
                    }
                },
                onPickImage = {
                    // Launch camera: check permission first
                    if (ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        val photoFile = File(
                            context.cacheDir, "camera_photos"
                        ).apply { mkdirs() }.let {
                            File(it, "IMG_${System.currentTimeMillis()}.jpg")
                        }
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", photoFile
                        )
                        cameraPhotoUri = uri
                        cameraLauncher.launch(uri)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onPickFile = { filePickerLauncher.launch("*/*") },
                onStartAudio = {
                    if (ContextCompat.checkSelfPermission(context,
                            android.Manifest.permission.RECORD_AUDIO) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) {
                        onStartAudio()
                    } else {
                        permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                },
                onStopAudio = onStopAudio,
                isBroadcast = isBroadcast,
                isInternetPeer = isInternetPeer
            )
        },
        modifier = Modifier.fillMaxSize().imePadding()
    ) { padding ->
        val listState = rememberLazyListState()

        // Auto-scroll to bottom when new messages arrive, only if user is near the bottom
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                val totalItems = listState.layoutInfo.totalItemsCount
                // Only auto-scroll if user is within 3 items of the bottom (or list just started)
                if (totalItems <= 1 || lastVisibleItem >= totalItems - 3) {
                    listState.animateScrollToItem(messages.size - 1)
                }
            }
        }

        if (messages.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isBroadcast) "Send a message to all peers"
                           else "Start chatting with $peerId",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 8.dp)
                    .imePadding(), // Add imePadding to LazyColumn so messages scroll up with keyboard
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    ChatBubble(message)
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    val isOutgoing = message.isOutgoing
    val alignment = if (isOutgoing) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isOutgoing) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isOutgoing) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    // System messages centered
    if (message.type == MessageType.SYSTEM) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Text(
                text = message.text ?: "",
                style = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
        return
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = alignment
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(bubbleColor, shape = RoundedCornerShape(
                    topStart = 12.dp, topEnd = 12.dp,
                    bottomStart = if (isOutgoing) 12.dp else 4.dp,
                    bottomEnd = if (isOutgoing) 4.dp else 12.dp
                ))
                .padding(10.dp)
        ) {
            // Sender name for incoming messages
            if (!isOutgoing) {
                Text(
                    text = message.peerId,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }

            when (message.type) {
                MessageType.TEXT -> {
                    Text(
                        text = message.text ?: "",
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                MessageType.IMAGE -> {
                    // Image thumbnail
                    if (message.filePath != null && File(message.filePath).exists()) {
                        AsyncImage(
                            model = File(message.filePath),
                            contentDescription = message.fileName,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Sending or no local file yet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .background(Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Image: ${message.fileName ?: "Image"}", color = textColor)
                        }
                    }

                    // Keep transfer UI minimal and robust.
                    if (message.transferProgress in 0..99) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sending...",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
                MessageType.FILE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AttachFile,
                            contentDescription = "File",
                            modifier = Modifier.size(24.dp),
                            tint = textColor
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = message.fileName ?: "File",
                                color = textColor,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (message.fileSize > 0) {
                                Text(
                                    text = formatFileSize(message.fileSize),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = textColor.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }

                    if (message.transferProgress in 0..99) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sending...",
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.7f)
                        )
                    }
                }
                else -> {
                    Text(text = message.text ?: "", color = textColor)
                }
            }

            // Timestamp
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = formatTimestamp(message.timestamp),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.5f),
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    onPickImage: () -> Unit,
    onPickFile: () -> Unit,
    onStartAudio: () -> Unit,
    onStopAudio: () -> Unit,
    isBroadcast: Boolean,
    isInternetPeer: Boolean
) {
    Surface(
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
            // PTT (push-to-talk) row — only for direct mesh peers (not internet relay)
            if (!isInternetPeer) {
                val interactionSource = remember { MutableInteractionSource() }
                val isPressed by interactionSource.collectIsPressedAsState()
                var wasEverPressed by remember { mutableStateOf(false) }
                DisposableEffect(isPressed) {
                    if (isPressed) {
                        wasEverPressed = true
                        onStartAudio()
                    } else if (wasEverPressed) {
                        onStopAudio()
                    }
                    onDispose {
                        if (isPressed) {
                            onStopAudio()
                        }
                    }
                }
                Button(
                    onClick = {},
                    interactionSource = interactionSource,
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPressed) Color.Red
                                         else MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (isPressed) "Recording... Release to stop"
                        else "Hold to Talk${if (isBroadcast) " (All Peers)" else ""}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Attachment row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {

                IconButton(onClick = onPickImage, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = "Take photo", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onPickFile, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file", modifier = Modifier.size(20.dp))
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message input row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = onMessageChange,
                    placeholder = {
                        Text(if (isBroadcast) "Broadcast message..." else "Message...")
                    },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                Button(
                    onClick = onSend,
                    enabled = messageText.isNotBlank(),
                    modifier = Modifier.size(48.dp),
                    shape = CircleShape,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

private val chatTimestampFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

private fun formatTimestamp(timestamp: Long): String =
    chatTimestampFormat.format(Date(timestamp))

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "${bytes}B"
        bytes < 1024 * 1024 -> "${bytes / 1024}KB"
        else -> String.format(java.util.Locale.US, "%.1fMB", bytes / (1024.0 * 1024.0))
    }
}
