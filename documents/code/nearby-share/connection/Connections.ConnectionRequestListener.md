<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener -->

# Connections.ConnectionRequestListener

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* This class, Connections.ConnectionRequestListener, is deprecated and should be replaced by ConnectionLifecycleCallback.
* It serves as a listener for when a remote endpoint requests a connection to a local endpoint.
* The class has one public method, onConnectionRequest, which is called when a connection request is received and provides details about the remote endpoint.



public static abstract class
**Connections.ConnectionRequestListener**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

**This class is deprecated.**
  
Use
`ConnectionLifecycleCallback`
instead.

Listener invoked when a remote endpoint requests a connection to a local endpoint.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [ConnectionRequestListener](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener#ConnectionRequestListener()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onConnectionRequest](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener#onConnectionRequest(java.lang.String,%20java.lang.String,%20byte[])) ( [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointName, byte[] handshakeData) Called when a remote endpoint requests a connection to a local endpoint. |

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

#### public **ConnectionRequestListener** ()





## Public Methods

#### public void **onConnectionRequest** ( [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointName, byte[] handshakeData)

Called when a remote endpoint requests a connection to a local endpoint.

##### Parameters

|  |  |
| --- | --- |
| remoteEndpointId | The ID of the remote endpoint requesting a connection. |
| remoteEndpointName | The human readable name of the remote endpoint. |
| handshakeData | Bytes of a custom message sent with the connection request. |