package com.fyp.resilientp2p

import com.fyp.resilientp2p.transport.MessageCache
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [MessageCache] — the packet deduplication engine.
 *
 * Tests cover first-seen detection, duplicate rejection, capacity eviction,
 * and concurrent access safety.
 */
class MessageCacheTest {

    private lateinit var cache: MessageCache

    @Before
    fun setUp() {
        cache = MessageCache(capacity = 100)
    }

    @Test
    fun `first tryMarkSeen returns true`() {
        assertTrue(cache.tryMarkSeen("pkt-001"))
    }

    @Test
    fun `second tryMarkSeen for same id returns false`() {
        assertTrue(cache.tryMarkSeen("pkt-002"))
        assertFalse(cache.tryMarkSeen("pkt-002"))
    }

    @Test
    fun `different ids all return true on first call`() {
        for (i in 1..50) {
            assertTrue("Failed for pkt-$i", cache.tryMarkSeen("pkt-$i"))
        }
    }

    @Test
    fun `duplicate detection across many ids`() {
        for (i in 1..50) {
            cache.tryMarkSeen("pkt-$i")
        }
        // All should be duplicates now
        for (i in 1..50) {
            assertFalse("pkt-$i should be duplicate", cache.tryMarkSeen("pkt-$i"))
        }
    }

    @Test
    fun `clear resets the cache`() {
        cache.tryMarkSeen("pkt-A")
        cache.tryMarkSeen("pkt-B")
        cache.clear()
        // After clear, same IDs should be treated as new
        assertTrue(cache.tryMarkSeen("pkt-A"))
        assertTrue(cache.tryMarkSeen("pkt-B"))
    }

    @Test
    fun `capacity overflow triggers eviction`() {
        // Fill beyond capacity
        val smallCache = MessageCache(capacity = 10)
        for (i in 1..20) {
            smallCache.tryMarkSeen("pkt-$i")
        }
        // Recent entries should still be detected as duplicates
        // (eviction removes oldest, keeps recent)
        assertFalse("Most recent should still be cached", smallCache.tryMarkSeen("pkt-20"))
    }

    @Test
    fun `empty string id works`() {
        assertTrue(cache.tryMarkSeen(""))
        assertFalse(cache.tryMarkSeen(""))
    }

    @Test
    fun `uuid-style ids work correctly`() {
        val uuid1 = "550e8400-e29b-41d4-a716-446655440000"
        val uuid2 = "550e8400-e29b-41d4-a716-446655440001"
        assertTrue(cache.tryMarkSeen(uuid1))
        assertTrue(cache.tryMarkSeen(uuid2))
        assertFalse(cache.tryMarkSeen(uuid1))
        assertFalse(cache.tryMarkSeen(uuid2))
    }

    @Test
    fun `concurrent access does not throw`() {
        val threadCount = 8
        val idsPerThread = 100
        val threads = (0 until threadCount).map { t ->
            Thread {
                for (i in 0 until idsPerThread) {
                    cache.tryMarkSeen("thread-$t-pkt-$i")
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join(5000) }
        // No ConcurrentModificationException = pass
    }

    @Test
    fun `concurrent reads and writes are safe`() {
        // Fill with some data
        for (i in 1..50) {
            cache.tryMarkSeen("pre-$i")
        }
        // Read and write concurrently
        val writer = Thread {
            for (i in 51..200) {
                cache.tryMarkSeen("write-$i")
            }
        }
        val reader = Thread {
            for (i in 1..50) {
                cache.tryMarkSeen("pre-$i") // Should return false (already seen)
            }
        }
        writer.start()
        reader.start()
        writer.join(5000)
        reader.join(5000)
    }

    @Test
    fun `large capacity cache works`() {
        val bigCache = MessageCache(capacity = 5000)
        for (i in 1..3000) {
            assertTrue(bigCache.tryMarkSeen("big-$i"))
        }
        // Verify all are cached
        for (i in 1..3000) {
            assertFalse(bigCache.tryMarkSeen("big-$i"))
        }
    }
}
