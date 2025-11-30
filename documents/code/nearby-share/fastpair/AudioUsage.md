<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage -->

# AudioUsage

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* AudioUsage identifies the audio usage of an SASS device.
* Deprecated constants like A2DP, HFP, and LE\_AUDIO have replacement constants for media and call usage.
* The current supported audio usage constants are CALL, MEDIA, and UNKNOWN.
* The CALL usage supports Bluetooth Classic HEADSET and LE Audio profiles, while MEDIA usage supports Bluetooth Classic A2DP and LE Audio profiles.



public abstract @interface
**AudioUsage**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Identify the audio usage of an SASS device.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [A2DP](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#A2DP) | *This constant is deprecated. Use `MEDIA` instead.* |
| int | [CALL](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#CALL) | The usage of this audio event is call. |
| int | [HFP](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#HFP) | *This constant is deprecated. Use `CALL` instead.* |
| int | [LE\_AUDIO](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#LE_AUDIO) | *This constant is deprecated. Use `MEDIA` or `CALL` instead.* |
| int | [MEDIA](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#MEDIA) | The usage of this audio event is media. |
| int | [UNKNOWN](/android/reference/com/google/android/gms/nearby/fastpair/AudioUsage#UNKNOWN) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **A2DP**

**This constant is deprecated.**
  
Use
`MEDIA`
instead.

Constant Value:

2

#### public static final int **CALL**

The usage of this audio event is call. It supports
`BluetoothProfile.HEADSET`
profile for Bluetooth Classic, and
`BluetoothProfile.LE_AUDIO`
profile for devices that support LE Audio.

Constant Value:

1

#### public static final int **HFP**

**This constant is deprecated.**
  
Use
`CALL`
instead.

Constant Value:

1

#### public static final int **LE\_AUDIO**

**This constant is deprecated.**
  
Use
`MEDIA`
or
`CALL`
instead.

Constant Value:

3

#### public static final int **MEDIA**

The usage of this audio event is media. It supports
`BluetoothProfile.A2DP`
profile for Bluetooth Classic, and
`BluetoothProfile.LE_AUDIO`
profile for devices that support LE Audio.

Constant Value:

2

#### public static final int **UNKNOWN**

Constant Value:

0