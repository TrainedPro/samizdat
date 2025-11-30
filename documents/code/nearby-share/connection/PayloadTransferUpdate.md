<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate -->

# PayloadTransferUpdate

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PayloadTransferUpdate describes the status for an active Payload transfer, either incoming or outgoing.
* It is delivered to PayloadCallback.onPayloadTransferUpdate.
* It includes methods to get the payload identifier, status, total bytes, and bytes transferred so far.
* It implements the Parcelable interface and has a Builder nested class.



public final class
**PayloadTransferUpdate**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Describes the status for an active
`Payload`
transfer, either incoming or outgoing. Delivered to
`PayloadCallback.onPayloadTransferUpdate(String, PayloadTransferUpdate)`
.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | | Builder class for PayloadTransferUpdate. |
| @interface | [PayloadTransferUpdate.Status](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status) | | The status of the payload transfer at the time of this update. |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#CREATOR) |  |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) other) |
| long | [getBytesTransferred](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#getBytesTransferred()) () Returns the number of bytes transferred so far. |
| long | [getPayloadId](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#getPayloadId()) () Returns the payload identifier. |
| int | [getStatus](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#getStatus()) () Returns the status of the payload. |
| long | [getTotalBytes](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#getTotalBytes()) () Returns the total number of bytes in the payload. |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#hashCode()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags) |

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








## Fields

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) > **CREATOR**






## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) other)

#### public long **getBytesTransferred** ()

Returns the number of bytes transferred so far.

#### public long **getPayloadId** ()

Returns the payload identifier.

#### public int **getStatus** ()

Returns the status of the payload.

#### public long **getTotalBytes** ()

Returns the total number of bytes in the payload.

#### public int **hashCode** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags)