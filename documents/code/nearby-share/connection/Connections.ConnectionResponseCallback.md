<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback -->

# Connections.ConnectionResponseCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The
  `Connections.ConnectionResponseCallback`
  interface is deprecated.
* Use
  `ConnectionLifecycleCallback`
  instead of this interface.
* This interface is a callback for responses to connection requests.
* The
  `onConnectionResponse`
  method is called when a response is received for a connection request, providing the remote endpoint ID, status, and handshake data.



public static interface
**Connections.ConnectionResponseCallback**

**This interface is deprecated.**
  
Use
`ConnectionLifecycleCallback`
instead.

Callback for responses to connection requests.

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onConnectionResponse](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback#onConnectionResponse(java.lang.String,%20com.google.android.gms.common.api.Status,%20byte[])) ( [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, [Status](/android/reference/com/google/android/gms/common/api/Status) status, byte[] handshakeData) Called when a response is received for a connection request. |












## Public Methods

#### public abstract void **onConnectionResponse** ( [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, [Status](/android/reference/com/google/android/gms/common/api/Status) status, byte[] handshakeData)

Called when a response is received for a connection request.

##### Parameters

|  |  |
| --- | --- |
| remoteEndpointId | The identifier for the remote endpoint that sent the response. |
| status | The status of the response. Valid values are `ConnectionsStatusCodes.STATUS_OK` , `ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED` , and `ConnectionsStatusCodes.STATUS_NOT_CONNECTED_TO_ENDPOINT` . |
| handshakeData | Bytes of a custom message provided in the connection response (on success). This array will not exceed `Connections.MAX_RELIABLE_MESSAGE_LEN` bytes in length. |