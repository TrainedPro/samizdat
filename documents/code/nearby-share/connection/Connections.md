<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections -->

# Connections

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The documentation provides details on using the Google Play Services Nearby Connections API for Android to manage connections between nearby devices.
* The recommended way to send data is using the
  `sendPayload`
  methods with
  `Payload.Type.BYTES`
  .
* Deprecated methods for sending byte array messages exist but should be avoided in favor of
  `sendPayload`
  .
* Apps can start advertising their presence using
  `startAdvertising`
  and discover other advertising endpoints using
  `startDiscovery`
  , with recommended and deprecated versions of these methods available.
* Methods are provided to stop advertising, stop discovery, and disconnect from all endpoints.
* All discussed methods require a
  `GoogleApiClient`
  and the
  `Nearby.CONNECTIONS_API`
  , and none require specific scopes.



public interface
**Connections**

**This interface is deprecated.**
  
Use
`ConnectionsClient`
.

Entry point for advertising and discovering nearby apps and services, and communicating
with them over established connections.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [Connections.ConnectionRequestListener](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener) | | *This class is deprecated. Use `ConnectionLifecycleCallback` instead.* |
| interface | [Connections.ConnectionResponseCallback](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback) | | *This interface is deprecated. Use `ConnectionLifecycleCallback` instead.* |
| class | [Connections.EndpointDiscoveryListener](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener) | | *This class is deprecated. Use `EndpointDiscoveryCallback` instead.* |
| interface | [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) | | *This interface is deprecated. Use `PayloadCallback` instead.* |
| interface | [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) | | Result delivered when a local endpoint starts being advertised. |

### Constant Summary

|  |  |  |
| --- | --- | --- |
| long | [DURATION\_INDEFINITE](/android/reference/com/google/android/gms/nearby/connection/Connections#DURATION_INDEFINITE) | *This constant is deprecated. Durations are no longer supported.* |
| int | [MAX\_BYTES\_DATA\_SIZE](/android/reference/com/google/android/gms/nearby/connection/Connections#MAX_BYTES_DATA_SIZE) | This specifies the maximum allowed size of `Payload.Type.BYTES` `Payload` s sent via the `sendPayload(GoogleApiClient, String, Payload)` method. |
| int | [MAX\_RELIABLE\_MESSAGE\_LEN](/android/reference/com/google/android/gms/nearby/connection/Connections#MAX_RELIABLE_MESSAGE_LEN) | *This constant is deprecated. Use `MAX_BYTES_DATA_SIZE` .* |
| int | [MAX\_UNRELIABLE\_MESSAGE\_LEN](/android/reference/com/google/android/gms/nearby/connection/Connections#MAX_UNRELIABLE_MESSAGE_LEN) | *This constant is deprecated. Use `MAX_BYTES_DATA_SIZE` .* |

### Public Method Summary

|  |  |
| --- | --- |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [acceptConnection](/android/reference/com/google/android/gms/nearby/connection/Connections#acceptConnection(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20com.google.android.gms.nearby.connection.PayloadCallback)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback) payloadCallback) Accepts a connection to a remote endpoint. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [acceptConnectionRequest](/android/reference/com/google/android/gms/nearby/connection/Connections#acceptConnectionRequest(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20byte[],%20com.google.android.gms.nearby.connection.Connections.MessageListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, byte[] handshakeData, [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) messageListener) *This method is deprecated. Use `acceptConnection(GoogleApiClient, String, PayloadCallback)` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [cancelPayload](/android/reference/com/google/android/gms/nearby/connection/Connections#cancelPayload(com.google.android.gms.common.api.GoogleApiClient,%20long)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, long payloadId) Cancels a `Payload` currently in-flight to or from remote endpoint(s). |
| abstract void | [disconnectFromEndpoint](/android/reference/com/google/android/gms/nearby/connection/Connections#disconnectFromEndpoint(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId) Disconnects from a remote endpoint. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [rejectConnection](/android/reference/com/google/android/gms/nearby/connection/Connections#rejectConnection(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId) Rejects a connection to a remote endpoint. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [rejectConnectionRequest](/android/reference/com/google/android/gms/nearby/connection/Connections#rejectConnectionRequest(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId) *This method is deprecated. Use `rejectConnection(GoogleApiClient, String)` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [requestConnection](/android/reference/com/google/android/gms/nearby/connection/Connections#requestConnection(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback) Sends a request to connect to a remote endpoint. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [sendConnectionRequest](/android/reference/com/google/android/gms/nearby/connection/Connections#sendConnectionRequest(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20java.lang.String,%20byte[],%20com.google.android.gms.nearby.connection.Connections.ConnectionResponseCallback,%20com.google.android.gms.nearby.connection.Connections.MessageListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] handshakeData, [Connections.ConnectionResponseCallback](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback) connectionResponseCallback, [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) messageListener) *This method is deprecated. Use `requestConnection(GoogleApiClient, String, String, ConnectionLifecycleCallback)` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [sendPayload](/android/reference/com/google/android/gms/nearby/connection/Connections#sendPayload(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20com.google.android.gms.nearby.connection.Payload)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload) Sends a `Payload` to a remote endpoint. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [sendPayload](/android/reference/com/google/android/gms/nearby/connection/Connections#sendPayload(com.google.android.gms.common.api.GoogleApiClient,%20java.util.List<java.lang.String>,%20com.google.android.gms.nearby.connection.Payload)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > endpointIds, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload) Variant of `sendPayload(GoogleApiClient, String, Payload)` that takes a list of remote endpoint IDs. |
| abstract void | [sendReliableMessage](/android/reference/com/google/android/gms/nearby/connection/Connections#sendReliableMessage(com.google.android.gms.common.api.GoogleApiClient,%20java.util.List<java.lang.String>,%20byte[])) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > remoteEndpointIds, byte[] payload) *This method is deprecated. Use `sendPayload(GoogleApiClient, List, Payload)` with `Payload.Type.BYTES` instead.* |
| abstract void | [sendReliableMessage](/android/reference/com/google/android/gms/nearby/connection/Connections#sendReliableMessage(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20byte[])) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] payload) *This method is deprecated. Use `sendPayload(GoogleApiClient, String, Payload)` with `Payload.Type.BYTES` instead.* |
| abstract void | [sendUnreliableMessage](/android/reference/com/google/android/gms/nearby/connection/Connections#sendUnreliableMessage(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20byte[])) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] payload) *This method is deprecated. Use `sendPayload(GoogleApiClient, String, Payload)` with `Payload.Type.BYTES` instead.* |
| abstract void | [sendUnreliableMessage](/android/reference/com/google/android/gms/nearby/connection/Connections#sendUnreliableMessage(com.google.android.gms.common.api.GoogleApiClient,%20java.util.List<java.lang.String>,%20byte[])) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > remoteEndpointIds, byte[] payload) *This method is deprecated. Use `sendPayload(GoogleApiClient, List, Payload)` with `Payload.Type.BYTES` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) > | [startAdvertising](/android/reference/com/google/android/gms/nearby/connection/Connections#startAdvertising(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback,%20com.google.android.gms.nearby.connection.AdvertisingOptions)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options) Starts advertising an endpoint for a local app. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) > | [startAdvertising](/android/reference/com/google/android/gms/nearby/connection/Connections#startAdvertising(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20com.google.android.gms.nearby.connection.AppMetadata,%20long,%20com.google.android.gms.nearby.connection.Connections.ConnectionRequestListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata) appMetadata, long durationMillis, [Connections.ConnectionRequestListener](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener) connectionRequestListener) *This method is deprecated. Use `startAdvertising(GoogleApiClient, String, String, ConnectionLifecycleCallback, AdvertisingOptions)` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [startDiscovery](/android/reference/com/google/android/gms/nearby/connection/Connections#startDiscovery(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20long,%20com.google.android.gms.nearby.connection.Connections.EndpointDiscoveryListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId, long durationMillis, [Connections.EndpointDiscoveryListener](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener) listener) *This method is deprecated. Use `startDiscovery(GoogleApiClient, String, EndpointDiscoveryCallback, DiscoveryOptions)` instead.* |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [startDiscovery](/android/reference/com/google/android/gms/nearby/connection/Connections#startDiscovery(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String,%20com.google.android.gms.nearby.connection.EndpointDiscoveryCallback,%20com.google.android.gms.nearby.connection.DiscoveryOptions)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback) endpointDiscoveryCallback, [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) options) Starts discovery for remote endpoints with the specified service ID. |
| abstract void | [stopAdvertising](/android/reference/com/google/android/gms/nearby/connection/Connections#stopAdvertising(com.google.android.gms.common.api.GoogleApiClient)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient) Stops advertising a local endpoint. |
| abstract void | [stopAllEndpoints](/android/reference/com/google/android/gms/nearby/connection/Connections#stopAllEndpoints(com.google.android.gms.common.api.GoogleApiClient)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient) Disconnects from, and removes all traces of, all connected and/or discovered endpoints. |
| abstract void | [stopDiscovery](/android/reference/com/google/android/gms/nearby/connection/Connections#stopDiscovery(com.google.android.gms.common.api.GoogleApiClient)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient) Stops discovery for remote endpoints, after a previous call to `startDiscovery(GoogleApiClient, String, com.google.android.gms.nearby.connection.EndpointDiscoveryCallback, DiscoveryOptions)` , when the client no longer needs to discover endpoints or goes inactive. |
| abstract void | [stopDiscovery](/android/reference/com/google/android/gms/nearby/connection/Connections#stopDiscovery(com.google.android.gms.common.api.GoogleApiClient,%20java.lang.String)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId) *This method is deprecated. Use `stopDiscovery(GoogleApiClient)` instead. You can only discover for a single serviceId at a time, so no need to pass in the serviceId.* |







## Constants

#### public static final long **DURATION\_INDEFINITE**

**This constant is deprecated.**
  
Durations are no longer supported.

Value for duration meaning advertising / discovery should continue indefinitely
until the application asks it to stop.

Constant Value:

0

#### public static final int **MAX\_BYTES\_DATA\_SIZE**

This specifies the maximum allowed size of
`Payload.Type.BYTES`
`Payload`
s
sent via the
`sendPayload(GoogleApiClient, String, Payload)`
method.

Constant Value:

32768

#### public static final int **MAX\_RELIABLE\_MESSAGE\_LEN**

**This constant is deprecated.**
  
Use
`MAX_BYTES_DATA_SIZE`
.

This gives the maximum payload size supported via the
`sendReliableMessage(GoogleApiClient, String, byte[])`
,
`sendConnectionRequest(GoogleApiClient, String, String, byte[],
Connections.ConnectionResponseCallback, Connections.MessageListener)`
, and
`acceptConnectionRequest(GoogleApiClient, String, byte[],
Connections.MessageListener)`
methods.

Constant Value:

4096

#### public static final int **MAX\_UNRELIABLE\_MESSAGE\_LEN**

**This constant is deprecated.**
  
Use
`MAX_BYTES_DATA_SIZE`
.

This gives the maximum payload size supported via the
`sendUnreliableMessage(GoogleApiClient, String, byte[])`
methods.

Constant Value:

1168







## Public Methods

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **acceptConnection** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback) payloadCallback)

Accepts a connection to a remote endpoint. This method must be called before
`Payload`
s
can be exchanged with the remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was
  accepted.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| endpointId | The identifier for the remote endpoint. Should match the value provided in a call to `ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)` . |
| payloadCallback | A callback for payloads exchanged with the remote endpoint. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **acceptConnectionRequest** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, byte[] handshakeData, [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) messageListener)

**This method is deprecated.**
  
Use
`acceptConnection(GoogleApiClient, String, PayloadCallback)`
instead.

Accepts a connection request from a remote endpoint. This method must be called
before messages can be received from the remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was
  accepted.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| endpointId | The identifier for the remote endpoint that sent the connection request. Should match the value provided in a call to `Connections.ConnectionRequestListener.onConnectionRequest(String, String, byte[])` . |
| handshakeData | Bytes of a custom message to send with the connection response. This message must not exceed `MAX_BYTES_DATA_SIZE` bytes in length. |
| messageListener | A listener notified when a message is received from the remote endpoint, or it disconnects. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **cancelPayload** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, long payloadId)

Cancels a
`Payload`
currently in-flight to or from remote endpoint(s).

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| payloadId | The identifier for the Payload to be canceled. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract void **disconnectFromEndpoint** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Disconnects from a remote endpoint.
`Payload`
s
can no longer be sent to or received from the endpoint after this method is called.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| endpointId | The identifier for the remote endpoint to disconnect from. |

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **rejectConnection** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Rejects a connection to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was
  rejected.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| endpointId | The identifier for the remote endpoint. Should match the value provided in a call to `ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)` . |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **rejectConnectionRequest** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId)

**This method is deprecated.**
  
Use
`rejectConnection(GoogleApiClient, String)`
instead.

Rejects a connection request from a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was
  rejected.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| remoteEndpointId | The identifier for the remote endpoint that sent the connection request. Should match the value provided in a call to `Connections.ConnectionRequestListener.onConnectionRequest(String, String, byte[])` . |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **requestConnection** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback)

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| name | A human readable name for the local endpoint, to appear on the remote endpoint. If null or empty, a name will be generated based on the device name or model. |
| endpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `EndpointDiscoveryCallback.onEndpointFound(String, DiscoveredEndpointInfo)` |
| connectionLifecycleCallback | A callback notified when the remote endpoint sends a response to the connection request. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **sendConnectionRequest** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] handshakeData, [Connections.ConnectionResponseCallback](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback) connectionResponseCallback, [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) messageListener)

**This method is deprecated.**
  
Use
`requestConnection(GoogleApiClient, String, String,
ConnectionLifecycleCallback)`
instead.

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| name | A human readable name for the local endpoint, to appear on the remote endpoint. If null or empty, a name will be generated based on the device name or model. |
| remoteEndpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `Connections.EndpointDiscoveryListener.onEndpointFound(String, String, String)` |
| handshakeData | Bytes of a custom message to send with the connection request. This message must not exceed `MAX_RELIABLE_MESSAGE_LEN` bytes in length. |
| connectionResponseCallback | A callback notified when the remote endpoint sends a response to the connection request. |
| messageListener | A listener notified when a message is received from the remote endpoint, or it disconnects. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **sendPayload** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload)

Sends a
`Payload`
to a remote endpoint. Payloads can only be sent to remote endpoints once a notice of
connection acceptance has been delivered via
`ConnectionLifecycleCallback.onConnectionResult(String,
ConnectionResolution)`

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL`
  if the device has not
  first performed advertisement or discovery (to set the
  `Strategy`
  ).
* `ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN`
  if there's no active (or
  pending) connection to the remote endpoint.
* `ConnectionsStatusCodes.STATUS_OK`
  if none of the above errors occurred.
  Note that this indicates that Nearby Connections will attempt to send the
  `Payload`
  ,
  but not that the send has successfully completed yet. Errors might still occur during
  transmission (and at different times for different endpoints), and will be delivered
  via
  `PayloadCallback.onPayloadTransferUpdate(String,
  PayloadTransferUpdate)`
  .

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| endpointId | The identifier for the remote endpoint to which the payload should be sent. |
| payload | The `Payload` to be sent. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **sendPayload** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > endpointIds, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload)

Variant of
`sendPayload(GoogleApiClient, String, Payload)`
that takes a list of remote
endpoint IDs. If none of the requested endpoints are connected,
`ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN`
will be returned.

#### public abstract void **sendReliableMessage** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > remoteEndpointIds, byte[] payload)

**This method is deprecated.**
  
Use
`sendPayload(GoogleApiClient, List, Payload)`
with
`Payload.Type.BYTES`
instead.

Variant of
`sendReliableMessage(GoogleApiClient, String, byte[])`
that takes a list of
remote endpoint IDs.

#### public abstract void **sendReliableMessage** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] payload)

**This method is deprecated.**
  
Use
`sendPayload(GoogleApiClient, String, Payload)`
with
`Payload.Type.BYTES`
instead.

Sends a message to a remote endpoint using a reliable protocol. Reliable messages
will be retried until delivered, and are delivered in the order they were sent to a
given endpoint. Messages can only be sent to remote endpoints once a connection request
was first sent and accepted (in either direction).

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| remoteEndpointId | The identifier for the remote endpoint to which the message should be sent. |
| payload | The bytes of the message to send to the remote endpoint. This message must not exceed `MAX_RELIABLE_MESSAGE_LEN` bytes in length. |

#### public abstract void **sendUnreliableMessage** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) remoteEndpointId, byte[] payload)

**This method is deprecated.**
  
Use
`sendPayload(GoogleApiClient, String, Payload)`
with
`Payload.Type.BYTES`
instead.

Sends a message to a remote endpoint using an unreliable protocol. Unreliable
messages may be dropped or delivered out of order. Messages can only be sent to remote
endpoints once a connection request was first sent and accepted (in either
direction).

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| remoteEndpointId | The identifier for the remote endpoint to which the message should be sent. |
| payload | The bytes of the message to send to the remote endpoint. This message must not exceed `MAX_UNRELIABLE_MESSAGE_LEN` bytes in length. |

#### public abstract void **sendUnreliableMessage** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > remoteEndpointIds, byte[] payload)

**This method is deprecated.**
  
Use
`sendPayload(GoogleApiClient, List, Payload)`
with
`Payload.Type.BYTES`
instead.

Variant of
`sendUnreliableMessage(GoogleApiClient, String, byte[])`
that takes a list of
remote endpoint IDs.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) > **startAdvertising** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options)

Starts advertising an endpoint for a local app.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if advertising started successfully.
* `ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING`
  if the app is already
  advertising.
* `ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL`
  if the app is
  currently connected to remote endpoints; call
  `stopAllEndpoints(GoogleApiClient)`
  first.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| name | A human readable name for this endpoint, to appear on other devices. If null or empty, a name will be generated based on the device name or model. |
| serviceId | An identifier to advertise your app to other endpoints. This can be an arbitrary string, so long as it uniquely identifies your service. A good default is to use your app's package name. |
| connectionLifecycleCallback | A callback notified when remote endpoints request a connection to this endpoint. |
| options | The options for advertising. |

##### Returns

* `PendingResult`
  to access the data when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) > **startAdvertising** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) name, [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata) appMetadata, long durationMillis, [Connections.ConnectionRequestListener](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener) connectionRequestListener)

**This method is deprecated.**
  
Use
`startAdvertising(GoogleApiClient, String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
instead.

Starts advertising an endpoint for a local app.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if advertising started successfully.
* `ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING`
  if the app is currently
  discovering.
* `ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING`
  if the app is already
  advertising.
* `ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL`
  if the app is
  currently connected to remote endpoints; call
  `stopAllEndpoints(GoogleApiClient)`
  first.

To advertise an endpoint you must specify a service ID in a meta-data tag with the
name
`com.google.android.gms.nearby.connection.SERVICE_ID`
inside your
application tag, like so:

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| name | A human readable name for this endpoint, to appear on other devices. If null or empty, a name will be generated based on the device name or model. |
| appMetadata | Metadata used to describe this application which can be used to prompt the user to launch or install the application. If null, only applications looking for the specified service ID will be able to discover this endpoint. |
| durationMillis | The duration of the advertisement in milliseconds, unless `stopAdvertising(com.google.android.gms.common.api.GoogleApiClient)` is called first. If `DURATION_INDEFINITE` is passed in, the advertisement will continue indefinitely until `stopAdvertising(com.google.android.gms.common.api.GoogleApiClient)` is called. |
| connectionRequestListener | A listener notified when remote endpoints request a connection to this endpoint. |

##### Returns

* `PendingResult`
  to access the data when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **startDiscovery** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId, long durationMillis, [Connections.EndpointDiscoveryListener](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener) listener)

**This method is deprecated.**
  
Use
`startDiscovery(GoogleApiClient, String, EndpointDiscoveryCallback,
DiscoveryOptions)`
instead.

Starts discovery for remote endpoints with the specified service ID.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if discovery started successfully.
* `ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING`
  if the app is already
  discovering the specified service.
* `ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING`
  if the app is currently
  advertising.
* `ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL`
  if the app is
  currently connected to remote endpoints; call
  `stopAllEndpoints(GoogleApiClient)`
  first.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| serviceId | The ID for the service to be discovered, as specified in its manifest. |
| durationMillis | The duration of discovery in milliseconds, unless `stopDiscovery(com.google.android.gms.common.api.GoogleApiClient, String)` is called first. If `DURATION_INDEFINITE` is passed in, discovery will continue indefinitely until `stopDiscovery(com.google.android.gms.common.api.GoogleApiClient, String)` is called. |
| listener | A listener notified when a remote endpoint is discovered. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **startDiscovery** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback) endpointDiscoveryCallback, [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) options)

Starts discovery for remote endpoints with the specified service ID.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if discovery started successfully.
* `ConnectionsStatusCodes.STATUS_ALREADY_DISCOVERING`
  if the app is already
  discovering the specified service.
* `ConnectionsStatusCodes.STATUS_OUT_OF_ORDER_API_CALL`
  if the app is
  currently connected to remote endpoints; call
  `stopAllEndpoints(GoogleApiClient)`
  first.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| serviceId | The ID for the service to be discovered, as specified in the corresponding call to `startAdvertising(GoogleApiClient, String, String, ConnectionLifecycleCallback, AdvertisingOptions)` . |
| endpointDiscoveryCallback | A callback notified when a remote endpoint is discovered. |
| options | The options for discovery. |

##### Returns

* `PendingResult`
  to access the status of the operation when available.

#### public abstract void **stopAdvertising** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient)

Stops advertising a local endpoint. Should be called after calling
`startAdvertising(GoogleApiClient, String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
, as soon as the application no longer needs to advertise
itself or goes inactive.
`Payload`
s
can still be sent to connected endpoints after advertising ends.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |

#### public abstract void **stopAllEndpoints** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient)

Disconnects from, and removes all traces of, all connected and/or discovered
endpoints. This call is expected to be preceded by a call to
`stopAdvertising(GoogleApiClient)`
or
`startDiscovery(GoogleApiClient, String, EndpointDiscoveryCallback,
DiscoveryOptions)`
as needed. After calling
`stopAllEndpoints(GoogleApiClient)`
, no further operations with remote
endpoints will be possible until a new call to one of
`startAdvertising(GoogleApiClient, String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
or
`startDiscovery(GoogleApiClient, String, EndpointDiscoveryCallback,
DiscoveryOptions)`
.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |

#### public abstract void **stopDiscovery** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient)

Stops discovery for remote endpoints, after a previous call to
`startDiscovery(GoogleApiClient, String,
com.google.android.gms.nearby.connection.EndpointDiscoveryCallback,
DiscoveryOptions)`
, when the client no longer needs to discover endpoints or
goes inactive.
`Payload`
s
can still be sent to connected endpoints after discovery ends.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |

#### public abstract void **stopDiscovery** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) apiClient, [String](//developer.android.com/reference/java/lang/String.html) serviceId)

**This method is deprecated.**
  
Use
`stopDiscovery(GoogleApiClient)`
instead. You can only discover for a single
serviceId at a time, so no need to pass in the serviceId.

Stops discovery for remote endpoints with the specified service ID. Should be called
after calling
`startDiscovery(com.google.android.gms.common.api.GoogleApiClient, String, long,
EndpointDiscoveryListener)`
, with the same service ID value, as soon as the
client no longer needs to discover endpoints or goes inactive. Messages can still be
sent to remote endpoints after discovery ends.

Required API:
`Nearby.CONNECTIONS_API`
  
Required Scopes: None

##### Parameters

|  |  |
| --- | --- |
| apiClient | The `GoogleApiClient` to service the call. |
| serviceId | The ID for the service to stop being discovered. |