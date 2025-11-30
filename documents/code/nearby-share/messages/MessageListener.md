<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/MessageListener -->

# MessageListener

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* MessageListener is an abstract class for receiving subscribed messages, with callbacks delivered when messages are found or lost.
* The MessageListener class includes public methods such as onBleSignalChanged, onDistanceChanged, onFound, and onLost.
* The onBleSignalChanged method is called when the BLE signal associated with a message changes.
* The onDistanceChanged method is called when the estimated distance to a message changes.
* The onFound method is called when messages are first detected nearby.
* The onLost method is called when a message is no longer detectable nearby.



public abstract class
**MessageListener**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

A listener for receiving subscribed messages. These callbacks will be delivered when
messages are found or lost.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener#MessageListener()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onBleSignalChanged](/android/reference/com/google/android/gms/nearby/messages/MessageListener#onBleSignalChanged(com.google.android.gms.nearby.messages.Message,%20com.google.android.gms.nearby.messages.BleSignal)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [BleSignal](/android/reference/com/google/android/gms/nearby/messages/BleSignal) bleSignal) Called when the Bluetooth Low Energy (BLE) signal associated with a message changes. |
| void | [onDistanceChanged](/android/reference/com/google/android/gms/nearby/messages/MessageListener#onDistanceChanged(com.google.android.gms.nearby.messages.Message,%20com.google.android.gms.nearby.messages.Distance)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) distance) Called when Nearby's estimate of the distance to a message changes. |
| void | [onFound](/android/reference/com/google/android/gms/nearby/messages/MessageListener#onFound(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Called when messages are found. |
| void | [onLost](/android/reference/com/google/android/gms/nearby/messages/MessageListener#onLost(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Called when a message is no longer detectable nearby. |

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

#### public **MessageListener** ()





## Public Methods

#### public void **onBleSignalChanged** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [BleSignal](/android/reference/com/google/android/gms/nearby/messages/BleSignal) bleSignal)

Called when the Bluetooth Low Energy (BLE) signal associated with a message
changes.

For example, this is called when we see the first BLE advertisement frame associated
with a message; or when we see subsequent frames with different RSSI.

**Note:**
This callback currently only works for BLE beacon
messages.

**Note:**
This callback is not supported by the version of
`Messages.subscribe(GoogleApiClient, PendingIntent)`
that takes a
`PendingIntent`
.

#### public void **onDistanceChanged** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) distance)

Called when Nearby's estimate of the distance to a message changes.

For example, this is called when we first gather enough information to make a
distance estimate; or when the message remains nearby, but gets closer or further
away.

**Note:**
This callback currently only works for BLE beacon
messages.

**Note:**
This callback is not supported by the version of
`Messages.subscribe(GoogleApiClient, PendingIntent)`
that takes a
`PendingIntent`
.

##### Parameters

|  |  |
| --- | --- |
| message | The message whose distance changed. |
| distance | The new distance estimate. |

#### public void **onFound** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Called when messages are found.

This method is called the first time the message is seen nearby. After a message has
been lost (see
`onLost(Message)`
), it's eligible for
`onFound(Message)`
again.

##### Parameters

|  |  |
| --- | --- |
| message | The found message. |

#### public void **onLost** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Called when a message is no longer detectable nearby.

**Note:**
This callback currently works best for BLE beacon messages.
For other messages, it may not be called in a timely fashion, or at all.

This method will not be called repeatedly (unless the message is found again between
lost calls).

##### Parameters

|  |  |
| --- | --- |
| message | The lost message. |