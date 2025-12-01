# Directory Summary: `nearby-share/`

This directory contains documentation for the Google Nearby API, which enables communication between nearby devices. It is divided into several key packages.

## Subdirectories

### `connection/`
- **Package**: `com.google.android.gms.nearby.connection`
- **Purpose**: The core **Nearby Connections API** for peer-to-peer discovery and data transfer.
- **Key Components**:
    - `ConnectionsClient`: The main entry point (replaces deprecated `Connections`).
    - `AdvertisingOptions` / `DiscoveryOptions`: Configuration for finding peers.
    - `Payload`: Container for data (Bytes, File, Stream).
    - `Strategy`: Defines connection topology (P2P_CLUSTER, P2P_STAR, P2P_POINT_TO_POINT).
- **Status**: Active and recommended for P2P connections.

### `messages/`
- **Package**: `com.google.android.gms.nearby.messages`
- **Purpose**: The **Nearby Messages API** for publishing and subscribing to small messages.
- **Key Components**:
    - `MessagesClient`: The main entry point.
    - `Message`: The data object.
    - `MessageFilter`: For filtering received messages.
- **Status**: **DEPRECATED**. Will be removed in late 2023. Use `ConnectionsClient` instead.

### `fastpair/`
- **Package**: `com.google.android.gms.nearby.fastpair`
- **Purpose**: APIs for **Fast Pair**, specifically **Smart Audio Source Switching (SASS)**.
- **Key Components**:
    - `FastPairClient`: Interface for SASS operations.
    - `AudioUsage`: Defines audio usage types (Media, Call).
- **Status**: Active, focused on specific Bluetooth Low Energy pairing and switching scenarios.

### `uwb/`
- **Package**: `com.google.android.gms.nearby.uwb`
- **Purpose**: APIs for **Ultra-Wideband (UWB)** ranging and spatial orientation.
- **Key Components**:
    - `UwbClient`: Main entry point for ranging.
    - `RangingParameters`: Configuration for ranging sessions (Config IDs, Update Rates).
    - `RangingPosition`: Result containing Distance, Azimuth, Elevation.
    - `UwbDevice` / `UwbAddress`: Representation of remote devices.
- **Status**: Active, requires devices with UWB hardware.

## Root Files

### `Nearby.md`
- **Description**: The main entry point for the Nearby API.
- **Key Methods**:
    - `getConnectionsClient(Context)`: Returns a `ConnectionsClient`.
    - `getMessagesClient(Context)`: Returns a `MessagesClient` (Deprecated).
    - `getFastPairClient(Context)`: Returns a `FastPairClient`.
    - `getUwbClient(Context)`: Returns a `UwbClient`.

### `DIR_SUMMARY.md`
- **Description**: This file.
