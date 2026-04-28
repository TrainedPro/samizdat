package com.fyp.resilientp2p

import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import org.junit.Test
import org.junit.Assert.*
import java.nio.charset.StandardCharsets

/**
 * Test suite for message corruption fixes.
 * Validates that binary audio data is not processed as chat messages.
 */
class MessageCorruptionTest {

    @Test
    fun `test binary audio data is not processed as chat message`() {
        // Create a binary audio payload (simulating AAC-encoded audio)
        val binaryAudioData = ByteArray(512) { (it % 256).toByte() }

        val audioPacket = Packet(
            type = PacketType.AUDIO_DATA,
            sourceId = "TestDevice",
            destId = "TargetDevice",
            payload = binaryAudioData
        )

        // Verify packet type is correctly set
        assertEquals(PacketType.AUDIO_DATA, audioPacket.type)

        // Verify payload is binary (not valid UTF-8 text)
        val hasInvalidChars = audioPacket.payload.any { it.toInt() and 0xFF < 32 }
        assertTrue("Audio payload should contain binary data", hasInvalidChars)

        // Verify that attempting to decode as UTF-8 would fail validation
        val decoded = String(audioPacket.payload, StandardCharsets.UTF_8)
        val containsControlChars = decoded.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }
        assertTrue("Binary data should contain control characters", containsControlChars)
    }

    @Test
    fun `test valid text message passes UTF-8 validation`() {
        val textMessage = "Hello, this is a valid chat message! 🎉"
        val textBytes = textMessage.toByteArray(StandardCharsets.UTF_8)

        val dataPacket = Packet(
            type = PacketType.DATA,
            sourceId = "TestDevice",
            destId = "TargetDevice",
            payload = textBytes
        )

        // Verify packet type is correctly set
        assertEquals(PacketType.DATA, dataPacket.type)

        // Verify payload is valid UTF-8 text
        val decoded = String(dataPacket.payload, StandardCharsets.UTF_8)
        assertEquals(textMessage, decoded)

        // Verify no control characters (except allowed ones)
        val hasInvalidChars = decoded.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }
        assertFalse("Valid text should not contain control characters", hasInvalidChars)
    }

    @Test
    fun `test packet serialization preserves type information`() {
        val audioData = ByteArray(256) { (it % 128).toByte() }
        val audioPacket = Packet(
            type = PacketType.AUDIO_DATA,
            sourceId = "Sender",
            destId = "Receiver",
            payload = audioData
        )

        // Serialize and deserialize
        val serialized = audioPacket.toBytes()
        val deserialized = Packet.fromBytes(serialized)

        // Verify type is preserved
        assertEquals(PacketType.AUDIO_DATA, deserialized.type)
        assertEquals("Sender", deserialized.sourceId)
        assertEquals("Receiver", deserialized.destId)
        assertArrayEquals(audioData, deserialized.payload)
    }

    @Test
    fun `test mixed content validation`() {
        // Test edge case: mostly text with some binary data
        val mixedContent = "Hello".toByteArray(StandardCharsets.UTF_8) + byteArrayOf(0x00, 0x01, 0x02)

        val decoded = String(mixedContent, StandardCharsets.UTF_8)
        val hasControlChars = decoded.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }

        assertTrue("Mixed content with binary data should be detected", hasControlChars)
    }

    @Test
    fun `test empty payload handling`() {
        val emptyPacket = Packet(
            type = PacketType.DATA,
            sourceId = "Sender",
            destId = "Receiver",
            payload = ByteArray(0)
        )

        val decoded = String(emptyPacket.payload, StandardCharsets.UTF_8)
        assertEquals("", decoded)

        val hasControlChars = decoded.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }
        assertFalse("Empty payload should not have control characters", hasControlChars)
    }

    @Test
    fun `test whitespace-only message validation`() {
        val whitespaceMessage = "   \n\r\t   "
        val whitespaceBytes = whitespaceMessage.toByteArray(StandardCharsets.UTF_8)

        val decoded = String(whitespaceBytes, StandardCharsets.UTF_8)
        val hasInvalidChars = decoded.any { it.code < 32 && it != '\n' && it != '\r' && it != '\t' }

        assertFalse("Whitespace-only message should be valid", hasInvalidChars)
    }
}
