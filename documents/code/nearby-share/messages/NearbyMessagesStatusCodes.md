<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes -->

# NearbyMessagesStatusCodes

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* NearbyMessagesStatusCodes provides status codes specific to the Nearby.Messages API.
* These status codes can be used with
  `Status.getStatusCode()`
  to understand the result of an operation.
* Various constants represent specific error conditions like insufficient permissions, quota limits, or unsupported BLE features.
* A method
  `getStatusCodeString(int statusCode)`
  is available to get a debug string for a given status code.



public class
**NearbyMessagesStatusCodes**
extends
[CommonStatusCodes](/android/reference/com/google/android/gms/common/api/CommonStatusCodes)

Nearby.Messages specific status codes, for use in
`Status.getStatusCode()`
.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [APP\_NOT\_OPTED\_IN](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#APP_NOT_OPTED_IN) | Status code indicating that the User has not granted the calling application permission to use Nearby.Messages. |
| int | [APP\_QUOTA\_LIMIT\_REACHED](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#APP_QUOTA_LIMIT_REACHED) | The app has reached its quota limit to use Nearby Messages API. |
| int | [BLE\_ADVERTISING\_UNSUPPORTED](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#BLE_ADVERTISING_UNSUPPORTED) | The client requested an operation that requires Bluetooth Low Energy advertising (such as publishing with `Strategy.BLE_ONLY` ), but this feature is not supported. |
| int | [BLE\_SCANNING\_UNSUPPORTED](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#BLE_SCANNING_UNSUPPORTED) | The client requested an operation that requires Bluetooth Low Energy scanning (such as subscribing with `Strategy.BLE_ONLY` ), but this feature is not supported. |
| int | [BLUETOOTH\_OFF](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#BLUETOOTH_OFF) | Bluetooth is currently off. |
| int | [DISALLOWED\_CALLING\_CONTEXT](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#DISALLOWED_CALLING_CONTEXT) | The app is issuing an operation using a `GoogleApiClient` bound to an inappropriate Context; see the relevant method's documentation (for example, `Messages.publish(GoogleApiClient, Message, PublishOptions)` ) to see its list of allowed Contexts. |
| int | [FORBIDDEN](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#FORBIDDEN) | The request could not be completed because it was disallowed. |
| int | [MISSING\_PERMISSIONS](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#MISSING_PERMISSIONS) | The request could not be completed because it was disallowed. |
| int | [NOT\_AUTHORIZED](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#NOT_AUTHORIZED) |  |
| int | [TOO\_MANY\_PENDING\_INTENTS](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#TOO_MANY_PENDING_INTENTS) | The app has issued more than 5 `PendingIntent` to the Messages#subscribe. |

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

### Public Method Summary

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | [getStatusCodeString](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes#getStatusCodeString(int)) (int statusCode) |

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

#### public static final int **APP\_NOT\_OPTED\_IN**

Status code indicating that the User has not granted the calling application
permission to use Nearby.Messages.

Resolution: The application can use the returned PendingIntent to request user
consent.

Constant Value:

2802

#### public static final int **APP\_QUOTA\_LIMIT\_REACHED**

The app has reached its quota limit to use Nearby Messages API. Use the Quota
request form for the Nearby Messages API in your project's developer console to request
more quota.

Constant Value:

2804

#### public static final int **BLE\_ADVERTISING\_UNSUPPORTED**

The client requested an operation that requires Bluetooth Low Energy advertising
(such as publishing with
`Strategy.BLE_ONLY`
),
but this feature is not supported.

##### See Also

* [Bluetooth Low
  Energy | Android Developers](//developer.android.com/guide/topics/connectivity/bluetooth-le.html)

Constant Value:

2821

#### public static final int **BLE\_SCANNING\_UNSUPPORTED**

The client requested an operation that requires Bluetooth Low Energy scanning (such
as subscribing with
`Strategy.BLE_ONLY`
),
but this feature is not supported.

##### See Also

* [Bluetooth Low
  Energy | Android Developers](//developer.android.com/guide/topics/connectivity/bluetooth-le.html)

Constant Value:

2822

#### public static final int **BLUETOOTH\_OFF**

Bluetooth is currently off.

Constant Value:

2820

#### public static final int **DISALLOWED\_CALLING\_CONTEXT**

The app is issuing an operation using a
`GoogleApiClient`
bound to an inappropriate Context; see the relevant method's documentation (for
example,
`Messages.publish(GoogleApiClient, Message, PublishOptions)`
) to see its list
of allowed Contexts.

Constant Value:

2803

#### public static final int **FORBIDDEN**

The request could not be completed because it was disallowed. The issue is not
resolvable by the client, and the request should not be retried.

Constant Value:

2806

#### public static final int **MISSING\_PERMISSIONS**

The request could not be completed because it was disallowed. Check the error
message to see what permission is missing and make sure the right
`NearbyPermissions`
is specified for
`MessagesOptions.Builder.setPermissions(int)`
.

Constant Value:

2807

#### public static final int **NOT\_AUTHORIZED**

Constant Value:

2805

#### public static final int **TOO\_MANY\_PENDING\_INTENTS**

The app has issued more than 5
`PendingIntent`
to the Messages#subscribe. Some requests need to be removed before adding more.

Constant Value:

2801







## Public Methods

#### public static [String](//developer.android.com/reference/java/lang/String.html) **getStatusCodeString** (int statusCode)

##### Returns

* An untranslated debug (not user-friendly!) string based on the given status
  code.