<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder -->

# MessageFilter.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* MessageFilter.Builder is used to build instances of MessageFilter.
* The builder includes methods to filter messages by application, Eddystone UIDs, iBeacon IDs, namespaced types, or by including a previously constructed filter.
* The method includeAudioBytes is deprecated and Nearby Messages will no longer support audio.
* The
  `build`
  method creates the final MessageFilter instance from the configured builder.



public static final class
**MessageFilter.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for
`MessageFilter`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) | [build](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#build()) () Builds an instance of `MessageFilter` . |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeAllMyTypes](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeAllMyTypes()) () Filters for all messages published by this application (and any other applications in the same Google Developers Console project), regardless of type. |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeAudioBytes](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeAudioBytes(int)) (int numAudioBytes) *This method is deprecated. Nearby Messages will no longer support audio.* |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeEddystoneUids](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeEddystoneUids(java.lang.String,%20java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) hexNamespace, [String](//developer.android.com/reference/java/lang/String.html) hexInstance) Includes [Eddystone UIDs](//github.com/google/eddystone/tree/master/eddystone-uid) . |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeFilter(com.google.android.gms.nearby.messages.MessageFilter)) ( [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) filter) Includes the previously constructed filter. |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeIBeaconIds](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeIBeaconIds(java.util.UUID,%20java.lang.Short,%20java.lang.Short)) ( [UUID](//developer.android.com/reference/java/util/UUID.html) proximityUuid, [Short](//developer.android.com/reference/java/lang/Short.html) major, [Short](//developer.android.com/reference/java/lang/Short.html) minor) Includes iBeacon IDs. |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | [includeNamespacedType](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder#includeNamespacedType(java.lang.String,%20java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) namespace, [String](//developer.android.com/reference/java/lang/String.html) type) Filters for all messages in the given `namespace` with the given `type` . |

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

#### public [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) **build** ()

Builds an instance of
`MessageFilter`
.

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeAllMyTypes** ()

Filters for all messages published by this application (and any other applications
in the same Google Developers Console project), regardless of type.

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeAudioBytes** (int numAudioBytes)

**This method is deprecated.**
  
Nearby Messages will no longer support audio.

Includes raw audio byte messages. This can only be called once to set the number of
audio bytes to be received.

Audio byte messages will have namespace=
`Message.MESSAGE_NAMESPACE_RESERVED`
,
and type=
`Message.MESSAGE_TYPE_AUDIO_BYTES`
.
Use
`AudioBytes.from(Message)`
to parse the message content.

##### Parameters

|  |  |
| --- | --- |
| numAudioBytes | Number of bytes for the audio bytes message (capped by `AudioBytes.MAX_SIZE` ). |

##### Throws

|  |  |
| --- | --- |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if numAudioBytes is less than zero or greater than `AudioBytes.MAX_SIZE` . |

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeEddystoneUids** ( [String](//developer.android.com/reference/java/lang/String.html) hexNamespace, [String](//developer.android.com/reference/java/lang/String.html) hexInstance)

Includes
[Eddystone
UIDs](//github.com/google/eddystone/tree/master/eddystone-uid)
.

Eddystone UID messages will have namespace=
`Message.MESSAGE_NAMESPACE_RESERVED`
,
and type=
`Message.MESSAGE_TYPE_EDDYSTONE_UID`
.
Use
`EddystoneUid.from(Message)`
to parse the message content.

##### Parameters

|  |  |
| --- | --- |
| hexNamespace | The 10-byte Eddystone UID namespace in hex format. For example, "a032ffed0532bca3846d". |
| hexInstance | An optional 6-byte Eddystone UID instance in hex format. For example, "00aabbcc2233". |

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeFilter** ( [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) filter)

Includes the previously constructed filter.

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeIBeaconIds** ( [UUID](//developer.android.com/reference/java/util/UUID.html) proximityUuid, [Short](//developer.android.com/reference/java/lang/Short.html) major, [Short](//developer.android.com/reference/java/lang/Short.html) minor)

Includes iBeacon IDs.

iBeacon ID messages will have namespace=
`Message.MESSAGE_NAMESPACE_RESERVED`
,
and type=
`Message.MESSAGE_TYPE_I_BEACON_ID`
.
Use
`IBeaconId.from(Message)`
to parse the message content.

##### Parameters

|  |  |
| --- | --- |
| proximityUuid | The proximity UUID. |
| major | An optional major value. |
| minor | An optional minor value. |

#### public [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) **includeNamespacedType** ( [String](//developer.android.com/reference/java/lang/String.html) namespace, [String](//developer.android.com/reference/java/lang/String.html) type)

Filters for all messages in the given
`namespace`
with the given
`type`
.

Namespaces are currently only settable for messages published via beacons.

##### Parameters

|  |  |
| --- | --- |
| namespace | The namespace that the message belongs to. It must be non-empty and cannot contain the following invalid character: star(\*). |
| type | The type of the message to include. It must non-null and cannot contain the following invalid character: star(\*). |

##### Throws

|  |  |
| --- | --- |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if namespace or type is not valid. |