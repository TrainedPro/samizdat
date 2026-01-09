package com.fyp.resilientp2p.data

import java.util.concurrent.atomic.AtomicLong

/**
 * Represents a directly connected peer (neighbor) in the mesh network.
 * Uses AtomicLong for lastSeen to prevent race conditions.
 */
class Neighbor(
        val peerId: String,
        @Volatile var peerName: String = "",
        @Volatile var quality: Int = 0,
        initialLastSeen: Long = System.currentTimeMillis()
) {
    val lastSeen: AtomicLong = AtomicLong(initialLastSeen)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Neighbor) return false
        return peerId == other.peerId
    }

    override fun hashCode(): Int = peerId.hashCode()

    fun copy(
        peerId: String = this.peerId,
        peerName: String = this.peerName,
        quality: Int = this.quality,
        lastSeen: Long = this.lastSeen.get()
    ): Neighbor = Neighbor(peerId, peerName, quality, lastSeen)

    override fun toString(): String = "Neighbor(peerId='$peerId', peerName='$peerName', quality=$quality, lastSeen=${lastSeen.get()})"
}
