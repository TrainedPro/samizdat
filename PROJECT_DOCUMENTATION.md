# ResilientP2PTestbed - Technical Reference Manual

**Project Name**: ResilientP2PTestbed (Samizdat)
**Platform**: Android (Kotlin / Jetpack Compose)
**Core API**: Google Nearby Connections (Strategies.P2P_CLUSTER)

---

## 1. Directory Structure

This section details the purpose of every top-level directory in the project repository.

### `app/`
The main Android application module.
*   `src/main/java`: Contains all Kotlin source code (`com.fyp.resilientp2p`).
*   `src/main/res`: Android resources (drawables, strings, layouts, etc.).
*   `src/main/AndroidManifest.xml`: The app manifest defining permissions, activities, and services.

### `documents/`
Stores project-related documentation, diagrams, and reference materials.
*   Contains UML Class diagrams, Sequence diagrams, and architecture PDFs.
*   Used for generating the Final Year Project report.

### `scripts/`
Helper Python scripts used during development for analysis and automation.
*   **`generate_class_diagram.py`**: A utility that scans the Kotlin source code to generate a PlantUML representation of the class structure.
*   **`generate_database_diagram.py`**: inspects Room database entities to visualize the schema.

### `gradle/`
Contains the Gradle Wrapper files (`gradle-wrapper.jar`, `gradle-wrapper.properties`).
*   Ensures the project builds with a consistent Gradle version regardless of the machine's local installation.

### `samples/`
Contains reference implementations or snippets (e.g., `walkie-talkie-sample`) used for researching specific features like audio streaming.

---

## 2. Technical Architecture Overview

The application is built on a **Clean Architecture** variant, heavily utilizing the **Manager Pattern** to encapsulate logic, separated from the UI.

### **Layers**
1.  **Presentation Layer**: Pure Jetpack Compose UI (`MainActivity`, `P2PComposables`). No Fragments or XML layouts are used for the main interface.
2.  **Domain/Logic Layer**: Singleton "Managers" (`P2PManager`, `HeartbeatManager`, `VoiceManager`) that handle business logic and state.
3.  **Data Layer**: Room Database and Data Classes definitions (`Packet`, `Neighbor`, `LogEntry`).
4.  **Infrastructure/Service Layer**: `P2PService` ensuring background execution.

### **State Management**
*   **Single Source of Truth**: The `P2PManager` holds the canonical `_state` (`MutableStateFlow<P2PState>`).
*   **Observation**: The UI observes this state flow using `collectAsStateWithLifecycle()`. Any change in the mesh (new neighbor, message received) updates this flow, triggering a UI recomposition.

---

## 3. File-by-File Deep Dive

This section provides a detailed explanation of every critical file in the codebase.

### 3.1 Root Package (`com.fyp.resilientp2p`)

#### `MainActivity.kt`
The application entry point.
*   **Permissions Handling**: Checks and requests runtime permissions required for Nearby Connections:
    *   `BLUETOOTH_SCAN`, `BLUETOOTH_ADVERTISE`, `BLUETOOTH_CONNECT` (Android 12+).
    *   `ACCESS_FINE_LOCATION` (Required for BLE discovery on all versions).
    *   `NEARBY_WIFI_DEVICES` (Android 13+).
*   **Service Binding**: Binds to `P2PService` immediately upon launch. This "promotes" the app process to a Foreground priority, preventing the OS from killing the mesh connection when the user minimizes the app.
*   **Graceful Shutdown**:
    *   Implements `gracefulShutdown()` which ensures radios are powered down properly.
    *   Unbinds the service and explicitly calls `stopService` to remove the persistent notification.
    *   Stops the `HeartbeatManager`.

#### `P2PApplication.kt`
The global `Application` subclass.
*   **Dependency Injection Root**: Manages the singleton instances of `P2PManager`, `HeartbeatManager`, and `AppDatabase`.
*   **Lifecycle**: Created before any Activity. Initializes the database and managers ensuring they exist for the entire app lifetime.

---

### 3.2 Managers Package (`com.fyp.resilientp2p.managers`)

#### `P2PManager.kt`
**Role**: The central controller of the mesh network. This is the largest and most complex file (approx 800 lines).

**Key Responsibilities**:
1.  **Nearby Connections Integration**:
    *   **Strategy**: Uses `Strategy.P2P_CLUSTER`. This enables M-to-N topology (a mesh), allowing a device to accept incoming connections while also connecting to others.
    *   **Advertising & Discovery**: Uniquely, this manager keeps *both* Advertising and Discovery active simultaneously. This "Always-On" approach allows the mesh to grow dynamically and heal partitions.
    *   **Callbacks**:
        *   `ConnectionLifecycleCallback`: Handles `onConnectionInitiated` (handshake), `onConnectionResult` (success/failure), and `onDisconnected`.
        *   `PayloadCallback`: Receives raw bytes (`Type.BYTES`) or audio streams (`Type.STREAM`).

2.  **Routing Logic (The "Protocol")**:
    *   Implements a custom **Distance Vector / Controlled Flooding** protocol.
    *   **`routingTable`**: A Map of `<DestinationID, NextHopID>`. Determines where to forward a packet.
    *   **`routingScores`**: Stores the "Score" (TTL) of a route. Higher TTL = Better Route (fewer hops).
    *   **Route Update**: When a packet arrives from Source `S` with TTL `T` via Neighbor `N`:
        *   Calculates `NewScore = T`.
        *   If `NewScore > CurrentScore(S)`, updates table: `Route(S) = N`.

3.  **Duplicate Connection Handling (Stability Fix)**:
    *   Addresses a race condition where two devices discover each other simultaneously.
    *   **Logic**: If `onConnectionInitiated` connects to an existing peer name, it checks stability.
        *   If the existing connection is "Stale" or "Ghost", it disconnects and accepts the new one.
        *   If the new connection ID > old connection ID (tie-breaker), it might swap them to prevent deadlock.

4.  **Error Handling (The "8012" Fix)**:
    *   Monitors `sendPayload` failures. If the Google API returns status `8012` (Endpoint IO Error), it indicates a radio stack stall.
    *   **Action**: Automatically calls `disconnectFromEndpoint(id)`, forcing a clean reset of the connection handle.

#### `HeartbeatManager.kt`
**Role**: Connectivity Watchdog.

**Logic**:
1.  **Periodic Ping**: Every `config.intervalMs` (default 8000ms), it iterates through all connected neighbors.
2.  **Ping Packet**: Sends a `PacketType.PING` containing a timestamp.
3.  **Response**: Expects a `PacketType.PONG` via `P2PManager`.
4.  **Zombie Detection**:
    *   Maintains a `lastSeenMap`.
    *   On each tick, checks: `if (now - lastSeen > 30,000ms) ==> Disconnect`.
    *   This removes peers that have physically walked away but for whom the OS hasn't yet triggered `onDisconnected`.

#### `VoiceManager.kt`
**Role**: Audio Subsystem Interface.

**Audio Pipeline**:
1.  **Capture**: Uses `AudioRecord` (16kHz, Mono, PCM 16-bit).
2.  **Stream Creation**: Writes raw audio bytes to a `ParcelFileDescriptor` pipe.
3.  **Transmission**: Hands the read-end of the pipe to `Nearby.sendPayload` as `Payload.Type.STREAM`.
4.  **Playback**:
    *   Receives `InputStream` from `P2PManager`.
    *   Buffering: Reads chunks into a buffer.
    *   Output: Writes to `AudioTrack` ensuring minimal latency.

---

### 3.3 Service Package (`com.fyp.resilientp2p.service`)

#### `P2PService.kt`
**Role**: Process Persistence.

*   **Foreground Service**: Required by Android to keep a process running when not visible.
*   **Notification**: displays a persistent status bar notification ("P2P Service is Running"), satisfying OS background execution requirements.
*   **Binder**: Provides a `LocalBinder` so `MainActivity` can communicate with it (though most communication happens via the singleton `P2PManager`).

---

### 3.4 Data Package (`com.fyp.resilientp2p.data`)

#### `Packet.kt`
**Role**: The "Wire Format" of the mesh protocol.
**Structure**:
```kotlin
data class Packet(
    val id: String,          // Unique UUID for deduplication
    val type: PacketType,    // DATA, PING, PONG, IDENTITY
    val sourceId: String,    // Author of the message
    val destId: String,      // Target (or "BROADCAST")
    val payload: ByteArray,  // The actual data (text, image bytes)
    val ttl: Int = 3,        // Time-To-Live (Hop Limit)
    val timestamp: Long      // Creation time
)
```
*   **Serialization**: Contains `toBytes()` and `fromBytes()` using JSON (Gson) or native serialization for converting the object to a `ByteArray` for transmission.

#### `Neighbor.kt`
Data class representing a direct peer.
*   Fields: `endpointId` (Google ID), `peerName` (User Device Name), `rssi`, `lastSeen`.

#### `AppDatabase.kt` / `LogDao.kt` / `LogEntry.kt`
**Role**: Logging & Debugging Persistence.
*   **Room Database**: Standard Android SQLite wrapper.
*   **Logs**: Stores debugging logs (`TRACE`, `DEBUG`, `INFO`) persistently so they can be exported to CSV (`exportLogs()` in `MainActivity`) for post-mortem analysis.

---

### 3.5 UI Package (`com.fyp.resilientp2p.ui`)

#### `P2PComposables.kt`
The entire visual interface.

*   **`RadarView`**:
    *   Uses a custom `Canvas` composable.
    *   Draws the central user node.
    *   Calculates `(x, y)` positions for neighbors using `cos/sin` functions to arrange them in a circle.
    *   Draws lines (`drawLine`) between connected nodes.
*   **`ChatScreen`**:
    *   A `LazyColumn` displaying a history of `LogEntry` items where `type == CHAT`.
    *   Differentiates between "Sent" (Align Right, Blue) and "Received" (Align Left, Gray).
*   **`DashboardContent`**:
    *   The main layout scaffolding (`Scaffold`).
    *   Houses the `RadarView` on top and `LiveLog` / `Chat` on bottom.

---

## 4. Operation & Nuances

### 4.1 "Always-On" Mesh Strategy
Standard usage of Nearby Connections usually involves one device advertising and another discovering, then stopping once connected.
**Our Approach**:
*   To form a mesh, *everyone* must be detectable and able to detect others.
*   We keep discovery running even after connection.
*   **Constraint**: This uses significant battery. The app includes a "Low Power Mode" toggle (in Advanced Settings) that duty-cycles this behavior.

### 4.2 Split Horizon & Loop Prevention
*   **Split Horizon**: When forwarding a packet, we explicitly exclude the neighbor we received it from. `filter { it != excludeEndpointId }`.
*   **Deduplication**: `P2PManager` maintains a `messageCache` of seen Packet IDs. If we have seen ID `X` before, we drop it. This effectively stops broadcast storms.

### 4.3 Self-Poisoning
A subtle bug identified during development involves "Self-Poisoning".
*   If Node A broadcasts "I am A", Node B receives it and may echo it back to A.
*   Node A must NOT add "Node A via Node B" to its routing table.
*   **Fix**: `if (packet.sourceId == localUsername) return`.

---

## 5. Known Limits

1.  **Hop Count**: Practical limit of ~3-4 hops due to radio latency and packet loss accumulation.
2.  **Bandwidth**: Each hop halves the effective throughput (Store-and-Forward delay). Voice is best used on 0-hop (Direct) or 1-hop connections.
3.  **Connection Count**: The API limits connections to approx 4-7. We enforce a soft limit of 4 to ensure stability.
4.  **Security**: Currently, packets are cleartext (JSON/Bytes). Encryption is a planned future upgrade.

---

**End of Technical Documentation**
