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

        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            audioRecorder = AudioRecorder(context, writeSide, log)
            audioRecorder?.start()

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
            return null
        }
    }

    @Synchronized
    fun stopRecording() {
        audioRecorder?.stop()
        audioRecorder = null
    }

    @Synchronized
    fun startPlaying(inputStream: InputStream) {
        // ALWAYS stop any existing player to avoid overlap (with try-catch for safety)
        try { audioPlayer?.stop() } catch (e: Exception) { log("Error stopping previous player: ${e.message}", LogLevel.WARN) }
        audioPlayer = AudioPlayer(inputStream, log)
        audioPlayer?.start()
    }

    @Synchronized
    fun stopPlaying() {
        audioPlayer?.stop()
        audioPlayer = null
    }

    fun isRecording() = audioRecorder?.isRecording() == true
    fun isPlaying() = audioPlayer?.isPlaying() == true
}
