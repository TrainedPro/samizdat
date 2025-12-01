# Directory Summary: `uwb/`

This directory contains documentation for the `com.google.android.gms.nearby.uwb` package, which provides APIs for Ultra-Wideband (UWB) ranging and interaction with nearby UWB devices.

## File Details

### `RangingCapabilities.md`
- **Description**: Describes the UWB ranging capabilities of the device.
- **Key Methods**: `supportsDistance()`, `supportsAzimuthalAngle()`, `supportsElevationAngle()`, `getMinRangingInterval()`, `getSupportedChannels()`, `getSupportedConfigIds()`.

### `RangingControleeParameters.md`
- **Description**: Parameters for adding a controlee to a ranging session, specifically for Provisioned STS individual key cases.
- **Key Methods**: `getAddress()`, `getSubSessionId()`, `getSubSessionKey()`.

### `RangingMeasurement.md`
- **Description**: Represents a single ranging measurement value.
- **Key Method**: `getValue()`.

### `RangingParameters.Builder.md`
- **Description**: Builder for `RangingParameters`.
- **Key Methods**: `addPeerDevice()`, `setComplexChannel()`, `setSessionId()`, `setUwbConfigId()`, `setRangingUpdateRate()`.

### `RangingParameters.RangingUpdateRate.md`
- **Description**: Annotation for ranging update rates.
- **Key Constants**: `AUTOMATIC` (1), `INFREQUENT` (2), `FREQUENT` (3), `UNKNOWN` (0).

### `RangingParameters.SlotDuration.md`
- **Description**: Annotation for slot duration settings.
- **Key Constants**: `DURATION_1_MS` (1), `DURATION_2_MS` (2).

### `RangingParameters.UwbConfigId.md`
- **Description**: Annotation for UWB configuration IDs (FiRa defined).
- **Key Constants**:
    - `CONFIG_ID_1`: Unicast, Static STS, 240ms interval.
    - `CONFIG_ID_2`: One-to-many, Static STS, 200ms interval.
    - `CONFIG_ID_3`: Like ID 1 but no AoA.
    - `CONFIG_ID_4`: Like ID 1 but P-STS enabled.
    - `CONFIG_ID_5`: Like ID 2 but P-STS enabled.
    - `CONFIG_ID_6`: Like ID 3 but P-STS enabled.
    - `CONFIG_ID_7`: Like ID 2 but P-STS individual controlee key.

### `RangingParameters.md`
- **Description**: Parameters required to start a UWB ranging session.
- **Key Methods**: `getComplexChannel()`, `getPeerDevices()`, `getSessionId()`, `getUwbConfigId()`, `isAoaDisabled()`.

### `RangingPosition.md`
- **Description**: Position of a device during ranging.
- **Key Methods**: `getDistance()`, `getAzimuth()`, `getElevation()`, `getRssiDbm()`, `getElapsedRealtimeNanos()`.

### `RangingSessionCallback.RangingSuspendedReason.md`
- **Description**: Reasons for ranging suspension.
- **Key Constants**: `STOPPED_BY_PEER`, `WRONG_PARAMETERS`, `FAILED_TO_START`, `SYSTEM_POLICY`.

### `RangingSessionCallback.md`
- **Description**: Callback interface for ranging session events.
- **Key Methods**: `onRangingInitialized()`, `onRangingResult()`, `onRangingSuspended()`.

### `UwbAddress.md`
- **Description**: Represents a UWB address (MAC address).
- **Key Methods**: `getAddress()` (returns byte array).

### `UwbAvailabilityObserver.UwbStateChangeReason.md`
- **Description**: Reasons for UWB availability state changes.
- **Key Constants**: `REASON_SYSTEM_POLICY`, `REASON_COUNTRY_CODE_ERROR`, `REASON_UNKNOWN`.

### `UwbAvailabilityObserver.md`
- **Description**: Observer for UWB availability changes.
- **Key Method**: `onUwbStateChanged(boolean isAvailable, int reason)`.

### `UwbClient.md`
- **Description**: Main interface for UWB operations.
- **Key Methods**:
    - `startRanging()`, `stopRanging()`.
    - `addControlee()`, `removeControlee()`.
    - `getComplexChannel()`, `getLocalAddress()`.
    - `getRangingCapabilities()`.
    - `isAvailable()`.
    - `subscribeToUwbAvailability()`, `unsubscribeFromUwbAvailability()`.

### `UwbComplexChannel.Builder.md`
- **Description**: Builder for `UwbComplexChannel`.

### `UwbComplexChannel.md`
- **Description**: Represents the channel and preamble index a device is active on.
- **Key Methods**: `getChannel()`, `getPreambleIndex()`.

### `UwbDevice.md`
- **Description**: Represents a remote UWB device.
- **Key Methods**: `getAddress()`, `createForAddress()`.

### `UwbRangeDataNtfConfig.Builder.md`
- **Description**: Builder for `UwbRangeDataNtfConfig`.

### `UwbRangeDataNtfConfig.RangeDataNtfConfig.md`
- **Description**: Annotation for range data notification configurations.
- **Key Constants**: `RANGE_DATA_NTF_ENABLE` (default), `RANGE_DATA_NTF_DISABLE`, `RANGE_DATA_NTF_ENABLE_PROXIMITY_EDGE_TRIG`, `RANGE_DATA_NTF_ENABLE_PROXIMITY_LEVEL_TRIG`.

### `UwbRangeDataNtfConfig.md`
- **Description**: Configuration for range data notifications.
- **Key Methods**: `getRangeDataNtfConfigType()`, `getNtfProximityNear()`, `getNtfProximityFar()`.

### `UwbStatusCodes.md`
- **Description**: Status codes for UWB operations.
- **Key Constants**: `STATUS_OK`, `RANGING_ALREADY_STARTED`, `UWB_SYSTEM_CALLBACK_FAILURE`, `SERVICE_NOT_AVAILABLE`.

### `README.md`
- **Description**: Package summary.
