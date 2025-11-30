<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Message -->

# Message

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* A Message object is used to share content with nearby devices, consisting of client-specified content and a type.
* The message type can be used with MessageFilter.Builder to control which messages an application receives during a subscription.
* Messages have a maximum content size of 102400 bytes and a maximum type length of 32 characters.
* Messages can be created with content only (using a default empty type) or with both content and a specified type.
* Message objects are considered equal if their namespace, type, and content are equal.



public class
**Message**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

A message that will be shared with nearby devices. This message consists of some
client-specified content and a type. The type can be used in the
`MessageFilter.Builder`
to limit which messages an application receives in a subscription.

##### See Also

* `Messages.publish(com.google.android.gms.common.api.GoogleApiClient,
  Message)`
* `Messages.subscribe(com.google.android.gms.common.api.GoogleApiClient,
  MessageListener)`

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [MAX\_CONTENT\_SIZE\_BYTES](/android/reference/com/google/android/gms/nearby/messages/Message#MAX_CONTENT_SIZE_BYTES) | The maximum content size in number of bytes. |
| int | [MAX\_TYPE\_LENGTH](/android/reference/com/google/android/gms/nearby/messages/Message#MAX_TYPE_LENGTH) | The maximum `String.length()` for the message type. |
| [String](//developer.android.com/reference/java/lang/String.html) | [MESSAGE\_NAMESPACE\_RESERVED](/android/reference/com/google/android/gms/nearby/messages/Message#MESSAGE_NAMESPACE_RESERVED) | A namespace reserved for special Messages. |
| [String](//developer.android.com/reference/java/lang/String.html) | [MESSAGE\_TYPE\_AUDIO\_BYTES](/android/reference/com/google/android/gms/nearby/messages/Message#MESSAGE_TYPE_AUDIO_BYTES) | *This constant is deprecated. Nearby Messages will no longer support audio.* |
| [String](//developer.android.com/reference/java/lang/String.html) | [MESSAGE\_TYPE\_EDDYSTONE\_UID](/android/reference/com/google/android/gms/nearby/messages/Message#MESSAGE_TYPE_EDDYSTONE_UID) | See `MessageFilter.Builder.includeEddystoneUids(String, String)` and `EddystoneUid.from(Message)` . |
| [String](//developer.android.com/reference/java/lang/String.html) | [MESSAGE\_TYPE\_I\_BEACON\_ID](/android/reference/com/google/android/gms/nearby/messages/Message#MESSAGE_TYPE_I_BEACON_ID) | See `MessageFilter.Builder.includeIBeaconIds(UUID, Short, Short)` and `IBeaconId.from(Message)` . |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [Message](/android/reference/com/google/android/gms/nearby/messages/Message) > | [CREATOR](/android/reference/com/google/android/gms/nearby/messages/Message#CREATOR) |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Message](/android/reference/com/google/android/gms/nearby/messages/Message#Message(byte[])) (byte[] content) Creates a new message with the given content and the default type (empty string). |
|  | [Message](/android/reference/com/google/android/gms/nearby/messages/Message#Message(byte[],%20java.lang.String)) (byte[] content, [String](//developer.android.com/reference/java/lang/String.html) type) Creates a new message with the given content and type. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/messages/Message#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) object) `Message` objects are equal if the namespace, type, and content are equal. |
| byte[] | [getContent](/android/reference/com/google/android/gms/nearby/messages/Message#getContent()) () Returns the raw bytes content of the message. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getNamespace](/android/reference/com/google/android/gms/nearby/messages/Message#getNamespace()) () Returns the non-empty string for a public namespace or empty for the private one. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getType](/android/reference/com/google/android/gms/nearby/messages/Message#getType()) () Returns the type that describes the content of the message. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/messages/Message#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/Message#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/messages/Message#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags) |

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







## Constants

#### public static final int **MAX\_CONTENT\_SIZE\_BYTES**

The maximum content size in number of bytes.

Constant Value:

102400

#### public static final int **MAX\_TYPE\_LENGTH**

The maximum
`String.length()`
for the message type.

Constant Value:

32

#### public static final [String](//developer.android.com/reference/java/lang/String.html) **MESSAGE\_NAMESPACE\_RESERVED**

A namespace reserved for special Messages. See MESSAGE\_TYPE\_\* for examples.

Constant Value:

"\_\_reserved\_namespace"

#### public static final [String](//developer.android.com/reference/java/lang/String.html) **MESSAGE\_TYPE\_AUDIO\_BYTES**

**This constant is deprecated.**
  
Nearby Messages will no longer support audio.

Message type refers to an
`AudioBytes`
based message which contains raw byte[] data to be directly sent or received over the
near-ultrasound audio medium.

Constant Value:

"\_\_audio\_bytes"

#### public static final [String](//developer.android.com/reference/java/lang/String.html) **MESSAGE\_TYPE\_EDDYSTONE\_UID**

See
`MessageFilter.Builder.includeEddystoneUids(String, String)`
and
`EddystoneUid.from(Message)`
.

Constant Value:

"\_\_eddystone\_uid"

#### public static final [String](//developer.android.com/reference/java/lang/String.html) **MESSAGE\_TYPE\_I\_BEACON\_ID**

See
`MessageFilter.Builder.includeIBeaconIds(UUID, Short, Short)`
and
`IBeaconId.from(Message)`
.

Constant Value:

"\_\_i\_beacon\_id"



## Fields

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [Message](/android/reference/com/google/android/gms/nearby/messages/Message) > **CREATOR**



## Public Constructors

#### public **Message** (byte[] content)

Creates a new message with the given content and the default type (empty
string).

Use this constructor when the application has only one message type or as the
default type to exchange with nearby devices.

##### Parameters

|  |  |
| --- | --- |
| content | An arbitrary array holding the content of the message. The maximum content size is `MAX_CONTENT_SIZE_BYTES` . |

#### public **Message** (byte[] content, [String](//developer.android.com/reference/java/lang/String.html) type)

Creates a new message with the given content and type.

Use this constructor when your application has multiple types of data to exchange.
For example, a poll application could publish a message with type "question", and
subscribe for messages of type "answer" that are published by the same app running on
other nearby devices.

##### Parameters

|  |  |
| --- | --- |
| content | An arbitrary array holding the content of the message. The maximum content size is `MAX_CONTENT_SIZE_BYTES` . |
| type | A string that describe what the bytes of the content represent. The maximum type length is `MAX_TYPE_LENGTH` . |





## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) object)

`Message`
objects are equal if the namespace, type, and content are equal.

#### public byte[] **getContent** ()

Returns the raw bytes content of the message.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getNamespace** ()

Returns the non-empty string for a public namespace or empty for the private one.
The private namespace contains messages are meant to be used and exchanged by apps that
created them.

A public namespace contains messages that are publicly known and is accessible to
any application who is interested in them. One example of a message in a public
namespace is a beacon attachment. See
[Beacons](//developers.google.com/beacons)
for more details on namespace and beacon
attachment.

##### Returns

* An empty String for the private namespace or non-empty for public
  namespaces.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getType** ()

Returns the type that describes the content of the message.

Returns an empty String if no type was specified when the message was created.

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags)