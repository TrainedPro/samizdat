<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig -->

# UwbRangeDataNtfConfig

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbRangeDataNtfConfig provides configurable range data notification reports for a UWB session.
* It includes a Builder class for creating new instances and a RangeDataNtfConfig interface to represent the selected configuration type.
* Public methods allow retrieval of proximity far and near distances in centimeters, as well as the range data notification config type.



public final class
**UwbRangeDataNtfConfig**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Configurable range data notification reports for a UWB session.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) | | Creates a new instance of `UwbRangeDataNtfConfig` . |
| @interface | [UwbRangeDataNtfConfig.RangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig) | | Represents which range data notification config is selected. |

### Public Method Summary

|  |  |
| --- | --- |
| int | [getNtfProximityFar](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig#getNtfProximityFar()) () Gets the proximity far distance in centimeters. |
| int | [getNtfProximityNear](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig#getNtfProximityNear()) () Gets the proximity near distance in centimeters. |
| int | [getRangeDataNtfConfigType](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig#getRangeDataNtfConfigType()) () Gets the range data notification config type. |

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












## Public Methods

#### public int **getNtfProximityFar** ()

Gets the proximity far distance in centimeters.

#### public int **getNtfProximityNear** ()

Gets the proximity near distance in centimeters.

#### public int **getRangeDataNtfConfigType** ()

Gets the range data notification config type.