<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig -->

# UwbRangeDataNtfConfig.RangeDataNtfConfig

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The UwbRangeDataNtfConfig.RangeDataNtfConfig interface represents the selection of range data notification configurations.
* Range data notifications can be disabled or enabled, with enabled being the default setting.
* Notifications can be triggered based on a peer device entering or exiting a configured range (proximity edge trigger).
* Notifications can also be triggered when a peer device is within a configured range (proximity level trigger).



public static abstract @interface
**UwbRangeDataNtfConfig.RangeDataNtfConfig**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Represents which range data notification config is selected.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [RANGE\_DATA\_NTF\_DISABLE](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig#RANGE_DATA_NTF_DISABLE) | Range data notification will be disabled. |
| int | [RANGE\_DATA\_NTF\_ENABLE](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig#RANGE_DATA_NTF_ENABLE) | Range data notification will be enabled (default). |
| int | [RANGE\_DATA\_NTF\_ENABLE\_PROXIMITY\_EDGE\_TRIG](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig#RANGE_DATA_NTF_ENABLE_PROXIMITY_EDGE_TRIG) | Range data notification is enabled when peer device enters or exits the configured range. |
| int | [RANGE\_DATA\_NTF\_ENABLE\_PROXIMITY\_LEVEL\_TRIG](/android/reference/com/google/android/gms/nearby/uwb/UwbRangeDataNtfConfig.RangeDataNtfConfig#RANGE_DATA_NTF_ENABLE_PROXIMITY_LEVEL_TRIG) | Range data notification is enabled when peer device is in the configured range. |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **RANGE\_DATA\_NTF\_DISABLE**

Range data notification will be disabled.

Constant Value:

0

#### public static final int **RANGE\_DATA\_NTF\_ENABLE**

Range data notification will be enabled (default).

Constant Value:

1

#### public static final int **RANGE\_DATA\_NTF\_ENABLE\_PROXIMITY\_EDGE\_TRIG**

Range data notification is enabled when peer device enters or exits the configured
range.

Constant Value:

3

#### public static final int **RANGE\_DATA\_NTF\_ENABLE\_PROXIMITY\_LEVEL\_TRIG**

Range data notification is enabled when peer device is in the configured range.

Constant Value:

2