package com.fyp.resilientp2p.managers

import android.content.Context
import com.fyp.resilientp2p.data.CloudBridgeStatus
import com.fyp.resilientp2p.data.LogLevel
import com.fyp.resilientp2p.data.RemoteMeshInfo
import com.fyp.resilientp2p.transport.Packet
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CloudBridgeManager(
    private val context: Context,
    private val localDeviceName: String,
    private val onPacketReceived: (Packet) -> Unit,
    private val onStatusChanged: (CloudBridgeStatus) -> Unit,
    private val onRemoteMeshesUpdated: (Map<String, RemoteMeshInfo>) -> Unit,
    private val log: (String, LogLevel) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var mqttClient: Mqtt5Client? = null
    private var currentNetworkId: String = "local"
    private val brokerHost = "broker.hivemq.com"
    private val clientId = "Samizdat_${UUID.randomUUID().toString().take(8)}"

    // Track discovered remote meshes
    private val remoteMeshes = ConcurrentHashMap<String, RemoteMeshInfo>()
    // Track which mesh topics we're already subscribed to
    private val subscribedMeshTopics = ConcurrentHashMap.newKeySet<String>()

    // Discovery topic for mesh announcements
    private val DISCOVERY_TOPIC = "samizdat/discovery"
    // How often to send presence heartbeats (ms)
    private val HEARTBEAT_INTERVAL_MS = 15000L
    // How long before a remote mesh is considered offline (ms)
    private val MESH_TIMEOUT_MS = 60000L

    fun connect(networkId: String) {
        if (networkId == "local") {
            disconnect()
            return
        }
        
        currentNetworkId = networkId
        onStatusChanged(CloudBridgeStatus.CONNECTING)
        log("Connecting to Cloud Bridge for network: $networkId", LogLevel.INFO)

        scope.launch {
            try {
                mqttClient = Mqtt5Client.builder()
                    .identifier(clientId)
                    .serverHost(brokerHost)
                    .automaticReconnectWithDefaultConfig() // Enable automatic reconnects
                    .addConnectedListener { context ->
                        log("MQTT Client Connected to $brokerHost", LogLevel.INFO)
                        onStatusChanged(CloudBridgeStatus.CONNECTED)
                        // Subscribe on every (re)connect to ensure subscriptions are active
                        subscribeOwnMesh()
                        subscribeDiscovery()
                    }
                    .addDisconnectedListener { context ->
                        log("MQTT Client Disconnected: ${context.cause.message}", LogLevel.WARN)
                        // Delay status update slightly to avoid flickering during quick reconnects
                        scope.launch {
                            delay(2000)
                            if (mqttClient?.state?.isConnected == false) {
                                onStatusChanged(CloudBridgeStatus.DISCONNECTED)
                            }
                        }
                    }
                    .build()

                mqttClient?.toAsync()?.connect()?.whenComplete { _, throwable ->
                    if (throwable != null) {
                        log("Initial Cloud Bridge connection attempt failed: ${throwable.message}", LogLevel.ERROR)
                        onStatusChanged(CloudBridgeStatus.DISCONNECTED)
                    } else {
                        // Handled by ConnectedListener
                        startPresenceHeartbeat()
                        startMeshCleanup()
                    }
                }
            } catch (e: Exception) {
                log("Error initializing MQTT: ${e.message}", LogLevel.ERROR)
                onStatusChanged(CloudBridgeStatus.DISCONNECTED)
            }
        }
    }

    /**
     * Subscribe to our own mesh topic to receive packets from other gateways in same mesh
     */
    private fun subscribeOwnMesh() {
        if (mqttClient?.state?.isConnected == false) return
        
        val topic = "samizdat/mesh/$currentNetworkId"
        subscribedMeshTopics.add(currentNetworkId)
        mqttClient?.toAsync()?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback { publish -> handleIncomingMeshPacket(publish) }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    log("Failed to subscribe to $topic: ${throwable.message}", LogLevel.ERROR)
                } else {
                    log("Subscribed to own mesh topic: $topic", LogLevel.DEBUG)
                }
            }
    }

    /**
     * Subscribe to the global discovery topic to find other meshes
     */
    private fun subscribeDiscovery() {
        if (mqttClient?.state?.isConnected == false) return

        mqttClient?.toAsync()?.subscribeWith()
            ?.topicFilter(DISCOVERY_TOPIC)
            ?.callback { publish -> handleDiscoveryMessage(publish) }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    log("Failed to subscribe to discovery: ${throwable.message}", LogLevel.ERROR)
                } else {
                    log("Subscribed to mesh discovery topic", LogLevel.DEBUG)
                }
            }
    }

    /**
     * Subscribe to a remote mesh topic to receive cross-mesh packets
     */
    private fun subscribeToRemoteMesh(remoteNetworkId: String) {
        if (mqttClient?.state?.isConnected == false) return
        if (subscribedMeshTopics.contains(remoteNetworkId)) return
        
        val topic = "samizdat/mesh/$remoteNetworkId"
        subscribedMeshTopics.add(remoteNetworkId)
        
        mqttClient?.toAsync()?.subscribeWith()
            ?.topicFilter(topic)
            ?.callback { publish -> handleIncomingMeshPacket(publish) }
            ?.send()
            ?.whenComplete { _, throwable ->
                if (throwable != null) {
                    log("Failed to subscribe to remote mesh $remoteNetworkId: ${throwable.message}", LogLevel.ERROR)
                    subscribedMeshTopics.remove(remoteNetworkId)
                } else {
                    log("Subscribed to remote mesh: $remoteNetworkId", LogLevel.DEBUG)
                }
            }
    }

    /**
     * Handle a packet from any mesh topic (own or remote)
     */
    private fun handleIncomingMeshPacket(publish: Mqtt5Publish) {
        val payload = publish.payloadAsBytes
        try {
            val packet = Packet.fromBytes(payload)
            
            // Self-echo prevention: don't process packets we sent
            if (packet.sourceId == localDeviceName) return
            
            log("Received cloud packet from ${packet.sourceId} in mesh ${packet.networkId}", LogLevel.TRACE)
            onPacketReceived(packet)
        } catch (e: Exception) {
            log("Error parsing cloud packet: ${e.message}", LogLevel.TRACE)
        }
    }

    /**
     * Handle a discovery heartbeat from another mesh
     */
    private fun handleDiscoveryMessage(publish: Mqtt5Publish) {
        try {
            val json = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
            val parts = json.removeSurrounding("{", "}").split(",").associate { entry ->
                val keyValue = entry.split(":", limit = 2)
                if (keyValue.size == 2) {
                    keyValue[0].trim().removeSurrounding("\"") to keyValue[1].trim().removeSurrounding("\"")
                } else "" to ""
            }

            val meshId = parts["meshId"] ?: return
            val gatewayId = parts["gatewayId"] ?: return
            val peerCount = parts["peerCount"]?.toIntOrNull() ?: 0
            // Parse comma-separated peer names (empty string → empty list)
            val peerNames = parts["peers"]
                ?.split(",")
                ?.filter { it.isNotBlank() }
                ?: emptyList()

            // Don't track our own mesh
            if (meshId == currentNetworkId) return
            // Don't track our own heartbeats
            if (gatewayId == localDeviceName) return

            val meshInfo = RemoteMeshInfo(
                networkId = meshId,
                gatewayName = gatewayId,
                peerCount = peerCount,
                peerNames = peerNames,
                lastSeen = System.currentTimeMillis()
            )
            
            val isNew = !remoteMeshes.containsKey(meshId)
            remoteMeshes[meshId] = meshInfo
            onRemoteMeshesUpdated(HashMap(remoteMeshes))

            if (isNew) {
                log("Discovered remote mesh: $meshId (Gateway: $gatewayId, Peers: $peerCount)", LogLevel.INFO)
            }

            // Auto-subscribe to this mesh's packets for cross-mesh bridging
            subscribeToRemoteMesh(meshId)
        } catch (e: Exception) {
            log("Error parsing discovery message: ${e.message}", LogLevel.TRACE)
        }
    }

    /**
     * Periodically announce our mesh's presence on the discovery topic
     */
    private fun startPresenceHeartbeat() {
        scope.launch {
            while (mqttClient != null) {
                if (mqttClient?.state?.isConnected == true) {
                    try {
                        publishPresence(0, emptyList()) // P2PManager will override with real peer list
                    } catch (e: Exception) {
                        log("Error in presence heartbeat: ${e.message}", LogLevel.TRACE)
                    }
                }
                delay(HEARTBEAT_INTERVAL_MS)
            }
        }
    }

    /**
     * Publish mesh presence with current peer list.
     * peerNames = all device names visible in this local mesh (direct + routed)
     */
    fun publishPresence(peerCount: Int, peerNames: List<String> = emptyList()) {
        val client = mqttClient ?: return
        if (client.state.isConnected != true) return
        if (currentNetworkId == "local") return

        // Encode peer names as a CSV string (no commas allowed in device names)
        val peersField = peerNames.joinToString(",")
        val json = """{"meshId":"$currentNetworkId","gatewayId":"$localDeviceName","peerCount":$peerCount,"peers":"$peersField","timestamp":${System.currentTimeMillis()}}"""
        scope.launch {
            try {
                client.toAsync().publishWith()
                    .topic(DISCOVERY_TOPIC)
                    .payload(json.toByteArray(StandardCharsets.UTF_8))
                    .send()
            } catch (e: Exception) {
                // Ignore silent errors during high churn
            }
        }
    }

    /**
     * Periodically clean up stale remote meshes
     */
    private fun startMeshCleanup() {
        scope.launch {
            while (mqttClient != null) {
                delay(15000) // Check frequently
                val now = System.currentTimeMillis()
                val stale = remoteMeshes.filter { now - it.value.lastSeen > MESH_TIMEOUT_MS }
                if (stale.isNotEmpty()) {
                    stale.keys.forEach { meshId ->
                        remoteMeshes.remove(meshId)
                        subscribedMeshTopics.remove(meshId)
                        log("Remote mesh timed out: $meshId", LogLevel.DEBUG)
                    }
                    onRemoteMeshesUpdated(HashMap(remoteMeshes))
                }
            }
        }
    }

    /**
     * Publish a packet to our own mesh topic (for other gateways and remote meshes to receive)
     */
    fun publishPacket(packet: Packet) {
        val client = mqttClient ?: return
        if (client.state.isConnected != true) return
        
        // Ensure the packet carries our networkId
        val cloudPacket = packet.copy(networkId = currentNetworkId)
        val topic = "samizdat/mesh/$currentNetworkId"
        
        scope.launch {
            try {
                client.toAsync().publishWith()
                    .topic(topic)
                    .payload(cloudPacket.toBytes())
                    .send()
            } catch (e: Exception) {
                log("Failed to publish packet to cloud: ${e.message}", LogLevel.TRACE)
            }
        }
    }

    fun disconnect() {
        log("Disconnecting from Cloud Bridge", LogLevel.INFO)
        onStatusChanged(CloudBridgeStatus.DISCONNECTED)
        remoteMeshes.clear()
        subscribedMeshTopics.clear()
        onRemoteMeshesUpdated(emptyMap())
        mqttClient?.toAsync()?.disconnect()
        mqttClient = null
    }

    fun isConnected(): Boolean = mqttClient?.state?.isConnected == true
}
