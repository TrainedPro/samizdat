<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions -->

# ConnectionOptions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionOptions are used for specifying options when requesting a connection using ConnectionsClient.
* The class includes a Builder for creating ConnectionOptions instances.
* Key methods allow getting the connection type, determining if low power mode is preferred, and converting connection types to strings.
* The
  `getDisruptiveUpgrade`
  method is deprecated;
  `getConnectionType`
  should be used instead.



public final class
**ConnectionOptions**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Options for a call to
`ConnectionsClient.requestConnection(byte[], String,
ConnectionLifecycleCallback)`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) | | Builder class for ConnectionOptions. |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#CREATOR) |  |

### Public Method Summary

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | [convertConnectionTypeToString](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#convertConnectionTypeToString(int)) (int connectionType) Converts the `ConnectionType` to a string. |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) other) |
| int | [getConnectionType](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#getConnectionType()) () Returns the `ConnectionType` which indicate the client prefer or not prefer to disrupt the current Wi-Fi or Bluetooth connections. |
| boolean | [getDisruptiveUpgrade](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#getDisruptiveUpgrade()) () *This method is deprecated. Use `getConnectionType()` instead.* |
| boolean | [getLowPower](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#getLowPower()) () Gets whether to attempt to connect with the lowest possible power (like BLE) if true. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags) |

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

From interface android.os.Parcelable

|  |  |
| --- | --- |
| abstract int | describeContents () |
| abstract void | writeToParcel ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) arg0, int arg1) |








## Fields

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) > **CREATOR**






## Public Methods

#### public static [String](//developer.android.com/reference/java/lang/String.html) **convertConnectionTypeToString** (int connectionType)

Converts the
`ConnectionType`
to a string.

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) other)

#### public int **getConnectionType** ()

Returns the
`ConnectionType`
which indicate the client prefer or not prefer to disrupt the current Wi-Fi or
Bluetooth connections. The default is
`ConnectionType.BALANCED`
.

#### public boolean **getDisruptiveUpgrade** ()

**This method is deprecated.**
  
Use
`getConnectionType()`
instead.

Gets the disruptive upgrade flag. A disruptive upgrade may disconnect the device
from its primary Wi-Fi network, or otherwise modify Wi-Fi and/or BT state to optimize
for faster throughput. By default, this option is true.

#### public boolean **getLowPower** ()

Gets whether to attempt to connect with the lowest possible power (like BLE) if
true. By default, this option is false.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags)