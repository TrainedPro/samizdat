<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder -->

# UwbRangeDataNtfConfig.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbRangeDataNtfConfig.Builder is a class used to create instances of UwbRangeDataNtfConfig.
* It has a public constructor and several public methods for setting configuration parameters.
* The
  `build()`
  method creates the UwbRangeDataNtfConfig object and can throw exceptions if the configuration is invalid.
* Methods like
  `setNtfProximityFar`
  ,
  `setNtfProximityNear`
  , and
  `setRangeDataConfigType`
  are used to configure the range data notification settings.



public static class
**UwbRangeDataNtfConfig.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Creates a new instance of
`UwbRangeDataNtfConfig`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) | [build](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder#build()) () Builds `UwbRangeDataNtfConfig` . |
| [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) | [setNtfProximityFar](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder#setNtfProximityFar(int)) (int ntfProximityFar) Sets the proximity far distance in centimeters (0 <= near <= far <= 20,000). |
| [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) | [setNtfProximityNear](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder#setNtfProximityNear(int)) (int ntfProximityNear) Sets the proximity near distance in centimeters (0 <= near <= far <= 20,000). |
| [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) | [setRangeDataConfigType](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder#setRangeDataConfigType(int)) (int rangeDataConfigType) Sets the range data notification config type of `UwbRangeDataNtfConfig.RangeDataNtfConfig` . |

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

#### public [UwbRangeDataNtfConfig](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig) **build** ()

Builds
`UwbRangeDataNtfConfig`
.

##### Throws

|  |  |
| --- | --- |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if config type is not in `UwbRangeDataNtfConfig.RangeDataNtfConfig` . |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if (0 <= near <= far <= 20,000) is not satified. |

#### public [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) **setNtfProximityFar** (int ntfProximityFar)

Sets the proximity far distance in centimeters (0 <= near <= far <=
20,000).

#### public [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) **setNtfProximityNear** (int ntfProximityNear)

Sets the proximity near distance in centimeters (0 <= near <= far <=
20,000).

#### public [UwbRangeDataNtfConfig.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.Builder) **setRangeDataConfigType** (int rangeDataConfigType)

Sets the range data notification config type of
`UwbRangeDataNtfConfig.RangeDataNtfConfig`
.