package com.fyp.resilientp2p.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Room DAO for the `shared_files` table ([SharedFile]).
 *
 * @see SharedFile
 * @see AppDatabase
 */
@Dao
interface SharedFileDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(file: SharedFile): Long

    /** Update download progress for a file. */
    @Query("UPDATE shared_files SET downloadedChunks = :chunks, localPath = :localPath WHERE sha256 = :sha256")
    suspend fun updateProgress(sha256: String, chunks: Int, localPath: String?)

    /** Mark file as fully downloaded. */
    @Query("UPDATE shared_files SET downloadedChunks = totalChunks, localPath = :localPath WHERE sha256 = :sha256")
    suspend fun markComplete(sha256: String, localPath: String)

    /** Get all shared files, newest first. */
    @Query("SELECT * FROM shared_files ORDER BY announcedAt DESC")
    fun getAllFiles(): Flow<List<SharedFile>>

    /** Get a file by hash. */
    @Query("SELECT * FROM shared_files WHERE sha256 = :sha256 LIMIT 1")
    suspend fun getFile(sha256: String): SharedFile?

    /** Check if a file is already known. */
    @Query("SELECT COUNT(*) FROM shared_files WHERE sha256 = :sha256")
    suspend fun exists(sha256: String): Int

    /** Get files we have locally (downloaded). */
    @Query("SELECT * FROM shared_files WHERE localPath IS NOT NULL ORDER BY announcedAt DESC")
    fun getLocalFiles(): Flow<List<SharedFile>>

    /** Delete all shared files. */
    @Query("DELETE FROM shared_files")
    suspend fun deleteAll()
}
