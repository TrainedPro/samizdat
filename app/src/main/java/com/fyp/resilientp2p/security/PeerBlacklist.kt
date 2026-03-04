package com.fyp.resilientp2p.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import androidx.core.content.edit

/**
 * Persistent peer blacklist with **expiring bans** for the Samizdat mesh network.
 *
 * Blacklisted peers have ALL their packets silently dropped at the first hop.
 * The blacklist is persisted in [SharedPreferences] so it survives app restarts.
 *
 * ## Ban Duration & Cooldown
 * Auto-bans are **not permanent**: they start at [BASE_BAN_DURATION_MS] (1 hour) and
 * double on each repeat offence up to [MAX_BAN_DURATION_MS] (24 hours). This prevents
 * false-positive permanent exclusion while still punishing persistent abusers.
 *
 * Manual bans (from UI) are permanent and never expire.
 *
 * ## Auto-blacklisting
 * The system auto-blacklists peers that exceed the rate limiter threshold
 * [autoBlacklistAfterViolations] times within a window.
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
        private const val KEY_BAN_EXPIRY = "ban_expiry"
        private const val KEY_BAN_COUNT = "ban_count"
        /**
         * Default: auto-blacklist after 50 rate-limit violations in a session.
         * Raised from 10 → 50 because legitimate peers can hit rate limits
         * during bursty file/audio transfers.
         */
        const val DEFAULT_AUTO_BLACKLIST_THRESHOLD = 50

        /** First auto-ban lasts 1 hour. Doubles on each repeat offence. */
        const val BASE_BAN_DURATION_MS = 60 * 60 * 1000L       // 1 hour
        /** Maximum auto-ban duration after exponential backoff. */
        const val MAX_BAN_DURATION_MS = 24 * 60 * 60 * 1000L   // 24 hours
        /** Expiry value meaning "never expires" (manual bans). */
        const val PERMANENT = 0L
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** In-memory blacklist: peerId → expiryTimestamp (0 = permanent). */
    private val blacklist = ConcurrentHashMap<String, Long>()

    /** How many times a peer has been auto-banned (for exponential backoff). */
    private val banCounts = ConcurrentHashMap<String, Int>()

    /** Rate-limit violation counter per peer, keyed by "peerId:reason" (session-scoped). */
    private val violationCounts = ConcurrentHashMap<String, Int>()

    init {
        // Load persisted blacklist
        val savedPeers = prefs.getStringSet(KEY_BLACKLIST, emptySet()) ?: emptySet()
        val expiryPrefs = prefs.getString(KEY_BAN_EXPIRY, "") ?: ""
        val countPrefs = prefs.getString(KEY_BAN_COUNT, "") ?: ""

        // Parse expiry map: "peer1=12345,peer2=0,..."
        val expiryMap = parseMap(expiryPrefs)
        val countMap = parseMap(countPrefs)

        for (peer in savedPeers) {
            val expiry = expiryMap[peer] ?: PERMANENT
            // Skip already-expired entries on load
            if (expiry != PERMANENT && expiry < System.currentTimeMillis()) {
                continue
            }
            blacklist[peer] = expiry
            countMap[peer]?.let { banCounts[peer] = it.toInt() }
        }

        persist()
        Log.i(TAG, "Loaded ${blacklist.size} blacklisted peers (${savedPeers.size - blacklist.size} expired on load)")
    }

    /**
     * Returns true if [peerId] is blacklisted (and ban hasn't expired).
     * Lazily removes expired bans.
     */
    fun isBlacklisted(peerId: String): Boolean {
        val expiry = blacklist[peerId] ?: return false
        if (expiry == PERMANENT) return true
        if (System.currentTimeMillis() < expiry) return true
        // Ban expired — remove
        blacklist.remove(peerId)
        persist()
        Log.i(TAG, "AUTO_UNBAN peer=$peerId (ban expired)")
        return false
    }

    /**
     * Add a peer to the blacklist with optional expiry.
     *
     * @param peerId The peer's device name or endpoint ID.
     * @param reason Human-readable reason for logging.
     * @param expiryMs Absolute timestamp when ban expires, or [PERMANENT] for never.
     */
    fun blacklist(peerId: String, reason: String = "manual", expiryMs: Long = PERMANENT) {
        blacklist[peerId] = expiryMs
        persist()
        val expiryStr = if (expiryMs == PERMANENT) "permanent" else "${(expiryMs - System.currentTimeMillis()) / 60000}min"
        Log.w(TAG, "BLACKLISTED peer=$peerId reason=$reason expiry=$expiryStr")
    }

    /**
     * Remove a peer from the blacklist. Persisted immediately.
     */
    fun unblacklist(peerId: String) {
        if (blacklist.remove(peerId) != null) {
            resetViolations(peerId)
            persist()
            Log.i(TAG, "UNBLACKLISTED peer=$peerId")
        }
    }

    /**
     * Returns the current blacklist as an immutable set (only non-expired entries).
     */
    fun getBlacklist(): Set<String> {
        val now = System.currentTimeMillis()
        return blacklist.entries
            .filter { it.value == PERMANENT || it.value > now }
            .map { it.key }
            .toSet()
    }

    /**
     * Record a categorized violation for [peerId].
     *
     * Only "rate_limit" violations count toward auto-blacklisting. Other
     * categories (e.g. "hmac") are logged but do NOT trigger auto-ban,
     * because false positives occur during key re-exchange races.
     *
     * @param peerId The offending peer.
     * @param reason Category of violation: "rate_limit", "hmac", etc.
     * @return true if the peer was auto-blacklisted as a result.
     */
    fun recordViolation(peerId: String, reason: String = "rate_limit"): Boolean {
        val key = "$peerId:$reason"
        val count = violationCounts.merge(key, 1) { old, inc -> old + inc } ?: 1
        Log.d(TAG, "Violation peer=$peerId reason=$reason count=$count")

        // Only rate-limit violations trigger auto-blacklisting
        if (reason == "rate_limit" && autoBlacklistAfterViolations > 0 &&
            count >= autoBlacklistAfterViolations) {
            // Exponential backoff: 1h → 2h → 4h → 8h → 16h → 24h (cap)
            val banNumber = banCounts.merge(peerId, 1) { old, _ -> old + 1 } ?: 1
            val duration = (BASE_BAN_DURATION_MS * (1L shl (banNumber - 1).coerceAtMost(5)))
                .coerceAtMost(MAX_BAN_DURATION_MS)
            val expiryMs = System.currentTimeMillis() + duration

            blacklist(peerId, "auto-blacklist after $count violations (ban #$banNumber, ${duration / 60000}min)", expiryMs)
            return true
        }
        return false
    }

    /**
     * Reset all violation counters for [peerId].
     * Called when a peer reconnects, giving them a clean slate for violations
     * (but NOT removing any active ban or ban count).
     */
    fun resetViolations(peerId: String) {
        val removed = violationCounts.keys.removeAll { it.startsWith("$peerId:") }
        if (removed) {
            Log.d(TAG, "Reset violations for peer=$peerId")
        }
    }

    /**
     * Get the current violation count for a specific peer and reason.
     */
    fun getViolationCount(peerId: String, reason: String = "rate_limit"): Int =
        violationCounts["$peerId:$reason"] ?: 0

    /**
     * Get the number of times a peer has been auto-banned (for exponential backoff).
     */
    fun getBanCount(peerId: String): Int = banCounts[peerId] ?: 0

    /**
     * Clear all blacklist entries, violation counts, and ban history.
     */
    fun clear() {
        blacklist.clear()
        violationCounts.clear()
        banCounts.clear()
        persist()
        Log.i(TAG, "Blacklist cleared")
    }

    private fun persist() {
        prefs.edit {
            putStringSet(KEY_BLACKLIST, blacklist.keys.toSet())
            putString(KEY_BAN_EXPIRY, blacklist.entries.joinToString(",") { "${it.key}=${it.value}" })
            putString(KEY_BAN_COUNT, banCounts.entries.joinToString(",") { "${it.key}=${it.value}" })
        }
    }

    /** Parse "key1=val1,key2=val2,..." into a map. */
    private fun parseMap(encoded: String): MutableMap<String, Long> {
        if (encoded.isBlank()) return mutableMapOf()
        return encoded.split(",")
            .mapNotNull { entry ->
                val parts = entry.split("=", limit = 2)
                if (parts.size == 2) parts[0] to (parts[1].toLongOrNull() ?: 0L)
                else null
            }
            .toMap()
            .toMutableMap()
    }
}
