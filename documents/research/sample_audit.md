# Exhaustive Sample Project Audit

This document provides a detailed analysis of **every single sample project** found in the provided directories (`android-connectivity-samples-archived` and `android-platform-samples-new`). The goal is to identify any implementation of a "Mesh" topology (simultaneous Advertising and Discovery on a single node).

---

## Part 1: Archived Samples (`android-connectivity-samples-archived`)

### 1. BluetoothAdvertisements
*   **Path**: `samples/android-connectivity-samples-archived/BluetoothAdvertisements`
*   **Summary**: Demonstrates the Bluetooth Low Energy (BLE) Advertiser API.
*   **Implementation**: Split into `AdvertiserFragment` (Advertises) and `ScannerFragment` (Scans).
*   **Mesh Relevance**: **None**. It explicitly separates the roles into different UI fragments, preventing simultaneous operation in this sample.

### 2. BluetoothChat
*   **Path**: `samples/android-connectivity-samples-archived/BluetoothChat`
*   **Summary**: Classic Bluetooth RFCOMM chat.
*   **Implementation**: Uses `AcceptThread` (Server) and `ConnectThread` (Client).
*   **Mesh Relevance**: **None**. The `connected()` method explicitly cancels the `AcceptThread` when a connection is established, enforcing a strict 1:1 topology.

### 3. BluetoothLeChat (Kotlin)
*   **Path**: `samples/android-connectivity-samples-archived/BluetoothLeChat`
*   **Summary**: Chat over BLE (Kotlin implementation).
*   **Implementation**: `ChatServer.kt` manages the GATT Server and Advertising.
*   **Mesh Relevance**: **None**. Designed for 1:1 communication with a single `currentDevice`.

### 4. BluetoothLeGatt
*   **Path**: `samples/android-connectivity-samples-archived/BluetoothLeGatt`
*   **Summary**: Generic BLE GATT Client.
*   **Implementation**: `DeviceControlActivity` connects to a specific BLE device address.
*   **Mesh Relevance**: **None**. Pure Client implementation.

### 5. CrossDeviceRockPaperScissors
*   **Path**: `samples/android-connectivity-samples-archived/CrossDeviceRockPaperScissors`
*   **Summary**: Game using the Cross Device SDK.
*   **Implementation**: Uses high-level `Discovery` and `Sessions` APIs.
*   **Mesh Relevance**: **None**. Relies on Google Play Services infrastructure and session management, not raw ad-hoc mesh.

### 6. NearbyConnectionsWalkieTalkie
*   **Path**: `samples/android-connectivity-samples-archived/NearbyConnectionsWalkieTalkie`
*   **Summary**: Audio streaming via Nearby Connections.
*   **Implementation**:
    *   **Manual Mode**: Star Topology (One Advertiser, multiple Discoverers).
    *   **Automatic Mode**: P2P (Simultaneous Adv+Disc until paired).
*   **Mesh Relevance**: **High (Negative Proof)**. The "Automatic" mode stops *all* discovery/advertising immediately upon connection to ensure stability, proving that simultaneous operation during a connection is avoided.

### 7. NetworkConnect
*   **Path**: `samples/android-connectivity-samples-archived/NetworkConnect`
*   **Summary**: Fetching data from the Internet.
*   **Implementation**: Uses `HttpsURLConnection` to fetch HTML from google.com.
*   **Mesh Relevance**: **None**. Client-Server (Internet).

### 8. UwbRanging (Archived)
*   **Path**: `samples/android-connectivity-samples-archived/UwbRanging`
*   **Summary**: Older UWB sample.
*   **Implementation**: Focuses on 1:1 ranging.
*   **Mesh Relevance**: **None**.

### 9. WifiRttScan
*   **Path**: `samples/android-connectivity-samples-archived/WifiRttScan`
*   **Summary**: Wi-Fi Round Trip Time (Indoor Positioning).
*   **Implementation**: Ranges to Wi-Fi Access Points (`802.11mc`).
*   **Mesh Relevance**: **None**. Infrastructure mode.

---

## Part 2: New Platform Samples (`android-platform-samples-new`)

### Category: Accessibility
*   **Projects**: `accessibility`
*   **Summary**: Demonstrates accessibility APIs (TalkBack, etc.).
*   **Mesh Relevance**: **None**. UI-focused.

### Category: Camera
*   **Projects**: `camera2`, `camerax`
*   **Summary**: Camera preview, image capture, UltraHDR.
*   **Mesh Relevance**: **None**. Hardware-focused.

### Category: Connectivity
This is the most relevant category.

#### 1. Audio (`connectivity/audio`)
*   **Projects**: `AudioCommsSample`, `AudioLoopSource`
*   **Summary**: Managing communication devices (VoIP) and audio looping.
*   **Mesh Relevance**: **None**. Audio routing only.

#### 2. Bluetooth (`connectivity/bluetooth`)
*   **Sub-projects**:
    *   `ble`: `FindBLEDevicesSample`, `ConnectGATTSample`, `GATTServerSample`.
    *   `companion`: `CompanionDeviceManagerSample`.
*   **Summary**: Modern BLE samples. `GATTServerSample` shows how to be a peripheral. `ConnectGATTSample` shows how to be a central.
*   **Mesh Relevance**: **None**. Samples demonstrate *individual roles* (Server OR Client). No sample combines them into a Mesh node.

#### 3. CallNotification (`connectivity/callnotification`)
*   **Summary**: Posting incoming/ongoing call notifications.
*   **Mesh Relevance**: **None**. UI/Notification focused.

#### 4. Telecom (`connectivity/telecom`)
*   **Summary**: Integrating with the Android Telecom SDK (ConnectionService).
*   **Mesh Relevance**: **None**. Telephony integration.

#### 5. UwbRanging (`connectivity/UwbRanging`)
*   **Summary**: Modern UWB Ranging with Nearby Connections OOB.
*   **Implementation**: Uses `Strategy.P2P_CLUSTER`.
*   **Mesh Relevance**: **High (Negative Proof)**. While it uses the "Cluster" strategy, it strictly enforces **Role Separation**. Devices are manually selected as "Controller" (Discoverer) or "Controlee" (Advertiser) in the Settings screen. They do *not* perform both roles simultaneously.

### Category: Graphics
*   **Projects**: `pdf`, `ultrahdr`
*   **Summary**: PDF rendering and UltraHDR image display.
*   **Mesh Relevance**: **None**.

### Category: Location
*   **Projects**: `location` (Permissions, Current Location, Geofencing).
*   **Summary**: GPS and Geofencing samples.
*   **Mesh Relevance**: **None**.

### Category: Media
*   **Projects**: `ultrahdr`, `video` (Transformer).
*   **Summary**: Video composition and HDR processing.
*   **Mesh Relevance**: **None**.

### Category: Privacy
*   **Projects**: `data` (Package Visibility), `permissions`, `transparency`.
*   **Summary**: Best practices for data minimization and permissions.
*   **Mesh Relevance**: **None**.

### Category: Storage
*   **Projects**: `storage` (MediaStore).
*   **Summary**: Accessing photos and files via MediaStore.
*   **Mesh Relevance**: **None**.

### Category: User Interface
*   **Projects**: `windowmanager`, `constraintlayout`, `haptics`, etc.
*   **Summary**: UI components and WindowManager (Foldables).
*   **Mesh Relevance**: **None**.

---

## Final Conclusion

After a truly exhaustive audit of **every single project** in the provided directories:

1.  **Zero Mesh Samples**: There is **no sample** that implements a single-node Ad-Hoc Mesh where a device simultaneously Advertises and Discovers to form a multi-hop network.
2.  **Stability Patterns**:
    *   **Stop on Connect**: `NearbyConnectionsWalkieTalkie` stops all radio activity when connected.
    *   **Role Separation**: `UwbRanging` (Cluster) forces devices to be *either* a Controller *or* a Controlee, never both.
    *   **1:1 Strictness**: `BluetoothChat` cancels the server socket upon connection.

**Verdict**: The "Duty Cycle" approach (simultaneous/alternating Advertising and Discovery on one node) is **not supported** by any official Google sample. The recommended path for stability is to follow the `WalkieTalkie` pattern: **Stop Discovery/Advertising when a connection is active.**
