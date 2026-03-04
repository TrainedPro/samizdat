package com.fyp.resilientp2p.audio

import android.content.Context
import android.os.ParcelFileDescriptor
import android.os.Process
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.transport.Packet
import com.fyp.resilientp2p.transport.PacketType
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Multi-hop mesh audio streaming with AAC-LC compression.
 *
 * Replaces the old direct-only Payload.fromStream approach with chunked
 * [PacketType.AUDIO_DATA] packets that route through the mesh like any
 * other packet. AAC-LC encoding reduces bandwidth from ~16 KB/s (raw PCM)
 * to ~3–4 KB/s, critical for multi-hop where each relay consumes bandwidth.
 *
 * ## Sender flow
 * 1. [startStreaming] sends [PacketType.AUDIO_CONTROL] START
 * 2. [AudioRecorder] captures PCM → AAC encoder → batched AUDIO_DATA packets
 * 3. [stopStreaming] sends AUDIO_CONTROL STOP and releases resources
 *
 * ## Receiver flow
 * 1. [handleAudioControl] START → create decoder + playback pipe
 * 2. [handleAudioData] → decode AAC → write PCM to playback pipe
 * 3. [handleAudioControl] STOP → release playback resources
 *
 * Audio frames are batched (default [BATCH_FRAMES] = 5 → 100 ms of audio per
 * packet) to reduce per-packet overhead while keeping latency acceptable.
 *
 * @see AudioCodecManager
 * @see AudioRecorder
 * @see AudioPlayer
 */
class MeshAudioManager(
    private val context: Context,
    private val localUsername: String,
    private val log: (String, LogLevel) -> Unit
) {
    companion object {
        private const val TAG = "MeshAudio"

        /**
         * Number of 20 ms AAC frames batched into a single AUDIO_DATA packet.
         * 5 frames = 100 ms per packet → 10 packets/sec. Good balance between
         * latency and per-packet overhead on the mesh.
         */
        const val BATCH_FRAMES = 5

        /** Bytes of raw PCM per batch: 5 frames × 320 B/frame = 1600 B. */
        const val BATCH_PCM_BYTES = BATCH_FRAMES * AudioCodecManager.BYTES_PER_FRAME

        /** Session payload header: "sessionId|command" for AUDIO_CONTROL. */
        private const val CTRL_START = "START"
        private const val CTRL_STOP = "STOP"
    }

    /** Callback set by P2PManager to inject packets into the mesh. */
    var sendPacket: ((Packet) -> Unit)? = null

    /** Callback to get the last-known RTT (ms) for a peer. Used to size the jitter buffer. */
    var getPeerRttMs: ((String) -> Long)? = null

    // --- Sender state ---
    private val isRecording = AtomicBoolean(false)
    private var audioRecorder: AudioRecorder? = null
    private var encoder: AudioCodecManager.AACEncoder? = null
    private var sendThread: Thread? = null
    private var currentSessionId: String? = null
    private var currentTargetPeer: String? = null
    private val seqNo = AtomicInteger(0)

    // --- Receiver state (per session) ---
    private var decoder: AudioCodecManager.AACDecoder? = null
    private var audioPlayer: AudioPlayer? = null
    private var playbackPipeOut: PipedOutputStream? = null
    private var activeReceiveSession: String? = null

    /**
     * Audio dedup: sliding window of recently-seen sequence numbers per session.
     * Audio packets skip the MessageCache (UUID dedup) to avoid thrashing the 2000-entry
     * cache at 10+ packets/sec. Instead, we use the seqNo already embedded in the
     * AUDIO_DATA header to detect and discard duplicates from mesh forwarding.
     *
     * Window size of 64 balances memory usage with reorder tolerance.
     */
    private val audioSeqWindow = java.util.Collections.newSetFromMap(
        object : java.util.LinkedHashMap<Int, Boolean>(64, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Boolean>?): Boolean =
                size > 64
        }
    )

    // --- Public API ---

    /**
     * Start streaming audio to [targetPeerId] over the mesh.
     *
     * Sends an AUDIO_CONTROL START packet, then begins recording + encoding + sending
     * AUDIO_DATA packets. Call [stopStreaming] to end the session.
     *
     * @return true if streaming started, false on failure
     */
    @Synchronized
    fun startStreaming(targetPeerId: String): Boolean {
        if (isRecording.get()) {
            log("[$TAG] Already streaming", LogLevel.WARN)
            return false
        }

        // Permission check
        if (androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.RECORD_AUDIO
            ) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            log("[$TAG] RECORD_AUDIO permission missing", LogLevel.ERROR)
            return false
        }

        val sessionId = UUID.randomUUID().toString().take(8)
        currentSessionId = sessionId
        currentTargetPeer = targetPeerId
        seqNo.set(0)

        // Create AAC encoder
        encoder = AudioCodecManager.createEncoder()
        if (encoder == null) {
            log("[$TAG] Failed to create AAC encoder", LogLevel.ERROR)
            cleanup()
            return false
        }

        // Create pipe for AudioRecorder → send thread
        val pipe: Array<ParcelFileDescriptor>
        try {
            pipe = ParcelFileDescriptor.createPipe()
        } catch (e: Exception) {
            log("[$TAG] Pipe creation failed: ${e.message}", LogLevel.ERROR)
            cleanup()
            return false
        }
        val readPfd = pipe[0]
        val writePfd = pipe[1]

        // Start AudioRecorder writing raw PCM to the pipe
        try {
            audioRecorder = AudioRecorder(context, writePfd, log)
            audioRecorder?.start()
        } catch (e: Exception) {
            log("[$TAG] AudioRecorder failed: ${e.message}", LogLevel.ERROR)
            try { readPfd.close() } catch (_: Exception) {}
            try { writePfd.close() } catch (_: Exception) {}
            audioRecorder = null
            cleanup()
            return false
        }

        if (audioRecorder?.isRecording() != true) {
            log("[$TAG] AudioRecorder did not start", LogLevel.WARN)
            audioRecorder?.stop()
            try { readPfd.close() } catch (_: Exception) {}
            cleanup()
            return false
        }

        // Send START control
        sendControlPacket(sessionId, targetPeerId, CTRL_START)

        // Launch sender thread: reads PCM from pipe, encodes AAC, sends AUDIO_DATA packets
        isRecording.set(true)
        sendThread = Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_AUDIO)
            val inputStream = ParcelFileDescriptor.AutoCloseInputStream(readPfd)
            val batchBuffer = ByteArray(BATCH_PCM_BYTES)
            var batchOffset = 0

            try {
                while (isRecording.get()) {
                    val toRead = BATCH_PCM_BYTES - batchOffset
                    val bytesRead = inputStream.read(batchBuffer, batchOffset, toRead)
                    if (bytesRead <= 0) break
                    batchOffset += bytesRead

                    if (batchOffset >= BATCH_PCM_BYTES) {
                        // Full batch → encode and send
                        encodeBatchAndSend(sessionId, targetPeerId, batchBuffer, batchOffset)
                        batchOffset = 0
                    }
                }
                // Flush remaining partial batch
                if (batchOffset > 0 && isRecording.get()) {
                    encodeBatchAndSend(sessionId, targetPeerId, batchBuffer, batchOffset)
                }
            } catch (e: Exception) {
                if (isRecording.get()) {
                    log("[$TAG] Send thread error: ${e.message}", LogLevel.WARN)
                }
            } finally {
                try { inputStream.close() } catch (_: Exception) {}
            }
        }, "MeshAudio-Send")
        sendThread?.start()

        log("[$TAG] Streaming started → $targetPeerId session=$sessionId", LogLevel.INFO)
        return true
    }

    /**
     * Stop the current outgoing audio stream.
     */
    @Synchronized
    fun stopStreaming() {
        if (!isRecording.getAndSet(false)) return

        val session = currentSessionId
        val target = currentTargetPeer

        audioRecorder?.stop()
        audioRecorder = null
        sendThread?.interrupt()
        try { sendThread?.join(1000) } catch (_: Exception) {}
        sendThread = null
        encoder?.release()
        encoder = null

        if (session != null && target != null) {
            sendControlPacket(session, target, CTRL_STOP)
        }

        currentSessionId = null
        currentTargetPeer = null
        log("[$TAG] Streaming stopped", LogLevel.INFO)
    }

    /**
     * Handle an incoming [PacketType.AUDIO_CONTROL] packet.
     *
     * Payload format: `sessionId|START` or `sessionId|STOP`
     */
    @Synchronized
    fun handleAudioControl(packet: Packet) {
        val text = String(packet.payload, Charsets.UTF_8)
        val sep = text.indexOf('|')
        if (sep < 0) return
        val sessionId = text.substring(0, sep)
        val command = text.substring(sep + 1)

        when (command) {
            CTRL_START -> {
                // Stop any previous session
                stopPlayback()

                activeReceiveSession = sessionId
                // Clear dedup window for new session
                synchronized(audioSeqWindow) { audioSeqWindow.clear() }
                decoder = AudioCodecManager.createDecoder()
                if (decoder == null) {
                    log("[$TAG] Failed to create AAC decoder", LogLevel.ERROR)
                    return
                }

                try {
                    val pipeOut = PipedOutputStream()
                    val pipeIn = PipedInputStream(pipeOut, 16_384)
                    playbackPipeOut = pipeOut
                    val rtt = getPeerRttMs?.invoke(packet.sourceId) ?: -1L
                    audioPlayer = AudioPlayer(pipeIn, log, peerRttMs = rtt)
                    audioPlayer?.start()
                    log("[$TAG] Playback started for session=$sessionId from=${packet.sourceId} rtt=${rtt}ms", LogLevel.INFO)
                } catch (e: Exception) {
                    log("[$TAG] Playback setup failed: ${e.message}", LogLevel.ERROR)
                    stopPlayback()
                }
            }
            CTRL_STOP -> {
                if (activeReceiveSession == sessionId) {
                    log("[$TAG] Playback stopped for session=$sessionId from=${packet.sourceId}", LogLevel.INFO)
                    stopPlayback()
                }
            }
        }
    }

    /**
     * Handle an incoming AUDIO_DATA packet.
     *
     * Payload format: sessionId (8B ASCII) + seqNo (4B int) + aacData (remaining bytes)
     */
    @Synchronized
    fun handleAudioData(packet: Packet) {
        val payload = packet.payload
        if (payload.size < 12) return // 8 (session) + 4 (seq) minimum

        val sessionId = String(payload, 0, 8, Charsets.US_ASCII)

        // Only accept data for our active session
        if (sessionId != activeReceiveSession) return

        // Sequence-number dedup: extract seqNo from bytes 8..11 and check window.
        // This prevents duplicate playback when multiple mesh paths deliver
        // the same audio frame. Replaces the skipped MessageCache entry.
        val seqNoValue = ByteBuffer.wrap(payload, 8, 4).int
        synchronized(audioSeqWindow) {
            if (!audioSeqWindow.add(seqNoValue)) {
                // Already seen this sequence number — duplicate from mesh forwarding
                return
            }
        }

        // Skip seqNo bytes (offset 8..11), extract AAC data
        val aacData = payload.copyOfRange(12, payload.size)
        if (aacData.isEmpty()) return

        val localDecoder = decoder ?: return
        val localPipeOut = playbackPipeOut ?: return

        try {
            val pcmData = localDecoder.decode(aacData)
            if (pcmData.isNotEmpty()) {
                localPipeOut.write(pcmData)
                localPipeOut.flush()
            }
        } catch (e: Exception) {
            // Broken pipe → playback already stopped
            if (e.message?.contains("Pipe") == true) return
            log("[$TAG] Decode/play error: ${e.message}", LogLevel.WARN)
        }
    }

    /** @return true if currently recording and transmitting audio. */
    fun isStreaming(): Boolean = isRecording.get()

    /** @return true if currently playing received audio. */
    fun isPlaying(): Boolean = audioPlayer?.isPlaying() == true

    // --- Internal helpers ---

    /**
     * Encode a batch of raw PCM bytes to AAC, then send as a single AUDIO_DATA packet.
     */
    private fun encodeBatchAndSend(
        sessionId: String,
        targetPeerId: String,
        pcmBuffer: ByteArray,
        pcmLength: Int
    ) {
        val enc = encoder ?: return
        val pcm = if (pcmLength == pcmBuffer.size) pcmBuffer else pcmBuffer.copyOf(pcmLength)
        val aacBytes = enc.encode(pcm)
        if (aacBytes.isEmpty()) return

        val seq = seqNo.getAndIncrement()

        // Build payload: [session:8B][seq:4B][aacData]
        val sessionBytes = sessionId.toByteArray(Charsets.US_ASCII)
        val payload = ByteBuffer.allocate(8 + 4 + aacBytes.size)
            .put(sessionBytes, 0, 8)
            .putInt(seq)
            .put(aacBytes)
            .array()

        val packet = Packet(
            type = PacketType.AUDIO_DATA,
            sourceId = localUsername,
            destId = targetPeerId,
            payload = payload,
            ttl = Packet.DEFAULT_TTL
        )
        sendPacket?.invoke(packet)
    }

    /**
     * Send an AUDIO_CONTROL packet (START or STOP).
     */
    private fun sendControlPacket(sessionId: String, targetPeerId: String, command: String) {
        val packet = Packet(
            type = PacketType.AUDIO_CONTROL,
            sourceId = localUsername,
            destId = targetPeerId,
            payload = "$sessionId|$command".toByteArray(Charsets.UTF_8),
            ttl = Packet.DEFAULT_TTL
        )
        sendPacket?.invoke(packet)
    }

    private fun stopPlayback() {
        audioPlayer?.stop()
        audioPlayer = null
        try { playbackPipeOut?.close() } catch (_: Exception) {}
        playbackPipeOut = null
        decoder?.release()
        decoder = null
        activeReceiveSession = null
    }

    private fun cleanup() {
        currentSessionId = null
        currentTargetPeer = null
        encoder?.release()
        encoder = null
        seqNo.set(0)
    }

    /**
     * Release all resources. Called on application shutdown.
     */
    @Synchronized
    fun destroy() {
        stopStreaming()
        stopPlayback()
    }
}
