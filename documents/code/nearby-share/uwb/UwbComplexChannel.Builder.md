<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder -->

# UwbComplexChannel.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `UwbComplexChannel.Builder`
  is used to create a new instance of
  `UwbComplexChannel`
  .
* The builder has a constructor and methods to build the
  `UwbComplexChannel`
  instance and set its properties like channel and preamble index.
* The
  `build()`
  method finalizes the configuration and returns the
  `UwbComplexChannel`
  object.



public static class
**UwbComplexChannel.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Creates a new instance of
`UwbComplexChannel`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) | [build](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder#build()) () Builds a new instance of `UwbComplexChannel` . |
| [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) | [setChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder#setChannel(int)) (int channel) Sets the channel. |
| [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) | [setPreambleIndex](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder#setPreambleIndex(int)) (int preambleIndex) Sets the preamble index. |

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





## Public Methods

#### public [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) **build** ()

Builds a new instance of
`UwbComplexChannel`
.

#### public [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) **setChannel** (int channel)

Sets the channel.

#### public [UwbComplexChannel.Builder](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel.Builder) **setPreambleIndex** (int preambleIndex)

Sets the preamble index.