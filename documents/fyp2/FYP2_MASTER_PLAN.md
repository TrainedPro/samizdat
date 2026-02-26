# FYP2 Master Plan — Samizdat (ResilientP2PTestbed)

> Last updated: 2026-02-27  
> Branch strategy: `main` for Stage 1–2 (through Phase 3). Stage 3+ (Phase 4) on `feature/stage-3` after Phase 3 is fully tested and confirmed.

---

## Stage Overview

| Stage | Phases | Branch | Gate Condition |
|-------|--------|--------|----------------|
| **Stage 1** | Phase 1 (Polish) + Phase 2 (Cloud) | `main` | Cloud telemetry working, all P1 bugs fixed, tested on 4+ devices |
| **Stage 2** | Phase 3 (Gateway + Emergency) | `main` | Stage 1 confirmed stable |
| **Stage 3** | Phase 4 (Advanced) | `feature/stage-3` | Stage 2 confirmed stable |

---

## Phase 1: Stability, Testing & UI Polish

### 1.1 Automated Test Mode

**Problem:** Currently all testing requires manual operation — connect devices, send messages, check logs by hand. We need an automated test mode that can exercise the full stack without user interaction.

**Design:**

```
BUILD_CONFIG flag: `TEST_MODE = true/false`
Set via: ./gradlew assembleDebug -Ptest_mode=true
```

**What test mode does (automatic, on launch):**

1. **Auto-start networking** — Skip manual "Start" button, immediately advertise + discover
2. **Auto-accept connections** — No authentication dialog, instant accept
3. **Run test suite in sequence** after first peer connects:
   - **Text messaging test**: Send 10 numbered messages to each peer, verify receipt via ACK count
   - **Ping/RTT test**: Fire 20 pings at 500ms intervals, log min/avg/max RTT
   - **File transfer test**: Send a generated 100KB test file, verify receipt + integrity (SHA-256 hash)
   - **Audio codec test**: Record 3s of silence (known payload), stream, verify bytes received match expected at 8kHz/16-bit/mono
   - **Store-and-forward test**: Queue a message, disconnect from target, reconnect, verify delivery
   - **Route convergence test**: With 3+ devices, send message to non-neighbor, time until delivery
4. **Emit structured test results** as a JSON log block:
   ```json
   {
     "testRun": "2026-02-27T14:30:00Z",
     "device": "SM-S938B-abc123",
     "peers": ["TB132FU-def456", "SM-A730F-ghi789"],
     "results": {
       "textMessaging": { "sent": 10, "acked": 10, "avgLatencyMs": 45 },
       "pingRtt": { "min": 12, "avg": 34, "max": 87, "count": 20 },
       "fileTransfer": { "sizeBytees": 102400, "durationMs": 3200, "hashMatch": true },
       "audioStream": { "expectedBytes": 48000, "receivedBytes": 47800, "lossPercent": 0.4 },
       "storeForward": { "queued": true, "delivered": true, "deliveryTimeMs": 15400 },
       "routeConvergence": { "hops": 2, "convergenceTimeMs": 8200 }
     }
   }
   ```
5. **Auto-export results** to `test_results/` directory in CSV + JSON format

**Implementation steps:**
- [ ] Add `testMode` boolean to `BuildConfig` via `build.gradle.kts` buildConfigField
- [ ] Add `TestRunner.kt` manager class that orchestrates the test sequence
- [ ] Add `TestResultCollector.kt` that aggregates results + exports
- [ ] Auto-start mesh on launch when `BuildConfig.TEST_MODE == true`
- [ ] Auto-accept connections in test mode (skip auth dialog)
- [ ] Each test emits structured results to the collector
- [ ] On all tests complete, export results + trigger pull_logs.sh compatible CSV

### 1.2 File Transfer UI Overhaul

**Current state:** Bare-minimum 📷 and 📎 buttons. No progress indicator, no received file display, no image preview.

**Target UI:**

```
┌─────────────────────────────────┐
│  Chat with P2P-Node-7842        │
│─────────────────────────────────│
│  [Incoming messages area]       │
│  ┌───────────────────────┐      │
│  │ 📩 "Hello from Phone" │      │  ← Text message bubble
│  └───────────────────────┘      │
│  ┌───────────────────────┐      │
│  │ 📷 photo_001.jpg      │      │  ← Image thumbnail (clickable)
│  │ [===========   ] 73%  │      │  ← Transfer progress bar
│  └───────────────────────┘      │
│  ┌───────────────────────┐      │
│  │ 📎 report.pdf (2.1MB) │      │  ← File with size, tap to open
│  │ ✅ Delivered           │      │
│  └───────────────────────┘      │
│─────────────────────────────────│
│ [Message input            ] 📤  │
│ [PTT] [📷] [📎] [PING] [✕]    │
│─────────────────────────────────│
```

**Implementation steps:**
- [ ] Create `ChatMessage` sealed class: `TextMessage`, `ImageMessage`, `FileMessage`, `SystemMessage`
- [ ] Add `chatMessages: StateFlow<Map<String, List<ChatMessage>>>` to `P2PManager` (peer → messages)
- [ ] Record incoming text messages as `ChatMessage.TextMessage`
- [ ] Record incoming file receipts as `ChatMessage.FileMessage` / `ChatMessage.ImageMessage`
- [ ] Replace `AlertDialog` with full-screen `ModalBottomSheet` or dedicated `ChatScreen` composable
- [ ] Chat area: `LazyColumn` of message bubbles with sender/timestamp
- [ ] Image messages: `AsyncImage` (Coil) for thumbnails, click to fullscreen
- [ ] File messages: Show filename + size + status (sending/delivered/failed)
- [ ] Progress bar per active transfer (from `PayloadTransferUpdate`)
- [ ] Persistent chat history in Room DB (new `ChatMessageEntity`)
- [ ] "Open file" intent for non-image files

### 1.3 Remaining Audio Work

- [ ] Opus codec integration (via `MediaCodec` AAC as stepping stone, or JNI `libopus`)
  - Target: 6–8 KB/s at 8kHz, vs current 16 KB/s raw PCM
  - Fallback: If Opus too complex, use `MediaCodec` with AMR-WB (12.65 kbps)
- [ ] Audio routing through mesh (chunk STREAM into DATA packets, reassemble on receiver)
  - Create `AUDIO_DATA` chunking: 20ms frames (320 bytes at 8kHz) with sequence numbers
  - Receiver jitter buffer reassembles in order before playback
- [ ] Push-to-talk vs continuous mode UI toggle

---

## Phase 2: Cloud Telemetry & Monitoring

### 2.1 Cloud Architecture

**Principle: Managed, not flooded.** The cloud backend is NOT a message relay. It's a research telemetry platform.

```
                         ┌──────────────┐
[Device A] ──(mesh)──>   │              │   ──> [Web Dashboard]
[Device B] ──(mesh)──>   │  Cloud API   │   ──> [Alerting]
[Device C] ──(WiFi)──>   │  (Firebase)  │   ──> [Log Search]
                         └──────────────┘
```

### 2.2 What Gets Uploaded (and What Doesn't)

**UPLOADED (telemetry & aggregated stats):**

| Data Type | Frequency | Size Estimate | Retention |
|-----------|-----------|---------------|-----------|
| Device registration | Once | ~200B | Permanent |
| Stats snapshot | Every 5 min | ~1KB | 30 days rolling |
| Routing table snapshot | Every 5 min | ~500B | 7 days rolling |
| Connection events (connect/disconnect) | On event | ~200B each | 30 days rolling |
| Error logs (ERROR/WARN only) | On event | ~300B each | 30 days rolling |
| Test mode results | On completion | ~2KB | Permanent |
| Store-forward delivery reports | On delivery | ~200B | 30 days rolling |

**NOT UPLOADED:**

- Chat message content (privacy — never leaves the mesh)
- Full DEBUG/TRACE logs (too voluminous — 30K lines/5min would destroy any cloud budget)
- Audio streams
- File transfer content
- Raw packet payloads

**Estimated volume per device:** ~50KB/hour = ~1.2MB/day = ~36MB/month  
**For 10 test devices:** ~360MB/month (well within Firebase free tier: 1GB storage, 10GB transfer)

### 2.3 Upload Mechanism

```kotlin
// WorkManager periodic job — runs every 15 min when internet available
class TelemetryUploadWorker : CoroutineWorker {
    override suspend fun doWork(): Result {
        // 1. Read stats snapshot from NetworkStats
        // 2. Read ERROR/WARN logs since last upload from Room DB
        // 3. Batch into single JSON payload
        // 4. Upload to Firebase/Supabase
        // 5. Mark uploaded entries (lastUploadTimestamp)
        // 6. Clean up entries older than retention period
        return Result.success()
    }
}
```

**Anti-flooding safeguards:**
- **Rate limit:** Max 1 upload per 5 minutes, even if WorkManager fires more often
- **Batch size cap:** Max 100 log entries per upload (oldest dropped if over limit)
- **Dedup:** Server-side dedup by `(deviceId, timestamp)` composite key
- **Circuit breaker:** If 3 consecutive uploads fail, back off exponentially (5m → 15m → 1h)
- **WiFi-only option:** Setting to restrict uploads to WiFi (no metered data)

### 2.4 Cloud Messages — The Gateway Relay Problem

**Question:** "How would messages through cloud work? How do we ensure it isn't flooding? How do we ensure the DB isn't billions of messages?"

**Design: Cloud Message Relay (for Phase 3 gateway)**

The cloud relay is NOT a message store. It's a **transient router with TTL.**

```
Flow:
1. Device A sends message to Device D (not on local mesh)
2. Gateway X (has internet) forwards to cloud relay
3. Cloud relay checks: is Device D's gateway online?
   - YES → Forward immediately → delete from relay
   - NO → Store temporarily (max 24h TTL)
4. When Device D's gateway comes online, cloud pushes pending messages → delete
```

**Anti-flooding controls:**

| Control | Value | Rationale |
|---------|-------|-----------|
| **Per-device send rate** | Max 60 messages/hour to cloud relay | Prevents DoS |
| **Message TTL in cloud** | 24 hours | Not an archive — just a buffer |
| **Max pending per device** | 100 messages | Oldest dropped when exceeded |
| **Max message size** | 10KB (text only — no files via cloud) | Files stay on mesh |
| **No read history** | Messages deleted after delivery confirm | Privacy + storage |
| **Receive-only devices** | Can receive from cloud but NOT send through it | Configurable flag per device |
| **Total DB cap** | 10,000 messages across all devices | Hard ceiling, FIFO eviction |

**Cost estimate:** At 10KB × 10,000 messages = 100MB. Firebase free tier handles this trivially.

### 2.5 Dashboard (Web)

**Minimum viable dashboard:**
- Device list with online/offline status
- Per-device stats (battery, connections, uptime)
- Mesh topology graph (nodes = devices, edges = connections + RTT)
- Log search/filter (ERROR/WARN only, by device, by time range)
- Test mode results comparison across devices

**Tech stack:** Firebase Hosting + vanilla HTML/JS + Chart.js, or a simple Next.js app.

### 2.6 Implementation Steps

- [ ] Choose cloud platform (Firebase recommended — lowest friction for Android)
- [ ] Define Firestore schema: `devices/{id}`, `stats/{id}/{timestamp}`, `logs/{id}/{timestamp}`
- [ ] Add `TelemetryManager.kt` — batches stats + error logs
- [ ] Add `TelemetryUploadWorker.kt` (WorkManager) — periodic upload with safeguards
- [ ] Add Firebase SDK dependency to `build.gradle.kts`
- [ ] Device registration on first launch (anonymous auth + device metadata)
- [ ] Stats snapshot upload every 5 min (when internet available)
- [ ] Error log upload (batched, max 100/upload)
- [ ] Dashboard: basic device list + stats viewer
- [ ] Dashboard: mesh topology graph
- [ ] Dashboard: log search
- [ ] Dashboard: test results comparison

---

## Phase 3: Internet Gateway, Emergency Features & Mesh Bridging

> **Branch: `main`** — Only started after Phase 2 is fully tested and stable.

### 3.1 Internet Gateway Detection

**How it works:**
1. Device checks `ConnectivityManager` for active internet
2. If internet available, device advertises itself as `GATEWAY` via extended `ROUTE_ANNOUNCE`:
   ```
   ROUTE_ANNOUNCE payload: "destName1:score1,...,__GATEWAY__:1"
   ```
3. Other mesh nodes learn: "I can reach the internet via this neighbor"
4. When a message has no local route AND a gateway is reachable, forward to gateway
5. Gateway encapsulates and sends to cloud relay

**Implementation steps:**
- [ ] `InternetDetector.kt` — monitors ConnectivityManager for internet state changes
- [ ] Extended ROUTE_ANNOUNCE with `__GATEWAY__` flag
- [ ] Gateway routing logic in `forwardPacket()`: no local route → try gateway → cloud relay
- [ ] Cloud relay server (Firebase Cloud Functions or lightweight FastAPI)
- [ ] Gateway registration with cloud relay (WebSocket or MQTT keepalive)
- [ ] Encapsulation protocol: wrap mesh Packet in cloud relay envelope with TTL+routing hints
- [ ] NAT transparency: use WebSocket (works through any NAT)
- [ ] Bidirectional: cloud relay pushes incoming messages to gateway → gateway injects into mesh

### 3.2 Emergency Broadcast System

**Use case:** Natural disaster, no cell service, infrastructure down.

**Features:**
- [ ] `EMERGENCY` packet type with highest routing priority
- [ ] Emergency messages bypass all rate limits and store-forward TTL limits
- [ ] Flood to entire mesh regardless of destination (all nodes receive)
- [ ] **SOS Beacon Mode:**
  - Toggle in UI (big red button)
  - Periodically broadcasts: GPS location + battery level + custom distress message
  - Every 30 seconds while active
  - All peers display SOS alerts prominently with location
- [ ] GPS-tagged messages: every emergency message includes lat/lon if available
- [ ] Priority UI: Emergency messages shown as red banners, not buried in logs
- [ ] Emergency message history persistence (never auto-deleted)

### 3.3 Security Hardening

- [ ] End-to-end encryption for private messages (X25519 Diffie-Hellman key exchange)
  - Key exchange during IDENTITY handshake
  - Per-peer shared secret → AES-256-GCM for message payload
  - Forward secrecy: ratchet keys on each session
- [ ] Message integrity (HMAC-SHA256 on every packet)
  - Prevents tampering during multi-hop relay
- [ ] Peer authentication challenge-response
  - Prevent impersonation: on IDENTITY, sender proves they own the name
  - Optional: shared passphrase for trusted networks
- [ ] Anti-malware / anti-abuse:
  - [ ] Rate limiting per peer (max 100 packets/second, configurable)
  - [ ] Payload size validation (already exists, but add per-type limits)
  - [ ] Blacklist: ability to block a peer by name/ID
  - [ ] Anomaly detection: flag peers that send excessive traffic or malformed packets

---

## Phase 4: Advanced Research Features

> **Branch: `feature/stage-3`** — Research-grade, not required for core submission.

### 4.1 Device Triangulation & Location

**Use case:** "Find my device" within mesh range, or optimize routing based on physical proximity.

**Approach:**
- Collect RTT measurements from 3+ peers
- Convert RTT → estimated distance:
  - WiFi Direct: $d = \frac{RTT \times c}{2}$ (speed of light, ~0.3m/ns)
  - Bluetooth: Much noisier, use statistical averaging over 10+ samples
- Trilateration: solve system of distance equations for 2D position
- Display as radar/map view relative to self

**Alternative (better hardware):**
- Android UWB API (`androidx.core.uwb`) for cm-level ranging if devices support it
- Use RTT as fallback for non-UWB devices

**Routing optimization:**
- Prefer routes through physically closer peers (lower latency)
- Predict link quality based on movement direction (if moving away, preemptively find alternate route)

**Implementation steps:**
- [ ] `LocationEstimator.kt` — trilateration from RTT map
- [ ] `RadarView` composable — shows nearby peers as dots at estimated distances
- [ ] Integrate UWB ranging API (optional, hardware-dependent)
- [ ] Proximity-aware routing score bonus

### 4.2 Mesh Chat Groups

- [ ] Named channels (e.g., "Emergency", "General", "Team-A")
- [ ] Group membership advertised via ROUTE_ANNOUNCE extension
- [ ] Messages to a group flood to all members (identified by group ID prefix)
- [ ] History sync: when a device reconnects, pull missed messages from peers who have them
- [ ] Room DB persistence per channel

### 4.3 Delay-Tolerant Networking (Sneakernet)

- [ ] Extended store-and-forward TTL (configurable up to 7 days)
- [ ] Physical carry detection: if a device enters a new mesh, auto-flush queued messages
- [ ] Encounter logging: track which meshes a device has visited (for DTN analysis)

### 4.4 Distributed File Sharing

- [ ] Content-addressable storage (SHA-256 hash as file ID)
- [ ] File announcement protocol: "I have file <hash>, size <n>"
- [ ] Request-response: any peer can request by hash → routed to nearest holder
- [ ] Chunked parallel download from multiple holders (BitTorrent-style)
- [ ] Dedup: only store one copy per unique hash

### 4.5 On-Device Mesh Health Dashboard

- [ ] Real-time topology graph (Canvas-based, nodes as circles, edges as lines with RTT labels)
- [ ] Historical RTT/throughput graphs per peer (line charts)
- [ ] Battery drain correlation with mesh activity
- [ ] Packet loss heatmap (which peers drop the most traffic)

---

## Technical Debt Backlog

| # | Item | Priority | Status |
|---|------|----------|--------|
| 1 | `fallbackToDestructiveMigration()` → proper Room migrations | Medium | Open |
| 2 | Zero unit tests → need tests for Packet, MessageCache, routing logic | High | Open |
| 3 | Version bump strategy (hardcoded v1.0) | Low | Open |
| 4 | `PROJECT_DOCUMENTATION.md` out of date | Low | Open |
| 5 | `audit_report.md` issues status update | Low | Open |

---

## Completed Work Log

### Audit Fixes (All Fixed — 2026-02-27)

- [x] **CRITICAL: Log state update race** — Atomic `state.copy` inside `updateState`
- [x] **CRITICAL: HeartbeatManager lifecycle** — `ProcessLifecycleOwner` replaces unreliable `onTerminate()`
- [x] **CRITICAL: stopAll() shutdown race** — Scope recreated before map clears
- [x] **HIGH: Poison Reverse** — Route announcements now send score=0 for routes learned from recipient
- [x] **HIGH: Packet MAX_STRING_LENGTH** — Reduced from 1024 to 256 bytes
- [x] **HIGH: AudioTrack.write() return** — Checked, breaks on error
- [x] **MEDIUM: Connection limit race** — `synchronized(routingLock)` around neighbors.size check
- [x] **MEDIUM: MessageCache eviction** — Random sampling O(n) replaces O(n log n) sort
- [x] **MEDIUM: Route score TTL cap** — `minOf(packet.ttl, 10) * 100`
- [x] **LOW: Exit App graceful shutdown** — Delegates to `showExitConfirmationDialog()` instead of bare `finishAffinity()`
- [x] **LOW: Device ID length** — Increased from 8 to 12 characters

### Feature Implementation (Completed)

- [x] P2P mesh networking (P2P_CLUSTER, 4 neighbors max)
- [x] Custom binary packet protocol with DoS-safe deserialization
- [x] Distance-vector routing with scored paths + implicit withdrawal
- [x] IDENTITY packets TTL=0 (prevents forwarding corruption)
- [x] Heartbeat-based liveness (5s, adaptive) + zombie detection
- [x] Store-and-forward (2-hour TTL, Room DB persistence)
- [x] Reconnection queue with exponential backoff
- [x] Text messaging (direct + multi-hop + broadcast)
- [x] Voice PTT streaming (8kHz PCM, adaptive jitter buffer)
- [x] File/image transfer (picker UI + FILE_META metadata + reception handler)
- [x] Per-peer stats (RTT, uptime, traffic) in UI
- [x] CSV log export + Room DB persistent logging (INFO+ to DB)
- [x] Foreground Service for background operation
- [x] Battery monitoring
- [x] Advanced options dialog (hybrid mode, low power, log level)

### Bug Fixes (Completed)

- [x] Ghost routes (implicit withdrawal in ROUTE_ANNOUNCE)
- [x] IDENTITY forwarding corruption (TTL=0 + name mismatch guard)
- [x] Peer stats race condition (`ensurePeerTracked` + `renamePeer`)
- [x] `sendFile()` peer name vs endpoint ID bug
- [x] Log volume reduction (TRACE/DEBUG to logcat only)
- [x] Direct neighbor routing pollution (exclude from routing table)

---

## Immediate Next Steps (Priority Order)

1. **Automated test mode** (Phase 1.1) — enables all other testing
2. **File transfer UI overhaul** (Phase 1.2) — chat screen with message history
3. **Unit tests** (Tech Debt #2) — Packet serialization, MessageCache, routing
4. **Update PROJECT_DOCUMENTATION.md** (Tech Debt #4)
5. **Cloud telemetry implementation** (Phase 2) — Firebase + WorkManager
6. **Cloud dashboard** (Phase 2.5) — web viewer for telemetry data

After Phase 2 is stable → continue on `main` for Phase 3 (Gateway + Emergency). Branch `feature/stage-3` only for Phase 4 (Advanced).
