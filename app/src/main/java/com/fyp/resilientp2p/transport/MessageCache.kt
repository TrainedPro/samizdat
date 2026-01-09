package com.fyp.resilientp2p.transport

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Thread-safe message cache for packet deduplication.
 * Uses atomic operations to prevent race conditions.
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

    /**
     * @deprecated Use tryMarkSeen() for atomic check-and-mark
     */
    fun hasSeen(packetId: String): Boolean {
        return cache.containsKey(packetId)
    }

    /**
     * @deprecated Use tryMarkSeen() for atomic check-and-mark
     */
    fun markSeen(packetId: String) {
        cache[packetId] = System.currentTimeMillis()
        if (cache.size > capacity) {
            evictOldEntries()
        }
    }

    private fun evictOldEntries() {
        // Use lock to prevent multiple concurrent evictions
        if (!cleanupLock.compareAndSet(false, true)) return
        try {
            val now = System.currentTimeMillis()
            // Use removeIf for atomic removal based on condition
            cache.entries.removeIf { now - it.value > ttl }
            
            // If still over capacity, remove oldest 25% (better than clear())
            if (cache.size > capacity) {
                val sortedEntries = cache.entries.sortedBy { it.value }
                val toRemove = sortedEntries.take(capacity / 4)
                toRemove.forEach { cache.remove(it.key, it.value) } // Atomic conditional remove
            }
        } finally {
            cleanupLock.set(false)
        }
    }

    fun clear() {
        cache.clear()
    }

    fun cleanup(ttlMs: Long) {
        val now = System.currentTimeMillis()
        cache.entries.removeIf { now - it.value > ttlMs }
    }
}
