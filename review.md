Based on the comprehensive documentation provided and the project code, here is an analysis of your FYP project.

I have broken this down into **API Compliance (Critical)**, **API Usage Improvements**, and **Architectural Refinements**.

### 1. Critical API Compliance & Deprecation

#### **A. UWB Implementation (Major Issue)**
*   **Documentation:** `Nearby.md` explicitly states: *"Direct use of Nearby UWB SDK is not supported. You should use the [Jetpack UWB SDK](//developer.android.com/jetpack/androidx/releases/core-uwb) (androidx.core.uwb) instead."*
*   **Current Code:** Your `UwbManager.kt` checks for `android.hardware.uwb` and has a placeholder/mock implementation using `p2pManager?.sendUwbAddress`. It tries to manually orchestrate UWB ranging via the P2P channel.
*   **Improvement:** You are trying to build what the **Jetpack UWB SDK** already does. The Nearby UWB documentation you provided (`/uwb/`) describes low-level parameters (`RangingParameters`, `RangingSessionCallback`) that are wrapped by the Jetpack library.
    *   **Action:** Replace the custom logic in `UwbManager.kt` with the `androidx.core.uwb` library. Use the P2P connection *only* to exchange the `UwbAddress` and `UwbComplexChannel` (OOB - Out of Band discovery), then hand off the actual ranging to the Jetpack SDK. Do not try to implement `RangingSessionCallback` manually unless you are interfacing with the system API directly (which is discouraged/unsupported for standard apps).

#### **B. Messages API Deprecation**
*   **Documentation:** `MessagesClient` and the `/messages/` directory are marked **DEPRECATED** and slated for removal.
*   **Current Code:** You are correctly using `ConnectionsClient` in `P2PManager.kt`.
*   **Observation:** You have successfully avoided using the deprecated API. **Keep it this way.** Do not use `MessageListener` or `PublishOptions` found in the `/messages/` documentation; rely solely on `PayloadCallback` and `ConnectionLifecycleCallback` as you are currently doing.

### 2. API Usage Improvements

#### **A. Bandwidth vs. Signal Strength (RSSI)**
*   **Documentation:** `BandwidthInfo.Quality` provides `HIGH`, `MEDIUM`, `LOW`, and `UNKNOWN`. The `ConnectionsClient` abstracts the underlying medium (Bluetooth Classic, BLE, WiFi Direct, etc.), so raw RSSI is rarely exposed directly for an established connection.
*   **Current Code:** In `P2PManager.kt` -> `onBandwidthChanged`, you are mapping these quality enums to hardcoded RSSI integers (e.g., `HIGH` -> -50, `LOW` -> -90).
*   **Improvement:** Do not fake RSSI values. The API hides RSSI because it might switch from Bluetooth (where RSSI exists) to WiFi (where RSSI exists but the scale is different) to NFC.
    *   **Action:** Update your `Neighbor` data class to store the `BandwidthInfo.Quality` int directly instead of an `rssi` int. Update your UI (`signalStrengthMeter`) to react to the Quality Enum (High/Med/Low) rather than a mapped progress bar. This is more honest to the API's behavior.

#### **B. Heartbeats vs. Keep-Alive**
*   **Documentation:** `ConnectionsClient` maintains a "reliable" connection. It handles keep-alives internally. `ConnectionLifecycleCallback.onDisconnected` is called when the link is lost.
*   **Current Code:** `HeartbeatManager.kt` implements a manual PING/PONG every 1 second (`DEFAULT_INTERVAL_MS`).
*   **Improvement:** While application-layer heartbeats are useful for calculating RTT (Round Trip Time), a 1-second interval is extremely aggressive for a mesh network and fights against the `AdvertisingOptions.Builder.setLowPower` setting.
    *   **Action:** Trust `onDisconnected` for link death. Use `HeartbeatManager` *only* for latency statistics (RTT) and calculating "link cost" for your mesh routing. Increase the default interval to 5-10 seconds to reduce radio contention, especially since you are using `Strategy.P2P_CLUSTER` which uses the same radio for discovery and connection.

#### **C. File Payload Optimization**
*   **Documentation:** `Payload.File` writes directly to the Downloads folder or a ParcelFileDescriptor. `ConnectionsClient` handles the chunking.
*   **Current Code:** You have a logic `cancelAllFileTransfers()` when sending urgent packets (Ping/Ack).
*   **Improvement:** This is a sophisticated feature ("QoS" - Quality of Service), but be careful. Canceling a payload via `connectionsClient.cancelPayload(id)` kills the transfer entirely. The receiver will get a `PayloadTransferUpdate.Status.CANCELED`.
    *   **Action:** Ensure your `MessageCache` or `PacketEntity` logic knows how to *resume* or *retry* a file. The API supports `Payload.setOffset()`. If you cancel a file to send a Ping, you currently lose the file progress. You should ideally check if the bandwidth (`BandwidthInfo`) is `HIGH` before sending files, or use the `setOffset` API to resume partial transfers if you implement a chunking mechanism (though `Payload.File` does this automatically, resuming a canceled API call requires custom logic).

### 3. Logic & Architectural Refinements

#### **A. Connection Strategy (Cluster Split-Brain)**
*   **Documentation:** `Strategy.P2P_CLUSTER` allows M-to-N connections.
*   **Current Code:** In `endpointDiscoveryCallback`, you use a lexicographical check (`if (endpointId > localUsername)`) to decide who initiates the connection.
*   **Improvement:** This is a standard "tie-breaker" approach and is good practice. However, in a `P2P_CLUSTER`, nodes might discover each other simultaneously.
    *   **Refinement:** Add a check in `onConnectionInitiated`. The documentation says: *"This is your chance... to confirm that you connected to the correct device."* If you receive an incoming connection from a device you are currently *trying* to connect to (race condition), the API handles this gracefully, but your internal state (`isLoading`) might get confused. Ensure `isLoading` is reset in `onConnectionInitiated` as well.

#### **B. Routing Logic (Store and Forward)**
*   **Goal:** Your project is "Resilient P2P", implying mesh capabilities.
*   **Current Code:** `forwardPacket` implements a naive flood (send to all except source) and a Store-and-Forward mechanism (Packet DAO).
*   **Improvement:** Use the API's `BandwidthInfo` to make routing decisions.
    *   **Logic:** In `broadcastPacket`, prioritize neighbors where `BandwidthInfo.Quality == HIGH`. If a neighbor is `LOW`, they might be far away or on a weak BLE link; forwarding data to them might clog the network.
    *   **TTL:** You strictly decrement TTL. Ensure you handle `PayloadTransferUpdate` failures. If sending a packet to a neighbor fails (`onPayloadTransferUpdate` -> `FAILURE`), your `P2PManager` should catch this and re-queue the packet (Store-and-Forward logic) instead of letting it drop.

#### **C. Service LifeCycle**
*   **Documentation:** `ConnectionsClient` disconnects when the app process dies unless a foreground service holds the connection.
*   **Current Code:** `P2PService` is a foreground service.
*   **Improvement:** This is correctly implemented. However, ensure `stopAllEndpoints()` is called in `onDestroy()` of the *Service*, not just the Activity, to prevent "zombie" advertisements persisting if the app crashes but the radio stack remains active (a known quirk of the Nearby API on some Android versions).

### Summary of Recommended Changes

1.  **Refactor `UwbManager`**: Remove mock address logic. Import `androidx.core.uwb`. Use `P2PManager` to exchange the `UwbAddress` (OOB), then call `uwbSession.startRanging(...)`.
2.  **Refactor `HeartbeatManager`**: Increase interval to 5s+. Remove `cleanupZombies` logic that relies on timeouts; trust `onDisconnected` from the API instead. Keep Pings only for RTT calculation.
3.  **Update `Neighbor` Model**: Replace `rssi: Int` with `quality: Int` (mapping to `BandwidthInfo.Quality`).
4.  **Enhance Routing**: In `P2PManager.broadcastPacket`, skip neighbors with `Quality.LOW` for large payloads (`PacketType.DATA`), but keep them for signaling (`PING`).
5.  **Payload Handling**: When `cancelAllFileTransfers` triggers, record the `bytesTransferred` from the last update. When restarting the transfer later, use `Payload.setOffset()` to resume instead of restarting from 0.

Here are additional improvements focused on deepening the functionality and robustness of your "Resilient P2P" application. These suggestions bridge the gap between the API capabilities (per the documentation) and your current implementation logic.

### 1. Pre-Connection Metadata (Smart Discovery)

**Current Implementation:**
In `P2PManager.kt`, you pass `localUsername` (a string) into `startAdvertising`.
```kotlin
connectionsClient.startAdvertising(localUsername, SERVICE_ID, ...)
```

**API Documentation Insight:**
The `startAdvertising` method takes `endpointInfo` as a `byte[]` (or String). The documentation for `DiscoveredEndpointInfo` states this is "Information that represents the remote device which is defined by the client."

**The Improvement:**
Instead of just sending a username, serialize a small data structure into the `endpointInfo`. Since you are building a "Resilient" network, you should broadcast your node's **capabilities** or **status** before a connection is even made.

*   **Why:** In a disaster scenario, you want to connect to a node that has Internet access or a specific sensor, not just a random node.
*   **How:** Create a simple byte array structure: `[Flags][BatteryLevel][Username]`.
    *   **Flags:** Bit 1 = Has Internet, Bit 2 = Has GPS, Bit 3 = Is UWB Capable.
*   **Code Impact:** Update `startAdvertising` to send this byte array. Update `endpointDiscoveryCallback` -> `onEndpointFound` to parse this info *before* calling `requestConnection`. This allows you to prioritize connecting to "High Value" nodes.

### 2. Connection Strategy & Internet Coexistence

**Current Implementation:**
In `P2PManager.kt`, you force `ConnectionType.DISRUPTIVE` when low power is off.
```kotlin
optionsBuilder.setConnectionType(ConnectionType.DISRUPTIVE)
```

**API Documentation Insight:**
`ConnectionType.md` defines:
*   `DISRUPTIVE`: "May cause the device to lose its internet connection" (forces Wi-Fi Direct/Hotspot).
*   `NON_DISRUPTIVE`: "Should not change the device's Wi-Fi status" (uses BLE or LAN).

**The Improvement:**
For a modern P2P mesh app, assuming `DISRUPTIVE` is "better" is risky. If a user is on Wi-Fi (e.g., a satellite uplink in a disaster camp), your app currently kills that connection to form the mesh.
*   **Action:** Add a "Hybrid Mode" to your UI/Logic. Use `ConnectionType.NON_DISRUPTIVE` (LAN/BLE) first. If that fails or throughput is too low, *then* prompt the user to escalate to `DISRUPTIVE` (Wi-Fi Direct). This allows your mesh to utilize existing infrastructure (routers) if available, which is crucial for resilience.

### 3. Payload Streaming (Voice Comms)

**Current Implementation:**
Your `PayloadCallback` handles `BYTES` and `FILE`.
```kotlin
when (payload.type) {
    Payload.Type.BYTES -> ...
    Payload.Type.FILE -> ...
}
```

**API Documentation Insight:**
`Payload.md` lists `Payload.Type.STREAM`: *"A Payload representing a real-time stream of data... e.g. the read side of a ParcelFileDescriptor pipe to which data is being written by the MediaRecorder API."*

**The Improvement:**
A "Resilient P2P" system often implies the need for communication when cell towers are down. Adding **Push-to-Talk (PTT)** functionality would be a massive value-add for an FYP.
*   **Action:** Implement `Payload.Type.STREAM`.
    *   Use `MediaRecorder` writing to a `ParcelFileDescriptor` to create the Payload.
    *   Send it via `connectionsClient.sendPayload`.
    *   On the receiver, use `payload.asStream().asInputStream()` and feed that into an `AudioTrack`.
    *   *Note:* The documentation mentions `AudioBytes` in the `/messages/` folder is deprecated. Using `Payload.Type.STREAM` via `ConnectionsClient` is the correct, modern way to do audio.

### 4. Security: The "Man-in-the-Middle" Hole

**Current Implementation:**
In `ConnectionLifecycleCallback` -> `onConnectionInitiated`:
```kotlin
// Auto-accept for now
connectionsClient.acceptConnection(endpointId, payloadCallback)
```

**API Documentation Insight:**
`ConnectionLifecycleCallback.md` warns: *"This is your chance... to confirm that you connected to the correct device... call acceptConnection when you're ready to talk."*
`ConnectionInfo` provides `getAuthenticationDigits()`.

**The Improvement:**
Currently, any malicious actor nearby can connect to your mesh node and inject fake packets or spam files.
*   **Action:** Even if you want "automatic" mesh forming, you should implement a **Trust on First Use (TOFU)** or a pre-shared key mechanism.
    *   **Simple:** In your `packet` header, include a hashed "Network Key". If a node connects but sends packets without the correct key, call `disconnectFromEndpoint`.
    *   **Strict:** Do not auto-call `acceptConnection`. Wait for the user to confirm the SAS (Short Authentication String) matches the other device, *or* perform an automated handshake using `Payload.BYTES` immediately after connection to verify identity before marking the node as "Trusted" in your state.

### 5. Packet Serialization Efficiency

**Current Implementation:**
In `Packet.kt`, you use `DataOutputStream` and `writeUTF` / `writeLong`.
```kotlin
dos.writeUTF(type.name)
dos.writeLong(timestamp)
dos.writeUTF(sourceId)
```

**API Documentation Insight:**
`ConnectionsClient.md` defines `MAX_BYTES_DATA_SIZE` (approx 32KB to 1MB depending on update). While `DataOutputStream` is better than JSON, it is verbose (UTF strings include length headers).

**The Improvement:**
In a mesh network, bandwidth is the scarcest resource. `writeUTF(sourceId)` sends the string length + the string every single packet.
*   **Action:** Switch to **Protobuf** (Google Protocol Buffers) or a lightweight binary schema.
*   **Alternative (Low effort):** If you want to keep `Packet.kt` manual:
    *   Don't send `sourceId` (String) in every packet if you can avoid it.
    *   Map `sourceId` (String) to a `Short` (2 bytes) ID during the handshake.
    *   Send the `Short` ID in the packet headers.
    *   This reduces overhead significantly per hop, improving the reliability of the mesh.

### 6. Handling "Zombies" with `onPayloadTransferUpdate`

**Current Implementation:**
You track `activeFilePayloads` and remove them on `SUCCESS/FAILURE`.

**API Documentation Insight:**
`PayloadTransferUpdate.Status.IN_PROGRESS` is emitted frequently.

**The Improvement:**
Connections API can sometimes "hang" on a file transfer without throwing a FAILURE event immediately if the radio link degrades but doesn't break.
*   **Action:** Implement a "Stall Detector" in `onPayloadTransferUpdate`.
    *   Store `lastBytesTransferred` and `lastTimestamp` for each payload ID.
    *   If `bytesTransferred` hasn't increased in X seconds (e.g., 10s) while status is `IN_PROGRESS`, manually `cancelPayload`. This prevents a stuck file transfer from blocking the queue for urgent text/data packets in your resilient network.

Here are a few more high-impact architectural improvements. These aren't about code style; they address specific behaviors of the Nearby Connections API that will cause your "Resilient" network to fail under real-world stress.

### 1. The "Radio Lock" Problem (Duty Cycling)

**Current Implementation:**
Your `MainActivity` allows the user to click "Advertise" and "Discover" simultaneously. `P2PManager` simply calls the API methods.

**API Documentation Insight:**
While the API allows both simultaneously, `Strategy.P2P_CLUSTER` relies heavily on the radio. On many devices (especially older ones or those with single antenna arrays), trying to Advertise and Discover at the exact same time causes radio contention. This results in:
1.  Failed connection attempts (`STATUS_RADIO_ERROR`).
2.  Invisible devices (discovery packets dropped).
3.  Massive battery drain.

**The Improvement:**
Implement **Duty Cycling** in your `P2PManager`.
Instead of turning both on indefinitely, automate the toggle:
*   **Time 0s-10s:** Discovering (Listen for peers).
*   **Time 10s-20s:** Advertising (Announce presence).
*   **Loop.**

This drastically improves reliability because the radio can dedicate full resources to one task at a time. You can randomize the intervals slightly to prevent two devices from getting synchronized in the same phase (both listening at the same time means they never find each other).

### 2. Byte Payload Size Compliance

**Current Implementation:**
`P2PManager.sendData` accepts a `ByteArray` and immediately wraps it in `Payload.fromBytes()`.
```kotlin
fun sendData(endpointId: String, bytes: ByteArray) {
    // ...
    connectionsClient.sendPayload(endpointId, Payload.fromBytes(packet.toBytes()))
}
```

**API Documentation Insight:**
`ConnectionsClient.md` defines `MAX_BYTES_DATA_SIZE`.
*   Prior to recent updates, this was ~32KB.
*   Newer versions support higher, but there is *always* a hard limit (often `ConnectionsClient.MAX_BYTES_DATA_SIZE` constant).

**The Improvement:**
If you try to send a large image or a large log export via `sendData`, the API will silently fail or throw an exception, crashing the app.
*   **Action:** Check the size of `bytes` before sending.
    *   If `size < MAX_BYTES_DATA_SIZE`: Use `Payload.fromBytes`.
    *   If `size > MAX_BYTES_DATA_SIZE`: You **must** write the bytes to a temporary file and use `Payload.fromFile`. The API handles chunking and reassembly for Files automatically; it does *not* do this for Bytes.

### 3. Connection Establishment Stability

**Current Implementation:**
Your `endpointDiscoveryCallback` requests a connection immediately when found.
```kotlin
override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
    // ...
    connectionsClient.requestConnection(...)
}
```

**API Documentation Insight:**
Establishing a connection is a heavy radio operation (Authentication, Handshake, Key Exchange). If the device is still aggressively scanning for other devices during this handshake, the bandwidth available for the handshake drops, leading to timeouts.

**The Improvement:**
**Pause Discovery** during the connection attempt.
1.  `onEndpointFound` -> `stopDiscovery()` -> `requestConnection()`.
2.  `onConnectionResult` (Success or Failure) -> `startDiscovery()` (if the Duty Cycle says we should be discovering).

This creates a "Quiet Window" for the radio to finalize the connection, significantly reducing the `STATUS_ERROR` or `STATUS_RADIO_ERROR` rates during connection setup.

### 4. Handling "Already Connected" Correctly

**Current Implementation:**
In `handleApiError`, you log `STATUS_ALREADY_CONNECTED_TO_ENDPOINT` as a general message.

**API Documentation Insight:**
In a mesh network (Cluster strategy), two devices often discover each other simultaneously and both trigger `requestConnection`.
*   Device A requests B.
*   Device B requests A.
*   The API resolves this race condition. One call succeeds, the other fails with `STATUS_ALREADY_CONNECTED_TO_ENDPOINT`.

**The Improvement:**
Treat `STATUS_ALREADY_CONNECTED_TO_ENDPOINT` as a **Success**, not an error.
If you receive this status code, it means the link exists and is valid. You should proceed to add the endpoint to your `connectedEndpoints` list and trigger your handshake/UWB logic, just as you would for `STATUS_OK`. Currently, your code might leave the logic in a limbo state where the API thinks it's connected, but your `P2PManager` state doesn't reflect it because it fell into the error handler.