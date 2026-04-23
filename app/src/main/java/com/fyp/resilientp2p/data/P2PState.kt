package com.fyp.resilientp2p.data

/**
 * Routing table entry describing how to reach a known mesh peer.
 *
 * @property nextHop The direct neighbor through which this peer is reachable.
 * @property hopCount Number of intermediate relays (1 = direct neighbor).
 */
data class RouteInfo(val nextHop: String, val hopCount: Int)

/**
 * Immutable snapshot of the entire P2P node state, collected into the Compose UI
 * via `StateFlow<P2PState>`. Rebuilt on every state mutation inside [P2PManager].
 *
 * @property isAdvertising Whether Nearby Connections advertising is active.
 * @property isDiscovering Whether Nearby Connections discovery is active.
 * @property connectedEndpoints List of directly connected peer device names.
 * @property connectingEndpoints Peers currently in the connection handshake phase.
 * @property knownPeers Full mesh routing table (peer name → [RouteInfo]).
 * @property isManualConnectionEnabled Whether user-initiated connections are allowed.
 * @property authenticationDigits Token digits shown during connection authentication.
 * @property authenticatingEndpointId Endpoint currently awaiting user auth confirmation.
 * @property isMeshMaintenanceActive Whether background route maintenance is running.
 * @property isHybridMode Whether WiFi+BLE dual-transport mode is active.
 * @property isLowPower Whether low-power mode (reduced heartbeat, no audio) is on.
 * @property logs Recent log entries for the scrolling log view.
 * @property logLevel Current UI log filter level.
 * @property localDeviceName This device's advertised name on the mesh.
 * @property stats Current [NetworkStatsSnapshot] for the dashboard.
 * @property isGateway Whether this device is advertising as an internet gateway.
 * @property hasInternet Whether this device currently has validated internet.
 * @property gatewayEnabled Whether the user has enabled gateway mode (can be toggled off).
 * @property internetPeers Internet-discovered peers reachable through cloud relay.
 * @property emergencyCount Total emergency broadcasts received this session.
 *
 * @see com.fyp.resilientp2p.managers.P2PManager
 */
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
        val localDeviceName: String = "Unknown",
        val stats: NetworkStatsSnapshot = NetworkStatsSnapshot(),
        val isGateway: Boolean = false,
        val hasInternet: Boolean = false,
        val gatewayEnabled: Boolean = true,
        val internetPeers: Set<String> = emptySet(),
        val emergencyCount: Int = 0,
        /** Capability score [0–100] for each online peer. Used to show cloud-prefer status in UI. */
        val peerCapabilityScores: Map<String, Int> = emptyMap(),
        /** This device's own capability score [0–100]. */
        val localCapabilityScore: Int = -1
)
