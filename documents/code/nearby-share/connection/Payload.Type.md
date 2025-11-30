<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Payload.Type -->

# Payload.Type

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Payload.Type is an abstract interface implementing Annotation.
* It defines constants for different types of payloads: BYTES, FILE, and STREAM.
* BYTES represents a single byte array payload.
* FILE represents a file payload on the device.
* STREAM represents a real-time stream of data.



public static abstract @interface
**Payload.Type**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

The type of this payload.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [BYTES](/android/reference/com/google/android/gms/nearby/connection/Payload.Type#BYTES) | A Payload consisting of a single byte array. |
| int | [FILE](/android/reference/com/google/android/gms/nearby/connection/Payload.Type#FILE) | A Payload representing a file on the device. |
| int | [STREAM](/android/reference/com/google/android/gms/nearby/connection/Payload.Type#STREAM) | A Payload representing a real-time stream of data; e.g. |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **BYTES**

A Payload consisting of a single byte array.

Constant Value:

1

#### public static final int **FILE**

A Payload representing a file on the device.

Constant Value:

2

#### public static final int **STREAM**

A Payload representing a real-time stream of data; e.g. generated data for which the
total size is not known ahead of time.

Constant Value:

3