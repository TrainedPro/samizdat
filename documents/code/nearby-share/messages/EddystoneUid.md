<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/EddystoneUid -->

# EddystoneUid

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Eddystone UID is broadcast by BLE beacons.
* An Eddystone UID has a defined length of 16 bytes, consisting of a 10-byte namespace and a 6-byte instance.
* You can create an Eddystone UID using a 16-byte hex ID string or separate hex strings for the namespace and instance.
* Methods are available to retrieve the full hex ID, the namespace, and the instance as hex strings, and to convert a
  `Message`
  of type
  `Message.MESSAGE_TYPE_EDDYSTONE_UID`
  to an
  `EddystoneUid`
  .



public class
**EddystoneUid**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

An Eddystone UID, broadcast by BLE beacons.

##### See Also

* [Eddystone UID
  specification](//github.com/google/eddystone/tree/master/eddystone-uid)

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [INSTANCE\_LENGTH](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#INSTANCE_LENGTH) | Length of an Eddystone UID instance, in bytes. |
| int | [LENGTH](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#LENGTH) | Length of an Eddystone UID, in bytes. |
| int | [NAMESPACE\_LENGTH](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#NAMESPACE_LENGTH) | Length of an Eddystone UID namespace, in bytes. |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [EddystoneUid](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#EddystoneUid(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) hexId) Creates an ID. |
|  | [EddystoneUid](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#EddystoneUid(java.lang.String,%20java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) hexNamespace, [String](//developer.android.com/reference/java/lang/String.html) hexInstance) Creates an ID. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| static [EddystoneUid](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid) | [from](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#from(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Converts a Message of type `Message.MESSAGE_TYPE_EDDYSTONE_UID` to an EddystoneUid. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getHex](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#getHex()) () Returns the 16-byte ID, as a hex string. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getInstance](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#getInstance()) () Returns the instance (last 6 bytes), as a hex string. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getNamespace](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#getNamespace()) () Returns the namespace (first 10 bytes), as a hex string. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid#toString()) () |

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

#### public static final int **INSTANCE\_LENGTH**

Length of an Eddystone UID instance, in bytes.

Constant Value:

6

#### public static final int **LENGTH**

Length of an Eddystone UID, in bytes. An Eddystone UID consists of a 10-byte
namespace, followed by a 6-byte instance.

Constant Value:

16

#### public static final int **NAMESPACE\_LENGTH**

Length of an Eddystone UID namespace, in bytes.

Constant Value:

10




## Public Constructors

#### public **EddystoneUid** ( [String](//developer.android.com/reference/java/lang/String.html) hexId)

Creates an ID.

##### Parameters

|  |  |
| --- | --- |
| hexId | Hex representation of a 16-byte ID (namespace plus instance). |

#### public **EddystoneUid** ( [String](//developer.android.com/reference/java/lang/String.html) hexNamespace, [String](//developer.android.com/reference/java/lang/String.html) hexInstance)

Creates an ID.

##### Parameters

|  |  |
| --- | --- |
| hexNamespace | Hex representation of a 10-byte namespace. |
| hexInstance | Hex representation of a 6-byte instance. |





## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public static [EddystoneUid](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid) **from** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Converts a Message of type
`Message.MESSAGE_TYPE_EDDYSTONE_UID`
to an EddystoneUid.

##### See Also

* `MessageFilter.Builder.includeEddystoneUids(String, String)`

#### public [String](//developer.android.com/reference/java/lang/String.html) **getHex** ()

Returns the 16-byte ID, as a hex string.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getInstance** ()

Returns the instance (last 6 bytes), as a hex string.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getNamespace** ()

Returns the namespace (first 10 bytes), as a hex string.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()