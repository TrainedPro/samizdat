package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * A group chat message persisted in Room.
 *
 * Similar to [ChatMessage] but tied to a [ChatGroup] via [groupId].
 *
 * @property groupId The group/channel this message belongs to.
 * @property senderName The peer name that sent this message.
 * @property text Message text content.
 * @property timestamp Epoch-ms when the message was created.
 * @property packetId Original packet UUID for dedup.
 */
@Entity(
    tableName = "group_messages",
    indices = [
        Index(value = ["groupId", "timestamp"]),
        Index(value = ["packetId"], unique = true)
    ]
)
data class GroupMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val senderName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val packetId: String = UUID.randomUUID().toString()
)
