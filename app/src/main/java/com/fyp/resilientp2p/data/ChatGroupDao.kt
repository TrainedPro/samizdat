package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `chat_groups` table ([ChatGroup]).
 *
 * Supports creating, joining, leaving groups and retrieving group lists.
 *
 * @see ChatGroup
 * @see AppDatabase
 */
@Dao
interface ChatGroupDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(group: ChatGroup)

    @Update
    suspend fun update(group: ChatGroup)

    /** Get all groups, newest first. */
    @Query("SELECT * FROM chat_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<ChatGroup>>

    /** Get a single group by ID. */
    @Query("SELECT * FROM chat_groups WHERE groupId = :groupId LIMIT 1")
    suspend fun getGroup(groupId: String): ChatGroup?

    /** Check if a group exists. */
    @Query("SELECT COUNT(*) FROM chat_groups WHERE groupId = :groupId")
    suspend fun exists(groupId: String): Int

    /** Delete a group. */
    @Query("DELETE FROM chat_groups WHERE groupId = :groupId")
    suspend fun delete(groupId: String)

    /** Delete all groups. */
    @Query("DELETE FROM chat_groups")
    suspend fun deleteAll()
}
