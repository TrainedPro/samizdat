# Directory Summary: `connection/`

This directory contains documentation for the `com.google.android.gms.nearby.connection` package, which provides the core Nearby Connections API for peer-to-peer discovery and data transfer.

## File Details

### `AdvertisingOptions.Builder.md`
- **Description**: Builder for `AdvertisingOptions`.
- **Key Methods**: `setStrategy()`, `setConnectionType()` (replaces deprecated `setDisruptiveUpgrade`), `setLowPower()`.

### `AdvertisingOptions.md`
- **Description**: Configuration options for `startAdvertising`.
- **Key Methods**: `getStrategy()`, `getConnectionType()`, `getLowPower()`.

### `AppIdentifier.md`
- **Description**: **Deprecated**. Identifier for an application (package name).
- **Key Method**: `getIdentifier()`.

### `AppMetadata.md`
- **Description**: **Deprecated**. Metadata about an application.
- **Key Method**: `getAppIdentifiers()`.

### `BandwidthInfo.Quality.md`
- **Description**: Annotation defining bandwidth quality levels.
- **Key Constants**: `HIGH` (6-60 MBps), `MEDIUM` (60-200 KBps), `LOW` (5 KBps), `UNKNOWN`.

### `BandwidthInfo.md`
- **Description**: Information about connection bandwidth.
- **Key Method**: `getQuality()`.

### `ConnectionInfo.md`
- **Description**: Information about a connection being initiated.
- **Key Methods**: `getAuthenticationDigits()` (4-digit token), `getEndpointName()`, `isIncomingConnection()`, `getEndpointInfo()`.

### `ConnectionLifecycleCallback.md`
- **Description**: Callback for connection lifecycle events.
- **Key Methods**:
    - `onConnectionInitiated(String, ConnectionInfo)`: Must accept/reject here.
    - `onConnectionResult(String, ConnectionResolution)`: Result of accept/reject.
    - `onDisconnected(String)`.
    - `onBandwidthChanged(String, BandwidthInfo)`.

### `ConnectionOptions.Builder.md`
- **Description**: Builder for `ConnectionOptions`.
- **Key Methods**: `setConnectionType()`, `setLowPower()`.

### `ConnectionOptions.md`
- **Description**: Options for `requestConnection`.
- **Key Methods**: `getConnectionType()`, `getLowPower()`.

### `ConnectionResolution.md`
- **Description**: Result of a connection request.
- **Key Method**: `getStatus()`.

### `Connections.MessageListener.md`
- **Description**: **Deprecated** listener for messages. Replaced by `PayloadCallback`.

### `Connections.StartAdvertisingResult.md`
- **Description**: Result when advertising starts.
- **Key Method**: `getLocalEndpointName()`.

### `Connections.md`
- **Description**: **Deprecated** static interface. Replaced by `ConnectionsClient`.

### `ConnectionsClient.md`
- **Description**: The main entry point for the Nearby Connections API.
- **Key Methods**:
    - `startAdvertising()`, `stopAdvertising()`.
    - `startDiscovery()`, `stopDiscovery()`.
    - `requestConnection()`, `acceptConnection()`, `rejectConnection()`.
    - `sendPayload()`, `cancelPayload()`.
    - `disconnectFromEndpoint()`.

### `ConnectionsStatusCodes.md`
- **Description**: Status codes for API results.
- **Key Constants**:
    - `STATUS_OK`.
    - `STATUS_ALREADY_ADVERTISING`, `STATUS_ALREADY_DISCOVERING`.
    - `STATUS_CONNECTION_REJECTED`.
    - `MISSING_PERMISSION_*` (Bluetooth, Location, Wifi).

### `DiscoveredEndpointInfo.md`
- **Description**: Info about a discovered remote endpoint.
- **Key Methods**: `getEndpointName()`, `getServiceId()`, `getEndpointInfo()`.

### `DiscoveryOptions.Builder.md`
- **Description**: Builder for `DiscoveryOptions`.
- **Key Methods**: `setStrategy()`, `setLowPower()`.

### `DiscoveryOptions.md`
- **Description**: Configuration options for `startDiscovery`.
- **Key Methods**: `getStrategy()`, `getLowPower()`.

### `EndpointDiscoveryCallback.md`
- **Description**: Callback for discovery events.
- **Key Methods**: `onEndpointFound()`, `onEndpointLost()`.

### `Payload.File.md`
- **Description**: Represents a file payload.
- **Key Methods**: `asUri()`, `asParcelFileDescriptor()`, `getSize()`.

### `Payload.Stream.md`
- **Description**: Represents a stream payload.
- **Key Methods**: `asInputStream()`, `asParcelFileDescriptor()`.

### `Payload.Type.md`
- **Description**: Annotation for payload types.
- **Key Constants**: `BYTES` (1), `FILE` (2), `STREAM` (3).

### `Payload.md`
- **Description**: The container for data transferred between devices.
- **Key Methods**: `fromBytes()`, `fromFile()`, `fromStream()`, `getType()`, `getId()`.

### `PayloadCallback.md`
- **Description**: Callback for payload transfer events.
- **Key Methods**: `onPayloadReceived()`, `onPayloadTransferUpdate()`.

### `PayloadTransferUpdate.Builder.md`
- **Description**: Builder for `PayloadTransferUpdate`.

### `PayloadTransferUpdate.Status.md`
- **Description**: Status of a payload transfer.
- **Key Constants**: `SUCCESS`, `FAILURE`, `IN_PROGRESS`, `CANCELED`.

### `PayloadTransferUpdate.md`
- **Description**: Progress update for a transfer.
- **Key Methods**: `getBytesTransferred()`, `getTotalBytes()`, `getStatus()`, `getPayloadId()`.

### `README.md`
- **Description**: Package summary.

### `Strategy.Builder.md`
- **Description**: Builder for `Strategy`.

### `Strategy.md`
- **Description**: Connection strategy (topology).
- **Key Constants**:
    - `P2P_CLUSTER` (M-to-N, loose topology).
    - `P2P_STAR` (1-to-N, star topology).
    - `P2P_POINT_TO_POINT` (1-to-1, high bandwidth).
