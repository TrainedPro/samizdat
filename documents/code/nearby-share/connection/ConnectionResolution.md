<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution -->

# ConnectionResolution

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionResolution is the result after a connection is initiated.
* It has a constructor that takes a Status object.
* It has a public method
  `getStatus`
  which returns the status of the response, indicating success or rejection.



public final class
**ConnectionResolution**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

The result after
`ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [ConnectionResolution](/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution#ConnectionResolution(com.google.android.gms.common.api.Status)) ( [Status](/android/reference/com/google/android/gms/common/api/Status) status) Creates a new `ConnectionResolution` . |

### Public Method Summary

|  |  |
| --- | --- |
| [Status](/android/reference/com/google/android/gms/common/api/Status) | [getStatus](/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution#getStatus()) () The status of the response. |

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

#### public **ConnectionResolution** ( [Status](/android/reference/com/google/android/gms/common/api/Status) status)

Creates a new
`ConnectionResolution`
.





## Public Methods

#### public [Status](/android/reference/com/google/android/gms/common/api/Status) **getStatus** ()

The status of the response. Valid values are
`CommonStatusCodes.SUCCESS`
and
`ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED`
.