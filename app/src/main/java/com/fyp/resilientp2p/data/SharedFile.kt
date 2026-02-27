package com.fyp.resilientp2p.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Metadata for a file shared on the mesh via content-addressable distribution.
 *
 * Files are identified by their SHA-256 hash (content-addressed).
 * The [localPath] is non-null only if this device has downloaded the file.
 *
 * @property sha256 SHA-256 hex hash of the file content (primary key).
 * @property fileName Human-readable file name.
 * @property mimeType MIME type (e.g. "image/jpeg").
 * @property fileSize Total file size in bytes.
 * @property chunkSize Size of each transfer chunk (default 32 KB).
 * @property totalChunks Total number of chunks = ceil(fileSize / chunkSize).
 * @property sharedBy Peer name that originally announced this file.
 * @property announcedAt Epoch-ms when the announcement was received.
 * @property localPath Local file path if downloaded, null otherwise.
 * @property downloadedChunks Number of chunks downloaded so far.
 */
@Entity(
    tableName = "shared_files",
    indices = [Index(value = ["sharedBy"]), Index(value = ["announcedAt"])]
)
data class SharedFile(
    @PrimaryKey val sha256: String,
    val fileName: String,
    val mimeType: String = "application/octet-stream",
    val fileSize: Long,
    val chunkSize: Int = DEFAULT_CHUNK_SIZE,
    val totalChunks: Int,
    val sharedBy: String,
    val announcedAt: Long = System.currentTimeMillis(),
    val localPath: String? = null,
    val downloadedChunks: Int = 0
) {
    companion object {
        /** Default chunk size: 32 KB — balances throughput and memory on constrained devices. */
        const val DEFAULT_CHUNK_SIZE = 32 * 1024
    }
}
