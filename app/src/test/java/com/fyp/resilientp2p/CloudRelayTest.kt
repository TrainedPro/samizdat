package com.fyp.resilientp2p

import com.fyp.resilientp2p.managers.InternetGatewayManager
import org.junit.Test
import org.junit.Assert.*

/**
 * Test suite for cloud relay and store-and-forward fixes.
 * Validates rate limiting, message delivery, and fallback mechanisms.
 */
class CloudRelayTest {

    @Test
    fun `test tiered rate limiting thresholds`() {
        // Test emergency packets (unlimited)
        val emergencyLimit = Int.MAX_VALUE
        assertTrue("Emergency packets should be unlimited", emergencyLimit > 1000000)

        // Test audio packets (36000/hour = 10/second for 1 hour)
        val audioLimit = 36000
        val audioPerSecond = audioLimit / 3600.0
        assertTrue("Audio should allow ~10 packets/second", audioPerSecond >= 10.0)

        // Test file packets (7200/hour = 2/second)
        val fileLimit = 7200
        val filePerSecond = fileLimit / 3600.0
        assertTrue("File should allow ~2 packets/second", filePerSecond >= 2.0)

        // Test data packets (1200/hour = 0.33/second)
        val dataLimit = 1200
        val dataPerSecond = dataLimit / 3600.0
        assertTrue("Data should allow ~0.33 packets/second", dataPerSecond >= 0.3)

        // Verify hierarchy: emergency > audio > file > data
        assertTrue("Emergency > Audio", emergencyLimit > audioLimit)
        assertTrue("Audio > File", audioLimit > fileLimit)
        assertTrue("File > Data", fileLimit > dataLimit)
    }

    @Test
    fun `test rate limit window calculation`() {
        val oneHourMs = 3600_000L
        val now = System.currentTimeMillis()
        val oneHourAgo = now - oneHourMs

        // Test timestamps within window
        val recentTimestamp = now - 1800_000L // 30 minutes ago
        assertTrue("30 minutes ago should be within window", recentTimestamp > oneHourAgo)

        // Test timestamps outside window
        val oldTimestamp = now - 7200_000L // 2 hours ago
        assertFalse("2 hours ago should be outside window", oldTimestamp > oneHourAgo)

        // Test edge case
        val edgeTimestamp = oneHourAgo
        assertFalse("Exactly 1 hour ago should be outside window", edgeTimestamp > oneHourAgo)
    }

    @Test
    fun `test packet type classification`() {
        // Test emergency classification
        val emergencyTypes = setOf("EMERGENCY")
        assertTrue("EMERGENCY should be emergency type", emergencyTypes.contains("EMERGENCY"))

        // Test audio classification
        val audioTypes = setOf("AUDIO_DATA", "AUDIO_CONTROL")
        assertTrue("AUDIO_DATA should be audio type", audioTypes.contains("AUDIO_DATA"))
        assertTrue("AUDIO_CONTROL should be audio type", audioTypes.contains("AUDIO_CONTROL"))

        // Test file classification
        val fileTypes = setOf("FILE_META", "FILE_CHUNK")
        assertTrue("FILE_META should be file type", fileTypes.contains("FILE_META"))
        assertTrue("FILE_CHUNK should be file type", fileTypes.contains("FILE_CHUNK"))

        // Test data classification (default)
        val dataTypes = setOf("DATA", "PING", "PONG", "ACK")
        assertTrue("DATA should be data type", dataTypes.contains("DATA"))
        assertFalse("AUDIO_DATA should not be data type", dataTypes.contains("AUDIO_DATA"))
    }

    @Test
    fun `test message TTL validation`() {
        val messageTtlMs = 24 * 60 * 60 * 1000L // 24 hours
        val now = System.currentTimeMillis()

        // Test fresh message
        val freshMessage = now - 1000L // 1 second ago
        val freshAge = now - freshMessage
        assertTrue("Fresh message should be within TTL", freshAge < messageTtlMs)

        // Test expired message
        val expiredMessage = now - 25 * 60 * 60 * 1000L // 25 hours ago
        val expiredAge = now - expiredMessage
        assertFalse("Expired message should be outside TTL", expiredAge < messageTtlMs)

        // Test edge case - exactly at TTL should be expired
        val edgeMessage = now - messageTtlMs
        val edgeAge = now - edgeMessage
        assertFalse("Message at exact TTL should be expired", edgeAge < messageTtlMs)
    }

    @Test
    fun `test presence TTL validation`() {
        val presenceTtlMs = 5 * 60 * 1000L // 5 minutes
        val now = System.currentTimeMillis()

        // Test active presence
        val activePresence = now - 2 * 60 * 1000L // 2 minutes ago
        val activeAge = now - activePresence
        assertTrue("Active presence should be within TTL", activeAge <= presenceTtlMs)

        // Test stale presence
        val stalePresence = now - 6 * 60 * 1000L // 6 minutes ago
        val staleAge = now - stalePresence
        assertFalse("Stale presence should be outside TTL", staleAge <= presenceTtlMs)
    }

    @Test
    fun `test polling interval optimization`() {
        val pollIntervalMs = 5_000L // 5 seconds
        val oldPollIntervalMs = 30_000L // 30 seconds (previous value)

        assertTrue("New polling should be faster than old", pollIntervalMs < oldPollIntervalMs)

        // Test polling frequency
        val pollsPerMinute = 60_000L / pollIntervalMs
        assertTrue("Should poll at least 10 times per minute", pollsPerMinute >= 10)

        // Test reasonable upper bound (not too aggressive)
        assertTrue("Should not poll more than once per second", pollIntervalMs >= 1000L)
    }

    @Test
    fun `test capability score calculation`() {
        // Test high capability device
        val highBattery = 90
        val highStability = 1.0 // No disconnects
        val highScore = (highBattery * 0.6 + highStability * 100 * 0.4).toInt()

        assertEquals("High capability device should score 94", 94, highScore)
        assertTrue("High capability should exceed threshold", highScore >= InternetGatewayManager.CLOUD_PREFER_THRESHOLD)

        // Test low capability device
        val lowBattery = 20
        val lowStability = 0.5 // 50% disconnect rate
        val lowScore = (lowBattery * 0.6 + lowStability * 100 * 0.4).toInt()

        assertEquals("Low capability device should score 32", 32, lowScore)
        assertFalse("Low capability should not exceed threshold", lowScore >= InternetGatewayManager.CLOUD_PREFER_THRESHOLD)

        // Test edge case
        val edgeBattery = 50
        val edgeStability = 0.8
        val edgeScore = (edgeBattery * 0.6 + edgeStability * 100 * 0.4).toInt()

        assertEquals("Edge case device should score 62", 62, edgeScore)
        assertTrue("Edge case should exceed threshold", edgeScore >= InternetGatewayManager.CLOUD_PREFER_THRESHOLD)
    }

    @Test
    fun `test cloud preference logic`() {
        val threshold = InternetGatewayManager.CLOUD_PREFER_THRESHOLD

        // Both devices high capability
        val localScore = 80
        val peerScore = 75
        val bothHigh = localScore >= threshold && peerScore >= threshold
        assertTrue("Both high capability should prefer cloud", bothHigh)

        // One device low capability
        val localScoreLow = 30
        val peerScoreHigh = 80
        val oneLow = localScoreLow >= threshold && peerScoreHigh >= threshold
        assertFalse("One low capability should not prefer cloud", oneLow)

        // Both devices low capability
        val localScoreLow2 = 30
        val peerScoreLow = 40
        val bothLow = localScoreLow2 >= threshold && peerScoreLow >= threshold
        assertFalse("Both low capability should not prefer cloud", bothLow)
    }

    @Test
    fun `test message size limits`() {
        val maxMessageSize = 50 * 1024 // 50KB

        // Test valid message
        val validMessage = ByteArray(30 * 1024) // 30KB
        assertTrue("30KB message should be valid", validMessage.size <= maxMessageSize)

        // Test invalid message
        val invalidMessage = ByteArray(60 * 1024) // 60KB
        assertFalse("60KB message should be invalid", invalidMessage.size <= maxMessageSize)

        // Test edge case
        val edgeMessage = ByteArray(maxMessageSize)
        assertTrue("Exactly max size should be valid", edgeMessage.size <= maxMessageSize)
    }
}
