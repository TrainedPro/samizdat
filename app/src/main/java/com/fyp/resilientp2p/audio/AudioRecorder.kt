package com.fyp.resilientp2p.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.os.Process
import com.fyp.resilientp2p.data.LogLevel
import java.io.IOException
import java.io.OutputStream

/**
 * When created, you must pass a [ParcelFileDescriptor]. Once [start] is called, the file descriptor
 * will be written to until [stop] is called.
 */
class AudioRecorder(
        private val context: android.content.Context,
        file: ParcelFileDescriptor,
        private val log: (String, LogLevel) -> Unit
) {

    companion object {
        private const val TAG = "AudioRecorder"
    }

    /** The stream to write to. */
    private val outputStream: OutputStream = ParcelFileDescriptor.AutoCloseOutputStream(file)

    /**
     * If true, the background thread will continue to loop and record audio. Once false, the thread
     * will shut down.
     */
    @Volatile private var isAlive = false

    /** The background thread recording audio for us. */
    private var thread: Thread? = null

    /** @return True if actively recording. False otherwise. */
    fun isRecording(): Boolean {
        return isAlive
    }

    /** Starts recording audio. */
    fun start() {
        if (isRecording()) {
            log("[$TAG] Already running", LogLevel.WARN)
            return
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            log("[$TAG] RECORD_AUDIO permission missing", LogLevel.ERROR)
            return
        }

        isAlive = true
        thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val buffer = Buffer(log)
            val record =
                    AudioRecord(
                            MediaRecorder.AudioSource.DEFAULT,
                            buffer.sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffer.size
                    )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                log("[$TAG] Failed to start recording", LogLevel.WARN)
                isAlive = false
                // The following lines were part of the user's instruction but refer to undefined
                // variables
                // (neighbors, sourceEndpointId, packet) in this context.
                // To maintain syntactic correctness as per instructions, they are commented out.
                // If these variables are meant to be defined elsewhere, please provide that
                // context.
                // neighbors[sourceEndpointId]?.let {
                // it.lastSeen = System.currentTimeMillis()
                // log("Updated lastSeen for $sourceEndpointId to ${it.lastSeen} (MsgType:
                // ${packet.type})")
                // }
                return@Thread
            }

            record.startRecording()

            // While we're running, we'll read the bytes from the AudioRecord and write them
            // to our output stream.
            try {
                while (isRecording()) {
                    val len = record.read(buffer.data, 0, buffer.size)
                    if (len >= 0 && len <= buffer.size) {
                        try {
                            outputStream.write(buffer.data, 0, len)
                            outputStream.flush()
                        } catch (e: IOException) {
                            log("[$TAG] Error writing to output stream: ${e.message}", LogLevel.ERROR)
                            // Optionally, break the loop or handle the error further
                            break
                        }
                    } else {
                        log("[$TAG] Unexpected length returned: $len", LogLevel.WARN)
                    }
                }
            } catch (e: IOException) {
                // This usually happens if the peer disconnects or the pipe breaks.
                // We log as warning instead of error to reduce noise, as it's an expected condition
                // during disconnects.
                log("[$TAG] Audio stream ended/broke: ${e.message}", LogLevel.WARN)
            } finally {
                stopInternal()
                try {
                    record.stop()
                } catch (e: IllegalStateException) {
                    log("[$TAG] Failed to stop AudioRecord: ${e.message}", LogLevel.ERROR)
                }
                record.release()
            }
        }
        thread?.start()
    }

    private fun stopInternal() {
        isAlive = false
        try {
            outputStream.close()
        } catch (e: IOException) {
            log("[$TAG] Failed to close output stream: ${e.message}", LogLevel.ERROR)
        }
    }

    /** Stops recording audio. */
    fun stop() {
        stopInternal()
        thread?.interrupt() // Interrupt any blocking Read/Write
        try {
            // Wait max 1000ms for thread to die, then give up to avoid UI freeze
            thread?.join(1000)
        } catch (e: InterruptedException) {
            log("[$TAG] Interrupted while joining AudioRecorder thread: ${e.message}", LogLevel.ERROR)
            Thread.currentThread().interrupt()
        }
    }

    private class Buffer(log: (String, LogLevel) -> Unit) : AudioBuffer(log) {
        override fun validSize(size: Int): Boolean {
            return size != AudioRecord.ERROR && size != AudioRecord.ERROR_BAD_VALUE
        }

        override fun getMinBufferSize(sampleRate: Int): Int {
            return AudioRecord.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            )
        }
    }
}
