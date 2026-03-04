package com.fyp.resilientp2p

import com.fyp.resilientp2p.transport.Hop
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [Packet] binary serialization/deserialization.
 *
 * Covers round-trip fidelity, all packet types, edge cases,
 * and malformed data rejection.
 */
class PacketTest {

    // --- Round-trip tests ---

    @Test
    fun `round-trip DATA packet preserves all fields`() {
        val original = Packet(
            id = UUID.randomUUID().toString(),
            type = PacketType.DATA,
            timestamp = System.currentTimeMillis(),
            sourceId = "DeviceA",
            destId = "DeviceB",
            payload = "Hello Mesh!".toByteArray(),
            ttl = 5,
            trace = listOf(Hop("DeviceA", -50), Hop("Relay1", -72)),
            sequenceNumber = 42
        )
        val bytes = original.toBytes()
        val restored = Packet.fromBytes(bytes)

        assertEquals(original.id, restored.id)
        assertEquals(original.type, restored.type)
        assertEquals(original.timestamp, restored.timestamp)
        assertEquals(original.sourceId, restored.sourceId)
        assertEquals(original.destId, restored.destId)
        assertArrayEquals(original.payload, restored.payload)
        assertEquals(original.ttl, restored.ttl)
        assertEquals(original.trace.size, restored.trace.size)
        assertEquals(original.trace[0].peerId, restored.trace[0].peerId)
        assertEquals(original.trace[0].rssi, restored.trace[0].rssi)
        assertEquals(original.trace[1].peerId, restored.trace[1].peerId)
        assertEquals(original.sequenceNumber, restored.sequenceNumber)
    }

    @Test
    fun `round-trip all packet types`() {
        for (type in PacketType.entries) {
            val pkt = Packet(
                type = type,
                sourceId = "Src",
                destId = "Dst",
                payload = "type=$type".toByteArray()
            )
            val restored = Packet.fromBytes(pkt.toBytes())
            assertEquals("Type mismatch for $type", type, restored.type)
            assertEquals("type=$type", String(restored.payload))
        }
    }

    @Test
    fun `round-trip empty payload`() {
        val pkt = Packet(
            type = PacketType.PING,
            sourceId = "A",
            destId = "B",
            payload = ByteArray(0)
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(0, restored.payload.size)
    }

    @Test
    fun `round-trip BROADCAST destination`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "Node1",
            destId = "BROADCAST",
            payload = "flood".toByteArray()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals("BROADCAST", restored.destId)
    }

    @Test
    fun `round-trip empty trace`() {
        val pkt = Packet(
            type = PacketType.IDENTITY,
            sourceId = "X",
            destId = "Y",
            trace = emptyList()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertTrue(restored.trace.isEmpty())
    }

    @Test
    fun `round-trip large trace (max hops)`() {
        val trace = (1..20).map { Hop("Hop$it", -40 - it) }
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "Src",
            destId = "Dst",
            trace = trace
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(20, restored.trace.size)
        assertEquals("Hop1", restored.trace[0].peerId)
        assertEquals("Hop20", restored.trace[19].peerId)
    }

    @Test
    fun `round-trip maximum TTL`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "A",
            destId = "B",
            ttl = 255
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(255, restored.ttl)
    }

    @Test
    fun `round-trip zero TTL`() {
        val pkt = Packet(
            type = PacketType.IDENTITY,
            sourceId = "A",
            destId = "B",
            ttl = 0
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(0, restored.ttl)
    }

    @Test
    fun `round-trip AUDIO_DATA packet`() {
        val aacPayload = ByteArray(64) { it.toByte() }
        val pkt = Packet(
            type = PacketType.AUDIO_DATA,
            sourceId = "Mic",
            destId = "Speaker",
            payload = aacPayload,
            ttl = 3
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.AUDIO_DATA, restored.type)
        assertArrayEquals(aacPayload, restored.payload)
    }

    @Test
    fun `round-trip AUDIO_CONTROL packet`() {
        val payload = "abc12345|START".toByteArray()
        val pkt = Packet(
            type = PacketType.AUDIO_CONTROL,
            sourceId = "A",
            destId = "B",
            payload = payload
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.AUDIO_CONTROL, restored.type)
        assertEquals("abc12345|START", String(restored.payload))
    }

    @Test
    fun `round-trip EMERGENCY packet`() {
        val pkt = Packet(
            type = PacketType.EMERGENCY,
            sourceId = "Alice",
            destId = "BROADCAST",
            payload = "SOS: Need help at coordinates 33.5,71.2".toByteArray()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.EMERGENCY, restored.type)
        assertTrue(String(restored.payload).startsWith("SOS"))
    }

    @Test
    fun `round-trip unicode peer names`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "設備A",
            destId = "جهاز-ب",
            payload = "unicode test".toByteArray()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals("設備A", restored.sourceId)
        assertEquals("جهاز-ب", restored.destId)
    }

    // --- Validation tests ---

    @Test(expected = IllegalArgumentException::class)
    fun `negative TTL throws`() {
        Packet(type = PacketType.DATA, sourceId = "A", destId = "B", ttl = -1).toBytes()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `TTL above 255 throws`() {
        Packet(type = PacketType.DATA, sourceId = "A", destId = "B", ttl = 256).toBytes()
    }

    @Test(expected = IllegalArgumentException::class)
    fun `payload above 1MB throws`() {
        val bigPayload = ByteArray(1024 * 1024 + 1)
        Packet(type = PacketType.DATA, sourceId = "A", destId = "B", payload = bigPayload).toBytes()
    }

    @Test(expected = Exception::class)
    fun `truncated bytes throws`() {
        val pkt = Packet(type = PacketType.DATA, sourceId = "A", destId = "B", payload = "test".toByteArray())
        val bytes = pkt.toBytes()
        Packet.fromBytes(bytes.copyOf(bytes.size / 2))
    }

    @Test(expected = Exception::class)
    fun `empty byte array throws`() {
        Packet.fromBytes(ByteArray(0))
    }

    // --- Serialization size tests ---

    @Test
    fun `minimal packet is compact`() {
        val pkt = Packet(type = PacketType.PING, sourceId = "A", destId = "B")
        val bytes = pkt.toBytes()
        // UUID(36) + type + timestamps + IDs + overhead ≈ 100-200 bytes
        assertTrue("Minimal packet should be < 300 bytes, was ${bytes.size}", bytes.size < 300)
    }

    @Test
    fun `sequence number preserved`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "A",
            destId = "B",
            sequenceNumber = Long.MAX_VALUE
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(Long.MAX_VALUE, restored.sequenceNumber)
    }
}
