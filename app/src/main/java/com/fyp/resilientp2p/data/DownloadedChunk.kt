package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Persisted record of a single downloaded chunk for file resume support.
 *
 * When a file download is interrupted (app killed, network drop), this table
 * lets the [com.fyp.resilientp2p.managers.FileShareManager] know which chunks
 * are already on disk so it only requests the missing ones on resume.
 *
 * @property sha256 The file's content hash (foreign key to [SharedFile]).
 * @property chunkIndex The zero-based chunk index that has been written to disk.
 */
@Entity(
    tableName = "downloaded_chunks",
    primaryKeys = ["sha256", "chunkIndex"]
)
data class DownloadedChunk(
    val sha256: String,
    val chunkIndex: Int
)

/**
 * Room DAO for persisting per-chunk download progress.
 *
 * @see DownloadedChunk
 * @see SharedFileDao
 */
@Dao
interface DownloadedChunkDao {

    /** Get all received chunk indices for a file. */
    @Query("SELECT chunkIndex FROM downloaded_chunks WHERE sha256 = :sha256")
    suspend fun getReceivedChunks(sha256: String): List<Int>

    /** Mark a single chunk as received. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markChunkReceived(chunk: DownloadedChunk)

    /** Clear all chunk records for a file (after download completes or is cancelled). */
    @Query("DELETE FROM downloaded_chunks WHERE sha256 = :sha256")
    suspend fun clearChunks(sha256: String)

    /** Clear all chunk records (full reset). */
    @Query("DELETE FROM downloaded_chunks")
    suspend fun deleteAll()
}
