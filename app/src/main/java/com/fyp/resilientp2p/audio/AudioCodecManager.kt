package com.fyp.resilientp2p.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import com.fyp.resilientp2p.data.LogLevel
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

/**
 * AAC-LC audio codec wrapper using Android's [MediaCodec] API.
 *
 * Compresses 8kHz mono 16-bit PCM audio to AAC-LC, reducing bandwidth from
 * ~16KB/s (raw PCM) to ~3-4KB/s. This is critical for multi-hop audio routing
 * where each hop consumes mesh bandwidth.
 *
 * ## Usage
 *
 * **Encoding (sender):**
 * ```kotlin
 * val encoder = AudioCodecManager.createEncoder()
 * val aacBytes = encoder.encode(pcmBytes)
 * encoder.release()
 * ```
 *
 * **Decoding (receiver):**
 * ```kotlin
 * val decoder = AudioCodecManager.createDecoder()
 * val pcmBytes = decoder.decode(aacBytes)
 * decoder.release()
 * ```
 *
 * @see AudioPlayer
 * @see AudioRecorder
 */
object AudioCodecManager {
    private const val TAG = "AudioCodecManager"

    /** Sample rate in Hz. Must match [AudioBuffer.sampleRate]. */
    const val SAMPLE_RATE = 8000
    /** AAC-LC target bitrate in bits/sec. 24kbps gives good quality at 8kHz mono. */
    const val BIT_RATE = 24_000
    /** Number of audio channels. */
    const val CHANNELS = 1
    /** Duration of each audio frame in milliseconds. */
    const val FRAME_DURATION_MS = 20
    /** Number of PCM samples per frame at [SAMPLE_RATE] and [FRAME_DURATION_MS]. */
    const val SAMPLES_PER_FRAME = SAMPLE_RATE * FRAME_DURATION_MS / 1000 // 160
    /** Number of PCM bytes per frame (16-bit = 2 bytes per sample). */
    const val BYTES_PER_FRAME = SAMPLES_PER_FRAME * 2 // 320 bytes

    /** Log callback — routes through P2PManager.log() when wired. */
    var logFn: ((String, LogLevel) -> Unit)? = null

    private fun log(msg: String, level: LogLevel = LogLevel.DEBUG) {
        logFn?.invoke(msg, level) ?: Log.d(TAG, msg)
    }

    /**
     * Creates an AAC-LC encoder backed by [MediaCodec].
     * Caller must call [AACEncoder.release] when done.
     *
     * @return A new [AACEncoder] instance, or null if codec creation fails
     */
    fun createEncoder(): AACEncoder? {
        return try {
            AACEncoder()
        } catch (e: Exception) {
            log("Failed to create AAC encoder: ${e.message}", LogLevel.ERROR)
            null
        }
    }

    /**
     * Creates an AAC-LC decoder backed by [MediaCodec].
     * Caller must call [AACDecoder.release] when done.
     *
     * @return A new [AACDecoder] instance, or null if codec creation fails
     */
    fun createDecoder(): AACDecoder? {
        return try {
            AACDecoder()
        } catch (e: Exception) {
            log("Failed to create AAC decoder: ${e.message}", LogLevel.ERROR)
            null
        }
    }

    /**
     * AAC-LC encoder: converts raw PCM audio frames to compressed AAC bytes.
     *
     * Thread safety: NOT thread-safe. Use from a single thread only.
     */
    class AACEncoder {
        private val codec: MediaCodec
        private val bufferInfo = MediaCodec.BufferInfo()
        @Volatile private var isReleased = false

        init {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE)
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, BYTES_PER_FRAME * 4)
            }
            codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
        }

        /**
         * Encode a chunk of raw PCM audio data to AAC.
         *
         * @param pcmData Raw 16-bit mono PCM audio bytes
         * @return Compressed AAC bytes, or empty array if encoding fails
         */
        fun encode(pcmData: ByteArray): ByteArray {
            if (isReleased) return ByteArray(0)
            val output = ByteArrayOutputStream()

            try {
                // Feed input
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: return ByteArray(0)
                    inputBuffer.clear()
                    inputBuffer.put(pcmData, 0, minOf(pcmData.size, inputBuffer.remaining()))
                    codec.queueInputBuffer(inputIndex, 0, minOf(pcmData.size, inputBuffer.capacity()), 0, 0)
                }

                // Drain output
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: break
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    output.write(chunk)
                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            } catch (e: Exception) {
                log("Encode error: ${e.message}", LogLevel.WARN)
            }

            return output.toByteArray()
        }

        /** Release codec resources. Must be called when encoding is complete. */
        @Synchronized
        fun release() {
            if (!isReleased) {
                isReleased = true
                try {
                    codec.stop()
                    codec.release()
                } catch (e: Exception) {
                    log("Encoder release error: ${e.message}", LogLevel.WARN)
                }
            }
        }
    }

    /**
     * AAC-LC decoder: converts compressed AAC frames back to raw PCM audio.
     *
     * Thread safety: NOT thread-safe. Use from a single thread only.
     */
    class AACDecoder {
        private val codec: MediaCodec
        private val bufferInfo = MediaCodec.BufferInfo()
        @Volatile private var isReleased = false

        init {
            val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, SAMPLE_RATE, CHANNELS).apply {
                setInteger(MediaFormat.KEY_IS_ADTS, 0)
                // AAC decoder needs CSD (Codec Specific Data) for initialization
                // For AAC-LC 8kHz mono: profile=2(LC), freq_idx=11(8kHz), chan=1
                val csd = ByteBuffer.wrap(byteArrayOf(0x15.toByte(), 0x88.toByte()))
                setByteBuffer("csd-0", csd)
            }
            codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
            codec.configure(format, null, null, 0)
            codec.start()
        }

        /**
         * Decode a chunk of AAC audio data back to raw PCM.
         *
         * @param aacData Compressed AAC audio bytes
         * @return Decoded 16-bit mono PCM audio bytes, or empty array if decoding fails
         */
        fun decode(aacData: ByteArray): ByteArray {
            if (isReleased) return ByteArray(0)
            val output = ByteArrayOutputStream()

            try {
                // Feed input
                val inputIndex = codec.dequeueInputBuffer(10_000)
                if (inputIndex >= 0) {
                    val inputBuffer = codec.getInputBuffer(inputIndex) ?: return ByteArray(0)
                    inputBuffer.clear()
                    inputBuffer.put(aacData, 0, minOf(aacData.size, inputBuffer.remaining()))
                    codec.queueInputBuffer(inputIndex, 0, minOf(aacData.size, inputBuffer.capacity()), 0, 0)
                }

                // Drain output
                var outputIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                while (outputIndex >= 0) {
                    val outputBuffer = codec.getOutputBuffer(outputIndex) ?: break
                    val chunk = ByteArray(bufferInfo.size)
                    outputBuffer.get(chunk)
                    output.write(chunk)
                    codec.releaseOutputBuffer(outputIndex, false)
                    outputIndex = codec.dequeueOutputBuffer(bufferInfo, 0)
                }
            } catch (e: Exception) {
                log("Decode error: ${e.message}", LogLevel.WARN)
            }

            return output.toByteArray()
        }

        /** Release codec resources. Must be called when decoding is complete. */
        @Synchronized
        fun release() {
            if (!isReleased) {
                isReleased = true
                try {
                    codec.stop()
                    codec.release()
                } catch (e: Exception) {
                    log("Decoder release error: ${e.message}", LogLevel.WARN)
                }
            }
        }
    }
}
