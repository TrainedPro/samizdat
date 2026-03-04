package com.fyp.resilientp2p.security

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Lightweight per-peer reputation system inspired by libp2p GossipSub's peer scoring.
 *
 * Each peer starts with a neutral score (0). Good behaviour earns positive points,
 * bad behaviour earns penalties. The score is used to:
 * - **Prioritise forwarding**: prefer higher-scored peers as next-hop when multiple routes exist.
 * - **Throttle untrusted peers**: peers below [PENALTY_THRESHOLD] get deprioritised for forwarding.
 * - **Feed blacklist decisions**: persistent negative score is evidence of abuse.
 *
 * ## Scoring Events
 * | Event                    | Points | Rationale                              |
 * |--------------------------|--------|----------------------------------------|
 * | Valid packet delivered    | +1     | Peer is contributing useful traffic     |
 * | ACK received (delivery)  | +2     | End-to-end reliability proof            |
 * | Rate limit violation     | -10    | Potential flood attempt                 |
 * | HMAC failure             | -5     | Possible tampering (or key race)        |
 * | Forwarding budget exceed | -3     | Amplification attempt                   |
 * | Blacklist triggered      | -50    | Severe penalty                          |
 *
 * Scores decay toward 0 over time (not yet implemented — session-scoped).
 *
 * @see PeerBlacklist
 * @see RateLimiter
 */
class PeerTrustScorer {
    companion object {
        private const val TAG = "PeerTrust"

        // Score adjustments
        const val VALID_PACKET = 1
        const val ACK_DELIVERED = 2
        const val RATE_LIMIT_VIOLATION = -10
        const val HMAC_FAILURE = -5
        const val FORWARD_BUDGET_EXCEEDED = -3
        const val BLACKLISTED = -50

        /** Peers below this score are considered untrusted and deprioritised. */
        const val PENALTY_THRESHOLD = -20

        /** Floor: score can't go below this (prevents overflow on long-running sessions). */
        const val MIN_SCORE = -500

        /** Ceiling: score can't go above this. */
        const val MAX_SCORE = 500
    }

    /** peerId → trust score */
    private val scores = ConcurrentHashMap<String, AtomicInteger>()

    /** peerId → total valid packets (for stats/debugging) */
    private val validPacketCounts = ConcurrentHashMap<String, AtomicLong>()

    /**
     * Adjust the score for [peerId] by [delta] points.
     * Clamped to [MIN_SCORE]..[MAX_SCORE].
     *
     * @return The new score after adjustment.
     */
    fun adjustScore(peerId: String, delta: Int, reason: String = ""): Int {
        val atomic = scores.computeIfAbsent(peerId) { AtomicInteger(0) }
        val newScore = atomic.updateAndGet { current ->
            (current + delta).coerceIn(MIN_SCORE, MAX_SCORE)
        }
        if (delta < 0) {
            Log.d(TAG, "PENALTY peer=$peerId delta=$delta reason=$reason newScore=$newScore")
        }
        return newScore
    }

    /**
     * Record a valid packet received from [peerId]. Convenience for +[VALID_PACKET].
     */
    fun recordValidPacket(peerId: String) {
        adjustScore(peerId, VALID_PACKET)
        validPacketCounts.computeIfAbsent(peerId) { AtomicLong(0) }.incrementAndGet()
    }

    /**
     * Record a delivery confirmation (ACK) from [peerId]. Convenience for +[ACK_DELIVERED].
     */
    fun recordDeliveryConfirmed(peerId: String) {
        adjustScore(peerId, ACK_DELIVERED, "ack_delivered")
    }

    /**
     * Record a rate limit violation from [peerId]. Convenience for +[RATE_LIMIT_VIOLATION].
     */
    fun recordRateLimitViolation(peerId: String) {
        adjustScore(peerId, RATE_LIMIT_VIOLATION, "rate_limit")
    }

    /**
     * Record an HMAC failure from [peerId]. Convenience for +[HMAC_FAILURE].
     */
    fun recordHmacFailure(peerId: String) {
        adjustScore(peerId, HMAC_FAILURE, "hmac_failure")
    }

    /**
     * Record a forwarding budget excess from [peerId]. Convenience for +[FORWARD_BUDGET_EXCEEDED].
     */
    fun recordForwardBudgetExceeded(peerId: String) {
        adjustScore(peerId, FORWARD_BUDGET_EXCEEDED, "forward_exceeded")
    }

    /**
     * Get the current score for [peerId]. Returns 0 if unknown.
     */
    fun getScore(peerId: String): Int = scores[peerId]?.get() ?: 0

    /**
     * Returns true if [peerId] has a score below [PENALTY_THRESHOLD] (untrusted).
     */
    fun isUntrusted(peerId: String): Boolean = getScore(peerId) < PENALTY_THRESHOLD

    /**
     * Get all peer scores as an immutable snapshot. Useful for debugging/UI.
     */
    fun getAllScores(): Map<String, Int> = scores.mapValues { it.value.get() }

    /**
     * Reset score for [peerId] to 0. Called on reconnect.
     */
    fun resetPeer(peerId: String) {
        scores.remove(peerId)
        validPacketCounts.remove(peerId)
    }

    /**
     * Clear all scores.
     */
    fun clear() {
        scores.clear()
        validPacketCounts.clear()
    }
}
