# ResilientP2PTestbed - Comprehensive Developer Reference

**Project**: Samizdat (ResilientP2PTestbed)  
**Purpose**: Infrastructure-less Mesh Networking for Android  
**Core Stack**: Kotlin 2.0.21, Jetpack Compose (Material 3), Room 2.8.4, Google Nearby Connections 19.3.0  
**Build**: AGP 8.13.1, compileSdk 36, minSdk 24, targetSdk 36, Java 17

This document serves as the **definitive technical bible** for the codebase. It details the project structure, architecture, and provides a file-by-file deep dive into the implementation.

---

## 1. Directory Structure

### `root`

- **`app/`**: The primary Android module.
- **`dashboard/`**: Web-based telemetry dashboard (HTML/JS/CSS).
- **`documents/`**: UML diagrams, PDFs, roadmaps.
- **`gradle/`**: Build system wrapper + version catalog.
- **`logs/`**: Collected CSV logs from test sessions.
- **`samples/`**: Reference samples from Google connectivity SDKs.
- **`scripts/`**: Development utilities (Python diagram generators).
- **`PROJECT_DOCUMENTATION.md`**: This file.
- **`DESIGN_DECISIONS.md`**: Architecture decision records.
- **`audit_report.md`**: Comprehensive codebase audit results.

### `app/src/main`

- **`java/com/fyp/resilientp2p/`**: Source code root.
  - **`audio/`**: Audio capture, playback, codec, and mesh audio streaming.
  - **`data/`**: Room entities, DAOs, data classes, and TypeConverters.
  - **`managers/`**: Core business logic (P2P, Voice, Emergency, File Sharing, etc.).
  - **`security/`**: Encryption, rate limiting, and blacklisting.
  - **`service/`**: Foreground service for background mesh operation.
  - **`testing/`**: Automated test runner and endurance testing.
  - **`transport/`**: Packet protocol, message cache, and routing data.
  - **`ui/`**: Jetpack Compose screens and theme.
- **`res/`**: Drawables, strings, themes.
- **`AndroidManifest.xml`**: Permissions, `P2PService`, and `MainActivity`.

### `app/src/test`

- **`java/com/fyp/resilientp2p/`**: JUnit 4 unit tests.
  - `PacketTest.kt` — Packet serialization/deserialization (20+ tests).
  - `MessageCacheTest.kt` — Dedup cache, eviction, concurrency (12+ tests).
  - `RoutingTest.kt` — Routing score, TTL, trace, forwarding logic (14+ tests).
  - `AudioCodecTest.kt` — AAC codec configuration and bandwidth math (9 tests).
  - `FileShareTest.kt` — Chunk arithmetic, SHA-256, resume logic (15+ tests).
  - `MeshAudioTest.kt` — AUDIO_DATA/CONTROL packet format, batching (14+ tests).

---

## 2. Source Code Deep Dive

### 2.1 Package: `com.fyp.resilientp2p` (Root)

#### `P2PApplication.kt` (~200 lines)

Application singleton and manual dependency injection container.

- **Class**: `P2PApplication : Application`
- **Owns**: All long-lived managers (P2PManager, HeartbeatManager, TelemetryManager, SecurityManager, etc.)
- **Initialization** (`onCreate`):
  - Room database (lazy)
  - P2PManager → HeartbeatManager → TestRunner → EnduranceTestRunner → TelemetryManager
  - InternetGatewayManager, EmergencyManager, SecurityManager, RateLimiter, PeerBlacklist
  - LocationEstimator, EncounterLogger, FileShareManager (with resume DAO)
  - Cross-wires all managers and sets up group message handler
  - Wires test/endurance callbacks to telemetry uploads

#### `MainActivity.kt`

The Composition Root and UI Host.

- Retrieves all managers from `P2PApplication`.
- Sets content to `ResilientP2PApp` (Compose entry point).
- Runtime permission handling (adaptive to Android 12/13+/legacy).
- Foreground service binding via `P2PService`.
- `gracefulShutdown()`: orderly stop of heartbeat + P2P + service.
- `exportLogs()`: CSV dump of Room `log_entries` table.
- `configChanges` in manifest prevents rotation from killing activity.

---

### 2.2 Package: `com.fyp.resilientp2p.audio`

#### `AudioBuffer.kt`

Abstract base for audio I/O buffers. 8 kHz mono 16-bit PCM (16 KB/s raw).

#### `AudioRecorder.kt` (~158 lines)

Captures audio from microphone via `AudioRecord`, writes raw PCM to a `ParcelFileDescriptor` pipe. Thread-priority elevated to `THREAD_PRIORITY_AUDIO`.

#### `AudioPlayer.kt` (~161 lines)

Reads from `InputStream`, plays via `AudioTrack`. Adaptive jitter buffer (100–500 ms) based on peer RTT measurement:

- RTT unknown → 200 ms default
- RTT < 50 ms → 100 ms minimum
- RTT > 300 ms → 500 ms cap
- Otherwise → 1.5× RTT

#### `AudioCodecManager.kt` (~230 lines)

AAC-LC audio codec via Android's `MediaCodec` API.

- **Parameters**: 8 kHz, 24 kbps, mono, 20 ms frame duration
- **Inner classes**: `AACEncoder` (PCM → AAC) and `AACDecoder` (AAC → PCM)
- **Bandwidth reduction**: ~16 KB/s (raw PCM) → ~3–4 KB/s (AAC-LC)
- **Frame size**: 160 samples / 320 bytes per 20 ms frame
- Thread-safe: NO (use from single thread only)

#### `MeshAudioManager.kt` (~320 lines)

**Multi-hop mesh audio streaming with AAC-LC compression.** Replaces the old direct-only `Payload.fromStream` approach.

- **Sender flow**: AudioRecorder → raw PCM → AACEncoder → batched AUDIO_DATA packets (5 frames / 100 ms per packet → 10 packets/sec)
- **Receiver flow**: AUDIO_DATA → AACDecoder → PipedOutputStream → AudioPlayer
- **Session protocol**: AUDIO_CONTROL packets (START/STOP) with 8-char session IDs
- **Packet format (AUDIO_DATA)**: `[sessionId:8B][seqNo:4B][aacData...]`
- **Mesh bandwidth**: ~5 KB/s per audio stream (vs 16 KB/s raw PCM)
- **Multi-hop**: Packets are regular mesh packets — routed via `handlePacket`/`forwardPacket`
- **Fallback**: If mesh audio fails, falls back to direct `Payload.fromStream` for single-hop

---

### 2.3 Package: `com.fyp.resilientp2p.data`

#### `Packet.kt` → moved to `transport/Packet.kt`

(See §2.7 Transport)

#### `P2PState.kt`

UI state holder: `isAdvertising`, `isDiscovering`, `connectedEndpoints`, `knownPeers`, mode flags, stats snapshot.

#### `NetworkStats.kt` / `NetworkStatsSnapshot.kt`

Thread-safe live statistics tracker (AtomicLong-based):

- Bytes/packets sent/received/forwarded/dropped
- Per-peer RTT, sent/received counts
- Battery level/temp/µAh tracking (via `ACTION_BATTERY_CHANGED`)
- Store-and-forward queue depth
- Connection established/lost counts

#### `LogEntry.kt`

Room entity with levels (ERROR/WARN/METRIC/INFO/DEBUG/TRACE), log types (SYSTEM/CHAT), per-entry peerId, RSSI, latency, payload size.

#### `ChatMessage.kt`

Room entity for chat persistence: TEXT, IMAGE, FILE, SYSTEM message types. Transfer progress tracking for files.

#### `SharedFile.kt`

Content-addressable file metadata: SHA-256 hash, chunk size, total chunks, download progress, local path.

#### `DownloadedChunk.kt`

**File resume persistence.** Composite primary key `(sha256, chunkIndex)`. Persists per-chunk download status to Room so interrupted transfers resume from where they left off instead of restarting.

#### `PacketEntity.kt`

Store-and-forward queue persistence: packets destined for offline peers.

#### `TelemetryEvent.kt`

Cloud telemetry: 9 event types including ENDURANCE_REPORT, ENDURANCE_SNAPSHOT.

#### `ChatGroup.kt` / `GroupMessage.kt`

Named group chat channels with message persistence.

#### `EncounterLog.kt`

DTN encounter records for sneakernet analytics.

#### `AppDatabase.kt` (v9)

Room database with 9 entities, `fallbackToDestructiveMigration`, 10 DAOs.

#### `Converters.kt`

TypeConverters for `LogLevel` and `LogType` enums.

---

### 2.4 Package: `com.fyp.resilientp2p.managers`

#### `P2PManager.kt` (~2170 lines) — The Brain

**Core mesh networking engine.** Manages discovery, connections, packet routing, store-and-forward, file/audio transfers.

- **Strategy**: `P2P_CLUSTER` (M-to-N, max 4 connections)
- **Identity**: `Build.MODEL-UUID.take(8)`, persisted to SharedPreferences
- **Connection lifecycle**:
  - Self-check, duplicate race resolution (lexicographic tie-breaker)
  - `connectionLifecycleCallback`: add neighbor → send IDENTITY (with ECDH public key)
  - On disconnect: route pruning, encounter logging, reconnection scheduling
- **Payload handling**:
  - `BYTES`: `Packet.fromBytes()` → `handlePacket()`
  - `STREAM`: → `VoiceManager.startPlaying()` (legacy direct audio)
  - `FILE`: → file write with metadata correlation
- **Routing engine** (`handlePacket`):
  1. Deduplication via `MessageCache` (skip for AUDIO_DATA)
  2. Route learning: `Score = min(TTL, 10) × 100 + RSSI/10` with synchronized updates
  3. Local delivery: if `destId == me` or `BROADCAST` → `processPacket`
  4. Forwarding: decrement TTL → unicast via routing table, flood if no route, cloud relay as last resort
  5. EMERGENCY: always process locally AND flood entire mesh
- **`processPacket`**: Handles all 18+ packet types (DATA, IDENTITY, PING/PONG, ROUTE_ANNOUNCE, FILE_META, EMERGENCY, GROUP_MESSAGE, FILE_ANNOUNCE/REQUEST/CHUNK, ENCOUNTER_LOG, LOCATION_PING, AUDIO_DATA, AUDIO_CONTROL)
- **Audio**: `startAudioStreaming()` uses MeshAudioManager (AAC-encoded multi-hop) with fallback to direct STREAM. `stopAudioStreaming()` cleans up both.
- **Store-and-forward**: In-memory + Room persistence, 15s retry loop, 2h TTL, cloud relay fallback
- **Reconnection**: Exponential backoff, max 5 attempts per peer
- **Stats dump**: Every 30s emits STATS_DUMP log entry
- **Internet gateway relay**: When no route and no neighbors, relay DATA via InternetGatewayManager → Firestore

#### `HeartbeatManager.kt`

Heartbeat-based liveness detection:

- PING every 5s (adaptive: 2s high-bandwidth, 10s low-bandwidth)
- Zombie detection: threshold `max(interval × 6, 45s)`
- Exempts endpoints with active transfers
- Feeds RTT to `networkStats` and `locationEstimator`

#### `VoiceManager.kt` (~103 lines)

Owns AudioRecorder + AudioPlayer lifecycle. Creates `ParcelFileDescriptor` pipes for Nearby Connections STREAM payloads. Used as fallback for single-hop audio.

#### `InternetGatewayManager.kt` (~333 lines)

Internet gateway detection and cloud relay:

- `NetworkCallback` monitors internet availability
- `relayToCloud()`: POST to Firestore REST API with rate limiting (60/hour)
- `pollRelayMessages()`: GET from Firestore every 30s, inject matching messages into local mesh
- Advertises `__GATEWAY__` in ROUTE_ANNOUNCE when internet is available
- Firebase config via `BuildConfig` from `local.properties`

#### `EmergencyManager.kt`

SOS beacon and emergency broadcast:

- `sendEmergencyBroadcast()`: Injects EMERGENCY packet into mesh (floods to all)
- UI alert on received emergency packets

#### `TelemetryManager.kt`

Cloud telemetry collection:

- WorkManager upload every 15 min
- Records: discovery, connection, test results, endurance reports/snapshots
- Firestore REST API upload

#### `FileShareManager.kt` (~328 lines)

Content-addressable distributed file sharing:

- **Protocol**: FILE_ANNOUNCE → FILE_REQUEST → FILE_CHUNK
- **Chunk size**: 32 KB default
- **Resume**: `DownloadedChunkDao` persists which chunks are received. On `requestFile()`, loads persisted chunks from Room and only requests missing ones.
- **Integrity**: SHA-256 content addressing
- **Random-access write**: `RandomAccessFile` for out-of-order chunk assembly

#### `LocationEstimator.kt`

RTT-based trilateration for 2D peer positioning.

#### `EncounterLogger.kt`

DTN encounter record broadcast for sneakernet analytics.

---

### 2.5 Package: `com.fyp.resilientp2p.security`

#### `SecurityManager.kt`

ECDH key exchange + AES-256-GCM end-to-end encryption + HMAC-SHA256 packet integrity.

#### `RateLimiter.kt`

Per-peer sliding-window rate limiter with configurable window and threshold.

#### `PeerBlacklist.kt`

Persistent peer blacklist with auto-ban on violation threshold. SharedPreferences-backed.

---

### 2.6 Package: `com.fyp.resilientp2p.service`

#### `P2PService.kt`

Foreground service with `connectedDevice` type. Persistent notification "Mesh Network Active". Ensures the process stays alive while mesh is operational.

---

### 2.7 Package: `com.fyp.resilientp2p.transport`

#### `Packet.kt` (~249 lines)

The fundamental mesh protocol data unit with 18 packet types:

```
PING, PONG, ACK, DATA, GOSSIP, IDENTITY,
AUDIO_DATA, AUDIO_CONTROL,
ROUTE_ANNOUNCE, STORE_FORWARD, FILE_META, EMERGENCY,
GROUP_MESSAGE, FILE_ANNOUNCE, FILE_REQUEST, FILE_CHUNK,
ENCOUNTER_LOG, LOCATION_PING
```

Binary serialization via `DataOutputStream`/`DataInputStream`. Includes:

- UUID packet ID (deduplication)
- TTL (0–255, default 5)
- Route trace (`List<Hop>` with peerId + RSSI)
- Sequence number for ordering
- Size limits: 1 MB payload, 2 MB total, 256-byte strings

#### `MessageCache.kt` (~58 lines)

Thread-safe packet dedup cache:

- `ConcurrentHashMap<String, Long>` with `AtomicBoolean` cleanup lock
- Capacity: configurable (default 1000 in production, 2000 in P2PManager)
- TTL: 10 minutes per entry
- Eviction: removes expired + oldest 25% if still over capacity
- AUDIO_DATA packets bypass the cache (handled by sequence numbers)

---

### 2.8 Package: `com.fyp.resilientp2p.testing`

#### `TestRunner.kt`

15 automated functional tests covering:

- Connection/discovery, packet routing, broadcast delivery
- Latency measurement, file transfer, emergency broadcast
- Store-and-forward, heartbeat, security, group chat
- Results reported as JSON to TelemetryManager

#### `EnduranceTestRunner.kt` (~700 lines)

Long-running soak test with advanced metrics:

- Configurable duration (default 30 min)
- Tracks: RTT, jitter, packet loss, throughput, connection churn
- Battery consumption (µAh/mAh via BatteryManager)
- Store-and-forward effectiveness, per-peer RTT
- CSV (24 columns) and JSON export
- Cloud upload via TelemetryManager callbacks

#### `TestModeScreen.kt`

Two-tab UI: Functional Tests + Endurance Tests. Full report card with 5 metric categories.

---

### 2.9 Package: `com.fyp.resilientp2p.ui`

#### `P2PComposables.kt` (~1437 lines)

Top-level Compose wiring:

- `ResilientP2PApp`: Scaffold with top bar, dropdown menu
- `DashboardContent`: Mesh contacts, network stats, telemetry, logs
- `MeshContactsSection`: Peer list with click-to-chat (direct + routed peers)
- `NetworkStatsSection`: Collapsible card with battery, traffic, per-peer stats
- `ChatScreen` integration: audio callbacks, file send, message persistence
- Group chat dialog, emergency broadcast dialog, health dashboard
- `rememberSaveable` for rotation-safe state

#### `ChatScreen.kt` (~468 lines)

Full-screen chat interface:

- Message bubbles (TEXT, IMAGE, FILE, SYSTEM)
- Image thumbnails via Coil
- File transfer progress indicators
- **Push-to-Talk**: Full-width button, always visible (including broadcast mode), recording indicator, press-to-record/release-to-send
- Image picker + general file picker
- Auto-scroll with user-near-bottom detection

#### `theme/`

- `Color.kt`: Professional palette (TechBlue, TechTeal, StatusGreen, neon accents)
- `Theme.kt`: Material 3 light/dark mode support

---

### 2.10 `dashboard/`

Web-based telemetry dashboard:

- `index.html` + `dashboard.js` + `style.css`
- Connects to Firestore to display test results, endurance data
- Charts and metrics visualization

---

## 3. Protocol & Algorithms

### 3.1 The Routing Protocol

**Distance Vector** variant with TTL-based scoring.

- **Metric**: `Score = min(TTL, 10) × 100 + RSSI/10` (higher = better)
- **Default TTL**: 5 (supports up to 5-hop meshes)
- **Table**: `ConcurrentHashMap<Dest, NextHop>` with `routingLock` for atomicity
- **Updates**: Flood-based (IDENTITY) + explicit ROUTE_ANNOUNCE (periodic, 1-hop)
- **Acceptance criteria**: better score, OR dead next-hop, OR stale route (> 20s)
- **Score degradation**: −100 per hop for indirect routes
- **Implicit withdrawal**: Routes not in ROUTE_ANNOUNCE are pruned immediately
- **Stale pruning**: Routes older than 60s evicted in background

### 3.2 Audio Streaming Protocol

**Mesh-routed AAC-LC audio** via AUDIO_DATA / AUDIO_CONTROL packets:

1. **Session start**: AUDIO_CONTROL `"sessionId|START"` sent to target
2. **Data flow**: PCM → AAC encode → batch 5 frames (100 ms) → AUDIO_DATA packet
3. **Session stop**: AUDIO_CONTROL `"sessionId|STOP"` sent to target
4. **Multi-hop**: Packets routed through mesh like any DATA packet (TTL decremented, route table used)
5. **Bandwidth**: ~5 KB/s per stream (vs 16 KB/s raw PCM)

### 3.3 File Sharing Protocol

**Content-addressable chunked transfer** with resume:

1. **FILE_ANNOUNCE**: `sha256|fileName|mimeType|fileSize|chunkSize|totalChunks`
2. **FILE_REQUEST**: `sha256|chunkIndex` — only for missing chunks
3. **FILE_CHUNK**: `sha256|chunkIndex|<binary data>`
4. **Resume**: `DownloadedChunk` Room entity persists chunk status. On restart, only missing chunks are re-requested.

### 3.4 Cloud Relay Protocol

**Internet gateway relay** via Firestore:

1. Gateway nodes detect internet via `NetworkCallback`
2. Advertise `__GATEWAY__` in ROUTE_ANNOUNCE
3. When no route and no neighbors: `InternetGatewayManager.relayToCloud()` — POST to Firestore
4. Gateway nodes poll every 30s, inject matching messages into local mesh
5. 24h TTL on relay messages, rate limited to 60/hour

### 3.5 Security

- **ECDH** key exchange during IDENTITY handshake
- **AES-256-GCM** end-to-end encryption for DATA payloads
- **HMAC-SHA256** packet integrity verification
- **Per-peer rate limiting** with sliding window
- **Auto-ban** on configurable violation threshold

### 3.6 Stability Heuristics

1. **Duplicate Race**: Lexicographic endpoint ID comparison picks deterministic winner
2. **Radio Stall (8012)**: sendPayload failure → immediate disconnect
3. **Zombie Detection**: Heartbeat PING + configurable timeout + transfer exemption
4. **Self-Poisoning Prevention**: Refuse to add own identity to routing table

---

## 4. Room Database Schema (v9)

| Table               | Entity            | Description                         |
| ------------------- | ----------------- | ----------------------------------- |
| `logs`              | `LogEntry`        | Diagnostic/metric log entries       |
| `packet_queue`      | `PacketEntity`    | Store-and-forward packets           |
| `chat_messages`     | `ChatMessage`     | User chat messages + file transfers |
| `telemetry_events`  | `TelemetryEvent`  | Cloud telemetry snapshots           |
| `chat_groups`       | `ChatGroup`       | Named chat channels                 |
| `group_messages`    | `GroupMessage`    | Group chat messages                 |
| `shared_files`      | `SharedFile`      | Content-addressable file metadata   |
| `encounter_log`     | `EncounterLog`    | DTN encounter records               |
| `downloaded_chunks` | `DownloadedChunk` | Per-chunk file resume tracking      |

---

## 5. Dependencies

| Library                       | Purpose                                    |
| ----------------------------- | ------------------------------------------ |
| `play-services-nearby:19.3.0` | Google Nearby Connections API              |
| `room:2.8.4`                  | Local database (9 entities, SQL DAO layer) |
| `compose-bom:2025.12.00`      | Jetpack Compose UI framework               |
| `material3`                   | Material Design 3 components               |
| `coil-compose:2.7.0`          | Image loading for chat thumbnails          |
| `work-runtime-ktx:2.10.1`     | WorkManager for telemetry upload           |
| `lifecycle-process`           | ProcessLifecycleOwner for app lifecycle    |
| `junit:4.13.2`                | Unit testing framework                     |

---

## 6. Build Configuration

- **Compile SDK**: 36 | **Target SDK**: 36 | **Min SDK**: 24
- **Kotlin**: 2.0.21 | **JVM Target**: 17
- **AGP**: 8.13.1 | **KSP** for Room annotation processing
- **ProGuard**: Enabled for release (minify + shrink)
- **BuildConfig fields**: `TEST_MODE`, `FIREBASE_PROJECT_ID`, `FIREBASE_API_KEY`
- **configChanges**: `orientation|screenSize|screenLayout|keyboardHidden` on MainActivity

---

## 7. Test Infrastructure

### Unit Tests (JVM)

- 6 test files, ~85 test cases covering: Packet serialization, MessageCache, routing logic, audio codec config, file sharing protocol, mesh audio format
- Run: `./gradlew test`

### Automated Functional Tests (On-Device)

- `TestRunner`: 15 tests exercising real P2P connections
- Run from in-app Test Mode screen

### Endurance Tests (On-Device)

- `EnduranceTestRunner`: Configurable soak tests (default 30 min)
- Tracks battery, RTT, jitter, packet loss, throughput, connection stability
- Results uploaded to cloud via TelemetryManager

---

**End of Technical Bible**
