<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/PayloadCallback -->

# PayloadCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PayloadCallback is a listener for incoming and outgoing Payloads between connected endpoints.
* The onPayloadReceived method is called when a Payload is received from a remote endpoint.
* The onPayloadTransferUpdate method provides progress information about active Payload transfers.



public abstract class
**PayloadCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Listener for incoming/outgoing
`Payload`
s
between connected endpoints.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback#PayloadCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onPayloadReceived](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback#onPayloadReceived(java.lang.String,%20com.google.android.gms.nearby.connection.Payload)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload) Called when a `Payload` is received from a remote endpoint. |
| abstract void | [onPayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback#onPayloadTransferUpdate(java.lang.String,%20com.google.android.gms.nearby.connection.PayloadTransferUpdate)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) update) Called with progress information about an active Payload transfer, either incoming or outgoing. |

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

#### public **PayloadCallback** ()





## Public Methods

#### public abstract void **onPayloadReceived** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload)

Called when a
`Payload`
is received from a remote endpoint. Depending on the type of the
`Payload`
,
all of the data may or may not have been received at the time of this call. Use
`onPayloadTransferUpdate(String, PayloadTransferUpdate)`
to get updates on the
status of the data received.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint that sent the payload. |
| payload | The `Payload` object received. |

#### public abstract void **onPayloadTransferUpdate** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) update)

Called with progress information about an active Payload transfer, either incoming
or outgoing.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint that is sending or receiving this payload. |
| update | The `PayloadTransferUpdate` describing the status of the transfer. |