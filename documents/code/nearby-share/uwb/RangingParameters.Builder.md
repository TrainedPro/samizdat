<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder -->

# RangingParameters.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `RangingParameters.Builder`
  is a class used to create instances of
  `RangingParameters`
  .
* The builder allows setting various parameters for UWB ranging, including peer devices, channel configuration, session details, and update rates.
* Key methods include
  `addPeerDevice`
  to specify the target for ranging and
  `build`
  to finalize the
  `RangingParameters`
  object.
* You can disable angle of arrival measurement using
  `setIsAoaDisabled`
  and set configuration details like
  `setUwbConfigId`
  and
  `setRangingUpdateRate`
  .



public static class
**RangingParameters.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for creating
`RangingParameters`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [addPeerDevice](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#addPeerDevice(com.google.android.gms.nearby.uwb.UwbDevice)) ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) peerDevice) Sets the peer which should be ranged against. |
| [RangingParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters) | [build](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#build()) () Builds a new instance of `RangingParameters` . |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setComplexChannel(com.google.android.gms.nearby.uwb.UwbComplexChannel)) ( [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) complexChannel) Sets a complex channel. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setIsAoaDisabled](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setIsAoaDisabled(boolean)) (boolean isAoaDisabled) If true, disables angle of arrival measurement. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setRangingUpdateRate](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setRangingUpdateRate(int)) (int rangingUpdateRate) Sets the update rate. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setSessionId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setSessionId(int)) (int sessionId) Sets the session Id. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setSessionKeyInfo](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setSessionKeyInfo(byte[])) (byte[] sessionKeyInfo) Sets the session key Info. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setSlotDuration](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setSlotDuration(int)) (int slotDuration) Sets the slot duration. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setSubSessionId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setSubSessionId(int)) (int subSessionId) Sets the sub-session Id. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setSubSessionKeyInfo](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setSubSessionKeyInfo(byte[])) (byte[] subSessionKeyInfo) Set the sub-session key info. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setUwbConfigId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setUwbConfigId(int)) (int uwbConfigId) Sets the Config ID. |
| [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | [setUwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder#setUwbRangeDataNtfConfig(com.google.android.gms.nearby.uwb.UwbRangeDataNtfConfig)) ( [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) uwbRangeDataNtfConfig) Sets the UWB range data notification config. |

### Inherited Method Summary

From class java.lang.Object

|  |  |
| --- | --- |
| [Object](//developer.android.com/reference/java/lang/Object.html) | clone () |
| boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| void | finalize () |
| final [Class](//developer.android.com/reference/java/lang/Class.html) <?> | getClass () |
| int | hashCode () |
| final void | notify () |
| final void | notifyAll () |
| [String](//developer.android.com/reference/java/lang/String.html) | toString () |
| final void | wait (long arg0, int arg1) |
| final void | wait (long arg0) |
| final void | wait () |









## Public Constructors

#### public **Builder** ()





## Public Methods

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **addPeerDevice** ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) peerDevice)

Sets the peer which should be ranged against.

#### public [RangingParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters) **build** ()

Builds a new instance of
`RangingParameters`
.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setComplexChannel** ( [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) complexChannel)

Sets a complex channel. This should be set if using
`ROLE_CONTROLLEE`
.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setIsAoaDisabled** (boolean isAoaDisabled)

If true, disables angle of arrival measurement.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setRangingUpdateRate** (int rangingUpdateRate)

Sets the update rate. Default is
`RangingParameters.RangingUpdateRate.FREQUENT`
.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setSessionId** (int sessionId)

Sets the session Id.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setSessionKeyInfo** (byte[] sessionKeyInfo)

Sets the session key Info.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setSlotDuration** (int slotDuration)

Sets the slot duration.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setSubSessionId** (int subSessionId)

Sets the sub-session Id.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setSubSessionKeyInfo** (byte[] subSessionKeyInfo)

Set the sub-session key info.

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setUwbConfigId** (int uwbConfigId)

Sets the Config ID. This parameter must be explicitly set

#### public [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) **setUwbRangeDataNtfConfig** ( [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) uwbRangeDataNtfConfig)

Sets the UWB range data notification config.