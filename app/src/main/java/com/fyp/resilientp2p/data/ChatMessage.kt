package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a chat message in the P2P mesh.
 * Persisted in Room for chat history.
 */
@Entity(
    tableName = "chat_messages",
    indices = [Index(value = ["peerId", "timestamp"])]
)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val peerId: String,              // Peer this message is to/from
    val isOutgoing: Boolean,         // true = we sent it, false = received
    val type: MessageType = MessageType.TEXT,
    val text: String? = null,        // For TEXT messages
    val fileName: String? = null,    // For FILE/IMAGE messages
    val filePath: String? = null,    // Local path to the file (after receive)
    val mimeType: String? = null,    // MIME type for file/image
    val fileSize: Long = 0,          // File size in bytes
    val transferProgress: Int = -1,  // 0-100 for active transfers, -1 = complete/N/A
    val isBroadcast: Boolean = false // Broadcast message flag
)

enum class MessageType {
    TEXT,
    IMAGE,
    FILE,
    SYSTEM  // For system messages ("Peer joined", "Call started", etc.)
}
