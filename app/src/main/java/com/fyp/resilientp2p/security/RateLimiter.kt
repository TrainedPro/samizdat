package com.fyp.resilientp2p.security

import android.util.Log
import com.fyp.resilientp2p.transport.PacketType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Per-peer, per-category packet rate limiter to prevent denial-of-service attacks
 * from malicious mesh participants.
 *
 * ## Design Rationale — Tiered Approach (inspired by BitTorrent & real mesh networks)
 *
 * Real P2P networks like BitTorrent use **choking** (per-peer upload slots) and
 * **optimistic unchoking** to balance fairness. BATMAN-adv rate-limits even its own
 * OGM (Originator Messages) to prevent flooding. Our tiered limiter combines both:
 * different packet categories get different budgets, and even protocol-essential
 * types are bounded to prevent amplification attacks.
 *
 * | Category           | Budget (pkts/sec/peer) | Packet Types                                         |
 * |--------------------|----------------------|------------------------------------------------------|
 * | **PROTOCOL**       | 10                    | IDENTITY, ROUTE_ANNOUNCE                             |
 * | **EMERGENCY**      | 5                     | EMERGENCY (generous but not unlimited)                |
 * | **CONTROL**        | 50                    | PING, PONG, ACK, STORE_FORWARD, LOCATION_PING       |
 * | **DATA**           | 200                   | DATA, GROUP_MESSAGE, ENCOUNTER_LOG                   |
 * | **BULK_TRANSFER**  | 500                   | FILE_META/ANNOUNCE/REQUEST/CHUNK, AUDIO_DATA/CONTROL |
 *
 * Additionally, a **forwarding budget** limits how many packets a single peer can
 * cause us to relay per second, preventing amplification attacks where a malicious
 * peer injects packets addressed to non-existent destinations (flood fallback).
 *
 * Per-peer, not global: a single misbehaving peer should not starve legitimate ones.
 * Thread-safe via ConcurrentHashMap + @Synchronized tumbling window.
 *
 * @param protocolLimit Max protocol packets per peer per second.
 * @param emergencyLimit Max emergency packets per peer per second.
 * @param controlLimit Max control packets per peer per second.
 * @param dataLimit Max data packets per peer per second.
 * @param bulkLimit Max bulk-transfer packets per peer per second.
 * @param forwardBudget Max packets a single peer can trigger us to forward per second.
 * @see SecurityManager
 * @see PeerBlacklist
 */
class RateLimiter(
    private val protocolLimit: Int = DEFAULT_PROTOCOL_LIMIT,
    private val emergencyLimit: Int = DEFAULT_EMERGENCY_LIMIT,
    private val controlLimit: Int = DEFAULT_CONTROL_LIMIT,
    private val dataLimit: Int = DEFAULT_DATA_LIMIT,
    private val bulkLimit: Int = DEFAULT_BULK_LIMIT,
    private val forwardBudget: Int = DEFAULT_FORWARD_BUDGET
) {
    companion object {
        private const val TAG = "RateLimiter"

        /**
         * Default rate limits per category per peer per second.
         *
         * PROTOCOL (10/sec): IDENTITY is sent once on connect + on re-key;
         * ROUTE_ANNOUNCE every 8–10s. 10/sec is 100× normal, generous enough
         * for reconnect storms but prevents flooding.
         *
         * EMERGENCY (5/sec): SOS beacon sends every 30s. 5/sec allows burst
         * retries while capping amplification (BATMAN-adv uses similar OGM limits).
         */
        const val DEFAULT_PROTOCOL_LIMIT = 10
        const val DEFAULT_EMERGENCY_LIMIT = 5
        const val DEFAULT_CONTROL_LIMIT = 50
        const val DEFAULT_DATA_LIMIT = 200
        const val DEFAULT_BULK_LIMIT = 500

        /**
         * Max packets any single peer can cause us to forward per second.
         * Inspired by libp2p GossipSub's per-peer forwarding budget.
         * 100/sec is generous for normal routing but prevents one peer
         * from flooding the mesh through us.
         */
        const val DEFAULT_FORWARD_BUDGET = 100

        /** Tumbling window size in milliseconds. */
        private const val WINDOW_MS = 1000L

        /** Protocol-essential types — limited, not unlimited. */
        private val PROTOCOL_TYPES = setOf(
            PacketType.IDENTITY,
            PacketType.ROUTE_ANNOUNCE
        )

        /** Emergency types — separate, tighter limit. */
        private val EMERGENCY_TYPES = setOf(
            PacketType.EMERGENCY
        )

        /** Control plane packet types. */
        private val CONTROL_TYPES = setOf(
            PacketType.PING,
            PacketType.PONG,
            PacketType.ACK,
            PacketType.STORE_FORWARD,
            PacketType.LOCATION_PING
        )

        /** Bulk transfer packet types (file + audio). */
        private val BULK_TYPES = setOf(
            PacketType.FILE_META,
            PacketType.FILE_ANNOUNCE,
            PacketType.FILE_REQUEST,
            PacketType.FILE_CHUNK,
            PacketType.AUDIO_DATA,
            PacketType.AUDIO_CONTROL
        )
        // Everything else (DATA, GROUP_MESSAGE, ENCOUNTER_LOG, GOSSIP) → DATA category
    }

    /** Rate-limit category for tiered budgets. */
    enum class Category { PROTOCOL, EMERGENCY, CONTROL, DATA, BULK }

    /**
     * Per-peer, per-category rate tracking. Uses @Synchronized for atomic window reset.
     */
    private data class PeerWindow(
        val windowStart: AtomicLong = AtomicLong(0),
        val packetCount: AtomicLong = AtomicLong(0)
    ) {
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

    /** peerWindows keyed by "$peerId:$category" */
    private val peerWindows = ConcurrentHashMap<String, PeerWindow>()

    /** Total packets dropped due to rate limiting (for telemetry). */
    val totalDropped = AtomicLong(0)

    /**
     * Classify a packet type into a rate-limit category.
     */
    fun classify(type: PacketType): Category {
        if (type in PROTOCOL_TYPES) return Category.PROTOCOL
        if (type in EMERGENCY_TYPES) return Category.EMERGENCY
        if (type in CONTROL_TYPES) return Category.CONTROL
        if (type in BULK_TYPES) return Category.BULK
        return Category.DATA
    }

    /**
     * Check if a packet from [peerId] of [packetType] should be allowed.
     *
     * @param peerId The source peer's endpoint ID (physical transport identity).
     * @param packetType The packet type (used for tiered classification).
     * @return true if the packet is allowed, false if rate-limited.
     */
    fun allowPacket(peerId: String, packetType: PacketType): Boolean {
        val category = classify(packetType)

        val limit = when (category) {
            Category.PROTOCOL -> protocolLimit
            Category.EMERGENCY -> emergencyLimit
            Category.CONTROL -> controlLimit
            Category.DATA -> dataLimit
            Category.BULK -> bulkLimit
        }

        val key = "$peerId:${category.name}"
        val now = System.currentTimeMillis()
        val window = peerWindows.getOrPut(key) { PeerWindow() }

        val count = window.incrementAndGet(now, WINDOW_MS)
        if (count > limit) {
            totalDropped.incrementAndGet()
            Log.w(TAG, "RATE_LIMITED peer=$peerId category=$category count=$count limit=$limit")
            return false
        }
        return true
    }

    /**
     * Check if we should forward a packet on behalf of [peerId].
     * Prevents amplification attacks where one peer floods packets to
     * non-existent destinations, causing us to relay to all neighbors.
     *
     * @return true if forwarding is allowed, false if budget exceeded.
     */
    fun allowForward(peerId: String): Boolean {
        val key = "$peerId:FORWARD"
        val now = System.currentTimeMillis()
        val window = peerWindows.getOrPut(key) { PeerWindow() }

        val count = window.incrementAndGet(now, WINDOW_MS)
        if (count > forwardBudget) {
            totalDropped.incrementAndGet()
            Log.w(TAG, "FORWARD_BUDGET_EXCEEDED peer=$peerId count=$count limit=$forwardBudget")
            return false
        }
        return true
    }

    /**
     * Legacy overload for backward compatibility (treats as DATA category).
     */
    fun allowPacket(peerId: String): Boolean = allowPacket(peerId, PacketType.DATA)

    /**
     * Reset rate tracking for a peer (e.g., on reconnect).
     */
    fun resetPeer(peerId: String) {
        Category.entries.forEach { cat ->
            peerWindows.remove("$peerId:${cat.name}")
        }
        peerWindows.remove("$peerId:FORWARD")
    }

    /**
     * Clear all tracking state.
     */
    fun reset() {
        peerWindows.clear()
        totalDropped.set(0)
    }
}
