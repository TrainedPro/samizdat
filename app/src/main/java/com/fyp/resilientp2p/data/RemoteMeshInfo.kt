package com.fyp.resilientp2p.data

data class RemoteMeshInfo(
        val networkId: String,
        val gatewayName: String,
        val peerCount: Int,
        val peerNames: List<String> = emptyList(),   // individual device names in this remote mesh
        val lastSeen: Long = System.currentTimeMillis()
)

enum class CloudBridgeStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}
