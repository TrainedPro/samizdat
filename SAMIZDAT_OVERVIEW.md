w# Samizdat — Technology & Differentiation Overview

## What is Samizdat?

Samizdat is a **resilient peer-to-peer messaging application** for Android that works even when traditional internet infrastructure fails (natural disasters, outages, censorship). It creates a mesh network between nearby phones using Bluetooth and Wi-Fi, and bridges isolated meshes over the internet when available.

---

## Technologies Used

### 1. Google Nearby Connections API
- **What it does**: Creates direct BLE (Bluetooth Low Energy) and Wi-Fi Direct connections between nearby Android devices — no router, no internet needed.
- **Why we used it**: No custom hardware required. Every modern Android phone already has BLE and Wi-Fi built in.
- **Role in app**: Forms the core local mesh — devices within ~30m of each other connect automatically.

### 2. Multi-Hop Routing (Custom Implementation)
- **What it does**: Messages can travel through intermediary devices to reach a destination that isn't directly connected.
- **Why we built it**: Google Nearby only gives you point-to-point links. We wrote our own routing table with hop counting to extend the mesh range.
- **Role in app**: Device A → Device B → Device C (B relays the message even if A and C can't see each other directly).

### 3. MQTT via HiveMQ Cloud Broker
- **What it does**: A lightweight publish/subscribe messaging protocol over the internet.
- **Why we used it**: Ultra-low overhead, designed for unreliable networks, free public broker available.
- **Role in app**: Acts as the **Cloud Bridge** — gateways in different meshes subscribe to each other and relay packets across the internet in real time.

### 4. Firebase Firestore (Store-and-Forward)
- **What it does**: A cloud NoSQL database that stores messages persistently.
- **Why we used it**: MQTT is live only — if the destination gateway is offline, the message is lost. Firestore persists it for 48 hours.
- **Role in app**: When a gateway comes back online, it fetches any messages it missed. Readable fields (`from`, `to`, `message`) are stored — not opaque binary blobs.

### 5. Jetpack Compose (UI)
- **What it does**: Android's modern declarative UI toolkit.
- **Role in app**: All screens — dashboard, mesh contacts, remote meshes, chat bubbles, logs.

---

## Architecture: The Three-Layer Stack

```
┌─────────────────────────────────────┐
│  Layer 3: Firestore (48h TTL)       │  ← Store-and-Forward fallback
│  "What if the gateway was offline?" │
├─────────────────────────────────────┤
│  Layer 2: MQTT Cloud Bridge         │  ← Real-time cross-mesh relay
│  "Different cities, same app"       │
├─────────────────────────────────────┤
│  Layer 1: BLE/WiFi Mesh (Nearby)    │  ← Zero-infrastructure local mesh
│  "No internet? No problem."         │
└─────────────────────────────────────┘
```

Each layer is a fallback for the one below it. The app uses the fastest available path.

---

## How We Differ from Existing Apps

| Feature | Samizdat | Bridgefy | Meshtastic | Briar | goTenna |
|---------|----------|----------|------------|-------|---------|
| No special hardware | ✅ | ✅ | ❌ LoRa radio | ✅ | ❌ RF device |
| Multi-hop routing | ✅ | ✅ (flawed) | ✅ | ✅ | ✅ |
| Cloud bridge | ✅ MQTT | ✅ proprietary | ✅ MQTT | ❌ | ✅ |
| Store-and-forward | ✅ Firestore | ❌ | ❌ | ✅ | ✅ |
| Open source | ✅ | ❌ | ✅ | ✅ | ❌ |
| Cross-mesh peer visibility | ✅ | ❌ | ❌ | ❌ | ❌ |
| Per-device addressability | ✅ | ❌ | ❌ | ❌ | ❌ |

### Key Differentiators

**1. No hardware needed**
Unlike Meshtastic (LoRa radio ~$30–$60) or goTenna (dedicated RF device), Samizdat runs on any Android phone manufactured after 2016.

**2. Tri-layer redundancy**
No other open-source app combines BLE mesh + MQTT bridge + Firestore persistence in a single unified stack. Most pick two of three.

**3. Cross-mesh peer visibility**
When two meshes connect via the cloud bridge, every device in each mesh can see and message individual devices in the other mesh — not just the gateway. Bridgefy, Meshtastic, and Briar treat remote meshes as opaque.

**4. Per-device addressability over cloud**
You can send a message to "Alice" in a remote city's mesh. The message routes: `Your device → MQTT → Remote gateway → BLE → Alice`. The sender addresses Alice directly, not the gateway.

**5. Bridgefy had security flaws**
Academic research (Martin et al., 2021) showed Bridgefy's encryption was broken. Samizdat uses standard TLS (HiveMQ) + Firebase Auth for transport security.

---

## Research Angle

This work falls under **Delay-Tolerant Networking (DTN)** — a research area focused on communication in environments with intermittent connectivity. Unlike most DTN research which is simulation-based, Samizdat is a **real working implementation** on commodity hardware.

**Potential contributions for a research paper:**
- Novel tri-layer architecture combining local BLE mesh, MQTT relay, and Firestore persistence
- Cross-mesh peer discovery and direct addressability protocol
- Empirical evaluation: delivery rate, end-to-end latency, store-and-forward success rate on real devices

**Suggested target venues:** IEEE MASS, IEEE Access, ACM MobiSys (workshop)
