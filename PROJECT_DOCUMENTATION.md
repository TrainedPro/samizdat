# ResilientP2PTestbed - Comprehensive Developer Reference

**Project**: Samizdat (ResilientP2PTestbed)
**Purpose**: Infrastructure-less Mesh Networking for Android
**Core Stack**: Kotlin, Jetpack Compose, Room, Google Nearby Connections API

This document serves as the **definitive technical bible** for the codebase. It details the project structure, architecture, and provides a file-by-file, function-by-function deep dive into the implementation.

---

## 1. Directory Structure

### `root`

- **`app/`**: The primary Android module.
- **`documents/`**: UML diagrams and PDFs.
- **`gradle/`**: Build system wrapper.
- **`scripts/`**: Development utilities (Python).
- **`PROJECT_DOCUMENTATION.md`**: This file.

### `app/src/main`

- **`java/com/fyp/resilientp2p/`**: Source code root.
- **`assets/`**: Unused in current build (legacy).
- **`res/`**:
  - `drawable/`: Icons (Vector Drawables).
  - `values/`: Strings (`strings.xml`) and legacy Style definitions.
- **`AndroidManifest.xml`**: Defines Permissions, `P2PService`, and `MainActivity`.

---

## 2. Source Code Deep Dive (Depth-First Search)

We traverse `com.fyp.resilientp2p` and its sub-packages.

### 2.1 Package: `com.fyp.resilientp2p` (Root)

#### `P2PApplication.kt`

The application class, responsible for global state initialization.

- **Class**: `P2PApplication : Application`
- **Variables**:
  - `database`: `AppDatabase` (Lazy initialized Room DB).
  - `p2pManager`: `P2PManager` (Singleton).
  - `heartbeatManager`: `HeartbeatManager` (Singleton).
- **Functions**:
  - `onCreate()`:
    - Initializes the `AppDatabase`.
    - Initializes `P2PManager` with the Application Context.
    - Initializes `HeartbeatManager` passing the `P2PManager` instance (Dependency Injection).
- **Important Notes**:
  - This acts as a manual Dependency Injection container. The Managers are singletons that live as long as the OS process.

#### `MainActivity.kt`

The Composition Root and UI Host.

- **Class**: `MainActivity : AppCompatActivity`
- **Key Functions**:
  - `onCreate()`:
    - Retrieves Managers from `P2PApplication`.
    - Sets content to `ResilientP2PApp` (Compose entry).
    - Calls `getRequiredPermissions()` and requests them.
    - Calls `startAndBindService()` to promote process priority.
  - `getRequiredPermissions()`:
    - **Android 13+ (Tiramisu)**: Requests `UWB` (optional), `NEARBY_WIFI_DEVICES`.
    - **Android 12 (S)**: Requests `BLUETOOTH_SCAN/ADVERTISE/CONNECT`.
    - **Legacy**: Requests `ACCESS_FINE_LOCATION`.
    - _Logic_: Adapts to fragmentation in Android's Bluetooth permission model.
  - `startAndBindService()`:
    - Starts `P2PService` (Foreground) and binds `MainActivity` to it.
    - _Why?_ To ensure the `P2PManager` inside the specific Service scope (or App scope) stays alive.
  - `gracefulShutdown()`:
    - Called when user selects "Exit".
    - Stops Heartbeat, Stops P2PManager (`stopAll`), Unbinds Service, Stops Service.
    - _Critical_: Without this, the background service would keep the Bluetooth radio hot indefinitely.
  - `exportLogs()`:
    - Dumps the SQLite `log_entries` table to a CSV file in `ExternalFilesDir`.
    - Used for post-test verification of mesh performance (latency/RSSI).

---

### 2.2 Package: `com.fyp.resilientp2p.audio`

#### `AudioBuffer.kt` / `AudioPlayer.kt` / `AudioRecorder.kt`

Legacy/Refactored classes.

- _Note_: The active implementation uses `VoiceManager`. These files contain earlier experiments with `AudioTrack` buffer management and may be deprecated or used as internal helpers for `VoiceManager`.

---

### 2.3 Package: `com.fyp.resilientp2p.data`

#### `Packet.kt`

The core data structure for the custom protocol.

- **Data Class**: `Packet`
- **Fields**:
  - `id`: `String` (UUID). Unique ID for deduplication.
  - `type`: `PacketType` (Enum: `DATA`, `PING`, `PONG`, `ACK`, `IDENTITY`, `GOSSIP`, `AUDIO_DATA`, `AUDIO_CONTROL`, `ROUTE_ANNOUNCE`, `STORE_FORWARD`).
  - `sourceId`: `String` (Original Sender Name).
  - `destId`: `String` ("BROADCAST" or Specific Device Name).
  - `payload`: `ByteArray` (Actual content).
  - `ttl`: `Int` (Time-To-Live). Defaults to 5.
  - `timestamp`: `Long`.
  - `trace`: `List<Hop>` (Route trace with peerId and RSSI per hop).
  - `sequenceNumber`: `Long`.
- **Functions**:
  - `toBytes()`: Serializes the object using binary `DataOutputStream` -> ByteArray.
  - `fromBytes()`: Deserializes with extensive size validation against DoS.
- **Nuance**: Uses efficient binary serialization via `DataOutputStream`/`DataInputStream`. Includes size limits (`MAX_PAYLOAD_SIZE=1MB`, `MAX_TOTAL_PACKET_SIZE=2MB`) to prevent OOM attacks.

#### `PacketEntity.kt`

Room Entity for store-and-forward packet persistence.

- **Entity**: `@Entity(tableName = "packet_queue")`
- **Fields**: `id`, `destId`, `type`, `payload`, `timestamp`, `expiration`, `sourceId`.
- **DAO**: `PacketDao` with `insertPacket`, `getPacketsForPeer`, `deletePacket`, `cleanupExpired`.

#### `Neighbor.kt`

Represents a directly connected peer (neighbor) in the mesh network.

- **Fields**: `peerId` (Google Endpoint Hash), `peerName` (User String), `quality`, `lastSeen` (AtomicLong for thread safety).

#### `AppDatabase.kt`

Room Database definition.

- **Entities**: `LogEntry`, `PacketEntity`.
- **Version**: 4.
- Uses `fallbackToDestructiveMigration()` for schema changes.

#### `LogEntry.kt`

- **Entity**: `@Entity(tableName = "logs")`
- **Fields**: `timestamp`, `message`, `level` (ERROR/WARN/METRIC/INFO/DEBUG/TRACE), `logType` (SYSTEM/CHAT), `peerId`, `rssi`, `latencyMs`, `payloadSizeBytes`.

#### `P2PState.kt`

The UI State holder.

- **Fields**:
  - `isAdvertising`, `isDiscovering`: Booleans.
  - `connectedEndpoints`: List of Strings.
  - `connectingEndpoints`: List of Strings (in-flight connections).
  - `logs`: List of `LogEntry` (Transient buffer for UI, capped at 100).
  - `knownPeers`: Map of peers found via routing (destName -> RouteInfo).
  - `isManualConnectionEnabled`, `isHybridMode`, `isLowPower`: Mode flags.
  - `logLevel`: Current UI log filter level.
  - `localDeviceName`: This node's identity.
  - `stats`: `NetworkStatsSnapshot` for live dashboard.

#### `NetworkStats.kt`

Thread-safe live network statistics tracker.

- **Class**: `NetworkStats` (mutable, AtomicLong-based)
- **Tracks**: bytes/packets sent/received/forwarded/dropped, connections established/lost, per-peer RTT, battery level/temp, store-forward queued/delivered.
- **Methods**: `recordPeerConnected/Disconnected()`, `recordPacketSent/Received()`, `recordRtt()`, `snapshot()`, `reset()`.
- **Class**: `NetworkStatsSnapshot` (immutable data class for UI consumption).
- **Class**: `PeerStatsSnapshot` (per-peer immutable stats).

#### `Converters.kt`

Room TypeConverters for `LogLevel` and `LogType` enums.

---

### 2.4 Package: `com.fyp.resilientp2p.managers`

#### `P2PManager.kt` (The Brain)

**Lines**: ~1674
**Dependencies**: `ConnectionsClient`, `CoroutineScope`, `VoiceManager`, `LogDao`, `PacketDao`, `NetworkStats`.

- **Initialization**:
  - `STRATEGY = Strategy.P2P_CLUSTER`: Allows M-to-N connections.
  - `MAX_CONNECTIONS = 4`: Hard cap on direct neighbors.
  - `localUsername`: `Build.MODEL-UUID.take(8)` (unique, persisted to SharedPreferences).
  - Registers `BroadcastReceiver` for battery monitoring.
- **Connection Lifecycle (`connectionLifecycleCallback`)**:
  - `onConnectionInitiated()`:
    - **Self-Check**: Reject if name == `localUsername`.
    - **Duplicate Logic**: If already connected to "User A", checks if the new connection is "Fresher". Uses ID comparison as tie-breaker. Prevents "Death Spirals".
  - `onConnectionResult()`:
    - Success: Adds to `neighbors` map. Sends `IDENTITY` packet.
    - Failure: Logs code.
  - `onDisconnected()`:
    - Removes from `neighbors`.
    - **Route Pruning**: Removes all routes in `routingTable` where `nextHop == disconnectedId`.
- **Payload Handling (`payloadCallback`)**:
  - `BYTES`: Parses via `Packet.fromBytes()`. Calls `handlePacket()`.
  - `STREAM`: Hands `InputStream` to `VoiceManager.startPlaying()`.
- **The Routing Engine (`handlePacket`)**:
  1.  **Deduplication**: Checks `messageCache.tryMarkSeen()`. If seen? Drop + increment `totalPacketsDropped`.
  2.  **Route Learning**:
      - Calculates `Score = min(TTL, 10) * 100 + RSSI/10`.
      - Uses synchronized `routingLock` for atomic update across routing maps.
      - Accepts new route if: score better, OR current next-hop dead, OR current route stale (>20s).
  3.  **Local Delivery**:
      - If `destId == Me` or `BROADCAST`: Show in UI (`_payloadEvents.emit`).
  4.  **Forwarding**:
      - If `destId != Me` and `TTL > 0`: Call `forwardPacket`.
      - If TTL expired: Log + increment `totalPacketsDropped`.
- **`forwardPacket(packet)`**:
  - Decrements TTL.
  - **Unicast**: If `destId` is in `routingTable` and the next hop is alive, send only to that hop.
  - **Flooding**: If route unknown (and not broadcast), flood to all neighbors. If no neighbors, queue for store-and-forward (DATA/STORE_FORWARD types only).
  - **Broadcast (Split Horizon)**: Send to all neighbors except the sender.
  - **8012 Handling**: Inside `onFailureListener`, if `e.message` contains "8012", effectively kill the link (`disconnectFromEndpoint`).
  - **Stats**: All send paths track `networkStats.recordPacketSent()` and `totalPacketsForwarded`.
- **Store-and-Forward Engine**:
  - In-memory `pendingMessages` map + Room DB `PacketEntity` persistence.
  - Background loop every 15s attempts delivery for queued packets.
  - TTL: 2 hours. Expired packets auto-cleaned.
  - `triggerStoreForwardDelivery()` fires on new neighbor connection.
- **Reconnection Logic**:
  - `scheduleReconnection()` queues disconnected peers with exponential backoff.
  - Max 5 attempts per peer. Clears stale routes to allow rediscovery.
- **Stats Dump**:
  - Every 30 seconds, emits `STATS_DUMP` log entry with full metrics snapshot.
- **Audio**:
  - `startAudioStreaming()`: Calls `VoiceManager.startRecording()`, gets `ParcelFileDescriptor`, sends as `Payload.fromStream()`.

#### `HeartbeatManager.kt`

**Dependencies**: `P2PManager`.

- **Loop**: `startHeartbeat()` runs every 5 seconds (configurable, adaptive based on bandwidth).
- **Action**:
  - Iterates `p2pManager.getNeighborsSnapshot()`.
  - Sends `PING` packet directly to each neighbor (bypasses mesh routing).
  - Handles `PONG` responses to compute RTT, feeds to `networkStats.recordRtt()`.
- **Zombie Check** (every 10s):
  - Threshold: `max(intervalMs * 6, 45_000ms)` — conservative to avoid false positives.
  - Double-checks live data before disconnecting.
  - Exempts endpoints with active transfers.
  - Calls `p2pManager.disconnectFromEndpoint()` for zombies.
- **Adaptive**: Bandwidth events adjust heartbeat interval (LOW→10s, HIGH→2s).
- **Design Rationale**: Google's API usually detects drops, but in "Click-to-Connect" clusters, the socket can hang open indefinitely if the radio is jammed. This forces a cleanup.

#### `VoiceManager.kt`

- **Functions**:
  - `startRecording()`:
    - Creates `ParcelFileDescriptor.createPipe()`.
    - Launches a thread reading from `AudioRecord` and writing to `OutputStream`.
    - Returns the `ParcelFileDescriptor` (Read side) to be handed to Nearby Connections.
  - `startPlaying(inputStream)`:
    - Starts a thread reading from `inputStream` and writing to `AudioTrack`.

---

### 2.5 Package: `com.fyp.resilientp2p.service`

#### `P2PService.kt`

Foreground Service encapsulation.

- **Class**: `P2PService : Service`
- **Manifest**: Registered with `android:foregroundServiceType="connectedDevice"`.
- **`onStartCommand()`**:
  - Creates a `NotificationChannel` ("Mesh Service").
  - Builds a persistent `Notification` with content "Mesh Network Active".
  - Calls `startForeground(1, notification)`.
- **Binder**:
  - `LocalBinder` helper class returns the service instance.
  - Used by `MainActivity` to confirm the service is running, though logic is mostly static/singleton.

---

### 2.6 Package: `com.fyp.resilientp2p.transport`

#### `MessageCache.kt`

- **Role**: Stores seen Packet IDs to prevent broadcast loops.
- **Implementation**: `ConcurrentHashMap<String, Long>` with `AtomicBoolean` cleanup lock.
- **Capacity**: 2000 entries (configurable).
- **TTL**: 10 minutes per entry.
- **Eviction**: Automatic — removes expired entries, then oldest 25% if still over capacity.
- **API**: `tryMarkSeen()` (atomic check-and-mark), `clear()`.

---

### 2.7 Package: `com.fyp.resilientp2p.ui`

#### `P2PComposables.kt`

The Jetpack Compose UI definition.

- **`ResilientP2PApp` Composable**:
  - Setup `Scaffold`.
  - Top Bar: "Resilient Mesh" with dropdown menu (Advanced Options, Exit App).
  - Content: `DeviceStatusCard`, `DashboardContent`.
- **`DashboardContent` Composable**:
  - Layout: `Column`.
  - **MeshContactsSection**: Shows neighbors and routable peers with hop counts.
  - **NetworkStatsSection**: Collapsible card with Battery/Traffic/Connections/Store-Forward/Per-Peer stats.
  - **LogsSection**: Real-time log viewer with color-coded levels and export/clear buttons.

#### `theme/`

- `Color.kt`: Professional/Technical palette (TechBlue, TechTeal, Status colors, Neutral backgrounds).
- `Theme.kt`: `ResilientP2PTestbedTheme` wrapper with light/dark mode support.

---

### 2.8 Package: `com.fyp.resilientp2p.utils`

(Implicit) Helper functions found in other files, mostly standardized Kotlin extensions.

---

## 3. Protocol & Algorithms (For the Algorithm Team)

### 3.1 The Routing Protocol

We implement a **Distance Vector** variant with TTL-based scoring.

- **Metric**: `Score = min(TTL, 10) * 100 + RSSI/10` (higher = better).
- **Default TTL**: 5 (supports up to 5-hop meshes).
- **Table**: `ConcurrentHashMap<Dest, NextHop>` with synchronized updates via `routingLock`.
- **Updates**: Combination of flood-based (IDENTITY packets) and explicit ROUTE_ANNOUNCE packets (1-hop, periodic).
  - When a node receives a packet from Source `S` with `TTL=T`:
  - It knows that `S` is `(DEFAULT_TTL - T + 1)` hops away via the current sender `N`.
  - It updates the table if: path is better, current next-hop is dead, or current route is stale (>20s).
- **Route Announcements**: Each node periodically broadcasts its routing scores to neighbors. Score degrades by 100 per hop to prefer shorter paths.
- **Pruning**: Stale routes older than 60s are pruned.

### 3.2 Stability Heuristics

1.  **Duplicate Race**:
    - Code: `P2PManager.kt:150-180`.
    - If `A` connects to `B`, and `B` connects to `A` milliseconds later:
    - Normally, Nearby Connections might allow two sockets.
    - We enforce **Lexicographical Ordering** of Endpoint IDs to deterministically pick _one_ connection and kill the other.
2.  **Radio Stall (8012)**:
    - Code: `P2PManager.kt:forwardPacket`.
    - If `sendPayload` fails, we don't just log it. We assume the link is dead.

---

**End of Technical Bible**
