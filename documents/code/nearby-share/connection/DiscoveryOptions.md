<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions -->

# DiscoveryOptions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* DiscoveryOptions are used for configuring the discovery process in the Nearby Connections API.
* The DiscoveryOptions class includes a Builder for creating instances.
* You can get the strategy and low power setting for discovery using methods in this class.
* There is a deprecated constructor that should be avoided in favor of the Builder.



public final class
**DiscoveryOptions**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Options for a call to
`ConnectionsClient.startDiscovery(String, EndpointDiscoveryCallback,
DiscoveryOptions)`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) | | Builder class for DiscoveryOptions. |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#CREATOR) |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#DiscoveryOptions(com.google.android.gms.nearby.connection.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy) *This constructor is deprecated. Use `DiscoveryOptions.Builder` instead.* |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) other) |
| boolean | [getLowPower](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#getLowPower()) () Gets whether low power should be used, if true, only low power mediums (like BLE) will be used for discovery. |
| [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | [getStrategy](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#getStrategy()) () Gets the discovery strategy, the strategy to use for discovering, must match the strategy used in `AdvertisingOptions` . |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags) |

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

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) > **CREATOR**



## Public Constructors

#### public **DiscoveryOptions** ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy)

**This constructor is deprecated.**
  
Use
`DiscoveryOptions.Builder`
instead.





## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) other)

#### public boolean **getLowPower** ()

Gets whether low power should be used, if true, only low power mediums (like BLE)
will be used for discovery. By default, this option is false.

#### public [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) **getStrategy** ()

Gets the discovery strategy, the strategy to use for discovering, must match the
strategy used in
`AdvertisingOptions`
.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags)