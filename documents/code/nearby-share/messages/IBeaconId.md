<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/IBeaconId -->

# IBeaconId

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* IBeaconId represents an iBeacon ID broadcast by BLE beacons and iOS devices.
* The LENGTH constant defines the size of an iBeacon ID in bytes.
* An IBeaconId is created using a proximity UUID, major value, and minor value.
* Methods are available to retrieve the proximity UUID, major, and minor values, as well as convert a Message to an IBeaconId.



public class
**IBeaconId**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

An iBeacon ID, which can be broadcast by BLE beacons and iOS devices.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [LENGTH](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#LENGTH) | Length of an iBeacon ID, in bytes. |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [IBeaconId](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#IBeaconId(java.util.UUID,%20short,%20short)) ( [UUID](//developer.android.com/reference/java/util/UUID.html) proximityUuid, short major, short minor) Creates an iBeacon ID. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| static [IBeaconId](/android/reference/com/google/android/gms/nearby/messages/IBeaconId) | [from](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#from(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Converts a Message of type `Message.MESSAGE_TYPE_I_BEACON_ID` to an IBeaconId. |
| short | [getMajor](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#getMajor()) () Returns the major value. |
| short | [getMinor](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#getMinor()) () Returns the minor value. |
| [UUID](//developer.android.com/reference/java/util/UUID.html) | [getProximityUuid](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#getProximityUuid()) () Returns the proximity UUID. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/IBeaconId#toString()) () |

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







## Constants

#### public static final int **LENGTH**

Length of an iBeacon ID, in bytes. An iBeacon ID consists of a 16-byte proximity
UUID, followed by a 2-byte major value and a 2-byte minor value.

Constant Value:

20




## Public Constructors

#### public **IBeaconId** ( [UUID](//developer.android.com/reference/java/util/UUID.html) proximityUuid, short major, short minor)

Creates an iBeacon ID.





## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public static [IBeaconId](/android/reference/com/google/android/gms/nearby/messages/IBeaconId) **from** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Converts a Message of type
`Message.MESSAGE_TYPE_I_BEACON_ID`
to an IBeaconId.

##### See Also

* `MessageFilter.Builder.includeIBeaconIds(UUID, Short, Short)`

#### public short **getMajor** ()

Returns the major value.

#### public short **getMinor** ()

Returns the minor value.

#### public [UUID](//developer.android.com/reference/java/util/UUID.html) **getProximityUuid** ()

Returns the proximity UUID.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()