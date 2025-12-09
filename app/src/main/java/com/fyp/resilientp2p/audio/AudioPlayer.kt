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
open class AudioPlayer(private val inputStream: InputStream, private val log: (String, LogLevel) -> Unit) {

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
    fun isPlaying(): Boolean {
        return isAlive
    }

    /** Starts playing the stream. */
    fun start() {
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
            audioTrack.play()

            var len: Int = 0
            try {
                val data = buffer.data
                while (isPlaying() && inputStream.read(data).also { len = it } > 0) {
                    audioTrack.write(data, 0, len)
                }
            } catch (e: IOException) {
                log("[$TAG] Exception with playing stream: ${e.message}", LogLevel.ERROR)
            } finally {
                stopInternal()
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
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            log("[$TAG] Interrupted while joining AudioRecorder thread: ${e.message}", LogLevel.ERROR)
            Thread.currentThread().interrupt()
        }
    }

    /** The stream has now ended. */
    protected open fun onFinish() {}

    private class Buffer(log: (String, LogLevel) -> Unit) : AudioBuffer(log) {
        override fun validSize(size: Int): Boolean {
            return size != AudioTrack.ERROR && size != AudioTrack.ERROR_BAD_VALUE
        }

        override fun getMinBufferSize(sampleRate: Int): Int {
            return AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            )
        }
    }
}
