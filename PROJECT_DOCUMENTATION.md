# ResilientP2PTestbed - Comprehensive Developer Reference

**Project**: Samizdat (ResilientP2PTestbed)
**Purpose**: Infrastructure-less Mesh Networking for Android
**Core Stack**: Kotlin, Jetpack Compose, Room, Google Nearby Connections API

This document serves as the **definitive technical bible** for the codebase. It details the project structure, architecture, and provides a file-by-file, function-by-function deep dive into the implementation.

---

## 1. Directory Structure

### `root`
*   **`app/`**: The primary Android module.
*   **`documents/`**: UML diagrams and PDFs.
*   **`gradle/`**: Build system wrapper.
*   **`scripts/`**: Development utilities (Python).
*   **`PROJECT_DOCUMENTATION.md`**: This file.

### `app/src/main`
*   **`java/com/fyp/resilientp2p/`**: Source code root.
*   **`assets/`**: Unused in current build (legacy).
*   **`res/`**:
    *   `drawable/`: Icons (Vector Drawables).
    *   `values/`: Strings (`strings.xml`) and legacy Style definitions.
*   **`AndroidManifest.xml`**: Defines Permissions, `P2PService`, and `MainActivity`.

---

## 2. Source Code Deep Dive (Depth-First Search)

We traverse `com.fyp.resilientp2p` and its sub-packages.

### 2.1 Package: `com.fyp.resilientp2p` (Root)

#### `P2PApplication.kt`
The application class, responsible for global state initialization.

*   **Class**: `P2PApplication : Application`
*   **Variables**:
    *   `database`: `AppDatabase` (Lazy initialized Room DB).
    *   `p2pManager`: `P2PManager` (Singleton).
    *   `heartbeatManager`: `HeartbeatManager` (Singleton).
*   **Functions**:
    *   `onCreate()`:
        *   Initializes the `AppDatabase`.
        *   Initializes `P2PManager` with the Application Context.
        *   Initializes `HeartbeatManager` passing the `P2PManager` instance (Dependency Injection).
*   **Important Notes**:
    *   This acts as a manual Dependency Injection container. The Managers are singletons that live as long as the OS process.

#### `MainActivity.kt`
The Composition Root and UI Host.

*   **Class**: `MainActivity : AppCompatActivity`
*   **Key Functions**:
    *   `onCreate()`:
        *   Retrieves Managers from `P2PApplication`.
        *   Sets content to `ResilientP2PApp` (Compose entry).
        *   Calls `getRequiredPermissions()` and requests them.
        *   Calls `startAndBindService()` to promote process priority.
    *   `getRequiredPermissions()`:
        *   **Android 13+ (Tiramisu)**: Requests `UWB` (optional), `NEARBY_WIFI_DEVICES`.
        *   **Android 12 (S)**: Requests `BLUETOOTH_SCAN/ADVERTISE/CONNECT`.
        *   **Legacy**: Requests `ACCESS_FINE_LOCATION`.
        *   *Logic*: Adapts to fragmentation in Android's Bluetooth permission model.
    *   `startAndBindService()`:
        *   Starts `P2PService` (Foreground) and binds `MainActivity` to it.
        *   *Why?* To ensure the `P2PManager` inside the specific Service scope (or App scope) stays alive.
    *   `gracefulShutdown()`:
        *   Called when user selects "Exit".
        *   Stops Heartbeat, Stops P2PManager (`stopAll`), Unbinds Service, Stops Service.
        *   *Critical*: Without this, the background service would keep the Bluetooth radio hot indefinitely.
    *   `exportLogs()`:
        *   Dumps the SQLite `log_entries` table to a CSV file in `ExternalFilesDir`.
        *   Used for post-test verification of mesh performance (latency/RSSI).

---

### 2.2 Package: `com.fyp.resilientp2p.audio`

#### `AudioBuffer.kt` / `AudioPlayer.kt` / `AudioRecorder.kt`
Legacy/Refactored classes.
*   *Note*: The active implementation uses `VoiceManager`. These files contain earlier experiments with `AudioTrack` buffer management and may be deprecated or used as internal helpers for `VoiceManager`.

---

### 2.3 Package: `com.fyp.resilientp2p.data`

#### `Packet.kt`
The core data structure for the custom protocol.

*   **Data Class**: `Packet`
*   **Fields**:
    *   `id`: `String` (UUID). Unique ID for deduplication.
    *   `type`: `PacketType` (Enum: `DATA`, `PING`, `PONG`, `IDENTITY`).
    *   `sourceId`: `String` (Original Sender Name).
    *   `destId`: `String` ("BROADCAST" or Specific Device Name).
    *   `payload`: `ByteArray` (Actual content).
    *   `ttl`: `Int` (Time-To-Live). Defaults to 3.
    *   `timestamp`: `Long`.
*   **Functions**:
    *   `toBytes()`: Serializes the object to JSON -> ByteArray.
    *   `fromBytes()`: Deserializes.
*   **Nuance**: Using JSON is inefficient for bandwidth but excellent for debugging. Production version should switch to Protobuf.

#### `PacketEntity.kt`
Room Entity for storing packets (if needed).
*   Not strictly used for transient transport, simplified for persistent logging.

#### `Neighbor.kt`
Visual model for the UI.
*   **Fields**: `endpointId` (Google Hash), `peerName` (User String), `rssi`, `lastSeen`.

#### `AppDatabase.kt`
Room Database definition.
*   **Entities**: `LogEntry`, `PacketEntity`.
*   **Version**: 1.

#### `LogEntry.kt`
*   **Entity**: `@Entity(tableName = "log_entries")`
*   **Fields**: `timestamp`, `message`, `level` (INFO/ERROR), `logType` (SYSTEM/CHAT), `rssi`, `latencyMs`.

#### `P2PState.kt`
The UI State holder.
*   **Fields**:
    *   `isAdvertising`, `isDiscovering`: Booleans.
    *   `connectedEndpoints`: List of Strings.
    *   `logs`: List of `LogEntry` (Transient buffer for UI).
    *   `knownPeers`: Map of peers found via neighbors.

---

### 2.4 Package: `com.fyp.resilientp2p.managers`

#### `P2PManager.kt` (The Brain)
**Lines**: ~800
**Dependencies**: `ConnectionsClient`, `CoroutineScope`, `VoiceManager`.

*   **Initialization**:
    *   `STRATEGY = Strategy.P2P_CLUSTER`: Allows M-to-N connections.
    *   `localUsername`: `Build.MODEL`.
*   **Connection Lifecycle (`connectionLifecycleCallback`)**:
    *   `onConnectionInitiated()`:
        *   **Self-Check**: Reject if name == `localUsername`.
        *   **Duplicate Logic**: If already connected to "User A", checks if the new connection is "Fresher". Uses ID comparison as tie-breaker. Prevents "Death Spirals".
    *   `onConnectionResult()`:
        *   Success: Adds to `neighbors` map. Sends `IDENTITY` packet.
        *   Failure: Logs code.
    *   `onDisconnected()`:
        *   Removes from `neighbors`.
        *   **Route Pruning**: Removes all routes in `routingTable` where `nextHop == disconnectedId`.
*   **Payload Handling (`payloadCallback`)**:
    *   `BYTES`: Parses via `Packet.fromBytes()`. Calls `handlePacket()`.
    *   `STREAM`: Hands `InputStream` to `VoiceManager.startPlaying()`.
*   **The Routing Engine (`handlePacket`)**:
    1.  **Deduplication**: Checks `messageCache`. If seen? Return.
    2.  **Route Learning**:
        *   Calculates `Score = Packet.TTL`.
        *   If `Score > routingScores[packet.sourceId]`:
            *   Update `routingTable` -> `packet.sourceId` via `sourceEndpointId`.
    3.  **Local Delivery**:
        *   If `destId == Me` or `BROADCAST`: Show in UI (`_payloadEvents.emit`).
    4.  **Forwarding**:
        *   If `destId != Me` and `TTL > 0`: Call `forwardPacket`.
*   **`forwardPacket(packet)`**:
    *   Decrements TTL.
    *   **Unicast**: If `destId` is in `routingTable` and the next hop is alive, send only to that hop.
    *   **Flooding**: If route unknown (or Broadcast), send to *all* neighbors EXCEPT the one it came from (Split Horizon).
    *   **8012 Handling**: Inside `onFailureListener`, if `e.message` contains "8012", effectively kill the link (`disconnectFromEndpoint`).
*   **Audio**:
    *   `startAudioStreaming()`: Calls `VoiceManager.startRecording()`, gets `ParcelFileDescriptor`, sends as `Payload.fromStream()`.

#### `HeartbeatManager.kt`
**Dependencies**: `P2PManager`.

*   **Loop**: `startHeartbeatLoop()` runs every 8 seconds (configurable).
*   **Action**:
    *   Iterates `p2pManager.getNeighborsSnapshot()`.
    *   Sends `PING` packet.
*   **Check**:
    *   Iterates `lastSeenMap`.
    *   If `CurrentTime - LastSeen > 30000ms`, assumes dead.
    *   Calls `p2pManager.disconnectFromEndpoint()`.
*   **Design Rationale**: Google's API usually detects drops, but in "Click-to-Connect" clusters, the socket can hang open indefinitely if the radio is jammed. This forces a cleanup.

#### `VoiceManager.kt`
*   **Functions**:
    *   `startRecording()`:
        *   Creates `ParcelFileDescriptor.createPipe()`.
        *   Launches a thread reading from `AudioRecord` and writing to `OutputStream`.
        *   Returns the `ParcelFileDescriptor` (Read side) to be handed to Nearby Connections.
    *   `startPlaying(inputStream)`:
        *   Starts a thread reading from `inputStream` and writing to `AudioTrack`.

---

### 2.5 Package: `com.fyp.resilientp2p.service`

#### `P2PService.kt`
Foreground Service encapsulation.
*   **Class**: `P2PService : Service`
*   **Manifest**: Registered with `android:foregroundServiceType="connectedDevice"`.
*   **`onStartCommand()`**:
    *   Creates a `NotificationChannel` ("Mesh Service").
    *   Builds a persistent `Notification` with content "Mesh Network Active".
    *   Calls `startForeground(1, notification)`.
*   **Binder**:
    *   `LocalBinder` helper class returns the service instance.
    *   Used by `MainActivity` to confirm the service is running, though logic is mostly static/singleton.

---

### 2.6 Package: `com.fyp.resilientp2p.transport`

#### `MessageCache.kt`
*   **Role**: Stores seen Packet IDs to prevent broadcast loops.
*   **Implementation**: `ConcurrentHashMap<String, Long>`.
*   **Cleanup**: (Optional) Could define a purge method to remove old IDs to save RAM.

---

### 2.7 Package: `com.fyp.resilientp2p.ui`

#### `P2PComposables.kt`
The Jetpack Compose UI definition.

*   **`ResilientP2PApp` Composable**:
    *   Setup `Scaffold`.
    *   Top Bar: "Resilient Mesh".
    *   Content: `DashboardContent`.
*   **`DashboardContent` Composable**:
    *   Layout: `Column`.
    *   **Top Half**: `RadarView`.
    *   **Bottom Half**: `ChatScreen`.
*   **`RadarView` Composable**:
    *   **Canvas**:
        *   `center = size / 2`.
        *   `drawCircle(Color.Blue)` for Me.
        *   **Neighbors**: Loop through `state.connectedEndpoints`.
        *   **Math**:
            *   `angle = (360 / count) * index`
            *   `x = center.x + radius * cos(rad(angle))`
            *   `y = center.y + radius * sin(rad(angle))`
        *   Draws lines to them.
*   **`ChatScreen` Composable**:
    *   `LazyColumn`: Renders `state.logs` filtering for `LogType.CHAT`.
    *   `ChatBubble`: Custom box with rounded corners.
*   **`DebugPanel`**:
    *   (Hidden by default or overlay) Shows system logs (`LogType.SYSTEM`).

#### `theme/`
*   `Color.kt`: Purple/Teal standard definitions.
*   `Theme.kt`: `ResilientP2PTestbedTheme` wrapper.

---

### 2.8 Package: `com.fyp.resilientp2p.utils`
(Implicit) Helper functions found in other files, mostly standardized Kotlin extensions.

---

## 3. Protocol & Algorithms (For the Algorithm Team)

### 3.1 The Routing Protocol
We implement a **Distance Vector** variant.
*   **Metric**: Hop Count (derived from `TTL`).
*   **Table**: `HashMap<Dest, NextHop>`.
*   **Updates**: Flood-based.
    *   When a node receives a packet from Source `S` with `TTL=T`.
    *   It knows that `S` is `(InitialTTL - T)` hops away via the current sender `N`.
    *   It updates the table if this path is shorter than any known path.

### 3.2 Stability Heuristics
1.  **Duplicate Race**:
    *   Code: `P2PManager.kt:150-180`.
    *   If `A` connects to `B`, and `B` connects to `A` milliseconds later:
    *   Normally, Nearby Connections might allow two sockets.
    *   We enforce **Lexicographical Ordering** of Endpoint IDs to deterministically pick *one* connection and kill the other.
2.  **Radio Stall (8012)**:
    *   Code: `P2PManager.kt:forwardPacket`.
    *   If `sendPayload` fails, we don't just log it. We assume the link is dead.

---

**End of Technical Bible**
