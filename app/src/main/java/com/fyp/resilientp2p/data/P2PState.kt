package com.fyp.resilientp2p.data

data class RouteInfo(val nextHop: String, val hopCount: Int)

data class P2PState(
        val isAdvertising: Boolean = false,
        val isDiscovering: Boolean = false,
        val connectedEndpoints: List<String> = emptyList(),
        val connectingEndpoints: List<String> = emptyList(),
        val knownPeers: Map<String, RouteInfo> = emptyMap(),
        val isManualConnectionEnabled: Boolean = false,
        val authenticationDigits: String? = null,
        val authenticatingEndpointId: String? = null,
        val isMeshMaintenanceActive: Boolean = true,
        val isHybridMode: Boolean = false,
        val isLowPower: Boolean = false,
        val logs: List<LogEntry> = emptyList(),
        val logLevel: LogLevel = LogLevel.INFO,
        val localDeviceName: String = "Unknown"
)
