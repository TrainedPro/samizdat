# Feature Roadmap & Use Cases

> Last updated: 2026-02-13

## Current Status (v1.0)

### Working

- P2P mesh networking via Google Nearby Connections (P2P_CLUSTER strategy)
- Text messaging (direct + multi-hop routed)
- Voice streaming (PCM 8kHz mono over STREAM payloads, with adaptive RTT-based jitter buffer)
- Heartbeat-based liveness detection + RTT tracking
- Per-peer stats (RTT, uptime, traffic) in UI
- Store-and-forward message queuing (2-hour TTL)
- Periodic route announcements with implicit withdrawal
- CSV log export with version/device/timestamp metadata
- Room DB persistent logging for post-test analysis

### Known Limitations

- Voice still has residual lag (~200ms buffer helps but BT/WiFi Direct bandwidth under 3+ devices can be constrained)
- File/image transfer UI added (📷 image picker + 📎 file picker in ChatDialog)
- No cloud telemetry — all data stays on-device
- No internet gateway bridging between disconnected meshes
- Single mesh only — no cross-network routing

---

## Phase 1: Stability & Polish (Current Priority)

### 1.1 Remaining Bug Fixes

- [ ] Verify store-and-forward delivery works end-to-end after disconnect/reconnect
- [ ] Test mesh routing with 4+ devices (current testing covers 3)
- [ ] Validate route convergence time when topology changes rapidly
- [ ] Ensure export produces a single CSV per session (currently may produce multiples from old app versions)

### 1.2 Audio Improvements

- [x] Adaptive jitter buffer (scales 100-500ms based on measured peer RTT)
- [x] Sample rate reduced from 16kHz to 8kHz (bandwidth halved: 32KB/s → 16KB/s)
- [ ] Audio codec compression (Opus) — 8kHz PCM is 16KB/s, Opus can do ~6KB/s
- [ ] Push-to-talk vs continuous mode toggle
- [ ] Audio routing through mesh (currently direct-only; relay audio as chunked DATA packets for multi-hop voice)

### 1.3 File & Image Transfer

- [x] Image picker (📷) and file picker (📎) buttons in ChatDialog
- [x] FILE_META packet type for filename/MIME metadata correlation
- [x] FILE payload reception with auto-rename to original filename
- [x] Progress tracking UI (PayloadTransferUpdate already handled)
- [x] Toast notification on file received
- [ ] Thumbnail preview in chat for received images
- [ ] Chunked transfer for large files (>5MB) with resume capability
- [ ] Binary blob transfer API for arbitrary data types

---

## Phase 2: Cloud Telemetry & Monitoring

### 2.1 Cloud Platform Registration

**Goal:** Every device running the app registers with a cloud backend so researchers can monitor mesh behavior remotely.

**Architecture:**

```
[Device A] ──BT/WiFi──> [Device B] ──BT/WiFi──> [Device C]
     │                       │                       │
     └── WiFi/4G ─── [Cloud Backend (Firebase / Custom)] ─── [Dashboard]
```

**Data to upload (when internet available):**

- Device identity (anonymized), app version, OS version
- Battery stats over time (level, temperature, charging state)
- Network stats snapshots (connections, disconnections, RTT history, bytes transferred)
- Routing table snapshots (topology map over time)
- Store-and-forward delivery success rates
- Error/crash reports

**Candidates:**

- **Firebase Realtime DB + Cloud Functions** — easiest for Android, free tier generous
- **Supabase** — open-source alternative, PostgreSQL-backed, good REST API
- **Custom FastAPI backend on a VPS** — full control, cheapest at scale

**Implementation approach:**

1. Background WorkManager job that batches stats every 5 minutes
2. Upload when internet available (opportunistic sync)
3. Deduplicate on server using device ID + timestamp
4. Dashboard (web app) showing live mesh topology, per-device health, historical graphs

### 2.2 Centralized Logging

- [ ] Ship Room DB log entries to cloud in batches
- [ ] Cloud-side log aggregation with search/filter (ELK stack or Loki)
- [ ] Alerting on anomalies (high drop rate, zombie disconnections, etc.)

---

## Phase 3: Internet Gateway & Mesh Bridging

### 3.1 Internet Gateway Node

**Goal:** A device with both mesh connectivity AND internet can act as a gateway, bridging two physically disconnected meshes.

**Architecture:**

```
[Mesh A: Devices 1-3] ──BT──> [Gateway X] ──Internet──> [Gateway Y] ──BT──> [Mesh B: Devices 4-6]
```

**Design:**

- Gateway detects it has internet (ConnectivityManager)
- Registers with cloud relay server (WebSocket or MQTT)
- Forwards mesh packets to cloud when destination is not locally reachable
- Cloud relay routes to the correct gateway based on destination peer ID
- Gateway on the other side injects packet into its local mesh

**Packet flow:**

1. Device 1 sends message to Device 5
2. Device 1's mesh routes it to Gateway X (closest gateway)
3. Gateway X: no local route to Device 5 → forwards to cloud relay
4. Cloud relay: Device 5 is reachable via Gateway Y → forwards
5. Gateway Y injects into Mesh B → routes to Device 5

**Key challenges:**

- Gateway discovery (how does a device know to route to a gateway?)
- NAT traversal for WebSocket connections
- Packet deduplication across internet + mesh paths
- Latency vs reliability trade-offs

### 3.2 Multi-Mesh Topology

- [ ] Gateway advertisement protocol (special ROUTE_ANNOUNCE with `internet=true` flag)
- [ ] Cloud relay server (lightweight — just packet forwarding + device registry)
- [ ] Hybrid routing table: local routes (BT/WiFi) + cloud routes (internet-reachable peers)

---

## Phase 4: Advanced Features & Use Cases

### 4.1 Device Location & Triangulation

**Concept:** Use RTT measurements from multiple peers to estimate relative positions.

**Approach:**

- Collect RTT values from 3+ neighbors
- Convert RTT → approximate distance (speed of light for WiFi, more complex for BT)
- Trilateration to estimate 2D position relative to known anchors
- Display relative positions on a radar/map view

**Limitations:** BT RTT is very noisy and affected by walls/interference. WiFi RTT (802.11mc/FTM) is more accurate but requires hardware support. This is more of a research feature than production-grade.

**Alternative:** Use Android's UWB API (if devices support it) for centimeter-level ranging.

### 4.2 Emergency Broadcast System

**Use case:** Natural disaster, infrastructure down, no cell service.

- One-to-many emergency alerts that flood the entire mesh
- Priority queuing (emergency messages skip store-and-forward TTL limits)
- GPS-tagged messages for location sharing
- SOS beacon mode (periodic broadcast with GPS + battery level)

### 4.3 Mesh-Powered Chat Groups

- Named channels/groups that persist across sessions
- Group membership advertised via routing protocol
- History sync when a device reconnects (pull missed messages from peers)

### 4.4 Sneakernet / Delay-Tolerant Networking

**Use case:** Two meshes never directly connect, but a person physically walks between them carrying a device.

- Device A sends message for mesh B
- Carrier device stores it (store-and-forward with extended TTL)
- When carrier enters mesh B's range, message is delivered
- Essentially DTN (Delay-Tolerant Networking) over BT/WiFi

### 4.5 Distributed File Sharing

- Content-addressable storage (hash-based dedup)
- Peer announces available files via routing protocol extension
- Any device can request a file by hash → routed to nearest holder
- Chunked parallel download from multiple peers (BitTorrent-like)

### 4.6 Mesh Health Dashboard (On-Device)

- Real-time topology graph visualization (nodes + edges)
- Historical RTT/throughput graphs per peer
- Battery drain analysis (correlation with mesh activity)
- Packet loss heatmap

### 4.7 Security & Privacy

- End-to-end encryption for messages (Diffie-Hellman key exchange over mesh)
- Peer authentication (prevent spoofing/impersonation)
- Anonymous routing option (onion-routing-lite for sensitive scenarios)
- Message integrity verification (HMAC)

---

## Realistic Priority Order

| Priority | Feature                              | Effort    | Impact                  |
| -------- | ------------------------------------ | --------- | ----------------------- |
| 1        | Bug fixes + stability (Phase 1.1)    | Low       | Critical                |
| 2        | Image/file transfer UI (Phase 1.3)   | Medium    | High                    |
| 3        | Cloud telemetry (Phase 2.1)          | Medium    | High                    |
| 4        | Audio codec compression (Phase 1.2)  | Medium    | Medium                  |
| 5        | Internet gateway (Phase 3)           | High      | Very High               |
| 6        | Emergency broadcast (Phase 4.2)      | Low       | High                    |
| 7        | Chat groups (Phase 4.3)              | Medium    | Medium                  |
| 8        | Mesh health dashboard (Phase 4.6)    | Medium    | Medium                  |
| 9        | Security/encryption (Phase 4.7)      | High      | Critical (for real use) |
| 10       | Triangulation (Phase 4.1)            | High      | Research/Demo           |
| 11       | DTN sneakernet (Phase 4.4)           | Medium    | Niche                   |
| 12       | Distributed file sharing (Phase 4.5) | Very High | Research                |

---

## Technical Debt to Address

- [ ] Version bump strategy (currently hardcoded v1.0)
- [ ] Automated testing (unit tests for routing logic, integration tests for packet handling)
- [ ] CI/CD pipeline for APK builds
- [ ] ProGuard/R8 optimization for release builds
- [ ] Battery optimization (reduce heartbeat frequency when idle, more aggressive adaptive intervals)
