<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/StatusCallback -->

# StatusCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* StatusCallback is an abstract class for receiving callbacks on global status changes affecting a client of Nearby Messages.
* It has a public constructor, StatusCallback().
* It includes a public method, onPermissionChanged(boolean), which is called when permission to use Nearby is granted or revoked for the app.
* The onPermissionChanged method indicates whether permission is granted or not through a boolean parameter.



public abstract class
**StatusCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Callbacks for global status changes that affect a client of Nearby Messages.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback#StatusCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onPermissionChanged](/android/reference/com/google/android/gms/nearby/messages/StatusCallback#onPermissionChanged(boolean)) (boolean permissionGranted) Called when permission is granted or revoked for this app to use Nearby. |

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

#### public **StatusCallback** ()





## Public Methods

#### public void **onPermissionChanged** (boolean permissionGranted)

Called when permission is granted or revoked for this app to use Nearby.

##### Parameters

|  |  |
| --- | --- |
| permissionGranted | if true, your app is allowed to use Nearby, false otherwise. |