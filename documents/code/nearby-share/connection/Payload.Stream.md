<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Payload.Stream -->

# Payload.Stream

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Payload.Stream class represents a stream of data.
* You can access the stream data as an InputStream using the asInputStream() method or as a ParcelFileDescriptor using the asParcelFileDescriptor() method.
* The close() method is deprecated and you should use Payload.close() instead.



public static class
**Payload.Stream**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Represents a stream of data.

### Public Method Summary

|  |  |
| --- | --- |
| [InputStream](//developer.android.com/reference/java/io/InputStream.html) | [asInputStream](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream#asInputStream()) () Gets the `InputStream` from which to read the data for this Stream. |
| [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) | [asParcelFileDescriptor](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream#asParcelFileDescriptor()) () Gets the `ParcelFileDescriptor` from which to read the data for this Stream. |
| void | [close](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream#close()) () *This method is deprecated. Use `Payload.close()` instead.* |

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

#### public [InputStream](//developer.android.com/reference/java/io/InputStream.html) **asInputStream** ()

Gets the
`InputStream`
from which to read the data for this Stream.

When receiving a
`Payload`
of type
`Payload.Type.STREAM`
,
Nearby Connections will continuously write the incoming streamed data to this
InputStream; when no more data is available (likely because the sending device stopped
streaming data), this InputStream will be closed.

#### public [ParcelFileDescriptor](//developer.android.com/reference/android/os/ParcelFileDescriptor.html) **asParcelFileDescriptor** ()

Gets the
`ParcelFileDescriptor`
from which to read the data for this Stream.

When receiving a
`Payload`
of type
`Payload.Type.STREAM`
,
Nearby Connections will continuously write the incoming streamed data to this
ParcelFileDescriptor; when no more data is available (likely because the sending device
stopped streaming data), this ParcelFileDescriptor will be closed.

#### public void **close** ()

**This method is deprecated.**
  
Use
`Payload.close()`
instead.

Closes the
`ParcelFileDescriptor`
and
`InputStream`
to
release resource.