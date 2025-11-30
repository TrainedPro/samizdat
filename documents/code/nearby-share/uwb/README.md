<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/package-summary -->

# com.google.android.gms.nearby.uwb

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Annotations define settings and reasons related to UWB ranging, including update rate, slot duration, config ID, ranging suspension reasons, and UWB state change reasons.
* Interfaces provide mechanisms for UWB ranging callbacks, observing UWB availability changes, and interacting with UWB devices to perform ranging.
* Classes describe UWB ranging capabilities, parameters for ranging controllee, ranging measurements and position, UWB addresses and channels, UWB device representation, configurable range data notifications, and status codes for UWB results.

### Annotations

|  |  |
| --- | --- |
| [RangingParameters.RangingUpdateRate](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate) | Update rate settings. |
| [RangingParameters.SlotDuration](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.SlotDuration) | Slot duration settings. |
| [RangingParameters.UwbConfigId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId) | Represents which Config ID should be used. |
| [RangingSessionCallback.RangingSuspendedReason](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason) | Reason why ranging was stopped. |
| [UwbAvailabilityObserver.UwbStateChangeReason](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason) | Reason why UWB state changed |
| [UwbRangeDataNtfConfig.RangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig) | Represents which range data notification config is selected. |

### Interfaces

|  |  |
| --- | --- |
| [RangingSessionCallback](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback) | Callbacks used by startRanging. |
| [UwbAvailabilityObserver](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver) | Observer for UWB availability change events. |
| [UwbClient](/android/reference/com/google/android/gms/nearby/uwb/UwbClient) | Interface for getting UWB capabilities and interacting with nearby UWB devices to perform ranging. |

### Classes

|  |  |
| --- | --- |
| [RangingCapabilities](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities) | Describes UWB ranging capabilities for the current device. |
| [RangingControleeParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters) | Parameters passed to controller for `UwbClient.addControleeWithSessionParams(RangingControleeParameters)` when Provisioned STS individual key is used. |
| [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) | Measurement providing the value and confidence of the ranging. |
| [RangingParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters) | Set of parameters which should be passed to the UWB chip to start ranging. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | Builder for creating `RangingParameters` . |
| [RangingPosition](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition) | Position of a device during ranging. |
| [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) | Represents a UWB address. |
| [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) | Represents the channel which a UWB device is currently active on. |
| [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) | Creates a new instance of `UwbComplexChannel` . |
| [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) | Represents a UWB device. |
| [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) | Configurable range data notification reports for a UWB session. |
| [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) | Creates a new instance of `UwbRangeDataNtfConfig` . |
| [UwbStatusCodes](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes) | Status codes for nearby uwb results. |