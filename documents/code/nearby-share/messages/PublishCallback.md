<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/PublishCallback -->

# PublishCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PublishCallback is an abstract class used for handling events related to published messages.
* It includes a public constructor
  `PublishCallback()`
  and a public method
  `onExpired()`
  .
* The
  `onExpired()`
  method is called when a published message expires due to the specified TTL or user action.



public abstract class
**PublishCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Callback for events which affect published messages.

##### See Also

* `PublishOptions`

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback#PublishCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onExpired](/android/reference/com/google/android/gms/nearby/messages/PublishCallback#onExpired()) () The published message is expired. |

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

#### public **PublishCallback** ()





## Public Methods

#### public void **onExpired** ()

The published message is expired.

Called if any of the following happened:

* The specified TTL for the call elapsed.
* User stopped the Nearby actions for the app.

Using this callback is recommended for cases when you need to update state (e.g. UI
elements) when published messages expire.