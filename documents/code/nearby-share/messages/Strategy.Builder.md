<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder -->

# Strategy.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Strategy.Builder class is used to create instances of Strategy.
* You can set the discovery mode to determine how devices detect each other.
* You can set a distance type to limit message delivery to devices within a certain range.
* You can set the time to live for the publish or subscribe operation.



public static class
**Strategy.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for
`Strategy`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder#Builder()) () Creates a new `Strategy.Builder` . |

### Public Method Summary

|  |  |
| --- | --- |
| [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | [build](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder#build()) () Builds an instance of `Strategy` . |
| [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) | [setDiscoveryMode](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder#setDiscoveryMode(int)) (int discoveryMode) Sets the desired discovery mode that determines how devices will detect each other. |
| [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) | [setDistanceType](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder#setDistanceType(int)) (int distanceType) If used with a publish, the published message will only be delivered to subscribing devices that are at most the specified distance from this device. |
| [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) | [setTtlSeconds](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder#setTtlSeconds(int)) (int ttlSeconds) Sets the time to live in seconds for the publish or subscribe. |

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

Creates a new
`Strategy.Builder`
.
By default it will have the same settings as a
`Strategy.DEFAULT`
strategy.





## Public Methods

#### public [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) **build** ()

Builds an instance of
`Strategy`
.

#### public [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) **setDiscoveryMode** (int discoveryMode)

Sets the desired discovery mode that determines how devices will detect each
other.

The discovery mode is orthogonal to if device is publishing or subscribing for
messages. It's just the mechanism for detecting nearby devices. By default, the devices
will broadcast and scan for pairing codes, so other devices can detect the originator
from either a scan or a broadcast as well or both. Doing both allows for inclusion of
devices that can only broadcast.

##### Parameters

|  |  |
| --- | --- |
| discoveryMode | One of `Strategy.DISCOVERY_MODE_*` specifying how to detect nearby devices. |

#### public [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) **setDistanceType** (int distanceType)

If used with a publish, the published message will only be delivered to subscribing
devices that are at most the specified distance from this device.

If used with a subscribe, messages will only be delivered if the publishing device
is at most the specified distance from this device.

##### Parameters

|  |  |
| --- | --- |
| distanceType | One of `Strategy.DISTANCE_TYPE_*` specifying how close nearby devices must be. |

#### public [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) **setTtlSeconds** (int ttlSeconds)

Sets the time to live in seconds for the publish or subscribe. This must be either
`Strategy.TTL_SECONDS_INFINITE`
, or a positive integer between 1 and
`Strategy.TTL_SECONDS_MAX`
, inclusive.

If not set,
`Strategy.TTL_SECONDS_DEFAULT`
is used instead.