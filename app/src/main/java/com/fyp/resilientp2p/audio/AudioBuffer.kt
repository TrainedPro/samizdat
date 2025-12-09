package com.fyp.resilientp2p.audio

import com.fyp.resilientp2p.data.LogLevel

/** A buffer that handles the size calculation for AudioRecord and AudioTrack. */
abstract class AudioBuffer(private val log: (String, LogLevel) -> Unit) {
    companion object {
        private const val TAG = "AudioBuffer"
    }

    val size: Int
    val sampleRate = 44100
    val data: ByteArray

    init {
        var size = getMinBufferSize(sampleRate)
        if (!validSize(size)) {
            log("[$TAG] Failed to get min buffer size with sample rate $sampleRate", LogLevel.WARN)
            size = 0
        }
        this.size = size
        data = ByteArray(size)
    }

    protected abstract fun validSize(size: Int): Boolean

    protected abstract fun getMinBufferSize(sampleRate: Int): Int
}
