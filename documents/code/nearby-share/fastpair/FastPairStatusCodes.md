<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes -->

# FastPairStatusCodes

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The FastPairStatusCodes class provides status codes for nearby fast pair results.
* Specific status codes include FAILED\_INVALID\_ARGUMENTS, FAILED\_NOT\_SUPPORTED, FAILED\_PERMISSION\_DENIED, and SUCCESS.
* These status codes indicate whether an operation was successful or failed due to reasons like invalid arguments, lack of support, or insufficient permissions.
* The class inherits status codes and methods from com.google.android.gms.common.api.CommonStatusCodes.



public class
**FastPairStatusCodes**
extends
[CommonStatusCodes](/android/reference/com/google/android/gms/common/api/CommonStatusCodes)

Status codes for nearby fast pair results.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [FAILED\_INVALID\_ARGUMENTS](/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes#FAILED_INVALID_ARGUMENTS) | The client invokes the API with invalid arguments. |
| int | [FAILED\_NOT\_SUPPORTED](/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes#FAILED_NOT_SUPPORTED) | The client invokes the API but it's not supported for some reason. |
| int | [FAILED\_PERMISSION\_DENIED](/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes#FAILED_PERMISSION_DENIED) | The client doesn't have the required [permissions](//developer.android.com/reference/android/Manifest.permission) for calling the API. |
| int | [SUCCESS](/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes#SUCCESS) | The operation was successful. |

### Inherited Constant Summary

From class
com.google.android.gms.common.api.CommonStatusCodes

|  |  |  |
| --- | --- | --- |
| int | API\_NOT\_CONNECTED |  |
| int | CANCELED |  |
| int | CONNECTION\_SUSPENDED\_DURING\_CALL |  |
| int | DEVELOPER\_ERROR |  |
| int | ERROR |  |
| int | INTERNAL\_ERROR |  |
| int | INTERRUPTED |  |
| int | INVALID\_ACCOUNT |  |
| int | NETWORK\_ERROR |  |
| int | RECONNECTION\_TIMED\_OUT |  |
| int | RECONNECTION\_TIMED\_OUT\_DURING\_UPDATE |  |
| int | REMOTE\_EXCEPTION |  |
| int | RESOLUTION\_REQUIRED |  |
| int | SERVICE\_DISABLED |  |
| int | SERVICE\_VERSION\_UPDATE\_REQUIRED |  |
| int | SIGN\_IN\_REQUIRED |  |
| int | SUCCESS |  |
| int | SUCCESS\_CACHE |  |
| int | TIMEOUT |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [FastPairStatusCodes](/android/reference/com/google/android/gms/nearby/fastpair/FastPairStatusCodes#FastPairStatusCodes()) () |

### Inherited Method Summary

From class
com.google.android.gms.common.api.CommonStatusCodes

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | getStatusCodeString (int arg0) |

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

#### public static final int **FAILED\_INVALID\_ARGUMENTS**

The client invokes the API with invalid arguments.

Constant Value:

40502

#### public static final int **FAILED\_NOT\_SUPPORTED**

The client invokes the API but it's not supported for some reason. For example, it
is deprecated or not enabled on this platform.

Constant Value:

40504

#### public static final int **FAILED\_PERMISSION\_DENIED**

The client doesn't have the required
[permissions](//developer.android.com/reference/android/Manifest.permission)
for
calling the API.

Constant Value:

40503

#### public static final int **SUCCESS**

The operation was successful.

Constant Value:

0




## Public Constructors

#### public **FastPairStatusCodes** ()