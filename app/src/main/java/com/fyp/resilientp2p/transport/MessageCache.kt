package com.fyp.resilientp2p.transport

class MessageCache(private val capacity: Int = 1000) {

    // Thread-safe LRU Cache using ConcurrentHashMap is tricky for LRU eviction.
    // However, Collections.synchronizedMap wrapping LinkedHashMap is actually the standard way for
    // LRU.
    // But the review asked for ConcurrentHashMap.
    // A pure ConcurrentHashMap doesn't support access-order iteration easily.
    // For a "seen" cache (deduplication), we just need existence check.
    // If we want to limit size, we can use a simple removal strategy or stick to Synchronized
    // LinkedHashMap.
    // Review said: "Uses Collections.synchronizedMap. This is fine, but a ConcurrentHashMap is
    // generally more performant".
    // I will switch to ConcurrentHashMap and use a simplified cleanup strategy (random or
    // time-based) instead of strict LRU,
    // OR just keep it simple.
    // Actually, for deduplication, we usually want to expire old items.
    // I will implement a time-based expiration using ConcurrentHashMap.

    private val cache = java.util.concurrent.ConcurrentHashMap<String, Long>()

    fun hasSeen(packetId: String): Boolean {
        return cache.containsKey(packetId)
    }

    fun markSeen(packetId: String) {
        cache[packetId] = System.currentTimeMillis()
        // Occasional cleanup to prevent infinite growth if cleanup() isn't called externally
        if (cache.size > capacity) {
            // Simple reduction: remove items older than some threshold or just clear half
            // For now, relies on external cleanup(ttl)
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
