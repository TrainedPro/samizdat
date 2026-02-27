package com.fyp.resilientp2p.managers

import android.content.Context
import android.os.ParcelFileDescriptor
import com.fyp.resilientp2p.audio.AudioPlayer
import com.fyp.resilientp2p.audio.AudioRecorder
import com.fyp.resilientp2p.data.LogLevel
import java.io.InputStream

class VoiceManager(private val context: Context, private val log: (String, LogLevel) -> Unit) {

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    companion object {
        private const val TAG = "VoiceManager"
    }

    /**
     * Starts recording audio and returns a ParcelFileDescriptor for the read side of the pipe. The
     * P2PManager should send this PFD as a Payload.
     */
    @Synchronized
    fun startRecording(): ParcelFileDescriptor? {
        if (audioRecorder != null && audioRecorder!!.isRecording()) {
            log("[$TAG] Already recording", LogLevel.WARN)
            return null
        }

        var readSide: ParcelFileDescriptor? = null
        var writeSide: ParcelFileDescriptor? = null
        try {
            val pipe = ParcelFileDescriptor.createPipe()
            readSide = pipe[0]
            writeSide = pipe[1]

            try {
                audioRecorder = AudioRecorder(context, writeSide, log)
                audioRecorder?.start()
            } catch (e: Exception) {
                // AudioRecorder constructor or start() failed — close both FDs
                try { writeSide.close() } catch (_: Exception) {}
                try { readSide.close() } catch (_: Exception) {}
                readSide = null
                writeSide = null
                audioRecorder = null
                throw e
            }

            // Check if recording actually started
            if (audioRecorder?.isRecording() != true) {
                log("[$TAG] AudioRecorder failed to start", LogLevel.WARN)
                // AudioRecorder owns writeSide, call stop() to close it properly
                audioRecorder?.stop()
                audioRecorder = null
                readSide.close()
                return null
            }

            log("[$TAG] AudioRecorder started successfully", LogLevel.DEBUG)
            return readSide
        } catch (e: Exception) {
            log("[$TAG] Failed to create pipe or start recorder: ${e.message}", LogLevel.ERROR)
            // Ensure FDs are closed on any failure
            try { writeSide?.close() } catch (_: Exception) {}
            try { readSide?.close() } catch (_: Exception) {}
            audioRecorder = null
            return null
        }
    }

    @Synchronized
    fun stopRecording() {
        audioRecorder?.stop()
        audioRecorder = null
    }

    @Synchronized
    fun startPlaying(inputStream: InputStream, peerRttMs: Long = -1) {
        // ALWAYS stop any existing player to avoid overlap (with try-catch for safety)
        try { audioPlayer?.stop() } catch (e: Exception) { log("Error stopping previous player: ${e.message}", LogLevel.WARN) }
        try {
            audioPlayer = AudioPlayer(inputStream, log, peerRttMs)
            audioPlayer?.start()
        } catch (e: Exception) {
            log("[$TAG] Failed to create/start AudioPlayer: ${e.message}", LogLevel.ERROR)
            try { inputStream.close() } catch (_: Exception) {}
            audioPlayer = null
        }
    }

    @Synchronized
    fun stopPlaying() {
        audioPlayer?.stop()
        audioPlayer = null
    }

    @Synchronized
    fun isRecording() = audioRecorder?.isRecording() == true
    @Synchronized
    fun isPlaying() = audioPlayer?.isPlaying() == true
}
