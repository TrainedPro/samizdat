<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder -->

# ConnectionOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `ConnectionOptions.Builder`
  is a class used to build
  `ConnectionOptions`
  objects.
* It includes constructors to create a new builder or to initialize one from an existing
  `ConnectionOptions`
  .
* Key methods allow setting the connection type, including an option for low power usage.
* The
  `setDisruptiveUpgrade`
  method is deprecated and replaced by
  `setConnectionType`
  .
* The
  `build`
  method finalizes the configuration and returns a
  `ConnectionOptions`
  object.



public static final class
**ConnectionOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder class for ConnectionOptions.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#Builder()) () |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#Builder(com.google.android.gms.nearby.connection.ConnectionOptions)) ( [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) origin) |

### Public Method Summary

|  |  |
| --- | --- |
| [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) | [build](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#build()) () |
| [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) | [setConnectionType](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#setConnectionType(int)) (int connectionType) Sets whether the client should disrupt the current connection to optimize the transfer or not. |
| [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) | [setDisruptiveUpgrade](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#setDisruptiveUpgrade(boolean)) (boolean disruptiveUpgrade) *This method is deprecated. Use `setConnectionType(int)` instead.* |
| [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) | [setLowPower](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder#setLowPower(boolean)) (boolean lowPower) Sets whether to attempt to connect with the lowest possible power (like BLE) if true. |

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

#### public **Builder** ( [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) origin)





## Public Methods

#### public [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) **build** ()

#### public [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) **setConnectionType** (int connectionType)

Sets whether the client should disrupt the current connection to optimize the
transfer or not. The default is
`ConnectionType.BALANCED`
.

#### public [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) **setDisruptiveUpgrade** (boolean disruptiveUpgrade)

**This method is deprecated.**
  
Use
`setConnectionType(int)`
instead.

Sets the disruptive upgrade flag. A disruptive upgrade may disconnect the device
from its primary Wi-Fi network, or otherwise modify Wi-Fi and/or BT state to optimize
for faster throughput. By default, this option is true. Set this option to false if the
user needs to maintain an internet connection or if the Nearby connection is expected
to persist in the background.

#### public [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) **setLowPower** (boolean lowPower)

Sets whether to attempt to connect with the lowest possible power (like BLE) if
true. By default, this option is false.