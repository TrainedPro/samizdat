package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `group_messages` table ([GroupMessage]).
 *
 * @see GroupMessage
 * @see ChatGroup
 */
@Dao
interface GroupMessageDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(message: GroupMessage): Long

    /** Get all messages for a group, chronological. */
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getMessagesForGroup(groupId: String): Flow<List<GroupMessage>>

    /** Get recent messages for a group (for history sync). */
    @Query("SELECT * FROM group_messages WHERE groupId = :groupId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentMessages(groupId: String, limit: Int = 100): List<GroupMessage>

    /** Check if a packet was already stored (dedup). */
    @Query("SELECT COUNT(*) FROM group_messages WHERE packetId = :packetId")
    suspend fun existsByPacketId(packetId: String): Int

    /** Delete all messages for a group. */
    @Query("DELETE FROM group_messages WHERE groupId = :groupId")
    suspend fun deleteForGroup(groupId: String)

    /** Delete all group messages. */
    @Query("DELETE FROM group_messages")
    suspend fun deleteAll()
}
