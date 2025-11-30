<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener -->

# Connections.MessageListener

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Connections.MessageListener interface is deprecated and should be replaced with PayloadCallback.
* The onDisconnected method of this interface is deprecated and replaced by ConnectionLifecycleCallback.onDisconnected(String).
* The onMessageReceived method of this interface is deprecated and replaced by PayloadCallback.onPayloadReceived(String, Payload).



public static interface
**Connections.MessageListener**

**This interface is deprecated.**
  
Use
`PayloadCallback`
instead.

Listener for messages from a remote endpoint.

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onDisconnected](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener#onDisconnected(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) *This method is deprecated. Implement `ConnectionLifecycleCallback.onDisconnected(String)` instead.* |
| abstract void | [onMessageReceived](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener#onMessageReceived(java.lang.String,%20byte[],%20boolean)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, byte[] payload, boolean isReliable) *This method is deprecated. Implement `PayloadCallback.onPayloadReceived(String, Payload)` instead.* |












## Public Methods

#### public abstract void **onDisconnected** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

**This method is deprecated.**
  
Implement
`ConnectionLifecycleCallback.onDisconnected(String)`
instead.

Called when a remote endpoint is disconnected / becomes unreachable.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint that disconnected. |

#### public abstract void **onMessageReceived** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, byte[] payload, boolean isReliable)

**This method is deprecated.**
  
Implement
`PayloadCallback.onPayloadReceived(String, Payload)`
instead.

Called when a message is received from a remote endpoint.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint that sent the message. |
| payload | The bytes of the message sent by the remote endpoint. This array will not exceed `Connections.MAX_RELIABLE_MESSAGE_LEN` bytes for reliable messages, or `Connections.MAX_UNRELIABLE_MESSAGE_LEN` for unreliable ones. |
| isReliable | True if the message was sent reliably, false otherwise. |