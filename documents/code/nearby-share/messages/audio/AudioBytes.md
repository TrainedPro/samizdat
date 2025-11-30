<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes -->

# AudioBytes

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* This class is deprecated as Nearby Messages will no longer support audio.
* AudioBytes represents a message sent over near-ultrasound audio with a byte array payload limited by MAX\_SIZE.
* The class provides methods to convert between AudioBytes and Nearby Messages for publishing and subscribing data over audio.
* The maximum size of the audio message payload is defined by the constant MAX\_SIZE, which is 10 bytes.



public final class
**AudioBytes**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

**This class is deprecated.**
  
Nearby Messages will no longer support audio.

A message that will be sent directly over near-ultrasound audio. The payload can be an
arbitrary byte[] array limited in size as given by
`MAX_SIZE`
.

Use this in combination with the
`Messages`
API to send or receive data over audio. For instance, the
`toMessage()`
method can be used to fetch a Nearby
`Message`
object for a
`Messages.publish(GoogleApiClient, Message)`
call.

Similarly, the
`from(Message)`
method can be used to convert a Message obtained from a subscribe
call to an AudioBytes object.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [MAX\_SIZE](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#MAX_SIZE) | The maximum size of the audio message payload. |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [AudioBytes](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#AudioBytes(byte[])) (byte[] audioData) Creates an AudioBytes object from a byte[] payload for use with the Nearby Messages API. |

### Public Method Summary

|  |  |
| --- | --- |
| static [AudioBytes](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes) | [from](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#from(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Converts a Message of type `Message.MESSAGE_TYPE_AUDIO_BYTES` to an AudioBytes object. |
| byte[] | [getBytes](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#getBytes()) () Returns the byte array payload. |
| [Message](/android/reference/com/google/android/gms/nearby/messages/Message) | [toMessage](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#toMessage()) () Obtain a `Message` object for use with the `Messages.publish(GoogleApiClient, Message)` call. |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes#toString()) () |

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

#### public static final int **MAX\_SIZE**

The maximum size of the audio message payload. Only
`MAX_SIZE`
bytes will be sent over the audio medium.

Constant Value:

10




## Public Constructors

#### public **AudioBytes** (byte[] audioData)

Creates an AudioBytes object from a byte[] payload for use with the Nearby Messages
API.





## Public Methods

#### public static [AudioBytes](/android/reference/com/google/android/gms/nearby/messages/audio/AudioBytes) **from** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Converts a Message of type
`Message.MESSAGE_TYPE_AUDIO_BYTES`
to an AudioBytes object.

##### Parameters

|  |  |
| --- | --- |
| message | Input `Message` object. |

##### Returns

* Instance of a corresponding
  `AudioBytes`
  object.

##### See Also

* `MessageFilter.Builder.includeAudioBytes(int)`

#### public byte[] **getBytes** ()

Returns the byte array payload.

#### public [Message](/android/reference/com/google/android/gms/nearby/messages/Message) **toMessage** ()

Obtain a
`Message`
object for use with the
`Messages.publish(GoogleApiClient, Message)`
call.

##### Returns

* `Message`
  object.

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()