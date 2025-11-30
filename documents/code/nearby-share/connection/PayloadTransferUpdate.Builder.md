<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder -->

# PayloadTransferUpdate.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PayloadTransferUpdate.Builder is a builder class for creating PayloadTransferUpdate objects.
* It provides constructors to create a builder with default values or by copying an existing PayloadTransferUpdate.
* The builder includes methods to set the bytes transferred, payload ID, status, and total bytes for the payload transfer update.
* The
  `build()`
  method is used to create the final PayloadTransferUpdate instance from the builder.



public static final class
**PayloadTransferUpdate.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder class for PayloadTransferUpdate.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#Builder()) () Creates a builder using the default payload transfer update. |
|  | [Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#Builder(com.google.android.gms.nearby.connection.PayloadTransferUpdate)) ( [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) origin) Creates a builder, copying the payload transfer update. |

### Public Method Summary

|  |  |
| --- | --- |
| [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) | [build](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#build()) () Builds an instance of `PayloadTransferUpdate` . |
| [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | [setBytesTransferred](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#setBytesTransferred(long)) (long bytesTransferred) Sets the number of bytes transferred so far. |
| [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | [setPayloadId](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#setPayloadId(long)) (long payloadId) Sets a payload identifier. |
| [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | [setStatus](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#setStatus(int)) (int status) Sets the status of the payload. |
| [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | [setTotalBytes](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder#setTotalBytes(long)) (long totalBytes) Sets the total number of bytes in the payload. |

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

Creates a builder using the default payload transfer update.

#### public **Builder** ( [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) origin)

Creates a builder, copying the payload transfer update.





## Public Methods

#### public [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) **build** ()

Builds an instance of
`PayloadTransferUpdate`
.

#### public [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) **setBytesTransferred** (long bytesTransferred)

Sets the number of bytes transferred so far.

#### public [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) **setPayloadId** (long payloadId)

Sets a payload identifier.

#### public [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) **setStatus** (int status)

Sets the status of the payload.

#### public [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) **setTotalBytes** (long totalBytes)

Sets the total number of bytes in the payload.