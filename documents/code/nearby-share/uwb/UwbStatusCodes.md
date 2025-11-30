<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes -->

# UwbStatusCodes

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbStatusCodes provides status codes for nearby UWB results.
* Key status codes include success (STATUS\_OK), general error (STATUS\_ERROR), and specific errors like SERVICE\_NOT\_AVAILABLE, INVALID\_API\_CALL, NULL\_RANGING\_DEVICE, RANGING\_ALREADY\_STARTED, and UWB\_SYSTEM\_CALLBACK\_FAILURE.
* UwbStatusCodes inherits status codes from CommonStatusCodes, such as CANCELED, TIMEOUT, and INTERNAL\_ERROR.
* The class includes a method getStatusCodeString(int arg0) for retrieving the string representation of a status code.



public final class
**UwbStatusCodes**
extends
[CommonStatusCodes](/android/reference/com/google/android/gms/common/api/CommonStatusCodes)

Status codes for nearby uwb results.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [INVALID\_API\_CALL](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#INVALID_API_CALL) | The call is not valid. |
| int | [NULL\_RANGING\_DEVICE](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#NULL_RANGING_DEVICE) | The RangingDevice is null for controller or controlee. |
| int | [RANGING\_ALREADY\_STARTED](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#RANGING_ALREADY_STARTED) | The ranging is already started, this is a duplicated request. |
| int | [SERVICE\_NOT\_AVAILABLE](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#SERVICE_NOT_AVAILABLE) | The service not available on this device. |
| int | [STATUS\_ERROR](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#STATUS_ERROR) | The operation failed, without any more information. |
| int | [STATUS\_OK](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#STATUS_OK) | The operation was successful. |
| int | [UWB\_SYSTEM\_CALLBACK\_FAILURE](/android/reference/com/google/android/gms/nearby/uwb/UwbStatusCodes#UWB_SYSTEM_CALLBACK_FAILURE) | Unusual failures happened in UWB system callback, such as stopping ranging or removing a known controlee failed. |

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

#### public static final int **INVALID\_API\_CALL**

The call is not valid. For example, get Complex Channel for the controlee.

Constant Value:

42002

#### public static final int **NULL\_RANGING\_DEVICE**

The RangingDevice is null for controller or controlee.

Constant Value:

42001

#### public static final int **RANGING\_ALREADY\_STARTED**

The ranging is already started, this is a duplicated request.

Constant Value:

42003

#### public static final int **SERVICE\_NOT\_AVAILABLE**

The service not available on this device.

Constant Value:

42000

#### public static final int **STATUS\_ERROR**

The operation failed, without any more information.

Constant Value:

13

#### public static final int **STATUS\_OK**

The operation was successful.

Constant Value:

0

#### public static final int **UWB\_SYSTEM\_CALLBACK\_FAILURE**

Unusual failures happened in UWB system callback, such as stopping ranging or
removing a known controlee failed.

Constant Value:

42005