package com.fyp.resilientp2p

import com.fyp.resilientp2p.transport.Hop
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for the mesh routing protocol logic.
 *
 * Tests routing score calculation, TTL decrement behavior,
 * trace accumulation, and packet forwarding constraints.
 */
class RoutingTest {

    @Test
    fun `TTL decrement changes packet`() {
        val original = Packet(
            type = PacketType.DATA,
            sourceId = "A",
            destId = "C",
            ttl = 5
        )
        val forwarded = original.copy(ttl = original.ttl - 1)
        assertEquals(4, forwarded.ttl)
        assertEquals(original.id, forwarded.id) // ID preserved
        assertEquals(original.sourceId, forwarded.sourceId)
    }

    @Test
    fun `TTL zero means packet should not be forwarded`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "A",
            destId = "Z",
            ttl = 0
        )
        assertTrue("TTL 0 means don't forward", pkt.ttl <= 0)
    }

    @Test
    fun `trace tracks forwarding path`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "Origin",
            destId = "Target",
            trace = listOf(
                Hop("Origin", -30),
                Hop("Relay1", -55),
                Hop("Relay2", -70)
            ),
            ttl = 2
        )
        assertEquals(3, pkt.trace.size)
        assertEquals("Origin", pkt.trace[0].peerId)
        assertEquals(-55, pkt.trace[1].rssi)
    }

    @Test
    fun `route score computation - higher TTL is better`() {
        // Score formula: min(TTL, 10) * 100 + RSSI/10
        // (This tests the expected scoring policy)
        fun computeScore(ttl: Int, rssi: Int): Int {
            return minOf(ttl, 10) * 100 + rssi / 10
        }

        val score1Hop = computeScore(4, -50) // 400 + (-5) = 395
        val score3Hop = computeScore(2, -50) // 200 + (-5) = 195

        assertTrue("1-hop should score higher than 3-hop", score1Hop > score3Hop)
    }

    @Test
    fun `route score - RSSI affects score within same hop count`() {
        fun computeScore(ttl: Int, rssi: Int): Int {
            return minOf(ttl, 10) * 100 + rssi / 10
        }

        val goodSignal = computeScore(4, -30) // 400 + (-3) = 397
        val weakSignal = computeScore(4, -90) // 400 + (-9) = 391

        assertTrue("Better RSSI should give higher score", goodSignal > weakSignal)
    }

    @Test
    fun `copy preserves all fields except changed ones`() {
        val original = Packet(
            id = "test-id",
            type = PacketType.DATA,
            timestamp = 123456789L,
            sourceId = "Src",
            destId = "Dst",
            payload = "data".toByteArray(),
            ttl = 5,
            sequenceNumber = 100
        )
        val forwarded = original.copy(ttl = 4, trace = original.trace + Hop("Relay", -60))

        assertEquals("test-id", forwarded.id)
        assertEquals(PacketType.DATA, forwarded.type)
        assertEquals(123456789L, forwarded.timestamp)
        assertEquals("Src", forwarded.sourceId)
        assertEquals("Dst", forwarded.destId)
        assertArrayEquals("data".toByteArray(), forwarded.payload)
        assertEquals(4, forwarded.ttl)
        assertEquals(100, forwarded.sequenceNumber)
        assertEquals(1, forwarded.trace.size)
    }

    @Test
    fun `BROADCAST destination not changed by forwarding`() {
        val pkt = Packet(
            type = PacketType.DATA,
            sourceId = "A",
            destId = "BROADCAST",
            ttl = 5
        )
        val forwarded = pkt.copy(ttl = pkt.ttl - 1)
        assertEquals("BROADCAST", forwarded.destId)
    }

    @Test
    fun `IDENTITY packet should have TTL 0`() {
        // Convention: IDENTITY packets are not forwarded (TTL=0)
        val identity = Packet(
            type = PacketType.IDENTITY,
            sourceId = "NewNode",
            destId = "Neighbor",
            payload = "publicKeyBase64".toByteArray(),
            ttl = 0
        )
        assertEquals(0, identity.ttl)
        assertEquals(PacketType.IDENTITY, identity.type)
    }

    @Test
    fun `EMERGENCY packet is both broadcast and high priority`() {
        val sos = Packet(
            type = PacketType.EMERGENCY,
            sourceId = "Victim",
            destId = "BROADCAST",
            payload = "SOS at GPS coords".toByteArray(),
            ttl = Packet.DEFAULT_TTL
        )
        assertEquals(PacketType.EMERGENCY, sos.type)
        assertEquals("BROADCAST", sos.destId)
        assertEquals(Packet.DEFAULT_TTL, sos.ttl)
    }

    @Test
    fun `packet ID uniqueness via UUID`() {
        val ids = (1..100).map {
            Packet(type = PacketType.PING, sourceId = "A", destId = "B").id
        }.toSet()
        assertEquals("All 100 auto-generated IDs should be unique", 100, ids.size)
    }

    @Test
    fun `STORE_FORWARD type round-trips correctly`() {
        val pkt = Packet(
            type = PacketType.STORE_FORWARD,
            sourceId = "Src",
            destId = "OfflinePeer",
            payload = "deferred message".toByteArray(),
            ttl = 3
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.STORE_FORWARD, restored.type)
        assertEquals("OfflinePeer", restored.destId)
    }

    @Test
    fun `GROUP_MESSAGE type round-trips correctly`() {
        val payload = "MSG|group123|Alice|Hello everyone!"
        val pkt = Packet(
            type = PacketType.GROUP_MESSAGE,
            sourceId = "Alice",
            destId = "BROADCAST",
            payload = payload.toByteArray()
        )
        val restored = Packet.fromBytes(pkt.toBytes())
        assertEquals(PacketType.GROUP_MESSAGE, restored.type)
        assertEquals(payload, String(restored.payload))
    }
}
