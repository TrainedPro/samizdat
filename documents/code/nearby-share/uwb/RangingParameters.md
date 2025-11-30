<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingParameters -->

# RangingParameters

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingParameters defines the parameters needed to start UWB ranging.
* It includes nested classes for builder, ranging update rate, slot duration, and UWB configuration ID.
* Key methods provide access to the UWB complex channel, peer devices, ranging update rate, session IDs, session key info, slot duration, UWB config ID, and AoA status.
* Several constants are defined, including default values for session ID and slot duration.



public class
**RangingParameters**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Set of parameters which should be passed to the UWB chip to start ranging.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [RangingParameters.Builder](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.Builder) | | Builder for creating `RangingParameters` . |
| @interface | [RangingParameters.RangingUpdateRate](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate) | | Update rate settings. |
| @interface | [RangingParameters.SlotDuration](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.SlotDuration) | | Slot duration settings. |
| @interface | [RangingParameters.UwbConfigId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId) | | Represents which Config ID should be used. |

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [SESSION\_ID\_UNSET](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#SESSION_ID_UNSET) |  |
| int | [SLOT\_DURATION\_DEFAULT](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#SLOT_DURATION_DEFAULT) |  |
| int | [SUB\_SESSION\_ID\_UNSET](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#SUB_SESSION_ID_UNSET) |  |

### Public Method Summary

|  |  |
| --- | --- |
| [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) | [getComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getComplexChannel()) () Gets the UWB complex channel. |
| [List](//developer.android.com/reference/java/util/List.html) < [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) > | [getPeerDevices](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getPeerDevices()) () The peers to perform ranging with. |
| int | [getRangingUpdateRate](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getRangingUpdateRate()) () Gets the configured `RangingParameters.RangingUpdateRate` |
| int | [getSessionId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getSessionId()) () Gets the ID of the ranging session. |
| byte[] | [getSessionKeyInfo](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getSessionKeyInfo()) () Gets the session key info to use for the ranging. |
| int | [getSlotDuration](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getSlotDuration()) () Gets the slot duration. |
| int | [getSubSessionId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getSubSessionId()) () Gets the ID of the ranging sub-session. |
| byte[] | [getSubSessionKeyInfo](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getSubSessionKeyInfo()) () Gets the sub-session key info to use for the ranging. |
| int | [getUwbConfigId](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getUwbConfigId()) () Gets the UWB configuration ID. |
| [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) | [getUwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#getUwbRangeDataNtfConfig()) () Range data notification configuration (default = enabled). |
| boolean | [isAoaDisabled](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters#isAoaDisabled()) () Shows whether Angle-of-Arrival (AoA) is enabled or not. |

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







## Constants

#### public static final int **SESSION\_ID\_UNSET**

Constant Value:

0

#### public static final int **SLOT\_DURATION\_DEFAULT**

Constant Value:

2

#### public static final int **SUB\_SESSION\_ID\_UNSET**

Constant Value:

0







## Public Methods

#### public [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) **getComplexChannel** ()

Gets the UWB complex channel. It's optional for
`ROLE_CONTROLLER`
. It
should be set if device type is
`ROLE_CONTROLEE`
.

#### public [List](//developer.android.com/reference/java/util/List.html) < [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) > **getPeerDevices** ()

The peers to perform ranging with. If using unicast, length should be 1.

#### public int **getRangingUpdateRate** ()

Gets the configured
`RangingParameters.RangingUpdateRate`

#### public int **getSessionId** ()

Gets the ID of the ranging session. If the value is SESSION\_ID\_UNSET (0), it will be
created from the hash of controller address and complex channel values.

The same session IDs should be used at both ends (controller and controlee).

#### public byte[] **getSessionKeyInfo** ()

Gets the session key info to use for the ranging. If the profile uses STATIC STS,
this byte array is 8-byte long with first two bytes as Vendor ID and next six bytes as
STATIC STS IV. If the profile uses PROVISIONED STS, this byte array is 16 or 32-byte
long which represent session key.

The same session keys should be used at both ends (controller and controlee).

#### public int **getSlotDuration** ()

Gets the slot duration.

#### public int **getSubSessionId** ()

Gets the ID of the ranging sub-session. This value should be set when the profile
uses PROVISIONED STS individual responder cases. If the profile uses other STS, it
should remain SUB\_SESSION\_ID\_UNSET (0).

#### public byte[] **getSubSessionKeyInfo** ()

Gets the sub-session key info to use for the ranging. This byte array is 16 or
32-byte long when the profile uses PROVISIONED STS individual responder cases. If the
profile uses other STS, it should remain null.

#### public int **getUwbConfigId** ()

Gets the UWB configuration ID. The ID specifies one fixed set of FiRa UCI
parameters.

#### public [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) **getUwbRangeDataNtfConfig** ()

Range data notification configuration (default = enabled).

#### public boolean **isAoaDisabled** ()

Shows whether Angle-of-Arrival (AoA) is enabled or not.