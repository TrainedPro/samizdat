# ResilientP2PTestbed - Comprehensive Technical Report

## 1. Executive Summary & Vision
**Project**: ResilientP2PTestbed / "Samizdat" Mesh
**Goal**: enable resilient, infrastructure-free communication for emergency responders, protesters, or off-grid communities.
**Core Technology**: Google Nearby Connections (Bluetooth/WiFi Direct) + Custom Dynamic Mesh Routing.

This codebase demonstrates a **fully functional, self-healing mesh network** that supports:
1.  **Multi-Hop Messaging**: Chat across devices that are not directly connected.
2.  **Voice Broadcast**: Push-to-Talk audio streaming.
3.  **Resilience**: Auto-detection of "zombie" links and dynamic re-routing around failures.

---

## 2. Technical Architecture

### 2.1 High-Level Design
The app follows a **Single-Activity, Composition-Root** pattern using Jetpack Compose.
-   **`P2PApplication`**: The Singleton Dependency Injection container. Initializes Managers.
-   **`MainActivity`**: The UI host. Binds to `P2PService` to keep the app alive.
-   **`P2PService`**: A Foreground Service that ensures the mesh radios stay active even when the screen is off.

### 2.2 Core Components (The "Managers")
The logic is encapsulated in `Manager` classes to separate concerns from the UI.

#### **A. `P2PManager.kt` (The Brain)**
This is the most critical file. It wraps the Google Nearby Connections API and adds the Mesh Logic.
*   **Discovery Strategy**: Uses `Strategy.P2P_CLUSTER` to allow M-to-N connections (Cluster topology).
*   **Always-On Mesh**: Unlike standard demos, we keep Advertising and Discovery *active* simultaneously (verified by `stop-on-connect` removal).
*   **Routing Table**: A `ConcurrentHashMap<DestinationId, NextHopId>`.
    *   *Logic*: "If I want to send to User C, and my table says `C -> B`, I send the packet to neighbor B."
*   **Packet Handling**:
    *   `DATA`: Chat messages.
    *   `PING/PONG`: Heartbeats.
    *   `IDENTITY`: Routing updates (broadcasts "I am here" to neighbors).
    *   `VOICE`: Audio streams.

#### **B. `HeartbeatManager.kt` (The Pulse)**
*   **Problem**: Google's API sometimes claims a device is connected when it's actually out of range (stalled socket).
*   **Solution**:
    *   Every X seconds (default 8s), send a `PING` packet.
    *   Update `lastSeen` on `PONG`.
    *   If `now - lastSeen > 30s`, forcibly disconnect. **This is crucial for mesh stability.**

#### **C. `VoiceManager.kt` (The Ears)**
*   **Audio Pipeline**:
    *   **Recording**: `AudioRecord` -> `ParcelFileDescriptor` -> Pipe -> `Nearby.sendPayload`.
    *   **Playback**: `Nearby.onPayloadReceived` -> `InputStream` -> `AudioTrack`.
*   **Nuance**: Uses raw PCM audio. High quality but high bandwidth.

---

## 3. File-by-File Breakdown (Jury Defense Guide)

### **UI Layer (`app/src/main/java/com/fyp/resilientp2p/ui`)**
| File | Purpose |
| :--- | :--- |
| **`P2PComposables.kt`** | **The Main UI**. Contains the `RadarView` (circles), `ChatScreen`, and `Dashboard`. Note the `Canvas` drawing logic for the radar dots. |
| `Theme.kt`, `Color.kt` | Material 3 Theme definitions. |

### **Managers (`.../managers`)**
| File | Purpose |
| :--- | :--- |
| **`P2PManager.kt`** | **Primary Mesh Logic**. Handling payloads, routing tables, and Google API callbacks. |
| **`HeartbeatManager.kt`** | **Watchdog**. Detects dead links. |
| `VoiceManager.kt` | Audio hardware interface. |

### **Data Layer (`.../data`)**
| File | Purpose |
| :--- | :--- |
| `Packet.kt` | **Protocol Definition**. Defines the byte structure of messages (`sourceId`, `destId`, `ttl`, `payload`). |
| `Neighbor.kt` | UI Model for a connected peer. |
| `AppDatabase.kt` | Room Database (SQLite) for storing chat logs. |

### **Service (`.../service`)**
| File | Purpose |
| :--- | :--- |
| `P2PService.kt` | **Persistence**. Keeps the generic `P2PManager` code running in background via Notification. |

---

## 4. Key Nuances & "Secret Sauce"

### **1. The Duplicate Connection Fix**
*   **Scenario**: Device A and Device B see each other and *both* request connection simultaneously.
*   **Issue**: Race condition where one might connect and the other fail, or both partially connect, leading to a "zombie" state.
*   **Fix in `P2PManager`**: We inspect the `endpointName`. If we are already connected to "Device B", we compare the *new* EndpointID vs the *old* one. We gracefully disconnect the stale one and accept the fresh one. This eliminates "death spirals".

### **2. Self-Poisoning Prevention**
*   **Scenario**: In a mesh, your own broadcast ("I am A!") often bounces back to you via Neighbor B.
*   **Risk**: If you process it naively, you might think "Oh, the path to User A (myself) is via Neighbor B!" creating a routing loop.
*   **Fix**: `if (packet.sourceId == localUsername) return;` (Drop it immediately).

### **3. The "8012" Heuristic**
*   **Scenario**: Google API throws status code `8012` (Endpoint IO Error) when the radio stack is jammed or the peer silently vanished.
*   **Fix**: We specifically catch `8012` in `onFailure` and trigger an immediate `disconnect()`. This forces the radio stack to reset that handle, allowing instant reconnection.

---

## 5. Known Limitations (Defense against "Why isn't it perfect?")

1.  **Bandwidth Decay**:
    *   *Defense*: "Mesh networks inherently lose standard bandwidth by 50% per hop due to half-duplex radios. We optimized for *reachability*, not 4K video."
2.  **Connection Cap**:
    *   *Defense*: "The Android hardware stack limits us to ~4-6 direct connections. Our Cluster strategy mitigates this by allowing those 4 people to connect to 4 others, forming a tree."
3.  **Discovery Latency**:
    *   *Defense*: "Discovery can take 5-10 seconds. This is a limitation of the Bluetooth Low Energy (BLE) scanning interval to save battery."

## 6. Operation Guide
1.  **Start**: Give permissions.
2.  **Wait**: Radar shows dots.
3.  **Connect**: Auto-connect is OFF by default (Manual Mode). Tap a dot.
    *   *Note*: If "Hybrid Mode" is checked, it will try to auto-connect.
4.  **Chat**: Messages hop automatically.
