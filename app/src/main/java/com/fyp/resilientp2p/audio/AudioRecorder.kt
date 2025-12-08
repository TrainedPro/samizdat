package com.fyp.resilientp2p.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.ParcelFileDescriptor
import android.os.Process
import android.util.Log
import java.io.IOException
import java.io.OutputStream

/**
 * When created, you must pass a [ParcelFileDescriptor]. Once [start] is called, the file descriptor
 * will be written to until [stop] is called.
 */
class AudioRecorder(private val context: android.content.Context, file: ParcelFileDescriptor) {

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
            Log.w(TAG, "Already running")
            return
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.RECORD_AUDIO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "RECORD_AUDIO permission missing")
            return
        }

        isAlive = true
        thread = Thread {
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)

            val buffer = Buffer()
            val record =
                    AudioRecord(
                            MediaRecorder.AudioSource.DEFAULT,
                            buffer.sampleRate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            buffer.size
                    )

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                Log.w(TAG, "Failed to start recording")
                isAlive = false
                return@Thread
            }

            record.startRecording()

            // While we're running, we'll read the bytes from the AudioRecord and write them
            // to our output stream.
            try {
                while (isRecording()) {
                    val len = record.read(buffer.data, 0, buffer.size)
                    if (len >= 0 && len <= buffer.size) {
                        outputStream.write(buffer.data, 0, len)
                        outputStream.flush()
                    } else {
                        Log.w(TAG, "Unexpected length returned: $len")
                    }
                }
            } catch (e: IOException) {
                Log.e(TAG, "Exception with recording stream", e)
            } finally {
                stopInternal()
                try {
                    record.stop()
                } catch (e: IllegalStateException) {
                    Log.e(TAG, "Failed to stop AudioRecord", e)
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
            Log.e(TAG, "Failed to close output stream", e)
        }
    }

    /** Stops recording audio. */
    fun stop() {
        stopInternal()
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while joining AudioRecorder thread", e)
            Thread.currentThread().interrupt()
        }
    }

    private class Buffer : AudioBuffer() {
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
