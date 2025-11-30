<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback -->

# ConnectionLifecycleCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionLifecycleCallback is a listener for lifecycle events associated with a connection to a remote endpoint.
* It includes abstract methods for handling connection initiation, the final result of the connection after both sides accept or reject, and when a remote endpoint disconnects.
* It also includes a method called when a connection is established or its quality improves, indicated by a higher bandwidth.
* You should call ConnectionsClient.acceptConnection or ConnectionsClient.rejectConnection in the onConnectionInitiated method.



public abstract class
**ConnectionLifecycleCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Listener for lifecycle events associated with a connection to a remote endpoint.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#ConnectionLifecycleCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onBandwidthChanged](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#onBandwidthChanged(java.lang.String,%20com.google.android.gms.nearby.connection.BandwidthInfo)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [BandwidthInfo](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo) bandwidthInfo) Called when a connection is established or if the connection quality improves to a higher connection bandwidth. |
| abstract void | [onConnectionInitiated](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#onConnectionInitiated(java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionInfo)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionInfo](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo) connectionInfo) A basic encrypted channel has been created between you and the endpoint. |
| abstract void | [onConnectionResult](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#onConnectionResult(java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionResolution)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionResolution](/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution) resolution) Called after both sides have either accepted or rejected the connection. |
| abstract void | [onDisconnected](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback#onDisconnected(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) Called when a remote endpoint is disconnected or has become unreachable. |

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

#### public **ConnectionLifecycleCallback** ()





## Public Methods

#### public void **onBandwidthChanged** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [BandwidthInfo](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo) bandwidthInfo)

Called when a connection is established or if the connection quality improves to a
higher connection bandwidth.

#### public abstract void **onConnectionInitiated** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionInfo](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo) connectionInfo)

A basic encrypted channel has been created between you and the endpoint. Both sides
are now asked if they wish to accept or reject the connection before any data can be
sent over this channel.

This is your chance, before you accept the connection, to confirm that you connected
to the correct device. Both devices are given an identical token; it's up to you to
decide how to verify it before proceeding. Typically this involves showing the token on
both devices and having the users manually compare and confirm; however, this is only
required if you desire a secure connection between the devices.

Whichever route you decide to take (including not authenticating the other device),
call
`ConnectionsClient.acceptConnection(String, PayloadCallback)`
when you're
ready to talk, or
`ConnectionsClient.rejectConnection(String)`
to close the connection.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint. |
| connectionInfo | Other relevant information about the connection. |

#### public abstract void **onConnectionResult** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionResolution](/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution) resolution)

Called after both sides have either accepted or rejected the connection. If the
`ConnectionResolution`
's status is
`CommonStatusCodes.SUCCESS`
,
both sides have accepted the connection and may now send
`Payload`
s
to each other. Otherwise, the connection was rejected.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint. |
| resolution | The final result after tallying both devices' accept/reject responses. |

#### public abstract void **onDisconnected** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Called when a remote endpoint is disconnected or has become unreachable.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint that disconnected. |