# Directory Summary: `fastpair/`

This directory contains documentation for the `com.google.android.gms.nearby.fastpair` package, which provides APIs for Fast Pair, specifically focusing on Smart Audio Source Switching (SASS).

## File Details

### `AudioUsage.md`
- **Description**: Annotation identifying the audio usage of a SASS (Smart Audio Source Switching) device.
- **Key Constants**:
    - `CALL` (1): Usage is for calls (supports HEADSET, LE_AUDIO).
    - `MEDIA` (2): Usage is for media (supports A2DP, LE_AUDIO).
    - `UNKNOWN` (0).
    - **Deprecated**: `A2DP` (use `MEDIA`), `HFP` (use `CALL`), `LE_AUDIO` (use `MEDIA` or `CALL`).

### `FastPairClient.md`
- **Description**: Interface to Fast Pair APIs, enabling clients to implement SASS functionalities.
- **Key Methods**:
    - `isSassDeviceAvailable(int audioUsage)`: Checks if a SASS-supported device is available for the given usage.
    - `triggerSassForUsage(int audioUsage)`: Triggers SASS for the given usage.
- **Permissions**: Requires `BLUETOOTH_SCAN` (Android S+), `BLUETOOTH_CONNECT` (Android S+), or `ACCESS_FINE_LOCATION` (deprecated in S).

### `FastPairStatusCodes.md`
- **Description**: Status codes for Fast Pair API results.
- **Key Constants**:
    - `SUCCESS` (0).
    - `FAILED_INVALID_ARGUMENTS` (40502).
    - `FAILED_PERMISSION_DENIED` (40503).
    - `FAILED_NOT_SUPPORTED` (40504).

### `README.md`
- **Description**: Package summary listing annotations, interfaces, and classes.
