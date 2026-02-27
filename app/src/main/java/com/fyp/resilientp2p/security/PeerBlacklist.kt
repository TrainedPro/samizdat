package com.fyp.resilientp2p.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/**
 * Persistent peer blacklist for the Samizdat mesh network.
 *
 * Blacklisted peers have ALL their packets silently dropped at the first hop.
 * The blacklist is persisted in [SharedPreferences] so it survives app restarts.
 *
 * ## Use Cases
 * - Block a peer flooding the mesh with spam.
 * - Block a peer sending invalid/tampered packets (failed HMAC).
 * - Manual block by user from the UI.
 *
 * ## Auto-blacklisting
 * The system can optionally auto-blacklist peers that exceed the rate limiter
 * threshold N times within a window. This is configurable via [autoBlacklistAfterViolations].
 *
 * @param context Application context for SharedPreferences access.
 * @param autoBlacklistAfterViolations Number of rate-limit violations before auto-blacklisting.
 *        Set to 0 to disable auto-blacklisting.
 *
 * @see RateLimiter
 * @see SecurityManager
 */
class PeerBlacklist(
    context: Context,
    private val autoBlacklistAfterViolations: Int = DEFAULT_AUTO_BLACKLIST_THRESHOLD
) {
    companion object {
        private const val TAG = "PeerBlacklist"
        private const val PREFS_NAME = "peer_blacklist"
        private const val KEY_BLACKLIST = "blacklisted_peers"
        /** Default: auto-blacklist after 10 rate-limit violations in a session. */
        const val DEFAULT_AUTO_BLACKLIST_THRESHOLD = 10
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** In-memory blacklist set (mirrored to SharedPreferences). */
    private val blacklist = ConcurrentHashMap.newKeySet<String>()

    /** Rate-limit violation counter per peer (session-scoped, not persisted). */
    private val violationCounts = ConcurrentHashMap<String, Int>()

    init {
        // Load persisted blacklist
        val saved = prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
        blacklist.addAll(saved)
        Log.i(TAG, "Loaded ${blacklist.size} blacklisted peers")
    }

    /**
     * Returns true if [peerId] is blacklisted.
     */
    fun isBlacklisted(peerId: String): Boolean = blacklist.contains(peerId)

    /**
     * Add a peer to the blacklist. Persisted immediately.
     *
     * @param peerId The peer's device name.
     * @param reason Human-readable reason for logging.
     */
    fun blacklist(peerId: String, reason: String = "manual") {
        if (blacklist.add(peerId)) {
            persist()
            Log.w(TAG, "BLACKLISTED peer=$peerId reason=$reason")
        }
    }

    /**
     * Remove a peer from the blacklist. Persisted immediately.
     */
    fun unblacklist(peerId: String) {
        if (blacklist.remove(peerId)) {
            violationCounts.remove(peerId)
            persist()
            Log.i(TAG, "UNBLACKLISTED peer=$peerId")
        }
    }

    /**
     * Returns the current blacklist as an immutable set.
     */
    fun getBlacklist(): Set<String> = blacklist.toSet()

    /**
     * Record a rate-limit violation for [peerId]. If the violation count exceeds
     * [autoBlacklistAfterViolations], the peer is automatically blacklisted.
     *
     * @return true if the peer was auto-blacklisted as a result.
     */
    fun recordViolation(peerId: String): Boolean {
        if (autoBlacklistAfterViolations <= 0) return false
        val count = violationCounts.merge(peerId, 1) { old, new -> old + new } ?: 1
        if (count >= autoBlacklistAfterViolations) {
            blacklist(peerId, "auto-blacklist after $count rate-limit violations")
            return true
        }
        return false
    }

    /**
     * Clear all blacklist entries and violation counts.
     */
    fun clear() {
        blacklist.clear()
        violationCounts.clear()
        persist()
        Log.i(TAG, "Blacklist cleared")
    }

    private fun persist() {
        prefs.edit().putStringSet(KEY_BLACKLIST, blacklist.toSet()).apply()
    }
}
