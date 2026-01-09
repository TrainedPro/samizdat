package com.fyp.resilientp2p.audio

import com.fyp.resilientp2p.data.LogLevel

/** A buffer that handles the size calculation for AudioRecord and AudioTrack. */
abstract class AudioBuffer(private val log: (String, LogLevel) -> Unit) {
    companion object {
        private const val TAG = "AudioBuffer"
    }

    val size: Int
    val sampleRate = 16000
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
