<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities -->

# RangingCapabilities

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingCapabilities describes the UWB ranging capabilities for the current device.
* It includes constants for default values if the system API doesn't provide them, such as minimum slot duration, minimum ranging interval, supported channel, and range data notification config.
* It also includes fields for default supported lists like ranging update rates, slot durations, and config ids if the system API doesn't provide them.
* Public methods are available to retrieve supported features like minimum ranging interval, supported channels, config ids, notification configs, ranging update rates, and slot durations.
* Additional public methods indicate support for background ranging, azimuthal and elevation angle of arrival, distance ranging, and ranging interval reconfigure.



public class
**RangingCapabilities**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Describes UWB ranging capabilities for the current device.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| boolean | [DEFAULT\_SUPPORTS\_RANGING\_INTERVAL\_RECONFIGURE](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#DEFAULT_SUPPORTS_RANGING_INTERVAL_RECONFIGURE) | Ranging interval reconfigure is not supported by default if the system API doesn't provide. |
| float | [FIRA\_DEFAULT\_MIN\_SLOT\_DURATION\_MS](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#FIRA_DEFAULT_MIN_SLOT_DURATION_MS) | Default supported minimum slot duration if the system API doesn't provide it. |
| int | [FIRA\_DEFAULT\_RANGING\_INTERVAL\_MS](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#FIRA_DEFAULT_RANGING_INTERVAL_MS) | Default minimum ranging interval if the system API doesn't provide it. |
| int | [FIRA\_DEFAULT\_SUPPORTED\_CHANNEL](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#FIRA_DEFAULT_SUPPORTED_CHANNEL) | Default supported channel if the system API doesn't provide it. |
| int | [RANGE\_DATA\_NTF\_ENABLE](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#RANGE_DATA_NTF_ENABLE) | Default range data notification config if the system API doesn't provide it. |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [DEFAULT\_SUPPORTED\_RANGING\_UPDATE\_RATES](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#DEFAULT_SUPPORTED_RANGING_UPDATE_RATES) | Default supported ranging interval if the system API doesn't provide it. |
| public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [DEFAULT\_SUPPORTED\_SLOT\_DURATIONS](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#DEFAULT_SUPPORTED_SLOT_DURATIONS) | Default supported slot duration if the system API doesn't provide it. |
| public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [FIRA\_DEFAULT\_SUPPORTED\_CONFIG\_IDS](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#FIRA_DEFAULT_SUPPORTED_CONFIG_IDS) | Default supported config id if the system API doesn't provide it. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| int | [getMinRangingInterval](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getMinRangingInterval()) () Gets the minimum supported ranging interval in milliseconds. |
| [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [getSupportedChannels](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getSupportedChannels()) () Gets the supported channel number. |
| [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [getSupportedConfigIds](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getSupportedConfigIds()) () Gets the supported config ids. |
| [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [getSupportedNtfConfigs](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getSupportedNtfConfigs()) () Gets the supported range data notification configs. |
| [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [getSupportedRangingUpdateRates](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getSupportedRangingUpdateRates()) () Gets the supported ranging interval in milliseconds. |
| [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > | [getSupportedSlotDurations](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#getSupportedSlotDurations()) () Gets the supported slot duration in milliseconds. |
| boolean | [hasBackgroundRangingSupport](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#hasBackgroundRangingSupport()) () Whether a ranging session can be started when the app is in background. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#hashCode()) () |
| boolean | [supportsAzimuthalAngle](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#supportsAzimuthalAngle()) () Whether azimuthal angle of arrival is supported. |
| boolean | [supportsDistance](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#supportsDistance()) () Whether distance ranging is supported. |
| boolean | [supportsElevationAngle](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#supportsElevationAngle()) () Whether elevation angle of arrival is supported. |
| boolean | [supportsRangingIntervalReconfigure](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#supportsRangingIntervalReconfigure()) () Whether ranging interval reconfigure is supported. |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities#toString()) () |

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

#### public static final boolean **DEFAULT\_SUPPORTS\_RANGING\_INTERVAL\_RECONFIGURE**

Ranging interval reconfigure is not supported by default if the system API doesn't
provide.

Constant Value:

false

#### public static final float **FIRA\_DEFAULT\_MIN\_SLOT\_DURATION\_MS**

Default supported minimum slot duration if the system API doesn't provide it.

Constant Value:

2.0

#### public static final int **FIRA\_DEFAULT\_RANGING\_INTERVAL\_MS**

Default minimum ranging interval if the system API doesn't provide it.

Constant Value:

200

#### public static final int **FIRA\_DEFAULT\_SUPPORTED\_CHANNEL**

Default supported channel if the system API doesn't provide it.

Constant Value:

9

#### public static final int **RANGE\_DATA\_NTF\_ENABLE**

Default range data notification config if the system API doesn't provide it.

Constant Value:

1



## Fields

#### public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **DEFAULT\_SUPPORTED\_RANGING\_UPDATE\_RATES**

Default supported ranging interval if the system API doesn't provide it.

#### public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **DEFAULT\_SUPPORTED\_SLOT\_DURATIONS**

Default supported slot duration if the system API doesn't provide it.

#### public static final [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **FIRA\_DEFAULT\_SUPPORTED\_CONFIG\_IDS**

Default supported config id if the system API doesn't provide it.






## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public int **getMinRangingInterval** ()

Gets the minimum supported ranging interval in milliseconds.

#### public [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **getSupportedChannels** ()

Gets the supported channel number.

#### public [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **getSupportedConfigIds** ()

Gets the supported config ids.

#### public [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **getSupportedNtfConfigs** ()

Gets the supported range data notification configs.

#### public [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **getSupportedRangingUpdateRates** ()

Gets the supported ranging interval in milliseconds.

#### public [List](//developer.android.com/reference/java/util/List.html) < [Integer](//developer.android.com/reference/java/lang/Integer.html) > **getSupportedSlotDurations** ()

Gets the supported slot duration in milliseconds.

#### public boolean **hasBackgroundRangingSupport** ()

Whether a ranging session can be started when the app is in background.

#### public int **hashCode** ()

#### public boolean **supportsAzimuthalAngle** ()

Whether azimuthal angle of arrival is supported.

#### public boolean **supportsDistance** ()

Whether distance ranging is supported.

#### public boolean **supportsElevationAngle** ()

Whether elevation angle of arrival is supported.

#### public boolean **supportsRangingIntervalReconfigure** ()

Whether ranging interval reconfigure is supported.

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()