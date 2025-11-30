<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Payload.File -->

# Payload.File

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Payload.File class represents a file stored locally on an Android device.
* The deprecated
  `asJavaFile()`
  method retrieves the File, which is saved to the Downloads folder and should be moved after the transfer is complete.
* The
  `asParcelFileDescriptor()`
  method provides a streaming interface to read file data as it arrives.
* The
  `asUri()`
  method provides a Uri for accessing the file with read and write permissions.
* The
  `getSize()`
  method returns the size of the file in bytes.



public static class
**Payload.File**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Represents a file in local storage on the device.

### Public Method Summary

|  |  |
| --- | --- |
| [File](//developer.android.com/reference/java/io/File.html) | [asJavaFile](/android/reference/com/google/android/gms/nearby/connection/Payload.File#asJavaFile()) () *This method is deprecated. Use `asUri()` instead.* |
| [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) | [asParcelFileDescriptor](/android/reference/com/google/android/gms/nearby/connection/Payload.File#asParcelFileDescriptor()) () Gets the `ParcelFileDescriptor` from which to read the data of this File; useful when reading the File in a streaming fashion, before the entire contents have arrived from the remote endpoint. |
| [Uri](//developer.android.com/reference/android/net/Uri.html) | [asUri](/android/reference/com/google/android/gms/nearby/connection/Payload.File#asUri()) () Gets a file `Uri` for which the client package has read and write permissions (see `ContentResolver.openInputStream(Uri)` or `ContentResolver.openFileDescriptor(Uri, String)` ). |
| void | [close](/android/reference/com/google/android/gms/nearby/connection/Payload.File#close()) () *This method is deprecated. Use `Payload.close()` instead.* |
| long | [getSize](/android/reference/com/google/android/gms/nearby/connection/Payload.File#getSize()) () Gets the size of this Payload.File in bytes. |

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

#### public [File](//developer.android.com/reference/java/io/File.html) **asJavaFile** ()

**This method is deprecated.**
  
Use
`asUri()`
instead.

Gets the
`File`
that backs this
File. When receiving a File, this
`File`
will be written
to the device's Downloads folder (as returned by
`Environment.getExternalStoragePublicDirectory(String)`
), and can then be
copied or moved (read on for how to do this safely) to your app's private storage, if
necessary.

Note that when receiving a
`Payload.File`
,
the call to
`PayloadCallback.onPayloadReceived(String, Payload)`
occurs at the start of
the data transfer; i.e., at the time of the call, this
`File`
exists but may
not yet contain all of its data. Wait for updates to
`PayloadCallback.onPayloadTransferUpdate(String, PayloadTransferUpdate)`
to
indicate the transfer has completed before attempting to move the
`File`
. If you want to
read the data as it comes in, use
`asParcelFileDescriptor()`
to get a streaming interface to the incoming
`Payload.File`
data.

Note: The file is saved in the local device's Downloads folder under a generic name
with no extension. The client app is responsible for renaming this
`File`
and adding an
appropriate extension, if necessary, and all this (and possibly additional) metadata
should be transmitted by the remote device out-of-band (likely using a Payload of type
`Payload.Type.BYTES`
).

#### public [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) **asParcelFileDescriptor** ()

Gets the
`ParcelFileDescriptor`
from which to read the data of this File; useful when reading the File in a streaming
fashion, before the entire contents have arrived from the remote endpoint.

#### public [Uri](//developer.android.com/reference/android/net/Uri.html) **asUri** ()

Gets a file
`Uri`
for which the
client package has read and write permissions (see
`ContentResolver.openInputStream(Uri)`
or
`ContentResolver.openFileDescriptor(Uri, String)`
). Read and write permissions
can be persisted across reboots using
`ContentResolver.takePersistableUriPermission(Uri, int)`
. Alternatively, you
can copy the file contents to a location under your control and delete the original
file. The uri will be non-null when receiving file payloads. When sending file
payloads, the uri may be null, i.e. when the payload is created by
`Payload.fromFile(ParcelFileDescriptor)`
).

#### public void **close** ()

**This method is deprecated.**
  
Use
`Payload.close()`
instead.

Closes the
`ParcelFileDescriptor`
to release resource.

#### public long **getSize** ()

Gets the size of this Payload.File in bytes. For an incoming File, this is the
expected size of the
`File`
once all data has
been received. For an outgoing File, this is simply the size of the
`File`
or the available
data in the
`ParcelFileDescriptor`
at the time of construction.