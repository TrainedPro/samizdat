# Design Decisions — Samizdat (ResilientP2PTestbed)

This document records **every** non-trivial constant, threshold, architectural choice,
and assumption in the codebase, together with the reasoning behind each.
Use it as a reference when tuning, extending, or reviewing the system.

> **Convention:** each entry names the source file, constant/variable, and its value.

---

## Table of Contents

1. [Architecture](#1-architecture)
2. [Nearby Connections & Mesh Topology](#2-nearby-connections--mesh-topology)
3. [Packet Protocol](#3-packet-protocol)
4. [Routing](#4-routing)
5. [Heartbeat & Liveness](#5-heartbeat--liveness)
6. [Store-and-Forward](#6-store-and-forward)
7. [Audio Streaming](#7-audio-streaming)
8. [Audio Codec (AAC-LC)](#8-audio-codec-aac-lc)
9. [Internet Gateway & Cloud Relay](#9-internet-gateway--cloud-relay)
10. [Emergency Broadcast](#10-emergency-broadcast)
11. [Telemetry & Cloud Upload](#11-telemetry--cloud-upload)
12. [Database & Persistence](#12-database--persistence)
13. [Testing](#13-testing)
14. [Security (future — feature/security)](#14-security)
15. [UI & Compose](#15-ui--compose)

---

## 1. Architecture

### Application Singleton Pattern

- **File:** `P2PApplication.kt`
- **Decision:** All long-lived managers (`P2PManager`, `HeartbeatManager`, `TelemetryManager`,
  `InternetGatewayManager`, `EmergencyManager`, `TestRunner`) are owned by the `Application`
  subclass and shared with Activities/Services via `lateinit` properties.
- **Reason:** Android's `Application` outlives any Activity or Service. A singleton ensures
  that mesh connections, routing tables, and codec state survive configuration changes
  (rotation, backgrounding). Alternatives like Hilt/Dagger were rejected to keep the
  prototype simple and dependency-free.

### Foreground Service

- **File:** `P2PService.kt`
- **Constants:** `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` (API 34+), `START_STICKY`
- **Decision:** A persistent foreground service holds the Nearby Connections session alive.
- **Reason:** Without a foreground service, Android kills background BLE/WiFi-Direct
  sessions within minutes. `START_STICKY` restarts the service if the OS kills it.
  `FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE` is required since API 34.

### Manager Shutdown Order

- **File:** `P2PService.onDestroy()`
- **Order:** EmergencyManager → InternetGatewayManager → TelemetryManager → P2PManager → HeartbeatManager
- **Reason:** Emergency and gateway managers depend on P2PManager for packet injection,
  so they must be torn down first. HeartbeatManager is last because it only consumes
  neighbor state.

---

## 2. Nearby Connections & Mesh Topology

### Strategy

- **File:** `P2PManager.kt` → `STRATEGY`
- **Value:** `Strategy.P2P_CLUSTER`
- **Reason:** P2P_CLUSTER allows > 2 participants per session and supports both
  advertising and discovering simultaneously. P2P_STAR would limit the topology to
  a single hub; P2P_POINT_TO_POINT would allow only 2 peers.

### Maximum Connections

- **File:** `P2PManager.kt` → `MAX_CONNECTIONS = 4`
- **Reason:** Google Nearby Connections recommends ≤ 4 simultaneous connections for
  stable BLE/WiFi-Direct sessions. Beyond 4, BLE advertisement slots are exhausted
  and connection reliability drops sharply. This is a hard limit of the underlying
  transport, not an arbitrary cap. Mesh coverage beyond 4 hops is handled by
  multi-hop routing.

### Service ID

- **File:** `P2PManager.kt` → `SERVICE_ID = "com.fyp.resilientp2p"`
- **Reason:** Nearby Connections uses this string to filter discovery. All devices
  running Samizdat share this ID. Must match exactly on all devices.

### Stability Window

- **File:** `P2PManager.kt` → `STABILITY_WINDOW_MS = 15000L` (15 seconds)
- **Reason:** When a duplicate connection to the same peer is detected, we keep the
  existing connection if it has been active for < 15 s (considered "fresh"). Beyond
  15 s, the new connection is accepted and the old one torn down. This prevents
  connection flapping during the initial handshake storm.

### Connection Authentication

- **File:** `P2PManager.kt` → `connectionLifecycleCallback.onConnectionInitiated()`
- **Decision:** All connections are auto-accepted (`acceptConnection`). No manual
  token verification by default.
- **Reason:** Research testbed — friction-free pairing is needed for automated
  testing. The `authenticationDigits` field and UI dialog exist for optional
  manual verification but are only triggered when `isManualConnectionEnabled` is true.

---

## 3. Packet Protocol

### Wire Format

- **File:** `Packet.kt`
- **Decision:** Custom binary format using `DataOutputStream` / `DataInputStream` with
  big-endian byte order. Strings are length-prefixed (4-byte int + UTF-8 bytes).
- **Reason:** Minimizes overhead on BLE links where every byte counts. Protobuf/JSON
  would add ~30-50% overhead and require a dependency. The format is simple enough
  to debug with hex dumps.

### DEFAULT_TTL = 5

- **File:** `Packet.kt` → `Packet.DEFAULT_TTL`
- **Reason:** 5 hops covers the expected mesh diameter for a campus-scale deployment
  (up to ~100 m BLE range × 5 = 500 m effective coverage). Higher TTLs increase
  flooding traffic quadratically.

### MAX_STRING_LENGTH = 256 bytes

- **File:** `Packet.kt` → `MAX_STRING_LENGTH`
- **Reason:** Device names and UUIDs are always < 100 bytes. The 256-byte cap
  prevents a malformed packet from consuming unbounded memory during deserialization.

### MAX_PAYLOAD_SIZE = 1 MB

- **File:** `Packet.kt` → `MAX_PAYLOAD_SIZE`
- **Reason:** Matches the Nearby Connections `BYTES` payload limit. File transfers
  use Nearby's `FILE` payload type, so the 1 MB cap only applies to in-band data
  (chat messages, route announcements, emergency broadcasts).

### MAX_TOTAL_PACKET_SIZE = 2 MB

- **File:** `Packet.kt` → `MAX_TOTAL_PACKET_SIZE`
- **Reason:** Aggregate check applied before deserialization to prevent OOM DoS.
  The 2 MB ceiling accounts for payload + trace + string overhead.

### MAX_TRACE_SIZE = 256

- **File:** `Packet.kt` → `MAX_TRACE_SIZE`
- **Reason:** Prevents a crafted packet with millions of hop entries from consuming
  memory. 256 is far beyond any realistic mesh diameter.

### PacketType Enum

| Type | Purpose | Routing |
|------|---------|---------|
| `PING` / `PONG` | Heartbeat liveness + RTT | Direct neighbor only |
| `ACK` | Delivery confirmation | Unicast back to source |
| `DATA` | Chat messages + generic data | Unicast via routing table; falls back to store-forward |
| `GOSSIP` | Protocol-level gossip (reserved) | Broadcast |
| `IDENTITY` | Device name announcement | Direct neighbor only |
| `AUDIO_DATA` | Compressed audio frames | Unicast (multi-hop via routing) |
| `AUDIO_CONTROL` | Start/stop audio session | Unicast |
| `ROUTE_ANNOUNCE` | Periodic routing table advertisement | Broadcast to direct neighbors |
| `STORE_FORWARD` | Re-delivery of queued message | Unicast |
| `FILE_META` | File transfer metadata | Unicast |
| `EMERGENCY` | Emergency broadcast (SOS/alert) | **Flood entire mesh** (always rebroadcast regardless of destination) |

---

## 4. Routing

### Distance-Vector with Poison Reverse

- **File:** `P2PManager.kt` → `handleRouteAnnouncement()`, `broadcastRouteAnnouncement()`
- **Decision:** Each node periodically (every ~8-10 s) broadcasts its routing table to
  direct neighbors. Receiving nodes adopt routes if the hop count is lower than
  existing routes.
- **Reason:** Simple to implement, low overhead, converges quickly for meshes < 20 nodes.
  Link-state (OSPF-like) protocols were rejected as overkill for this scale.

### Route Stale Threshold = 30 s

- **File:** `P2PManager.kt` → route maintenance coroutine, `staleThreshold = 30000L`
- **Reason:** Route announcements are broadcast every 8-10 s. A 30 s threshold
  (≈3 missed announcements) balances quick failover against transient BLE stalls.

### Message Cache (Deduplication)

- **File:** `MessageCache.kt` → `capacity = 2000`, `ttl = 10 min`
- **Reason:** Prevents forwarding loops. 2 000 entries cover ~3 minutes of heavy
  traffic at 10 packets/second. The 10-minute TTL ensures stale entries are evicted
  even under low traffic.

---

## 5. Heartbeat & Liveness

### Default Interval = 5 000 ms

- **File:** `HeartbeatManager.kt` → `DEFAULT_INTERVAL_MS = 5000L`
- **Reason:** 5 s provides a good trade-off between liveness detection speed and
  bandwidth cost (~13 bytes/ping × 4 peers × 12/min = ~3 KB/min).

### Adaptive Heartbeat

- **File:** `HeartbeatManager.kt` → `bandwidthEvents` collector
- **Values:** HIGH → 2 000 ms, LOW → 10 000 ms, MEDIUM → unchanged
- **Reason:** On HIGH bandwidth (WiFi-Direct), faster heartbeats give sub-second
  failure detection. On LOW bandwidth (BLE congested), slower heartbeats prevent
  adding to the congestion.

### Zombie Detection Threshold

- **File:** `HeartbeatManager.kt` → `cleanupZombies()`
- **Formula:** `max(intervalMs × 6, 45_000L)`
- **Reason:** The previous threshold (3× interval = 15 s) caused false positives
  because BLE can stall for 10-20 s under contested bandwidth. The 45 s floor
  prevents premature disconnections. The 6× multiplier allows 5 missed heartbeats
  before declaring a peer dead.

### Payload Size = 64 bytes

- **File:** `HeartbeatManager.kt` → `DEFAULT_PAYLOAD_SIZE = 64`
- **Reason:** Embeds an 8-byte timestamp for RTT measurement. The remaining 56 bytes
  are zero-padding to detect MTU issues. Larger payloads waste bandwidth; smaller
  payloads won't surface BLE fragmentation problems.

---

## 6. Store-and-Forward

### Queue TTL = 2 hours

- **File:** `P2PManager.kt` → `STORE_FORWARD_TTL_MS = 2 * 60 * 60 * 1000L`
- **Reason:** Messages older than 2 hours are unlikely to be relevant in a real-time
  emergency scenario. Shorter TTLs (e.g. 15 min) were too aggressive for slow-moving
  mesh partitions.

### Persistence Backend

- **File:** `PacketEntity.kt`, `PacketDao.kt`
- **Decision:** Store-forward packets are persisted in Room DB so they survive process
  restarts and device reboots.
- **Reason:** The foreground service may be killed by the OS. Without DB persistence,
  queued messages would be lost.

---

## 7. Audio Streaming

### Sample Rate = 8 000 Hz

- **File:** `AudioBuffer.kt` → `sampleRate = 8000`, `AudioCodecManager.SAMPLE_RATE`
- **Reason:** Telephone-quality mono at 8 kHz produces 16 KB/s raw PCM. Over BLE
  (typical throughput 20-50 KB/s), this is the only viable quality level. 16 kHz would
  saturate the link.

### Jitter Buffer

- **File:** `AudioPlayer.kt` → `jitterBufferMs` calculation
- **Decision:** `max(40, peerRttMs / 2)` milliseconds of buffering before playback.
- **Reason:** Absorbs jitter from multi-hop routing and BLE scheduling. The RTT/2
  heuristic adapts to network conditions. The 40 ms floor prevents clicks on very
  low-latency links (direct WiFi neighbors).

---

## 8. Audio Codec (AAC-LC)

### Codec Choice: AAC-LC via MediaCodec

- **File:** `AudioCodecManager.kt`
- **Decision:** Hardware-accelerated AAC-LC encoding/decoding via Android's MediaCodec API.
- **Reason:** AAC-LC is universally supported on all Android devices (hardware codec).
  Opus would require a native library (JNI). AAC-LC at 24 kbps with 8 kHz mono
  achieves ~5:1 compression (16 KB/s → 3-4 KB/s), making multi-hop audio viable.

### BIT_RATE = 24 000 bps

- **File:** `AudioCodecManager.kt` → `BIT_RATE = 24_000`
- **Reason:** 24 kbps is the sweet spot for 8 kHz mono AAC-LC. Lower rates (16 kbps)
  introduce audible artifacts; higher rates (32 kbps) waste bandwidth with no
  perceptible quality gain at this sample rate.

### FRAME_DURATION_MS = 20

- **File:** `AudioCodecManager.kt` → `FRAME_DURATION_MS = 20`
- **Reason:** 20 ms frames (160 samples, 320 bytes PCM) balance latency vs. codec
  efficiency. 10 ms frames double the overhead; 40 ms frames add perceptible delay.

### CSD Bytes = `0x15, 0x88`

- **File:** `AudioCodecManager.AACDecoder` → `csd-0` ByteBuffer
- **Reason:** Codec Specific Data for AAC-LC. Encodes: profile=2 (LC), sampling
  frequency index=11 (8 kHz), channel configuration=1 (mono). Computed from the
  AudioSpecificConfig spec (ISO 14496-3).

---

## 9. Internet Gateway & Cloud Relay

### Gateway Detection

- **File:** `InternetGatewayManager.kt` → `registerNetworkCallback()`
- **Decision:** Uses `ConnectivityManager.NetworkCallback` with `NET_CAPABILITY_INTERNET`
  + `NET_CAPABILITY_VALIDATED`.
- **Reason:** `NET_CAPABILITY_VALIDATED` ensures the device actually passed Google's
  connectivity check, not just that WiFi is associated. Prevents advertising gateway
  status behind captive portals.

### GATEWAY_FLAG = "__GATEWAY__"

- **File:** `InternetGatewayManager.kt` → `GATEWAY_FLAG`
- **Decision:** Appended to `ROUTE_ANNOUNCE` payloads as `__GATEWAY__:1`.
- **Reason:** A distinct, non-clashing string that mesh peers parse to discover which
  neighbor has internet. Using the routing table (rather than a separate packet type)
  avoids adding protocol complexity.

### Cloud Relay Backend: Firestore REST API

- **File:** `InternetGatewayManager.kt` → `relayToCloud()`, `pollRelayMessages()`
- **Decision:** Uses raw Firestore REST API (no Firebase SDK) via `HttpURLConnection`.
- **Reason:** The Firebase Android SDK adds ~5 MB to APK size and pulls in Google
  Play Services dependencies. REST API keeps the app lightweight and avoids
  `google-services.json` configuration.

### MESSAGE_TTL_MS = 24 hours

- **File:** `InternetGatewayManager.kt` → `MESSAGE_TTL_MS = 24 * 60 * 60 * 1000L`
- **Reason:** Cloud relay is a transient message buffer, not permanent storage.
  24 hours gives mobile mesh partitions time to reconnect. Expired messages are
  deleted during polling.

### MAX_MESSAGE_SIZE = 10 KB

- **File:** `InternetGatewayManager.kt` → `MAX_MESSAGE_SIZE = 10 * 1024`
- **Reason:** Cloud relay is for text messages only (chat, emergency). File transfers
  go through Nearby's `FILE` payload. 10 KB prevents abuse of the free-tier Firestore.

### POLL_INTERVAL_MS = 30 s

- **File:** `InternetGatewayManager.kt` → `POLL_INTERVAL_MS = 30_000L`
- **Reason:** Balances message delivery latency against Firestore read costs.
  At 30 s, a gateway incurs ~2 880 reads/day (well within free tier).

### MAX_SEND_RATE_PER_HOUR = 60

- **File:** `InternetGatewayManager.kt` → `MAX_SEND_RATE_PER_HOUR = 60`
- **Reason:** Rate-limits outbound relay to 1 message/minute average. Prevents a
  rogue node from exhausting the Firestore write quota.

### MAX_PENDING_PER_DEVICE = 100

- **File:** `InternetGatewayManager.kt` → `MAX_PENDING_PER_DEVICE = 100`
- **Reason:** Caps the number of Firestore documents per destination device to
  prevent unbounded storage growth if a device goes offline permanently.

---

## 10. Emergency Broadcast

### EMERGENCY_TTL = 15

- **File:** `EmergencyManager.kt` → `EMERGENCY_TTL = 15`
- **Reason:** Triple the normal TTL (5) to ensure emergency messages reach the
  entire mesh even in sparse topologies. Emergency packets also bypass the normal
  `messageCache` dedup for their first hop (processed locally + flooded).

### Flood Strategy

- **File:** `P2PManager.kt` → `handlePacket()` for `EMERGENCY` type
- **Decision:** EMERGENCY packets are ALWAYS delivered locally AND forwarded to ALL
  neighbors, regardless of destination address.
- **Reason:** In emergencies, every node must display the alert. Standard unicast
  routing would only reach the addressed peer.

### SOS_BEACON_INTERVAL_MS = 30 s

- **File:** `EmergencyManager.kt` → `SOS_BEACON_INTERVAL_MS = 30_000L`
- **Reason:** Fast enough for rescuers to track movement, slow enough to avoid
  flooding the mesh. GPS updates are also requested at 30 s intervals for consistency.

### EMERGENCY_DEST = "EMERGENCY_BROADCAST"

- **File:** `EmergencyManager.kt` → `EMERGENCY_DEST`
- **Reason:** A sentinel destination address. The flood routing in `handlePacket()`
  ignores it; every node processes the message locally.

### Emergency Dedup Window = 60 s

- **File:** `EmergencyManager.kt` → `addToHistory()`, window = `60_000` ms
- **Reason:** SOS beacons repeat every 30 s. A 60 s window deduplicates consecutive
  beacons from the same source while still capturing message changes.

### Max Emergency History = 200

- **File:** `EmergencyManager.kt` → `addToHistory()`, limit = 200
- **Reason:** In-memory list for UI display. 200 entries is enough for extended
  incidents while capping memory at ~50 KB.

### GPS Location Provider

- **File:** `EmergencyManager.kt` → `startLocationUpdates()`
- **Decision:** Requests both GPS_PROVIDER and NETWORK_PROVIDER with 30 s interval + 10 m distance.
- **Reason:** GPS gives highest accuracy but may be unavailable indoors. Network
  provider (cell tower / WiFi triangulation) provides a fallback. The 30 s + 10 m
  thresholds match the SOS beacon interval.

---

## 11. Telemetry & Cloud Upload

### SNAPSHOT_INTERVAL_MS = 5 min

- **File:** `TelemetryManager.kt` → `SNAPSHOT_INTERVAL_MS = 5 * 60 * 1000L`
- **Reason:** Captures stats snapshots every 5 minutes. More frequent snapshots would
  grow the DB quickly; less frequent would miss transient events.

### UPLOAD_INTERVAL_MINUTES = 15

- **File:** `TelemetryManager.kt` → `UPLOAD_INTERVAL_MINUTES = 15L`
- **Reason:** WorkManager periodic task minimum is 15 minutes. Aligns with Android
  platform constraints.

### MAX_BATCH_SIZE = 100

- **File:** `TelemetryManager.kt` → `MAX_BATCH_SIZE = 100`
- **Reason:** Limits each upload to 100 events. Prevents timeout on slow connections
  and keeps Firestore write batches within limits.

### MAX_DB_EVENTS = 5 000

- **File:** `TelemetryManager.kt` → `MAX_DB_EVENTS = 5000`
- **Reason:** Hard cap on the telemetry_events table. At 100 events/day, this
  is 50 days of buffer. Older events are deleted on enforcement.

### MAX_UPLOAD_ATTEMPTS = 10

- **File:** `TelemetryManager.kt` → `MAX_UPLOAD_ATTEMPTS = 10`
- **Reason:** Events that fail 10 uploads are presumably malformed. Deleting them
  prevents eternal retry loops.

### UPLOADED_RETENTION_MS = 24 hours

- **File:** `TelemetryManager.kt` → `UPLOADED_RETENTION_MS = 24 * 60 * 60 * 1000L`
- **Reason:** Keeps uploaded events for 24 h as a safety buffer in case the cloud
  backend loses them. After 24 h, they are garbage-collected.

### Privacy Policy

- **File:** `TelemetryManager.kt` (class KDoc)
- **Uploaded:** stats snapshots, connection events, error/warn logs, test results.
- **NOT uploaded:** chat content, audio, file payloads, raw packets.
- **Reason:** Chat and audio are user-generated content; only infrastructure metrics
  are collected.

### MIN_UPLOAD_INTERVAL_MS = 5 min

- **File:** `TelemetryUploadWorker.kt` → `MIN_UPLOAD_INTERVAL_MS = 5 * 60 * 1000L`
- **Reason:** Even though WorkManager triggers every 15 min, the worker skips if
  < 5 min since last upload (guards against WorkManager re-scheduling quirks).

### MAX_CONSECUTIVE_FAILURES = 5

- **File:** `TelemetryUploadWorker.kt` → `MAX_CONSECUTIVE_FAILURES = 5`
- **Reason:** After 5 consecutive upload failures, the worker backs off to prevent
  battery drain on devices with flaky internet.

---

## 12. Database & Persistence

### Room Database Version = 7

- **File:** `AppDatabase.kt` → `version = 7`
- **Reason:** Incremented through development as tables were added
  (logs → packets → chat → telemetry). Uses `fallbackToDestructiveMigration()`
  because this is a research testbed where data persistence across schema changes
  is not required.

### Database Name = "p2p_testbed_db"

- **File:** `AppDatabase.kt` → `Room.databaseBuilder(…, "p2p_testbed_db")`
- **Reason:** Descriptive name distinguishable from other apps' databases.

### Log Index on Timestamp

- **File:** `LogEntry.kt` → `@Entity(indices = [Index(value = ["timestamp"])])`
- **Reason:** Logs are queried by time range for CSV export and the scrolling log
  viewer. Without an index, `ORDER BY timestamp DESC` would be a full table scan.

### Packet Queue Index on (destId, expiration)

- **File:** `PacketEntity.kt` → `@Entity(indices = [Index(value = ["destId", "expiration"])])`
- **Reason:** Two hot queries: "get packets for peer X" (uses destId prefix) and
  "delete expired" (uses expiration). A composite index serves both.

### Enum Converters with Safe Fallback

- **File:** `Converters.kt`
- **Decision:** Unknown enum values fall back to a safe default (e.g., `LogLevel.INFO`)
  rather than throwing.
- **Reason:** Protects against DB corruption after adding/removing enum values across
  app upgrades with destructive migration.

---

## 13. Testing

### PEER_WAIT_TIMEOUT_MS = 120 s

- **File:** `TestRunner.kt` → `PEER_WAIT_TIMEOUT_MS = 120_000L`
- **Reason:** 2 minutes to discover and connect to at least one peer. BLE discovery
  can take 30-60 s in practice; 2 min provides margin for slow devices.

### PING_COUNT = 20

- **File:** `TestRunner.kt` → `PING_COUNT = 20`
- **Reason:** 20 pings gives a statistically meaningful RTT sample while completing
  in ~10 s (at 500 ms intervals).

### PING_INTERVAL_MS = 500 ms

- **File:** `TestRunner.kt` → `PING_INTERVAL_MS = 500L`
- **Reason:** Fast enough for quick tests; slow enough to avoid saturating BLE.

### MSG_COUNT = 10

- **File:** `TestRunner.kt` → `MSG_COUNT = 10`
- **Reason:** 10 test messages are sufficient to measure delivery rate and ordering.

### PING_WAIT_TIMEOUT_MS = 15 s

- **File:** `TestRunner.kt` → `PING_WAIT_TIMEOUT_MS = 15_000L`
- **Reason:** Maximum wait for ping responses. A multi-hop mesh with 5 hops and
  ~1 s per hop needs up to 10 s round-trip; 15 s provides margin.

---

## 14. Security (future — feature/security)

> Planned for branch `feature/security`. Not on `main` so it can be easily
> included or excluded.

- **E2E Encryption:** X25519 key exchange during IDENTITY handshake, AES-256-GCM
  for payload encryption. KeyPair stored in Android Keystore.
- **Packet Integrity:** HMAC-SHA256 appended to every packet. Prevents tampering
  by relay nodes.
- **Rate Limiting:** Per-peer configurable rate limit (default 100 packets/sec).
  Protects against DOS from malicious mesh participants.
- **Peer Blacklisting:** Persistent blocklist (Room DB). Blacklisted peers' packets
  are dropped at the first hop.

---

## 15. UI & Compose

### Theme

- **File:** `MainActivity.kt` → `ResilientP2PTestbedTheme(darkTheme = false)`
- **Decision:** Light theme forced.
- **Reason:** Maximizes readability for demonstration and presentation contexts.

### Emergency Alert Colors

- **File:** `P2PComposables.kt`
- **Decision:** Red background for emergency alerts, green for gateway status, orange
  for SOS button.
- **Reason:** Standard emergency color coding (red = danger, green = safe, orange =
  caution) for at-a-glance recognition.

### Max Displayed Emergency Alerts = 3

- **File:** `P2PComposables.kt` → emergency alerts section
- **Reason:** Shows the 3 most recent alerts in the banner. More would consume too
  much screen real estate above the fold.

---

## Reconnection Strategy

### RECONNECT_DELAY_MS = 3 000 ms

- **File:** `P2PManager.kt` → `RECONNECT_DELAY_MS = 3000L`
- **Reason:** Base delay for exponential backoff. 3 s is long enough for BLE to
  settle after a disconnect but short enough for fast recovery.

### RECONNECT_MAX_ATTEMPTS = 5

- **File:** `P2PManager.kt` → `RECONNECT_MAX_ATTEMPTS = 5`
- **Reason:** After 5 attempts (up to ~96 s with backoff), the peer is considered
  permanently gone. Further attempts waste radio time.

### Exponential Backoff Formula

- **File:** `P2PManager.kt` → reconnection loop
- **Formula:** `RECONNECT_DELAY_MS * 2^min(attemptCount, 4)`
- **Values:** 3 s → 6 s → 12 s → 24 s → 48 s
- **Reason:** Exponential backoff reduces collision probability when multiple peers
  try to reconnect simultaneously. The `min(…, 4)` cap prevents absurdly long delays.

---

## Firebase Configuration

### No google-services.json

- **File:** `build.gradle.kts` (app), `InternetGatewayManager.kt`, `TelemetryUploadWorker.kt`
- **Decision:** Firebase keys are injected via `BuildConfig` from `local.properties`
  (gitignored). The Firebase Android SDK is **not** used.
- **Reason:** Avoids the 5+ MB SDK footprint and the rigid `google-services.json`
  build pipeline. REST API gives full control for the limited operations needed
  (Firestore read/write).

### Keys in local.properties

```properties
FIREBASE_API_KEY=...
FIREBASE_PROJECT_ID=samizdat
```

- **Reason:** Keeps secrets out of version control. Each developer must add their
  own keys. The build fails gracefully (empty strings) if keys are missing.
