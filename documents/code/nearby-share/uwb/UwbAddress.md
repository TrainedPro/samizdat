<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbAddress -->

# UwbAddress

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The UwbAddress class represents a UWB address.
* UwbAddress objects can be created from a HEX string or a byte array.
* The class provides methods to get the device address, check equality, and get hash code and string representations.



public class
**UwbAddress**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Represents a UWB address.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#UwbAddress(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) address) Creates a `UwbAddress` from a HEX string. |
|  | [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#UwbAddress(byte[])) (byte[] address) Creates a `UwbAddress` from a byte array. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| byte[] | [getAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#getAddress()) () Gets the device address (eg, MAC address). |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress#toString()) () |

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

#### public **UwbAddress** ( [String](//developer.android.com/reference/java/lang/String.html) address)

Creates a
`UwbAddress`
from a HEX string.

#### public **UwbAddress** (byte[] address)

Creates a
`UwbAddress`
from a byte array.





## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public byte[] **getAddress** ()

Gets the device address (eg, MAC address).

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()