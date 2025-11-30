<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback -->

# SubscribeCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* SubscribeCallback is an abstract class for handling subscription events.
* It includes a public constructor and a public method called onExpired.
* The onExpired method is called when a subscription expires due to TTL or user action.



public abstract class
**SubscribeCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Callback for events which affect subscriptions.

##### See Also

* `SubscribeOptions`

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback#SubscribeCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onExpired](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback#onExpired()) () The subscription is expired. |

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

#### public **SubscribeCallback** ()





## Public Methods

#### public void **onExpired** ()

The subscription is expired.

Called if any of the following happened:

* The specified TTL for the call elapsed.
* User stopped the Nearby actions for the app.

Using this callback is recommended for cases when you need to update state (e.g. UI
elements) when subscriptions expire.