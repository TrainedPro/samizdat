<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder -->

# DiscoveryOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* DiscoveryOptions.Builder is a builder class for DiscoveryOptions.
* It provides constructors for creating a builder with default options or by copying existing options.
* Public methods allow setting whether to use low power and setting the discovery strategy.
* The build method creates a DiscoveryOptions object from the builder.



public static final class
**DiscoveryOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder class for DiscoveryOptions.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder#Builder()) () Creates a builder using the default discovery options. |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder#Builder(com.google.android.gms.nearby.connection.DiscoveryOptions)) ( [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) origin) Creates a builder, copying the provided discovery options. |

### Public Method Summary

|  |  |
| --- | --- |
| [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) | [build](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder#build()) () |
| [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) | [setLowPower](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder#setLowPower(boolean)) (boolean lowPower) Sets whether to use low power. |
| [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) | [setStrategy](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder#setStrategy(com.google.android.gms.nearby.connection.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy) Sets the discovery strategy. |

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

Creates a builder using the default discovery options.

#### public **Builder** ( [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) origin)

Creates a builder, copying the provided discovery options.





## Public Methods

#### public [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) **build** ()

#### public [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) **setLowPower** (boolean lowPower)

Sets whether to use low power. If true, only low power mediums (like BLE) will be
used for discovery. By default, this option is false.

#### public [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) **setStrategy** ( [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) strategy)

Sets the discovery strategy. Must match the strategy used in
`AdvertisingOptions`
.