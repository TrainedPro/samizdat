package com.fyp.resilientp2p

import org.junit.Assert.*
import org.junit.Test
import java.security.MessageDigest

/**
 * Unit tests for file sharing protocol logic.
 *
 * Tests the payload format parsing for FILE_ANNOUNCE, FILE_REQUEST, FILE_CHUNK
 * packets, SHA-256 hashing, and chunk arithmetic used by
 * [com.fyp.resilientp2p.managers.FileShareManager].
 */
class FileShareTest {

    companion object {
        const val SEPARATOR = "|"
        const val CHUNK_SIZE = 32 * 1024 // 32 KB
    }

    // --- Announce payload format ---

    @Test
    fun `FILE_ANNOUNCE payload format is correct`() {
        val sha256 = "abc123def456"
        val fileName = "photo.jpg"
        val mimeType = "image/jpeg"
        val fileSize = 128_000L
        val chunkSize = CHUNK_SIZE
        val totalChunks = ((fileSize + chunkSize - 1) / chunkSize).toInt()

        val payload = "$sha256$SEPARATOR$fileName$SEPARATOR$mimeType$SEPARATOR$fileSize$SEPARATOR$chunkSize$SEPARATOR$totalChunks"
        val parts = payload.split(SEPARATOR)

        assertEquals(6, parts.size)
        assertEquals(sha256, parts[0])
        assertEquals(fileName, parts[1])
        assertEquals(mimeType, parts[2])
        assertEquals(fileSize.toString(), parts[3])
        assertEquals(chunkSize.toString(), parts[4])
        assertEquals(totalChunks.toString(), parts[5])
    }

    @Test
    fun `FILE_REQUEST payload format is correct`() {
        val sha256 = "deadbeef"
        val chunkIndex = 7
        val payload = "$sha256$SEPARATOR$chunkIndex"
        val parts = payload.split(SEPARATOR)

        assertEquals(2, parts.size)
        assertEquals(sha256, parts[0])
        assertEquals(chunkIndex, parts[1].toInt())
    }

    // --- Chunk arithmetic ---

    @Test
    fun `total chunks calculation for exact multiple`() {
        val fileSize = 3L * CHUNK_SIZE // Exactly 3 chunks
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(3, totalChunks)
    }

    @Test
    fun `total chunks calculation with remainder`() {
        val fileSize = 3L * CHUNK_SIZE + 1 // 3 full + 1 byte partial
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(4, totalChunks)
    }

    @Test
    fun `total chunks calculation for tiny file`() {
        val fileSize = 100L // 100 bytes
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(1, totalChunks)
    }

    @Test
    fun `total chunks calculation for empty file`() {
        val fileSize = 0L
        val totalChunks = if (fileSize == 0L) 0
                          else ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(0, totalChunks)
    }

    @Test
    fun `chunk offset calculation`() {
        val chunkIndex = 5
        val offset = chunkIndex.toLong() * CHUNK_SIZE
        assertEquals(5L * 32 * 1024, offset)
    }

    @Test
    fun `5MB file has correct chunk count`() {
        val fileSize = 5L * 1024 * 1024 // 5 MB
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(160, totalChunks) // 5MB / 32KB = 160
    }

    @Test
    fun `100MB file has correct chunk count`() {
        val fileSize = 100L * 1024 * 1024
        val totalChunks = ((fileSize + CHUNK_SIZE - 1) / CHUNK_SIZE).toInt()
        assertEquals(3200, totalChunks)
    }

    // --- SHA-256 computation ---

    @Test
    fun `SHA-256 of known string matches expected`() {
        val data = "hello world".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data).joinToString("") { "%02x".format(it) }
        assertEquals(
            "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9",
            hash
        )
    }

    @Test
    fun `SHA-256 of empty byte array`() {
        val data = ByteArray(0)
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data).joinToString("") { "%02x".format(it) }
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            hash
        )
    }

    @Test
    fun `SHA-256 is 64 hex characters`() {
        val data = "test data for hashing".toByteArray()
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data).joinToString("") { "%02x".format(it) }
        assertEquals(64, hash.length)
        assertTrue(hash.matches(Regex("[0-9a-f]{64}")))
    }

    // --- FILE_CHUNK header parsing ---

    @Test
    fun `find second separator in byte array`() {
        val data = "sha256hash|42|".toByteArray() + ByteArray(100) { it.toByte() }
        var count = 0
        var secondSepIndex = -1
        for (i in data.indices) {
            if (data[i] == '|'.code.toByte()) {
                count++
                if (count == 2) {
                    secondSepIndex = i
                    break
                }
            }
        }
        assertEquals(13, secondSepIndex) // "sha256hash|42|" → second "|" at index 13
    }

    @Test
    fun `chunk data extracted correctly after header`() {
        val header = "abc123|5|"
        val chunkData = ByteArray(100) { (it + 1).toByte() }
        val payload = header.toByteArray() + chunkData

        // Find second separator
        var count = 0
        var headerEnd = -1
        for (i in payload.indices) {
            if (payload[i] == '|'.code.toByte()) {
                count++
                if (count == 2) {
                    headerEnd = i
                    break
                }
            }
        }

        val extractedHeader = String(payload, 0, headerEnd)
        val extractedChunk = payload.copyOfRange(headerEnd + 1, payload.size)

        val parts = extractedHeader.split("|")
        assertEquals("abc123", parts[0])
        assertEquals(5, parts[1].toInt())
        assertEquals(100, extractedChunk.size)
        assertEquals(1, extractedChunk[0].toInt())
    }

    // --- Resume logic ---

    @Test
    fun `resume correctly identifies missing chunks`() {
        val totalChunks = 10
        val received = setOf(0, 1, 2, 5, 7) // 5 of 10 received

        val missing = (0 until totalChunks).filter { it !in received }
        assertEquals(listOf(3, 4, 6, 8, 9), missing)
    }

    @Test
    fun `complete download has no missing chunks`() {
        val totalChunks = 5
        val received = setOf(0, 1, 2, 3, 4)
        val missing = (0 until totalChunks).filter { it !in received }
        assertTrue(missing.isEmpty())
    }

    @Test
    fun `fresh download has all chunks missing`() {
        val totalChunks = 5
        val received = emptySet<Int>()
        val missing = (0 until totalChunks).filter { it !in received }
        assertEquals(5, missing.size)
    }
}
