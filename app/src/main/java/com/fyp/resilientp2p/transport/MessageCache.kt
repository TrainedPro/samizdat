package com.fyp.resilientp2p.transport

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe message cache for packet deduplication.
 *
 * Uses [ConcurrentHashMap] for lock-free reads and a non-blocking eviction
 * strategy. Expired entries (older than [ttl]) are removed first; if still
 * over [capacity], the oldest 25% are evicted using a partial scan rather
 * than a full sort to keep eviction at O(n) instead of O(n log n).
 */
class MessageCache(private val capacity: Int = 1000) {

    private val cache = ConcurrentHashMap<String, Long>()
    private val cleanupLock = AtomicBoolean(false)
    private val ttl = 10 * 60 * 1000L // 10 minutes

    /**
     * Atomically marks a packet as seen if not already present.
     * @return true if the packet was newly marked (first time seeing it), false if already seen
     */
    fun tryMarkSeen(packetId: String): Boolean {
        val now = System.currentTimeMillis()
        val previous = cache.putIfAbsent(packetId, now)

        // Trigger eviction if needed (non-blocking, best effort)
        if (cache.size > capacity) {
            evictOldEntries()
        }

        return previous == null
    }

    private fun evictOldEntries() {
        // Use lock to prevent multiple concurrent evictions
        if (!cleanupLock.compareAndSet(false, true)) return
        try {
            val now = System.currentTimeMillis()
            // Use removeIf for atomic removal based on condition — O(n)
            cache.entries.removeIf { now - it.value > ttl }

            // If still over capacity, remove oldest entries using threshold scan — O(n)
            if (cache.size > capacity) {
                val targetSize = capacity * 3 / 4
                // Find approximate cutoff timestamp without sorting:
                // scan once to find min/max, then remove entries below midpoint threshold
                var minTs = Long.MAX_VALUE
                var maxTs = Long.MIN_VALUE
                for (entry in cache.entries) {
                    if (entry.value < minTs) minTs = entry.value
                    if (entry.value > maxTs) maxTs = entry.value
                }
                // Remove entries in the oldest quartile (below 25th percentile timestamp)
                val threshold = minTs + (maxTs - minTs) / 4
                cache.entries.removeIf { it.value <= threshold }

                // If still over, hard-evict by iteration until under target
                if (cache.size > targetSize) {
                    val iter = cache.entries.iterator()
                    while (iter.hasNext() && cache.size > targetSize) {
                        iter.next()
                        iter.remove()
                    }
                }
            }
        } finally {
            cleanupLock.set(false)
        }
    }

    fun clear() {
        cache.clear()
    }
}
