package com.fyp.resilientp2p

import com.fyp.resilientp2p.audio.AudioCodecManager
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for [AudioCodecManager] constants and configuration.
 *
 * Note: MediaCodec (AAC encode/decode) requires Android runtime and cannot be
 * tested in pure JVM unit tests. These tests validate the codec configuration
 * constants that drive the audio pipeline. Full encode/decode round-trip tests
 * must be run as Android instrumented tests.
 */
class AudioCodecTest {

    @Test
    fun `sample rate is 8kHz`() {
        assertEquals(8000, AudioCodecManager.SAMPLE_RATE)
    }

    @Test
    fun `bit rate is 24kbps`() {
        assertEquals(24_000, AudioCodecManager.BIT_RATE)
    }

    @Test
    fun `mono channel count`() {
        assertEquals(1, AudioCodecManager.CHANNELS)
    }

    @Test
    fun `frame duration is 20ms`() {
        assertEquals(20, AudioCodecManager.FRAME_DURATION_MS)
    }

    @Test
    fun `samples per frame correct for 8kHz at 20ms`() {
        // 8000 samples/sec * 20ms / 1000 = 160 samples
        val expected = AudioCodecManager.SAMPLE_RATE * AudioCodecManager.FRAME_DURATION_MS / 1000
        assertEquals(expected, AudioCodecManager.SAMPLES_PER_FRAME)
        assertEquals(160, AudioCodecManager.SAMPLES_PER_FRAME)
    }

    @Test
    fun `bytes per frame correct for 16-bit samples`() {
        // 160 samples * 2 bytes/sample = 320 bytes
        val expected = AudioCodecManager.SAMPLES_PER_FRAME * 2
        assertEquals(expected, AudioCodecManager.BYTES_PER_FRAME)
        assertEquals(320, AudioCodecManager.BYTES_PER_FRAME)
    }

    @Test
    fun `raw PCM bandwidth calculation`() {
        // 8000 Hz * 16-bit * mono = 128 kbps = 16 KB/s
        val rawBitsPerSec = AudioCodecManager.SAMPLE_RATE * 16 * AudioCodecManager.CHANNELS
        assertEquals(128_000, rawBitsPerSec)
        assertEquals(16_000, rawBitsPerSec / 8) // bytes per second
    }

    @Test
    fun `AAC compression ratio is significant`() {
        val rawBytesPerSec = AudioCodecManager.SAMPLE_RATE * 2 * AudioCodecManager.CHANNELS // 16000
        val aacBytesPerSec = AudioCodecManager.BIT_RATE / 8 // 3000 bytes/sec
        val ratio = rawBytesPerSec.toDouble() / aacBytesPerSec
        // AAC should compress ~5x
        assertTrue("Compression ratio should be > 4x, was ${ratio}x", ratio > 4.0)
    }

    @Test
    fun `frames per second at 8kHz and 20ms`() {
        val framesPerSec = 1000 / AudioCodecManager.FRAME_DURATION_MS
        assertEquals(50, framesPerSec) // 50 frames/sec
    }

    @Test
    fun `mesh batch parameters are reasonable`() {
        // MeshAudioManager batches 5 frames = 100ms per packet = 10 packets/sec
        val batchFrames = 5
        val batchMs = batchFrames * AudioCodecManager.FRAME_DURATION_MS
        assertEquals(100, batchMs)

        val packetsPerSec = 1000 / batchMs
        assertEquals(10, packetsPerSec)

        val batchPcmBytes = batchFrames * AudioCodecManager.BYTES_PER_FRAME
        assertEquals(1600, batchPcmBytes)
    }
}
