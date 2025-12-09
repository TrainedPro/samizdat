package com.fyp.resilientp2p.managers

import android.content.Context
import android.os.ParcelFileDescriptor
import android.util.Log
import com.fyp.resilientp2p.audio.AudioPlayer
import com.fyp.resilientp2p.audio.AudioRecorder
import java.io.InputStream

class VoiceManager(private val context: Context) {

    private var audioRecorder: AudioRecorder? = null
    private var audioPlayer: AudioPlayer? = null

    companion object {
        private const val TAG = "VoiceManager"
    }

    /**
     * Starts recording audio and returns a ParcelFileDescriptor for the read side of the pipe. The
     * P2PManager should send this PFD as a Payload.
     */
    fun startRecording(): ParcelFileDescriptor? {
        if (audioRecorder != null && audioRecorder!!.isRecording()) {
            Log.w(TAG, "Already recording")
            return null
        }

        try {
            val pipe = ParcelFileDescriptor.createPipe()
            val readSide = pipe[0]
            val writeSide = pipe[1]

            audioRecorder = AudioRecorder(context, writeSide)
            audioRecorder?.start()

            Log.d(TAG, "AudioRecorder started successfully")
            return readSide
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create pipe or start recorder", e)
            return null
        }
    }

    fun stopRecording() {
        audioRecorder?.stop()
        audioRecorder = null
    }

    fun startPlaying(inputStream: InputStream) {
        if (audioPlayer != null && audioPlayer!!.isPlaying()) {
            stopPlaying()
        }

        audioPlayer = AudioPlayer(inputStream)
        audioPlayer?.start()
    }

    fun stopPlaying() {
        audioPlayer?.stop()
        audioPlayer = null
    }

    fun isRecording() = audioRecorder?.isRecording() == true
    fun isPlaying() = audioPlayer?.isPlaying() == true
}
