package com.fyp.resilientp2p.audio

import com.fyp.resilientp2p.data.LogLevel

/**
 * Abstract base for audio I/O buffers used by [AudioRecorder] and [AudioPlayer].
 *
 * Handles platform-specific minimum buffer size calculation at construction time.
 * Subclasses provide [getMinBufferSize] backed by `AudioRecord` or `AudioTrack`.
 *
 * Constants:
 * - Sample rate: 8 kHz (telephone quality, halves bandwidth vs 16 kHz).
 * - Encoding: 16-bit PCM mono.
 *
 * @param log Logging callback forwarded to [P2PManager.log].
 * @see AudioRecorder
 * @see AudioPlayer
 */
abstract class AudioBuffer(private val log: (String, LogLevel) -> Unit) {
    companion object {
        private const val TAG = "AudioBuffer"
    }

    val size: Int
    val sampleRate = 8000  // Telephone-quality; halves bandwidth from 32KB/s to 16KB/s
    val data: ByteArray

    init {
        val calculatedSize = getMinBufferSize(sampleRate)
        if (!validSize(calculatedSize)) {
            throw IllegalStateException("Failed to get valid audio buffer size with sample rate $sampleRate")
        }
        this.size = calculatedSize
        data = ByteArray(size)
    }

    protected abstract fun validSize(size: Int): Boolean

    protected abstract fun getMinBufferSize(sampleRate: Int): Int
}
