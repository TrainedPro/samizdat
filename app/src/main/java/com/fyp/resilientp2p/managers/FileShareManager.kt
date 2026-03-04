package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.SharedFile
import com.fyp.resilientp2p.data.SharedFileDao
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Content-addressable distributed file sharing over the mesh.
 *
 * ## Protocol
 * 1. **FILE_ANNOUNCE** — broadcaster sends `sha256|fileName|mimeType|fileSize|chunkSize|totalChunks`
 * 2. **FILE_REQUEST** — requester sends `sha256|chunkIndex` to the sharer
 * 3. **FILE_CHUNK** — sharer replies with `sha256|chunkIndex|<binary data>`
 *
 * Files are identified by their SHA-256 hash (content-addressed). Chunks are
 * downloaded in order and reassembled locally. Duplicate chunks are ignored.
 * Completed files are verified against their announced SHA-256 hash.
 *
 * @param context Android context for file storage.
 * @param sharedFileDao Room DAO for file metadata persistence.
 * @param localUsername This device's peer name (for packet sourcing).
 */
class FileShareManager(
    private val context: Context,
    private val sharedFileDao: SharedFileDao,
    private val localUsername: String,
    private val downloadedChunkDao: com.fyp.resilientp2p.data.DownloadedChunkDao? = null
) {
    companion object {
        private const val TAG = "FileShareManager"
        /** Default chunk size: 32 KB. */
        const val CHUNK_SIZE = SharedFile.DEFAULT_CHUNK_SIZE
        /** Header separator in announce/request payloads. */
        const val SEPARATOR = "|"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Callback to send a packet onto the mesh (set by P2PManager). */
    var sendPacket: ((Packet) -> Unit)? = null

    /** Tracks which chunks we've received per file hash (thread-safe sets). */
    private val receivedChunks = ConcurrentHashMap<String, MutableSet<Int>>()

    /** Local files available for sharing (sha256 → local path). */
    private val localFiles = ConcurrentHashMap<String, String>()

    /**
     * Multi-source tracking: maps file SHA-256 → set of peer IDs that announced
     * the file. When requesting chunks, requests are distributed across sources
     * in round-robin fashion (BitTorrent-style multi-peer download).
     */
    private val fileSources = ConcurrentHashMap<String, MutableSet<String>>()

    /** All shared files as a Flow (for UI). */
    val allFiles: Flow<List<SharedFile>> = sharedFileDao.getAllFiles()

    /** Files we have locally (for UI). */
    val downloadedFiles: Flow<List<SharedFile>> = sharedFileDao.getLocalFiles()

    /** Share directory for downloaded files. */
    private val shareDir: File by lazy {
        File(context.filesDir, "mesh_shared").apply { mkdirs() }
    }

    /**
     * Announce a local file to the mesh.
     * Computes SHA-256, registers it, and broadcasts a FILE_ANNOUNCE packet.
     *
     * @param filePath Path to the local file.
     * @param fileName Display name.
     * @param mimeType MIME type.
     * @return The SHA-256 hash of the file.
     */
    fun announceFile(filePath: String, fileName: String, mimeType: String): String? {
        val file = File(filePath)
        if (!file.exists() || !file.canRead()) {
            Log.w(TAG, "Cannot announce: file not readable: $filePath")
            return null
        }

        val sha256 = computeSha256(file) ?: return null
        val fileSize = file.length()
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()

        localFiles[sha256] = filePath

        scope.launch {
            val sharedFile = SharedFile(
                sha256 = sha256,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                chunkSize = CHUNK_SIZE,
                totalChunks = totalChunks,
                sharedBy = localUsername,
                localPath = filePath,
                downloadedChunks = totalChunks
            )
            sharedFileDao.insert(sharedFile)

            // Build announce payload
            val announcePayload =
                "$sha256$SEPARATOR$fileName$SEPARATOR$mimeType$SEPARATOR$fileSize$SEPARATOR$CHUNK_SIZE$SEPARATOR$totalChunks"
            val packet = Packet(
                id = UUID.randomUUID().toString(),
                type = PacketType.FILE_ANNOUNCE,
                sourceId = localUsername,
                destId = "BROADCAST",
                payload = announcePayload.toByteArray(StandardCharsets.UTF_8),
                timestamp = System.currentTimeMillis()
            )
            sendPacket?.invoke(packet)
            Log.d(TAG, "Announced file: $fileName ($sha256) ${fileSize}B $totalChunks chunks")
        }

        return sha256
    }

    /**
     * Handle a received FILE_ANNOUNCE packet.
     * Registers the file metadata if not already known.
     */
    fun handleFileAnnounce(packet: Packet) {
        val parts = String(packet.payload, StandardCharsets.UTF_8).split(SEPARATOR)
        if (parts.size < 6) {
            Log.w(TAG, "Malformed FILE_ANNOUNCE from ${packet.sourceId}")
            return
        }
        val sha256 = parts[0]
        val fileName = parts[1]
        val mimeType = parts[2]
        val fileSize = parts[3].toLongOrNull() ?: return
        val chunkSize = parts[4].toIntOrNull() ?: return
        val totalChunks = parts[5].toIntOrNull() ?: return

        // Always track additional sources (BitTorrent swarm principle)
        fileSources.computeIfAbsent(sha256) {
            java.util.Collections.synchronizedSet(mutableSetOf())
        }.add(packet.sourceId)

        scope.launch {
            if (sharedFileDao.exists(sha256) > 0) {
                // File already known — we just recorded the additional source above.
                Log.d(TAG, "Additional source for $sha256: ${packet.sourceId} (total: ${fileSources[sha256]?.size})")
                return@launch
            }
            val sharedFile = SharedFile(
                sha256 = sha256,
                fileName = fileName,
                mimeType = mimeType,
                fileSize = fileSize,
                chunkSize = chunkSize,
                totalChunks = totalChunks,
                sharedBy = packet.sourceId
            )
            sharedFileDao.insert(sharedFile)
            Log.d(TAG, "Registered file announce: $fileName from ${packet.sourceId}")
        }
    }

    /**
     * Request download of a file from the mesh.
     * Sends FILE_REQUEST packets for each chunk we don't have yet.
     * Supports resume: loads persisted chunk records from Room so only
     * missing chunks are requested after an interruption.
     *
     * @param sha256 The file's SHA-256 hash.
     */
    fun requestFile(sha256: String) {
        scope.launch {
            val file = sharedFileDao.getFile(sha256)
            if (file == null) {
                Log.w(TAG, "requestFile: Unknown file $sha256 — not in database")
                return@launch
            }
            val received = receivedChunks.computeIfAbsent(sha256) {
                java.util.Collections.synchronizedSet(mutableSetOf())
            }

            // Resume: load previously persisted chunk records
            if (received.isEmpty() && downloadedChunkDao != null) {
                val persisted = downloadedChunkDao.getReceivedChunks(sha256)
                if (persisted.isNotEmpty()) {
                    received.addAll(persisted)
                    Log.d(TAG, "Resumed $sha256: ${persisted.size}/${file.totalChunks} chunks already on disk")
                }
            }

            // Multi-source: collect all known sources, fall back to original sharedBy
            val sources = fileSources[sha256]?.toList()?.takeIf { it.isNotEmpty() }
                ?: listOf(file.sharedBy)
            val sourceCount = sources.size

            var requested = 0
            for (chunk in 0 until file.totalChunks) {
                if (chunk in received) continue
                // Round-robin chunk assignment across sources (BitTorrent multi-peer pattern)
                val targetPeer = sources[chunk % sourceCount]
                val requestPayload = "$sha256$SEPARATOR$chunk"
                val packet = Packet(
                    id = UUID.randomUUID().toString(),
                    type = PacketType.FILE_REQUEST,
                    sourceId = localUsername,
                    destId = targetPeer,
                    payload = requestPayload.toByteArray(StandardCharsets.UTF_8),
                    timestamp = System.currentTimeMillis()
                )
                sendPacket?.invoke(packet)
                requested++
            }
            Log.d(TAG, "Requested $requested chunks for $sha256 from $sourceCount source(s) (${received.size} already received)")
        }
    }

    /**
     * Handle a received FILE_REQUEST. Reads the requested chunk from local
     * storage and sends a FILE_CHUNK response.
     */
    fun handleFileRequest(packet: Packet) {
        val parts = String(packet.payload, StandardCharsets.UTF_8).split(SEPARATOR)
        if (parts.size < 2) return
        val sha256 = parts[0]
        val chunkIndex = parts[1].toIntOrNull() ?: return
        val filePath = localFiles[sha256] ?: return

        scope.launch {
            try {
                RandomAccessFile(filePath, "r").use { file ->
                    val offset = chunkIndex.toLong() * CHUNK_SIZE
                    file.seek(offset)
                    val buffer = ByteArray(CHUNK_SIZE)
                    val bytesRead = file.read(buffer)

                    if (bytesRead <= 0) return@launch
                    val chunkData = if (bytesRead < CHUNK_SIZE) buffer.copyOf(bytesRead) else buffer

                    // Build chunk payload: sha256|chunkIndex|<binary>
                    val header = "$sha256$SEPARATOR$chunkIndex$SEPARATOR"
                        .toByteArray(StandardCharsets.UTF_8)
                    val payload = header + chunkData

                    val responsePacket = Packet(
                        id = UUID.randomUUID().toString(),
                        type = PacketType.FILE_CHUNK,
                        sourceId = localUsername,
                        destId = packet.sourceId,
                        payload = payload,
                        timestamp = System.currentTimeMillis()
                    )
                    sendPacket?.invoke(responsePacket)
                    Log.d(TAG, "Sent chunk $chunkIndex of $sha256 to ${packet.sourceId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error reading chunk $chunkIndex of $sha256: ${e.message}")
            }
        }
    }

    /**
     * Handle a received FILE_CHUNK. Writes the chunk to local storage
     * and updates download progress.
     */
    fun handleFileChunk(packet: Packet) {
        val payload = packet.payload
        // Parse header: sha256|chunkIndex|
        val headerEnd = findSecondSeparator(payload)
        if (headerEnd < 0) return

        val header = String(payload, 0, headerEnd, StandardCharsets.UTF_8)
        val parts = header.split(SEPARATOR)
        if (parts.size < 2) return

        val sha256 = parts[0]
        val chunkIndex = parts[1].toIntOrNull() ?: return
        val chunkData = payload.copyOfRange(headerEnd + 1, payload.size)

        val received = receivedChunks.computeIfAbsent(sha256) {
            java.util.Collections.synchronizedSet(mutableSetOf())
        }
        // Atomic dedup: add() returns false if already present
        if (!received.add(chunkIndex)) return // Already have this chunk

        scope.launch {
            try {
                val file = sharedFileDao.getFile(sha256) ?: return@launch
                val destFile = File(shareDir, "${sha256}_${file.fileName}")
                RandomAccessFile(destFile.absolutePath, "rw").use { raf ->
                    raf.seek(chunkIndex.toLong() * file.chunkSize)
                    raf.write(chunkData)
                }

                received.add(chunkIndex)

                // Persist chunk receipt for resume-on-interrupt
                downloadedChunkDao?.markChunkReceived(
                    com.fyp.resilientp2p.data.DownloadedChunk(sha256, chunkIndex)
                )

                val downloadComplete = received.size >= file.totalChunks
                if (downloadComplete) {
                    // Verify file integrity against announced SHA-256
                    val actualHash = computeSha256(destFile)
                    if (actualHash != null && actualHash != sha256) {
                        Log.e(TAG, "SHA-256 MISMATCH for ${file.fileName}: expected=$sha256 actual=$actualHash — deleting corrupt file")
                        destFile.delete()
                        received.clear()
                        receivedChunks.remove(sha256)
                        downloadedChunkDao?.clearChunks(sha256)
                        sharedFileDao.updateProgress(sha256, 0, null)
                        return@launch
                    }
                }
                val localPath = if (downloadComplete) destFile.absolutePath else null
                sharedFileDao.updateProgress(sha256, received.size, localPath)

                if (localPath != null) {
                    localFiles[sha256] = localPath
                    receivedChunks.remove(sha256) // Cleanup tracking after complete download
                    downloadedChunkDao?.clearChunks(sha256) // Cleanup persisted chunks
                    Log.d(TAG, "File download complete: ${file.fileName} ($sha256)")
                } else {
                    Log.d(TAG, "Chunk $chunkIndex/${file.totalChunks} of ${file.fileName}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error writing chunk $chunkIndex of $sha256: ${e.message}")
            }
        }
    }

    /** Find the byte offset of the second "|" in a byte array. */
    private fun findSecondSeparator(data: ByteArray): Int {
        var count = 0
        for (i in data.indices) {
            if (data[i] == '|'.code.toByte()) {
                count++
                if (count == 2) return i
            }
        }
        return -1
    }

    /** Compute SHA-256 hex hash of a file. */
    private fun computeSha256(file: File): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            FileInputStream(file).use { fis ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (fis.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "SHA-256 computation failed: ${e.message}")
            null
        }
    }

    /** Cancel background coroutines. Call on application shutdown. */
    fun destroy() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
