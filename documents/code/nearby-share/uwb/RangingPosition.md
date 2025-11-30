<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingPosition -->

# RangingPosition

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingPosition represents the position of a device during ranging.
* It includes constants for RSSI values: RSSI\_MAX, RSSI\_MIN, and RSSI\_UNKNOWN.
* Public methods allow retrieval of azimuth, distance, elapsed time, elevation, and RSSI in dBm.
* The class inherits standard methods from
  `java.lang.Object`
  .



public class
**RangingPosition**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Position of a device during ranging.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [RSSI\_MAX](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#RSSI_MAX) |  |
| int | [RSSI\_MIN](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#RSSI_MIN) |  |
| int | [RSSI\_UNKNOWN](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#RSSI_UNKNOWN) |  |

### Public Method Summary

|  |  |
| --- | --- |
| [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) | [getAzimuth](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#getAzimuth()) () Gets the azimuth angle in degrees of the ranging device, or null if not available. |
| [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) | [getDistance](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#getDistance()) () Gets the distance in meters of the ranging device, or null if not available. |
| long | [getElapsedRealtimeNanos](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#getElapsedRealtimeNanos()) () Returns nanoseconds since boot when the ranging position was taken. |
| [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) | [getElevation](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#getElevation()) () Gets the elevation angle in degrees of the ranging device, or null if not available. |
| int | [getRssiDbm](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#getRssiDbm()) () Returns the measured RSSI in dBm. |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition#toString()) () |

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

#### public static final int **RSSI\_MAX**

Constant Value:

-1

#### public static final int **RSSI\_MIN**

Constant Value:

-127

#### public static final int **RSSI\_UNKNOWN**

Constant Value:

-128







## Public Methods

#### public [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) **getAzimuth** ()

Gets the azimuth angle in degrees of the ranging device, or null if not available.
The range is (-90, 90].

#### public [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) **getDistance** ()

Gets the distance in meters of the ranging device, or null if not available.

#### public long **getElapsedRealtimeNanos** ()

Returns nanoseconds since boot when the ranging position was taken.

#### public [RangingMeasurement](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement) **getElevation** ()

Gets the elevation angle in degrees of the ranging device, or null if not available.
The range is (-90, 90].

#### public int **getRssiDbm** ()

Returns the measured RSSI in dBm.

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()