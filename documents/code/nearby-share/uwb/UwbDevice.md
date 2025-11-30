<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbDevice -->

# UwbDevice

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbDevice represents a UWB device.
* You can create a UwbDevice using either a byte array or a String for the address.
* You can retrieve the address of a UwbDevice using the
  `getAddress()`
  method.
* The class includes standard methods like
  `equals()`
  ,
  `hashCode()`
  , and
  `toString()`
  .



public class
**UwbDevice**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Represents a UWB device.

### Public Method Summary

|  |  |
| --- | --- |
| static [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) | [createForAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#createForAddress(byte[])) (byte[] address) Creates a new UwbDevice for a given address. |
| static [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) | [createForAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#createForAddress(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) address) Creates a new UwbDevice for a given address. |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) | [getAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#getAddress()) () The device address (eg, MAC address). |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice#toString()) () |

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

#### public static [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) **createForAddress** (byte[] address)

Creates a new UwbDevice for a given address.

#### public static [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) **createForAddress** ( [String](//developer.android.com/reference/java/lang/String.html) address)

Creates a new UwbDevice for a given address.

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) **getAddress** ()

The device address (eg, MAC address).

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()