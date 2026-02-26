package com.fyp.resilientp2p.data

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

/**
 * Live network statistics for the mesh node.
 * Thread-safe via AtomicLong and ConcurrentHashMap.
 * Exposed to the UI via P2PState for real-time dashboard.
 * Persisted periodically to DB via log entries for post-test analysis.
 */
data class NetworkStatsSnapshot(
    val uptimeMs: Long = 0,
    val batteryLevel: Int = -1, // -1 = unknown
    val batteryTemperature: Float = 0f,
    val totalBytesSent: Long = 0,
    val totalBytesReceived: Long = 0,
    val totalPacketsSent: Long = 0,
    val totalPacketsReceived: Long = 0,
    val totalPacketsForwarded: Long = 0,
    val totalPacketsDropped: Long = 0,
    val totalConnectionsEstablished: Long = 0,
    val totalConnectionsLost: Long = 0,
    val currentNeighborCount: Int = 0,
    val currentRouteCount: Int = 0,
    val avgRttMs: Long = 0,
    val storeForwardQueued: Long = 0,
    val storeForwardDelivered: Long = 0,
    val peerStats: Map<String, PeerStatsSnapshot> = emptyMap()
)

data class PeerStatsSnapshot(
    val peerName: String,
    val connectedSinceMs: Long = 0,
    val lastRttMs: Long = -1,
    val bytesSent: Long = 0,
    val bytesReceived: Long = 0,
    val packetsSent: Long = 0,
    val packetsReceived: Long = 0,
    val disconnectCount: Long = 0
)

/**
 * Mutable thread-safe stats tracker used internally by P2PManager.
 * Call [snapshot()] to produce an immutable copy for the UI.
 */
class NetworkStats {
    val startTimeMs = System.currentTimeMillis()
    val totalBytesSent = AtomicLong(0)
    val totalBytesReceived = AtomicLong(0)
    val totalPacketsSent = AtomicLong(0)
    val totalPacketsReceived = AtomicLong(0)
    val totalPacketsForwarded = AtomicLong(0)
    val totalPacketsDropped = AtomicLong(0)
    val totalConnectionsEstablished = AtomicLong(0)
    val totalConnectionsLost = AtomicLong(0)
    val storeForwardQueued = AtomicLong(0)
    val storeForwardDelivered = AtomicLong(0)

    // Battery
    @Volatile var batteryLevel: Int = -1
    @Volatile var batteryTemperature: Float = 0f

    // Per-peer RTT tracking
    val peerRtt = ConcurrentHashMap<String, Long>() // peerId -> lastRttMs
    val peerConnectedSince = ConcurrentHashMap<String, Long>() // peerId -> timestamp
    val peerBytesSent = ConcurrentHashMap<String, AtomicLong>()
    val peerBytesReceived = ConcurrentHashMap<String, AtomicLong>()
    val peerPacketsSent = ConcurrentHashMap<String, AtomicLong>()
    val peerPacketsReceived = ConcurrentHashMap<String, AtomicLong>()
    val peerDisconnectCount = ConcurrentHashMap<String, AtomicLong>()

    fun recordPeerConnected(peerId: String) {
        peerConnectedSince[peerId] = System.currentTimeMillis()
        totalConnectionsEstablished.incrementAndGet()
    }

    fun recordPeerDisconnected(peerId: String) {
        peerConnectedSince.remove(peerId)
        peerDisconnectCount.getOrPut(peerId) { AtomicLong(0) }.incrementAndGet()
        totalConnectionsLost.incrementAndGet()
    }

    fun recordPacketSent(peerId: String, sizeBytes: Int) {
        totalPacketsSent.incrementAndGet()
        totalBytesSent.addAndGet(sizeBytes.toLong())
        peerPacketsSent.getOrPut(peerId) { AtomicLong(0) }.incrementAndGet()
        peerBytesSent.getOrPut(peerId) { AtomicLong(0) }.addAndGet(sizeBytes.toLong())
    }

    fun recordPacketReceived(peerId: String, sizeBytes: Int) {
        totalPacketsReceived.incrementAndGet()
        totalBytesReceived.addAndGet(sizeBytes.toLong())
        peerPacketsReceived.getOrPut(peerId) { AtomicLong(0) }.incrementAndGet()
        peerBytesReceived.getOrPut(peerId) { AtomicLong(0) }.addAndGet(sizeBytes.toLong())
    }

    fun recordRtt(peerId: String, rttMs: Long) {
        peerRtt[peerId] = rttMs
    }

    /**
     * Ensure a peer has a connectedSince entry. Called after IDENTITY rename to handle
     * the race where multiple peers connect as "Unknown" and the first rename consumes
     * the shared key, leaving subsequent peers with no stats entry.
     */
    fun ensurePeerTracked(peerId: String) {
        peerConnectedSince.putIfAbsent(peerId, System.currentTimeMillis())
    }

    /**
     * Rename a peer across all per-peer stat maps.
     * Called when IDENTITY packet arrives and updates a neighbor's name from "Unknown".
     */
    fun renamePeer(oldName: String, newName: String) {
        if (oldName == newName) return
        // Transfer connectedSince
        peerConnectedSince.remove(oldName)?.let { peerConnectedSince[newName] = it }
        // Transfer RTT
        peerRtt.remove(oldName)?.let { peerRtt[newName] = it }
        // Transfer byte counters
        peerBytesSent.remove(oldName)?.let { peerBytesSent[newName] = it }
        peerBytesReceived.remove(oldName)?.let { peerBytesReceived[newName] = it }
        // Transfer packet counters
        peerPacketsSent.remove(oldName)?.let { peerPacketsSent[newName] = it }
        peerPacketsReceived.remove(oldName)?.let { peerPacketsReceived[newName] = it }
        // Transfer disconnect count
        peerDisconnectCount.remove(oldName)?.let { peerDisconnectCount[newName] = it }
    }

    fun snapshot(currentNeighborCount: Int, currentRouteCount: Int): NetworkStatsSnapshot {
        val avgRtt = if (peerRtt.isNotEmpty()) peerRtt.values.average().toLong() else 0L
        val peerSnapshots = peerConnectedSince.entries.associate { (peerId, connSince) ->
            peerId to PeerStatsSnapshot(
                peerName = peerId,
                connectedSinceMs = connSince,
                lastRttMs = peerRtt[peerId] ?: -1,
                bytesSent = peerBytesSent[peerId]?.get() ?: 0,
                bytesReceived = peerBytesReceived[peerId]?.get() ?: 0,
                packetsSent = peerPacketsSent[peerId]?.get() ?: 0,
                packetsReceived = peerPacketsReceived[peerId]?.get() ?: 0,
                disconnectCount = peerDisconnectCount[peerId]?.get() ?: 0
            )
        }
        return NetworkStatsSnapshot(
            uptimeMs = System.currentTimeMillis() - startTimeMs,
            batteryLevel = batteryLevel,
            batteryTemperature = batteryTemperature,
            totalBytesSent = totalBytesSent.get(),
            totalBytesReceived = totalBytesReceived.get(),
            totalPacketsSent = totalPacketsSent.get(),
            totalPacketsReceived = totalPacketsReceived.get(),
            totalPacketsForwarded = totalPacketsForwarded.get(),
            totalPacketsDropped = totalPacketsDropped.get(),
            totalConnectionsEstablished = totalConnectionsEstablished.get(),
            totalConnectionsLost = totalConnectionsLost.get(),
            currentNeighborCount = currentNeighborCount,
            currentRouteCount = currentRouteCount,
            avgRttMs = avgRtt,
            storeForwardQueued = storeForwardQueued.get(),
            storeForwardDelivered = storeForwardDelivered.get(),
            peerStats = peerSnapshots
        )
    }

    fun reset() {
        totalBytesSent.set(0)
        totalBytesReceived.set(0)
        totalPacketsSent.set(0)
        totalPacketsReceived.set(0)
        totalPacketsForwarded.set(0)
        totalPacketsDropped.set(0)
        totalConnectionsEstablished.set(0)
        totalConnectionsLost.set(0)
        storeForwardQueued.set(0)
        storeForwardDelivered.set(0)
        peerRtt.clear()
        peerConnectedSince.clear()
        peerBytesSent.clear()
        peerBytesReceived.clear()
        peerPacketsSent.clear()
        peerPacketsReceived.clear()
        peerDisconnectCount.clear()
    }
}
