package com.fyp.resilientp2p.data

data class Neighbor(
        val peerId: String,
        var peerName: String = "",
        var quality: Int = 0,
        var lastSeen: Long = System.currentTimeMillis()
)
