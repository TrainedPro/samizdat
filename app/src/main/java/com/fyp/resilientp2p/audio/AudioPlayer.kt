package com.fyp.resilientp2p.audio

import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Process
import com.fyp.resilientp2p.data.LogLevel
import java.io.IOException
import java.io.InputStream

/**
 * A fire-once class. When created, you must pass a [InputStream]. Once [start] is called, the input
 * stream will be read from until either [stop] is called or the stream ends.
 */
open class AudioPlayer(
        private val inputStream: InputStream,
        private val log: (String, LogLevel) -> Unit,
        private val peerRttMs: Long = -1  // -1 = unknown, use default
) {

    companion object {
        private const val TAG = "AudioPlayer"
    }

    /**
     * If true, the background thread will continue to loop and play audio. Once false, the thread
     * will shut down.
     */
    @Volatile private var isAlive = false

    /** The background thread recording audio for us. */
    private var thread: Thread? = null

    /** @return True if currently playing. */
    fun isPlaying(): Boolean = isAlive

    /** Starts playing the stream. */
    fun start() {
        if (isPlaying()) {
            return
        }
        isAlive = true
        thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val buffer = Buffer(log)
            val audioTrack =
                    android.media.AudioTrack.Builder()
                            .setAudioAttributes(
                                    android.media.AudioAttributes.Builder()
                                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                            .setContentType(
                                                    android.media.AudioAttributes.CONTENT_TYPE_MUSIC
                                            )
                                            .build()
                            )
                            .setAudioFormat(
                                    AudioFormat.Builder()
                                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                            .setSampleRate(buffer.sampleRate)
                                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                            .build()
                            )
                            .setBufferSizeInBytes(buffer.size)
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .build()
            // State check before play
            if (audioTrack.state != AudioTrack.STATE_INITIALIZED) {
                log("[$TAG] Failed to initialize AudioTrack", LogLevel.ERROR)
                stopInternal()
                audioTrack.release()
                return@Thread
            }
            var len: Int = 0
            try {
                audioTrack.play()
                val data = buffer.data

                // --- Adaptive Jitter Buffer: pre-fill before playback ---
                // At 8kHz mono 16-bit PCM = 16000 bytes/sec.
                // Scale buffer size based on measured peer RTT:
                //   RTT unknown → 200ms default
                //   RTT < 50ms  → 100ms minimum
                //   RTT > 300ms → 500ms cap
                //   Otherwise   → RTT * 1.5
                val jitterMs = when {
                    peerRttMs <= 0 -> 200L   // unknown RTT, safe default
                    peerRttMs < 50 -> 100L   // very low latency link
                    peerRttMs > 300 -> 500L  // high latency, cap to avoid delay
                    else -> peerRttMs * 3 / 2  // 1.5x RTT
                }
                val jitterBytes = (buffer.sampleRate * 2 * jitterMs / 1000).toInt()
                val jitterBuf = java.io.ByteArrayOutputStream(jitterBytes)
                var jitterFilled = 0
                while (isPlaying() && jitterFilled < jitterBytes) {
                    val toRead = minOf(data.size, jitterBytes - jitterFilled)
                    val n = inputStream.read(data, 0, toRead)
                    if (n <= 0) break
                    jitterBuf.write(data, 0, n)
                    jitterFilled += n
                }
                if (jitterFilled > 0) {
                    val prefill = jitterBuf.toByteArray()
                    audioTrack.write(prefill, 0, prefill.size)
                    log(
                        "[$TAG] Jitter buffer pre-filled ${prefill.size} bytes " +
                            "(${jitterMs}ms, peerRtt=${
                                if (peerRttMs > 0) "${peerRttMs}ms" else "unknown"
                            })",
                        LogLevel.DEBUG
                    )
                }

                // --- Normal streaming loop ---
                while (isPlaying() && inputStream.read(data).also { len = it } > 0) {
                    val written = audioTrack.write(data, 0, len)
                    if (written < 0) {
                        log("[$TAG] AudioTrack.write error: $written", LogLevel.ERROR)
                        break
                    }
                }
            } catch (e: IOException) {
                log("[$TAG] Exception with playing stream: ${e.message}", LogLevel.ERROR)
            } finally {
                stopInternal()
                try { audioTrack.stop() } catch (_: IllegalStateException) {}
                audioTrack.release()
                onFinish()
            }
        }
        thread?.start()
    }

    private fun stopInternal() {
        isAlive = false
        try {
            inputStream.close()
        } catch (e: IOException) {
            log("[$TAG] Failed to close input stream: ${e.message}", LogLevel.ERROR)
        }
    }

    /** Stops playing the stream. */
    fun stop() {
        stopInternal()
        // Non-blocking: let thread die naturally to avoid ANR
    }

    /** The stream has now ended. */
    protected open fun onFinish() {}

    private class Buffer(log: (String, LogLevel) -> Unit) : AudioBuffer(log) {
        override fun validSize(size: Int): Boolean =
            size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE

        override fun getMinBufferSize(sampleRate: Int): Int {
            return AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            )
        }
    }
}
