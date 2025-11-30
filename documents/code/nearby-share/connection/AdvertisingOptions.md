<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions -->

# AdvertisingOptions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* AdvertisingOptions are used to configure how a device advertises its presence for Nearby Connections.
* The deprecated constructor and method
  `getDisruptiveUpgrade()`
  should be replaced by using
  `AdvertisingOptions.Builder`
  and
  `getConnectionType()`
  .
* You can specify whether to use low power mediums like BLE for advertising using
  `getLowPower()`
  .
* The advertising strategy must match the strategy used for discovery.
* The
  `getConnectionType()`
  method indicates the preference for disrupting current network connections.



public final class
**AdvertisingOptions**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Options for a call to
`ConnectionsClient.startAdvertising(String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | | Builder class for AdvertisingOptions. |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#CREATOR) |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#AdvertisingOptions(com.google.android.gms.nearby.connection.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy) *This constructor is deprecated. Use `AdvertisingOptions.Builder` instead.* |

### Public Method Summary

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | [convertConnectionTypeToString](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#convertConnectionTypeToString(int)) (int connectionType) Converts the `ConnectionType` to a string. |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) other) |
| int | [getConnectionType](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#getConnectionType()) () Returns the `ConnectionType` which indicates the client's preference for whether or not to disrupt the current Wi-Fi or Bluetooth connection. |
| boolean | [getDisruptiveUpgrade](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#getDisruptiveUpgrade()) () *This method is deprecated. Use `getConnectionType()` instead.* |
| boolean | [getLowPower](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#getLowPower()) () Gets whether low power should be used. |
| [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | [getStrategy](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#getStrategy()) () Gets the advertising strategy. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags) |

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

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) > **CREATOR**



## Public Constructors

#### public **AdvertisingOptions** ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy)

**This constructor is deprecated.**
  
Use
`AdvertisingOptions.Builder`
instead.





## Public Methods

#### public static [String](//developer.android.com/reference/java/lang/String.html) **convertConnectionTypeToString** (int connectionType)

Converts the
`ConnectionType`
to a string.

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) other)

#### public int **getConnectionType** ()

Returns the
`ConnectionType`
which indicates the client's preference for whether or not to disrupt the current Wi-Fi
or Bluetooth connection. The default is
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

Gets whether low power should be used. If true, only low power mediums (like BLE)
will be used for advertising. By default, this option is false.

#### public [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) **getStrategy** ()

Gets the advertising strategy. The strategy used for advertising must match the
strategy used in
`DiscoveryOptions`
.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags)