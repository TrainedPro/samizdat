<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder -->

# AdvertisingOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* AdvertisingOptions.Builder is a builder class for creating AdvertisingOptions.
* It provides constructors to create a builder with default options or by copying existing options.
* Methods are available to set the connection type, low power mode, and advertising strategy.
* The setDisruptiveUpgrade method is deprecated in favor of setConnectionType.
* The build method finalizes the builder and returns the configured AdvertisingOptions object.



public static final class
**AdvertisingOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder class for AdvertisingOptions.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#Builder()) () Creates a builder using the default advertising options. |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#Builder(com.google.android.gms.nearby.connection.AdvertisingOptions)) ( [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) origin) Creates a builder, copying the provided advertising options. |

### Public Method Summary

|  |  |
| --- | --- |
| [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) | [build](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#build()) () |
| [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | [setConnectionType](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#setConnectionType(int)) (int connectionType) Sets whether the client should disrupt the current connection to optimize the transfer or not. |
| [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | [setDisruptiveUpgrade](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#setDisruptiveUpgrade(boolean)) (boolean disruptiveUpgrade) *This method is deprecated. Use `setConnectionType(int)` instead.* |
| [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | [setLowPower](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#setLowPower(boolean)) (boolean lowPower) Sets whether low power should be used. |
| [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | [setStrategy](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder#setStrategy(com.google.android.gms.nearby.connection.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy) Sets the advertising strategy. |

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

#### public **Builder** ()

Creates a builder using the default advertising options.

#### public **Builder** ( [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) origin)

Creates a builder, copying the provided advertising options.





## Public Methods

#### public [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) **build** ()

#### public [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) **setConnectionType** (int connectionType)

Sets whether the client should disrupt the current connection to optimize the
transfer or not. The default is
`ConnectionType.BALANCED`
.

#### public [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) **setDisruptiveUpgrade** (boolean disruptiveUpgrade)

**This method is deprecated.**
  
Use
`setConnectionType(int)`
instead.

Sets the disruptive upgrade flag. A disruptive upgrade may disconnect the device
from its primary Wi-Fi network, or otherwise modify Wi-Fi and/or BT state to optimize
for faster throughput. By default, this option is true. Set this option to false if the
user needs to maintain an internet connection or if the Nearby connection is expected
to persist in the background.

#### public [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) **setLowPower** (boolean lowPower)

Sets whether low power should be used. If true, only low power mediums (like BLE)
will be used for advertising. By default, this option is false.

#### public [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) **setStrategy** ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy)

Sets the advertising strategy. The strategy used for advertising must match the
strategy used in
`DiscoveryOptions`
.