<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Strategy -->

# Strategy

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Strategy describes a set of strategies for publishing or subscribing for nearby messages.
* The
  `Strategy.Builder`
  can be used if one of the provided strategies doesn't work.
* Constants define various discovery modes, distance types, and time-to-live values for strategies.
* There is a deprecated
  `BLE_ONLY`
  field, and a recommended
  `DEFAULT`
  strategy field available.
* Several public methods are available including
  `equals`
  ,
  `hashCode`
  ,
  `toString`
  , and
  `writeToParcel`
  .



public class
**Strategy**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Describes a set of strategies for publishing or subscribing for nearby messages. If one of
the provided strategies doesn't work, consider using a
`Strategy.Builder`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) | | Builder for `Strategy` . |

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [DISCOVERY\_MODE\_BROADCAST](/android/reference/com/google/android/gms/nearby/messages/Strategy#DISCOVERY_MODE_BROADCAST) | To discover which devices are nearby, broadcast a pairing code for others to scan. |
| int | [DISCOVERY\_MODE\_DEFAULT](/android/reference/com/google/android/gms/nearby/messages/Strategy#DISCOVERY_MODE_DEFAULT) | To discover which devices are nearby, broadcast a pairing code and scan for other devices' pairing codes. |
| int | [DISCOVERY\_MODE\_SCAN](/android/reference/com/google/android/gms/nearby/messages/Strategy#DISCOVERY_MODE_SCAN) | To discover which devices are nearby, scan for other devices' pairing codes. |
| int | [DISTANCE\_TYPE\_DEFAULT](/android/reference/com/google/android/gms/nearby/messages/Strategy#DISTANCE_TYPE_DEFAULT) | Allows the message be exchanged over any distance. |
| int | [DISTANCE\_TYPE\_EARSHOT](/android/reference/com/google/android/gms/nearby/messages/Strategy#DISTANCE_TYPE_EARSHOT) | Allows the message be exchanged within earshot only. |
| int | [TTL\_SECONDS\_DEFAULT](/android/reference/com/google/android/gms/nearby/messages/Strategy#TTL_SECONDS_DEFAULT) | The default time to live in seconds. |
| int | [TTL\_SECONDS\_INFINITE](/android/reference/com/google/android/gms/nearby/messages/Strategy#TTL_SECONDS_INFINITE) | An infinite time to live in seconds. |
| int | [TTL\_SECONDS\_MAX](/android/reference/com/google/android/gms/nearby/messages/Strategy#TTL_SECONDS_MAX) | The maximum time to live in seconds, if not `TTL_SECONDS_INFINITE` . |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | [BLE\_ONLY](/android/reference/com/google/android/gms/nearby/messages/Strategy#BLE_ONLY) | *This field is deprecated. Use `DEFAULT` instead, which is also limited to only BLE.* |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) > | [CREATOR](/android/reference/com/google/android/gms/nearby/messages/Strategy#CREATOR) |  |
| public static final [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | [DEFAULT](/android/reference/com/google/android/gms/nearby/messages/Strategy#DEFAULT) | The default strategy, which is suitable for most applications. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/messages/Strategy#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) other) |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/messages/Strategy#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/Strategy#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/messages/Strategy#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags) |

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

From interface android.os.Parcelable

|  |  |
| --- | --- |
| abstract int | describeContents () |
| abstract void | writeToParcel ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) arg0, int arg1) |







## Constants

#### public static final int **DISCOVERY\_MODE\_BROADCAST**

To discover which devices are nearby, broadcast a pairing code for others to
scan.

Constant Value:

1

#### public static final int **DISCOVERY\_MODE\_DEFAULT**

To discover which devices are nearby, broadcast a pairing code and scan for other
devices' pairing codes. This is equivalent to
`DISCOVERY_MODE_BROADCAST`
|
`DISCOVERY_MODE_SCAN`
.

Constant Value:

3

#### public static final int **DISCOVERY\_MODE\_SCAN**

To discover which devices are nearby, scan for other devices' pairing codes.

Constant Value:

2

#### public static final int **DISTANCE\_TYPE\_DEFAULT**

Allows the message be exchanged over any distance.

Constant Value:

0

#### public static final int **DISTANCE\_TYPE\_EARSHOT**

Allows the message be exchanged within earshot only.

It is recommended that this configuration is used in conjunction with
`DISCOVERY_MODE_BROADCAST`
. This will improve the detection latency.

Constant Value:

1

#### public static final int **TTL\_SECONDS\_DEFAULT**

The default time to live in seconds.

Constant Value:

300

#### public static final int **TTL\_SECONDS\_INFINITE**

An infinite time to live in seconds.

Note: This is currently only supported for subscriptions.

Constant Value:

2147483647

#### public static final int **TTL\_SECONDS\_MAX**

The maximum time to live in seconds, if not
`TTL_SECONDS_INFINITE`
.

Constant Value:

86400



## Fields

#### public static final Strategy **BLE\_ONLY**

**This field is deprecated.**
  
Use
`DEFAULT`
instead, which is also limited to only BLE.

Use only Bluetooth Low Energy to discover nearby devices. Recommended if you are
only interested in messages attached to BLE beacons.

The time to live of this strategy is
`TTL_SECONDS_INFINITE`
and as such it's only supported for subscriptions.

Bluetooth Low Energy is not supported on all Android devices.

##### See Also

* [Bluetooth Low
  Energy | Android Developers](//developer.android.com/guide/topics/connectivity/bluetooth-le.html)

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) > **CREATOR**

#### public static final Strategy **DEFAULT**

The default strategy, which is suitable for most applications.

The default behavior is currently doing broadcasts and scans, using all available
sensors, to discover nearby devices, regardless of distance.






## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) other)

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags)