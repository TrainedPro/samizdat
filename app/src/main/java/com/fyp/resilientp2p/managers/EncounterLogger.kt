package com.fyp.resilientp2p.managers

import android.content.Context
import android.util.Log
import com.fyp.resilientp2p.data.EncounterDao
import com.fyp.resilientp2p.data.EncounterLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * DTN (Delay-Tolerant Networking) encounter logger.
 *
 * Tracks when this device encounters other mesh peers, logging connection/disconnection
 * timestamps and data exchange volumes. This supports:
 *
 * - **Sneakernet analytics:** measure how many packets move via physical carry.
 * - **Duplicate avoidance:** skip store-forward flush to peers we recently exchanged with.
 * - **Encounter-based routing:** prioritize peers we encounter frequently.
 *
 * ## DTN Extended TTL
 * Store-and-forward packets can optionally have TTLs up to 7 days (604_800_000 ms)
 * when DTN mode is enabled, far exceeding the default 2-hour TTL.
 *
 * @param encounterDao Room DAO for persisting encounter records.
 */
class EncounterLogger(
    private val encounterDao: EncounterDao
) {
    companion object {
        private const val TAG = "EncounterLogger"

        /** Default store-forward TTL: 2 hours in ms. */
        const val DEFAULT_TTL_MS = 2 * 60 * 60 * 1000L

        /** DTN extended TTL: 7 days in ms. */
        const val DTN_EXTENDED_TTL_MS = 7 * 24 * 60 * 60 * 1000L

        /** Cooldown before re-flushing to the same peer (10 minutes). */
        const val FLUSH_COOLDOWN_MS = 10 * 60 * 1000L
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Active encounters keyed by remotePeer → encounterDbId. */
    private val activeEncounters = ConcurrentHashMap<String, Long>()

    /** Tracks packets exchanged in current encounter per peer. */
    private val exchangeCounters = ConcurrentHashMap<String, Int>()

    /** Tracks bytes exchanged in current encounter per peer. */
    private val exchangeBytes = ConcurrentHashMap<String, Long>()

    /** Last flush timestamp per peer (to enforce cooldown). */
    private val lastFlushTime = ConcurrentHashMap<String, Long>()

    /** Whether DTN extended TTL mode is enabled. */
    @Volatile
    var dtnModeEnabled: Boolean = false

    /** Current effective TTL for store-forward packets. */
    val effectiveTtlMs: Long
        get() = if (dtnModeEnabled) DTN_EXTENDED_TTL_MS else DEFAULT_TTL_MS

    /**
     * Start tracking an encounter with a peer (called on connection establishment).
     *
     * @param localPeer This device's peer name.
     * @param remotePeer The connected peer's name.
     * @param rssi Signal strength of the connection.
     */
    fun onPeerConnected(localPeer: String, remotePeer: String, rssi: Int = 0) {
        scope.launch {
            val encounter = EncounterLog(
                localPeer = localPeer,
                remotePeer = remotePeer,
                startTime = System.currentTimeMillis(),
                rssi = rssi
            )
            val id = encounterDao.insert(encounter)
            activeEncounters[remotePeer] = id
            exchangeCounters[remotePeer] = 0
            exchangeBytes[remotePeer] = 0L
            Log.d(TAG, "Encounter started with $remotePeer (id=$id)")
        }
    }

    /**
     * Finish tracking an encounter (called on disconnection).
     *
     * @param remotePeer The disconnected peer's name.
     */
    fun onPeerDisconnected(remotePeer: String) {
        val encounterId = activeEncounters.remove(remotePeer) ?: return
        val packets = exchangeCounters.remove(remotePeer) ?: 0
        val bytes = exchangeBytes.remove(remotePeer) ?: 0L

        scope.launch {
            encounterDao.finishEncounter(
                id = encounterId,
                endTime = System.currentTimeMillis(),
                packets = packets,
                bytes = bytes
            )
            Log.d(TAG, "Encounter ended with $remotePeer: $packets pkts, $bytes bytes")
        }
    }

    /**
     * Record a packet exchange during an active encounter.
     *
     * @param remotePeer Peer involved.
     * @param packetSizeBytes Size of the packet.
     */
    fun recordExchange(remotePeer: String, packetSizeBytes: Int) {
        exchangeCounters.compute(remotePeer) { _, v -> (v ?: 0) + 1 }
        exchangeBytes.compute(remotePeer) { _, v -> (v ?: 0L) + packetSizeBytes }
    }

    /**
     * Check if we should flush store-forward packets to a peer
     * (respects cooldown to avoid re-sending to recently encountered peers).
     */
    fun shouldFlushTo(remotePeer: String): Boolean {
        val lastFlush = lastFlushTime[remotePeer] ?: return true
        return System.currentTimeMillis() - lastFlush > FLUSH_COOLDOWN_MS
    }

    /** Mark that we flushed store-forward packets to a peer. */
    fun markFlushed(remotePeer: String) {
        lastFlushTime[remotePeer] = System.currentTimeMillis()
    }

    /** Get the number of currently active encounters. */
    fun activeEncounterCount(): Int = activeEncounters.size

    /** Get all currently connected peers with active encounters. */
    fun activePeers(): Set<String> = activeEncounters.keys.toSet()

    /** Cancel background coroutines. Call on application shutdown. */
    fun destroy() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
    }
}
