package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `chat_messages` table ([ChatMessage]).
 *
 * Supports direct peer-to-peer chats and broadcast messages. File transfer
 * progress is updated in-place via [updateProgress] and [markTransferComplete].
 *
 * @see ChatMessage
 * @see AppDatabase
 */
@Dao
interface ChatDao {
    @Insert
    suspend fun insert(message: ChatMessage): Long

    @Update
    suspend fun update(message: ChatMessage)

    /** Get all messages for a specific peer (direct chat), ordered chronologically. */
    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId AND isBroadcast = 0 ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerId: String): Flow<List<ChatMessage>>

    /** Get all broadcast messages, ordered chronologically. */
    @Query("SELECT * FROM chat_messages WHERE isBroadcast = 1 ORDER BY timestamp ASC")
    fun getBroadcastMessages(): Flow<List<ChatMessage>>

    /** Update transfer progress for a file message. */
    @Query("UPDATE chat_messages SET transferProgress = :progress WHERE id = :messageId")
    suspend fun updateProgress(messageId: Long, progress: Int)

    /** Mark a file transfer as complete by setting filePath and progress = -1. */
    @Query("UPDATE chat_messages SET filePath = :filePath, transferProgress = -1 WHERE id = :messageId")
    suspend fun markTransferComplete(messageId: Long, filePath: String)

    /** Get last N messages for a peer (for previews). */
    @Query("SELECT * FROM chat_messages WHERE peerId = :peerId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(peerId: String, limit: Int = 50): List<ChatMessage>

    /** Delete all messages for a peer. */
    @Query("DELETE FROM chat_messages WHERE peerId = :peerId")
    suspend fun deleteMessagesForPeer(peerId: String)

    /** Delete all chat messages. */
    @Query("DELETE FROM chat_messages")
    suspend fun deleteAll()
}
