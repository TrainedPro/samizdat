package com.fyp.resilientp2p.transport

class MessageCache(private val capacity: Int = 1000) {

    // Thread-safe cache using ConcurrentHashMap for deduplication.
    // Implements time-based eviction to prevent memory leaks.

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun hasSeen(packetId: String): Boolean {
        return cache.containsKey(packetId)
    }

    fun markSeen(packetId: String) {
        cache[packetId] = System.currentTimeMillis()
        // Simple eviction: if size exceeds capacity, remove oldest entries
        if (cache.size > capacity) {
            val now = System.currentTimeMillis()
            // Remove items older than 10 minutes
            val ttl = 10 * 60 * 1000L
            val iterator = cache.entries.iterator()
            while (iterator.hasNext()) {
                if (now - iterator.next().value > ttl) {
                    iterator.remove()
                }
            }
            // If still too big, just clear it (drastic but safe for memory)
            if (cache.size > capacity) {
                cache.clear()
            }
        }
    }

    fun clear() {
        cache.clear()
    }

    fun cleanup(ttlMs: Long) {
        val now = System.currentTimeMillis()
        val iterator = cache.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (now - entry.value > ttlMs) {
                iterator.remove()
            }
        }
    }
}
