# Directory Summary: `messages/`

This directory contains documentation for the `com.google.android.gms.nearby.messages` package.
**Status**: **Deprecated**. The Nearby Messages API is slated for removal. Use `ConnectionsClient` instead.

## File Details

### `BleSignal.md`
- **Description**: Represents the BLE signal strength of a message.
- **Key Methods**: `getRssi()` (signal strength in dBm), `getTxPower()` (transmission power at 1 meter).
- **Key Constant**: `UNKNOWN_TX_POWER`.

### `Distance.Accuracy.md`
- **Description**: Annotation defining accuracy levels for distance estimates.
- **Key Constant**: `LOW` (estimated from BLE signal strength).

### `Distance.md`
- **Description**: Represents the distance to a message.
- **Key Methods**: `getMeters()` (distance in meters), `getAccuracy()`, `compareTo()`.
- **Key Constant**: `UNKNOWN` (NaN meters).

### `EddystoneUid.md`
- **Description**: Represents a 16-byte Eddystone UID (10-byte namespace + 6-byte instance).
- **Key Methods**: `getNamespace()`, `getInstance()`, `getHex()`.
- **Key Constants**: `NAMESPACE_LENGTH` (10), `INSTANCE_LENGTH` (6).

### `IBeaconId.md`
- **Description**: Represents an iBeacon ID (Proximity UUID + Major + Minor).
- **Key Methods**: `getProximityUuid()`, `getMajor()`, `getMinor()`.
- **Key Constant**: `LENGTH` (20 bytes).

### `Message.md`
- **Description**: The primary data object shared between devices.
- **Key Properties**: `content` (byte array), `type` (string), `namespace`.
- **Key Constants**: `MAX_CONTENT_SIZE_BYTES` (102,400 bytes), `MAX_TYPE_LENGTH` (32 chars).
- **Key Methods**: `getContent()`, `getType()`, `getNamespace()`.

### `MessageFilter.Builder.md`
- **Description**: Builder for creating `MessageFilter` instances.
- **Key Methods**: `includeEddystoneUids()`, `includeIBeaconIds()`, `includeNamespacedType()`.

### `MessageFilter.md`
- **Description**: Filters which messages are received during a subscription.
- **Key Field**: `INCLUDE_ALL_MY_TYPES` (matches all messages from the same project).

### `MessageListener.md`
- **Description**: Callback interface for receiving messages.
- **Key Methods**: `onFound(Message)`, `onLost(Message)`, `onDistanceChanged(Message, Distance)`, `onBleSignalChanged(Message, BleSignal)`.

### `Messages.md`
- **Description**: **Deprecated** static interface for the Messages API.
- **Key Note**: Replaced by `MessagesClient`.

### `MessagesClient.md`
- **Description**: The main entry point for the Nearby Messages API (Deprecated).
- **Key Methods**:
    - `publish(Message, PublishOptions)`: Broadcasts a message.
    - `subscribe(MessageListener, SubscribeOptions)`: Listens for messages.
    - `unpublish()`, `unsubscribe()`.
    - `registerStatusCallback()`: Monitors permission/status changes.

### `MessagesOptions.Builder.md`
- **Description**: Builder for `MessagesOptions`.
- **Key Method**: `setPermissions(int)` to request specific permissions (e.g., BLE only).

### `MessagesOptions.md`
- **Description**: Configuration options for the API client.
- **Key Field**: `NO_OPTIONS`.

### `NearbyMessagesStatusCodes.md`
- **Description**: Status codes specific to Nearby Messages.
- **Key Constants**: `APP_NOT_OPTED_IN` (user permission needed), `BLE_ADVERTISING_UNSUPPORTED`, `TOO_MANY_PENDING_INTENTS`.

### `NearbyPermissions.md`
- **Description**: Annotation for requested permission scopes.
- **Key Constants**: `BLE` (Bluetooth Low Energy), `MICROPHONE` (Audio), `BLUETOOTH` (Classic).

### `PublishCallback.md`
- **Description**: Callback for publish events.
- **Key Method**: `onExpired()` (called when TTL expires).

### `PublishOptions.Builder.md`
- **Description**: Builder for `PublishOptions`.
- **Key Methods**: `setStrategy()`, `setCallback()`.

### `PublishOptions.md`
- **Description**: Options for publishing a message.
- **Key Methods**: `getStrategy()`, `getCallback()`.

### `README.md`
- **Description**: Package summary.
- **Key Note**: Highlights the deprecation of the package.

### `StatusCallback.md`
- **Description**: Callback for global status changes.
- **Key Method**: `onPermissionChanged(boolean)` (called when user grants/revokes permission).

### `Strategy.Builder.md`
- **Description**: Builder for `Strategy` instances.
- **Key Methods**: `setDiscoveryMode()`, `setTtlSeconds()`, `setDistanceType()`.

### `Strategy.md`
- **Description**: Defines how messages are published/subscribed (discovery mode, TTL, distance).
- **Key Constants**:
    - `DISCOVERY_MODE_BROADCAST` / `SCAN`.
    - `DISTANCE_TYPE_EARSHOT` (close range).
    - `TTL_SECONDS_DEFAULT` (300s), `TTL_SECONDS_INFINITE`.
    - `DEFAULT` (Standard strategy).

### `SubscribeCallback.md`
- **Description**: Callback for subscription events.
- **Key Method**: `onExpired()` (called when subscription TTL ends).

### `SubscribeOptions.Builder.md`
- **Description**: Builder for `SubscribeOptions`.
- **Key Methods**: `setFilter()`, `setStrategy()`, `setCallback()`.

### `SubscribeOptions.md`
- **Description**: Options for subscribing to messages.
- **Key Methods**: `getFilter()`, `getStrategy()`, `getCallback()`.
