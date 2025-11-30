<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId -->

# RangingParameters.UwbConfigId

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The
  `RangingParameters.UwbConfigId`
  annotation represents which configuration ID should be used for UWB ranging.
* There are seven predefined configuration IDs (CONFIG\_ID\_1 to CONFIG\_ID\_7), each specifying different ranging parameters like mode, interval, and security features.
* CONFIG\_ID\_1 is used for unicast ranging with a 240 ms interval, suitable for device tracking tags.
* CONFIG\_ID\_2 is designed for one-to-many ranging with a 200 ms interval, typically for a smart phone interacting with multiple devices.
* Other CONFIG\_IDs represent variations of CONFIG\_ID\_1 and CONFIG\_ID\_2 with differences in Angle-of-arrival reporting and P-STS security modes.



public static abstract @interface
**RangingParameters.UwbConfigId**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Represents which Config ID should be used.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [CONFIG\_ID\_1](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_1) | FiRa-defined unicast `STATIC STS DS-TWR` ranging, deferred mode, ranging interval 240 ms. |
| int | [CONFIG\_ID\_2](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_2) | FiRa-defined one-to-many `STATIC STS DS-TWR` ranging, deferred mode, ranging interval 200 ms Typical use case: smart phone interacts with many smart devices. |
| int | [CONFIG\_ID\_3](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_3) | Same as `CONFIG_ID_1` , except Angle-of-arrival (AoA) data is not reported. |
| int | [CONFIG\_ID\_4](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_4) | Same as `CONFIG_ID_1` , except P-STS security mode is enabled. |
| int | [CONFIG\_ID\_5](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_5) | Same as `CONFIG_ID_2` , except P-STS security mode is enabled. |
| int | [CONFIG\_ID\_6](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_6) | Same as `CONFIG_ID_3` , except P-STS security mode is enabled. |
| int | [CONFIG\_ID\_7](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.UwbConfigId#CONFIG_ID_7) | Same as `CONFIG_ID_2` , except P-STS individual controlee key mode is enabled. |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **CONFIG\_ID\_1**

FiRa-defined unicast
`STATIC STS DS-TWR`
ranging, deferred mode, ranging
interval 240 ms.

Typical use case: device tracking tags.

Constant Value:

1

#### public static final int **CONFIG\_ID\_2**

FiRa-defined one-to-many
`STATIC STS DS-TWR`
ranging, deferred mode,
ranging interval 200 ms

Typical use case: smart phone interacts with many smart devices.

Constant Value:

2

#### public static final int **CONFIG\_ID\_3**

Same as
`CONFIG_ID_1`
, except Angle-of-arrival (AoA) data is not
reported.

Constant Value:

3

#### public static final int **CONFIG\_ID\_4**

Same as
`CONFIG_ID_1`
, except P-STS security mode is enabled.

Constant Value:

4

#### public static final int **CONFIG\_ID\_5**

Same as
`CONFIG_ID_2`
, except P-STS security mode is enabled.

Constant Value:

5

#### public static final int **CONFIG\_ID\_6**

Same as
`CONFIG_ID_3`
, except P-STS security mode is enabled.

Constant Value:

6

#### public static final int **CONFIG\_ID\_7**

Same as
`CONFIG_ID_2`
, except P-STS individual controlee key mode is
enabled.

Constant Value:

7