<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Payload -->

# Payload

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Payload represents data sent between devices, with the type of data determining how it is received on the other end.
* Payloads can represent files, streams of data, or byte arrays.
* Methods are available to retrieve the data based on the payload type, such as
  `asBytes()`
  ,
  `asFile()`
  , and
  `asStream()`
  .
* Payloads have a unique ID, can be closed to release resources, and support setting an offset for resuming transfers, file name, parent folder, and sensitivity for file types.



public class
**Payload**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

A Payload sent between devices. Payloads sent as a particular type will be received as
that same type on the other device, e.g. the data for a Payload of type
`Payload.Type.STREAM`
must be received by reading from the InputStream returned by
`asStream()`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [Payload.File](/android/reference/com/google/android/gms/nearby/connection/Payload.File) | | Represents a file in local storage on the device. |
| class | [Payload.Stream](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream) | | Represents a stream of data. |
| @interface | [Payload.Type](/android/reference/com/google/android/gms/nearby/connection/Payload.Type) | | The type of this payload. |

### Public Method Summary

|  |  |
| --- | --- |
| byte[] | [asBytes](/android/reference/com/google/android/gms/nearby/connection/Payload#asBytes()) () Non-null for payloads of type `Payload.Type.BYTES` . |
| [Payload.File](/android/reference/com/google/android/gms/nearby/connection/Payload.File) | [asFile](/android/reference/com/google/android/gms/nearby/connection/Payload#asFile()) () Non-null for payloads of type `Payload.Type.FILE` . |
| [Payload.Stream](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream) | [asStream](/android/reference/com/google/android/gms/nearby/connection/Payload#asStream()) () Non-null for payloads of type `Payload.Type.STREAM` . |
| void | [close](/android/reference/com/google/android/gms/nearby/connection/Payload#close()) () Closes to release any `ParcelFileDescriptor` and `InputStream` resources for `Payload.File` or `Payload.Stream` when the transferring stopped. |
| static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | [fromBytes](/android/reference/com/google/android/gms/nearby/connection/Payload#fromBytes(byte[])) (byte[] bytes) Creates a Payload of type `Payload.Type.BYTES` for sending to another device. |
| static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | [fromFile](/android/reference/com/google/android/gms/nearby/connection/Payload#fromFile(android.os.ParcelFileDescriptor)) ( [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) pfd) Creates a Payload of type `Payload.Type.FILE` (backed by a `ParcelFileDescriptor` ) for sending to another device; for example, the ParcelFileDescriptor obtained from a call to `ContentResolver.openFileDescriptor(Uri, String)` for a URI. |
| static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | [fromFile](/android/reference/com/google/android/gms/nearby/connection/Payload#fromFile(java.io.File)) ( [File](//developer.android.com/reference/java/io/File.html) javaFile) Creates a Payload of type `Payload.Type.FILE` (backed by a `File` ) for sending to another device. |
| static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | [fromStream](/android/reference/com/google/android/gms/nearby/connection/Payload#fromStream(android.os.ParcelFileDescriptor)) ( [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) pfd) Creates a Payload of type `Payload.Type.STREAM` (backed by a `ParcelFileDescriptor` ) for sending to another device; for example, the read side of a ParcelFileDescriptor pipe to which data is being written by the MediaRecorder API. |
| static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | [fromStream](/android/reference/com/google/android/gms/nearby/connection/Payload#fromStream(java.io.InputStream)) ( [InputStream](//developer.android.com/reference/java/io/InputStream.html) inputStream) Creates a Payload of type `Payload.Type.STREAM` (backed by an `InputStream` ) for sending to another device; for example, a `PipedInputStream` connected to a `PipedOutputStream` to which data is being written. |
| long | [getId](/android/reference/com/google/android/gms/nearby/connection/Payload#getId()) () A unique identifier for this payload. |
| long | [getOffset](/android/reference/com/google/android/gms/nearby/connection/Payload#getOffset()) () Returns the offset of this payload for resume sending or receiving. |
| int | [getType](/android/reference/com/google/android/gms/nearby/connection/Payload#getType()) () The type of this payload, one of `Payload.Type` . |
| void | [setFileName](/android/reference/com/google/android/gms/nearby/connection/Payload#setFileName(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) name) Sets the file name of this `Payload.Type.FILE` payload. |
| void | [setOffset](/android/reference/com/google/android/gms/nearby/connection/Payload#setOffset(long)) (long offset) Sets the offset from `getOffset()` when resuming a transfer. |
| void | [setParentFolder](/android/reference/com/google/android/gms/nearby/connection/Payload#setParentFolder(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) parentFolder) Sets the parent folder of this `Payload.Type.FILE` payload. |
| void | [setSensitive](/android/reference/com/google/android/gms/nearby/connection/Payload#setSensitive(boolean)) (boolean isSensitive) Sets whether or not the payload is sensitive. |

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












## Public Methods

#### public byte[] **asBytes** ()

Non-null for payloads of type
`Payload.Type.BYTES`
.

#### public [Payload.File](/android/reference/com/google/android/gms/nearby/connection/Payload.File) **asFile** ()

Non-null for payloads of type
`Payload.Type.FILE`
.

#### public [Payload.Stream](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream) **asStream** ()

Non-null for payloads of type
`Payload.Type.STREAM`
.

#### public void **close** ()

Closes to release any
`ParcelFileDescriptor`
and
`InputStream`
resources for
`Payload.File`
or
`Payload.Stream`
when the transferring stopped.

#### public static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) **fromBytes** (byte[] bytes)

Creates a Payload of type
`Payload.Type.BYTES`
for sending to another device.

#### public static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) **fromFile** ( [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) pfd)

Creates a Payload of type
`Payload.Type.FILE`
(backed by a
`ParcelFileDescriptor`
)
for sending to another device; for example, the ParcelFileDescriptor obtained from a
call to
`ContentResolver.openFileDescriptor(Uri, String)`
for a URI.

#### public static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) **fromFile** ( [File](//developer.android.com/reference/java/io/File.html) javaFile)

Creates a Payload of type
`Payload.Type.FILE`
(backed by a
`File`
) for sending to
another device. Note: The file will be saved in the remote device's Downloads folder
under a generic name with no extension. The client app on the remote device is
responsible for renaming this
`File`
and adding an
appropriate extension, if necessary, and all this (and possibly additional) metadata
should be transmitted by the local device out-of-band (likely using a Payload of type
`Payload.Type.BYTES`
).

The client app must have any necessary permissions to read the Java file.

##### Throws

|  |  |
| --- | --- |
| [FileNotFoundException](//developer.android.com/reference/java/io/FileNotFoundException.html) |  |

#### public static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) **fromStream** ( [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) pfd)

Creates a Payload of type
`Payload.Type.STREAM`
(backed by a
`ParcelFileDescriptor`
)
for sending to another device; for example, the read side of a ParcelFileDescriptor
pipe to which data is being written by the MediaRecorder API.

Nearby Connections will read continuously from the ParcelFileDescriptor (for data to
send) until it is closed.

#### public static [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) **fromStream** ( [InputStream](//developer.android.com/reference/java/io/InputStream.html) inputStream)

Creates a Payload of type
`Payload.Type.STREAM`
(backed by an
`InputStream`
)
for sending to another device; for example, a
`PipedInputStream`
connected to a
`PipedOutputStream`
to which data is being written.

Nearby Connections will read continuously from the InputStream (for data to send)
until it is closed.

#### public long **getId** ()

A unique identifier for this payload.

#### public long **getOffset** ()

Returns the offset of this payload for resume sending or receiving.

#### public int **getType** ()

The type of this payload, one of
`Payload.Type`
.

#### public void **setFileName** ( [String](//developer.android.com/reference/java/lang/String.html) name)

Sets the file name of this
`Payload.Type.FILE`
payload. Nearby Connections will pass the file name to the remote device and then the
remote device will attempt to save the payload file with the specified file name under
the device's download folder.

#### public void **setOffset** (long offset)

Sets the offset from
`getOffset()`
when resuming a transfer. The payload will be started to send from the offset. Only
supports type
`Payload.Type.FILE`
or
`Payload.Type.STREAM`
.

#### public void **setParentFolder** ( [String](//developer.android.com/reference/java/lang/String.html) parentFolder)

Sets the parent folder of this
`Payload.Type.FILE`
payload. Nearby Connections will pass the parent folder's name to the remote device and
then the remote device will create a folder with the specified name under the download
folder to save the incoming payload.

#### public void **setSensitive** (boolean isSensitive)

Sets whether or not the payload is sensitive. This setting is only supported for
`Payload.Type.FILE`
.
Sensitive
`Payload.Type.FILE`
payloads will be stored in private storage and cannot be read by apps other than the
caller. By default, this option is false.