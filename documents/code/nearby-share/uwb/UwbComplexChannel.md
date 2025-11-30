<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel -->

# UwbComplexChannel

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbComplexChannel represents the channel on which a UWB device is active.
* It includes methods to get the current channel and preamble index for a device.
* A nested Builder class is available for creating new instances of UwbComplexChannel.



public class
**UwbComplexChannel**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Represents the channel which a UWB device is currently active on.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) | | Creates a new instance of `UwbComplexChannel` . |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| int | [getChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel#getChannel()) () Gets the current channel for the device. |
| int | [getPreambleIndex](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel#getPreambleIndex()) () Gets the current preamble index for the device. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel#toString()) () |

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

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public int **getChannel** ()

Gets the current channel for the device.

#### public int **getPreambleIndex** ()

Gets the current preamble index for the device.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()