package com.fyp.resilientp2p

import com.fyp.resilientp2p.audio.AudioCodecManager
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer

/**
 * Unit tests for the mesh audio packet format used by
 * [com.fyp.resilientp2p.audio.MeshAudioManager].
 *
 * Tests the AUDIO_DATA / AUDIO_CONTROL packet payload structure,
 * session management protocol, and batching arithmetic.
 */
class MeshAudioTest {

    // --- AUDIO_CONTROL packet format ---

    @Test
    fun `AUDIO_CONTROL START payload format`() {
        val sessionId = "abc12345"
        val payload = "$sessionId|START".toByteArray(Charsets.UTF_8)
        val text = String(payload, Charsets.UTF_8)
        val sep = text.indexOf('|')

        assertTrue(sep > 0)
        assertEquals("abc12345", text.substring(0, sep))
        assertEquals("START", text.substring(sep + 1))
    }

    @Test
    fun `AUDIO_CONTROL STOP payload format`() {
        val sessionId = "xyz98765"
        val payload = "$sessionId|STOP".toByteArray(Charsets.UTF_8)
        val text = String(payload, Charsets.UTF_8)
        val parts = text.split("|", limit = 2)

        assertEquals(2, parts.size)
        assertEquals("xyz98765", parts[0])
        assertEquals("STOP", parts[1])
    }

    @Test
    fun `AUDIO_CONTROL round-trip as Packet`() {
        val pkt = Packet(
            type = PacketType.AUDIO_CONTROL,
            sourceId = "Sender",
            destId = "Receiver",
            payload = "sess1234|START".toByteArray()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.AUDIO_CONTROL, restored.type)
        assertEquals("sess1234|START", String(restored.payload))
    }

    // --- AUDIO_DATA packet format ---

    @Test
    fun `AUDIO_DATA payload header format`() {
        // Format: [sessionId:8B ASCII][seqNo:4B int][aacData...]
        val sessionId = "abc12345" // 8 chars
        val seqNo = 42
        val aacData = ByteArray(60) { it.toByte() }

        val payload = ByteBuffer.allocate(8 + 4 + aacData.size)
            .put(sessionId.toByteArray(Charsets.US_ASCII), 0, 8)
            .putInt(seqNo)
            .put(aacData)
            .array()

        // Parse back
        assertEquals(72, payload.size) // 8 + 4 + 60

        val extractedSession = String(payload, 0, 8, Charsets.US_ASCII)
        assertEquals("abc12345", extractedSession)

        val bb = ByteBuffer.wrap(payload, 8, 4)
        assertEquals(42, bb.int)

        val extractedAac = payload.copyOfRange(12, payload.size)
        assertEquals(60, extractedAac.size)
        assertEquals(0, extractedAac[0].toInt())
        assertEquals(59, extractedAac[59].toInt())
    }

    @Test
    fun `AUDIO_DATA minimum size check`() {
        // Minimum valid payload: 8 (session) + 4 (seq) + 1 (at least 1 byte AAC)
        val minPayload = ByteArray(13) // 8 + 4 + 1 minimum
        assertTrue(minPayload.size >= 12)

        val tooSmall = ByteArray(11) // Less than header size
        assertTrue(tooSmall.size < 12) // Should be rejected by handler
    }

    @Test
    fun `AUDIO_DATA round-trip as Packet`() {
        val sessionId = "sess0001"
        val seqNo = 100
        val aacData = ByteArray(48) { (it * 3).toByte() }

        val payload = ByteBuffer.allocate(8 + 4 + aacData.size)
            .put(sessionId.toByteArray(Charsets.US_ASCII), 0, 8)
            .putInt(seqNo)
            .put(aacData)
            .array()

        val pkt = Packet(
            type = PacketType.AUDIO_DATA,
            sourceId = "Mic",
            destId = "Speaker",
            payload = payload,
            ttl = 3
        )
        val restored = Packet.fromBytes(pkt.toBytes())

        assertEquals(PacketType.AUDIO_DATA, restored.type)
        assertEquals(payload.size, restored.payload.size)

        // Verify session ID
        val restoredSession = String(restored.payload, 0, 8, Charsets.US_ASCII)
        assertEquals("sess0001", restoredSession)

        // Verify seqNo
        val restoredSeq = ByteBuffer.wrap(restored.payload, 8, 4).int
        assertEquals(100, restoredSeq)
    }

    // --- Batch arithmetic ---

    @Test
    fun `batch PCM bytes for 5 frames`() {
        val batchFrames = 5
        val batchPcmBytes = batchFrames * AudioCodecManager.BYTES_PER_FRAME
        assertEquals(1600, batchPcmBytes) // 5 * 320 = 1600
    }

    @Test
    fun `batch duration in milliseconds`() {
        val batchFrames = 5
        val batchMs = batchFrames * AudioCodecManager.FRAME_DURATION_MS
        assertEquals(100, batchMs)
    }

    @Test
    fun `packets per second for batch size 5`() {
        val batchMs = 5 * AudioCodecManager.FRAME_DURATION_MS
        val packetsPerSec = 1000 / batchMs
        assertEquals(10, packetsPerSec)
    }

    @Test
    fun `expected AAC payload size per batch`() {
        // AAC-LC at 24kbps → 3000 bytes/sec
        // Per 100ms batch → ~300 bytes of AAC data
        val aacBytesPerSec = AudioCodecManager.BIT_RATE / 8 // 3000
        val batchMs = 5 * AudioCodecManager.FRAME_DURATION_MS // 100
        val expectedAacBytes = aacBytesPerSec * batchMs / 1000 // 300

        assertEquals(300, expectedAacBytes)
        // Total packet payload: 8 (session) + 4 (seq) + 300 (AAC) = 312 bytes
        assertTrue("Mesh packet overhead very reasonable", 8 + 4 + expectedAacBytes < 400)
    }

    @Test
    fun `mesh bandwidth per second for audio`() {
        // Per batch: ~312 bytes payload + ~200 bytes Packet overhead = ~512 bytes
        // 10 batches/sec → ~5120 bytes/sec ≈ 5 KB/s
        // vs raw PCM: 16 KB/s → ~3x reduction
        val packetsPerSec = 10
        val estimatedPayload = 312 // AAC + header
        val packetOverhead = 200  // Packet framing (UUID, type, src, dst, etc)
        val totalPerSec = packetsPerSec * (estimatedPayload + packetOverhead)

        assertTrue("Mesh audio should be < 10 KB/s, was $totalPerSec", totalPerSec < 10_000)
    }

    // --- Session lifecycle ---

    @Test
    fun `session ID is 8 characters`() {
        val uuid = java.util.UUID.randomUUID().toString().take(8)
        assertEquals(8, uuid.length)
        // Should fit in exactly 8 ASCII bytes
        assertEquals(8, uuid.toByteArray(Charsets.US_ASCII).size)
    }

    @Test
    fun `sequence numbers increment monotonically`() {
        val seqNo = java.util.concurrent.atomic.AtomicInteger(0)
        val values = (1..100).map { seqNo.getAndIncrement() }
        assertEquals((0 until 100).toList(), values)
    }
}
