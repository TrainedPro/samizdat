package com.fyp.resilientp2p.security

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-peer packet rate limiter to prevent denial-of-service attacks from
 * malicious mesh participants.
 *
 * Uses a tumbling window counter: each peer's packet count is tracked over
 * a configurable window (default 1 second). If the count exceeds the limit,
 * further packets from that peer are rejected until the window resets.
 *
 * ## Design Rationale
 * - **Per-peer, not global:** A single misbehaving peer should not block traffic
 *   from legitimate peers.
 * - **Configurable limit:** Different deployments may have different bandwidth budgets.
 *   Default is 100 packets/sec which is generous for BLE.
 * - **Thread-safe:** Uses ConcurrentHashMap + AtomicLong for lock-free operation.
 *
 * @param maxPacketsPerSecond Maximum packets allowed per peer per second.
 * @see SecurityManager
 * @see PeerBlacklist
 */
class RateLimiter(
    private val maxPacketsPerSecond: Int = DEFAULT_RATE_LIMIT
) {
    companion object {
        private const val TAG = "RateLimiter"
        /** Default rate limit: 100 packets/second/peer. */
        const val DEFAULT_RATE_LIMIT = 100
        /** Tumbling window size in milliseconds. */
        private const val WINDOW_MS = 1000L
    }

    /**
     * Per-peer rate tracking. Uses @Synchronized for atomic window reset.
     */
    private data class PeerWindow(
        val windowStart: AtomicLong = AtomicLong(0),
        val packetCount: AtomicLong = AtomicLong(0)
    ) {
        /**
         * Atomically check and potentially reset the window, then increment the count.
         * @return the count after increment within the current window.
         */
        @Synchronized
        fun incrementAndGet(now: Long, windowMs: Long): Long {
            if (now - windowStart.get() > windowMs) {
                windowStart.set(now)
                packetCount.set(1)
                return 1
            }
            return packetCount.incrementAndGet()
        }
    }

    private val peerWindows = ConcurrentHashMap<String, PeerWindow>()

    /** Total packets dropped due to rate limiting (for telemetry). */
    val totalDropped = AtomicLong(0)

    /**
     * Check if a packet from [peerId] should be allowed.
     *
     * @param peerId The source peer's device name.
     * @return true if the packet is allowed, false if rate-limited.
     */
    fun allowPacket(peerId: String): Boolean {
        val now = System.currentTimeMillis()
        val window = peerWindows.getOrPut(peerId) { PeerWindow() }

        val count = window.incrementAndGet(now, WINDOW_MS)
        if (count > maxPacketsPerSecond) {
            totalDropped.incrementAndGet()
            Log.w(TAG, "RATE_LIMITED peer=$peerId count=$count limit=$maxPacketsPerSecond")
            return false
        }
        return true
    }

    /**
     * Reset rate tracking for a peer (e.g., on reconnect).
     */
    fun resetPeer(peerId: String) {
        peerWindows.remove(peerId)
    }

    /**
     * Clear all tracking state.
     */
    fun reset() {
        peerWindows.clear()
        totalDropped.set(0)
    }
}
