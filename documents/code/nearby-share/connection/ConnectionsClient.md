<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient -->

# ConnectionsClient

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionsClient is the entry point for managing nearby app and service connections.
* The maximum size for byte payloads sent via sendPayload is 1047552 bytes.
* Connections can be accepted or rejected with acceptConnection and rejectConnection methods.
* The requestConnection method is used to send a connection request to a remote endpoint.
* Payloads can be sent to remote endpoints using the sendPayload method, and canceled with cancelPayload.
* The ConnectionsClient allows starting and stopping advertising and discovery for nearby services.
* stopAllEndpoints disconnects from and removes all traces of connected and discovered endpoints.



public interface
**ConnectionsClient**
implements
[HasApiKey](/android/reference/com/google/android/gms/common/api/HasApiKey)
<
[ConnectionsOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionsOptions)
>

Entry point for advertising and discovering nearby apps and services, and communicating
with them over established connections.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [MAX\_BYTES\_DATA\_SIZE](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#MAX_BYTES_DATA_SIZE) | This specifies the maximum allowed size of `Payload.Type.BYTES` `Payload` s sent via the `sendPayload(String, Payload)` method. |

### Public Method Summary

|  |  |
| --- | --- |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [acceptConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#acceptConnection(java.lang.String,%20com.google.android.gms.nearby.connection.PayloadCallback)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback) payloadCallback) Accepts a connection to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [cancelPayload](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#cancelPayload(long)) (long payloadId) Cancels a `Payload` currently in-flight to or from remote endpoint(s). |
| abstract void | [disconnectFromEndpoint](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#disconnectFromEndpoint(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) Disconnects from a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [rejectConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#rejectConnection(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) Rejects a connection to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [requestConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#requestConnection(byte[],%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback,%20com.google.android.gms.nearby.connection.ConnectionOptions)) (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) options) Sends a request to connect to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [requestConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#requestConnection(byte[],%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback)) (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback) Sends a request to connect to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [requestConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#requestConnection(java.lang.String,%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback)) ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback) Sends a request to connect to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [requestConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#requestConnection(java.lang.String,%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback,%20com.google.android.gms.nearby.connection.ConnectionOptions)) ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) options) Sends a request to connect to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [sendPayload](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#sendPayload(java.lang.String,%20com.google.android.gms.nearby.connection.Payload)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload) Sends a `Payload` to a remote endpoint. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [sendPayload](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#sendPayload(java.util.List<java.lang.String>,%20com.google.android.gms.nearby.connection.Payload)) ( [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > endpointIds, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload) Variant of `sendPayload(String, Payload)` that takes a list of remote endpoint IDs. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [startAdvertising](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#startAdvertising(byte[],%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback,%20com.google.android.gms.nearby.connection.AdvertisingOptions)) (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options) Starts advertising an endpoint for a local app. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [startAdvertising](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#startAdvertising(java.lang.String,%20java.lang.String,%20com.google.android.gms.nearby.connection.ConnectionLifecycleCallback,%20com.google.android.gms.nearby.connection.AdvertisingOptions)) ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options) Starts advertising an endpoint for a local app. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [startDiscovery](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#startDiscovery(java.lang.String,%20com.google.android.gms.nearby.connection.EndpointDiscoveryCallback,%20com.google.android.gms.nearby.connection.DiscoveryOptions)) ( [String](//developer.android.com/reference/java/lang/String.html) serviceId, [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback) endpointDiscoveryCallback, [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) options) Starts discovery for remote endpoints with the specified service ID. |
| abstract void | [stopAdvertising](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#stopAdvertising()) () Stops advertising a local endpoint. |
| abstract void | [stopAllEndpoints](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#stopAllEndpoints()) () Disconnects from, and removes all traces of, all connected and/or discovered endpoints. |
| abstract void | [stopDiscovery](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient#stopDiscovery()) () Stops discovery for remote endpoints, after a previous call to `startDiscovery(String, EndpointDiscoveryCallback, DiscoveryOptions)` , when the client no longer needs to discover endpoints or goes inactive. |







## Constants

#### public static final int **MAX\_BYTES\_DATA\_SIZE**

This specifies the maximum allowed size of
`Payload.Type.BYTES`
`Payload`
s
sent via the
`sendPayload(String, Payload)`
method.

Constant Value:

1047552







## Public Methods

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **acceptConnection** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback) payloadCallback)

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

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint. Should match the value provided in a call to `ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)` . |
| payloadCallback | A callback for payloads exchanged with the remote endpoint. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **cancelPayload** (long payloadId)

Cancels a
`Payload`
currently in-flight to or from remote endpoint(s).

##### Parameters

|  |  |
| --- | --- |
| payloadId | The identifier for the Payload to be canceled. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract void **disconnectFromEndpoint** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Disconnects from a remote endpoint.
`Payload`
s
can no longer be sent to or received from the endpoint after this method is called.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint to disconnect from. |

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **rejectConnection** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Rejects a connection to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was
  rejected.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint. Should match the value provided in a call to `ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)` . |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **requestConnection** (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) options)

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.
* `ConnectionsStatusCodes.STATUS_RADIO_ERROR`
  if we failed to connect because
  of an issue with Bluetooth/WiFi.
* `ConnectionsStatusCodes.STATUS_ERROR`
  if we failed to connect for any other
  reason.

##### Parameters

|  |  |
| --- | --- |
| endpointInfo | Identifing information about this endpoint (eg. name, device type), to appear on the remote device. Defined by client/application. |
| endpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `EndpointDiscoveryCallback.onEndpointFound(String, DiscoveredEndpointInfo)` |
| connectionLifecycleCallback | A callback notified when the remote endpoint sends a response to the connection request. |
| options | The options to set up a connection. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **requestConnection** (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback)

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.
* `ConnectionsStatusCodes.STATUS_RADIO_ERROR`
  if we failed to connect because
  of an issue with Bluetooth/WiFi.
* `ConnectionsStatusCodes.STATUS_ERROR`
  if we failed to connect for any other
  reason.

##### Parameters

|  |  |
| --- | --- |
| endpointInfo | Identifing information about this endpoint (eg. name, device type), to appear on the remote device. Defined by client/application. |
| endpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `EndpointDiscoveryCallback.onEndpointFound(String, DiscoveredEndpointInfo)` |
| connectionLifecycleCallback | A callback notified when the remote endpoint sends a response to the connection request. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **requestConnection** ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback)

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.
* `ConnectionsStatusCodes.STATUS_RADIO_ERROR`
  if we failed to connect because
  of an issue with Bluetooth/WiFi.
* `ConnectionsStatusCodes.STATUS_ERROR`
  if we failed to connect for any other
  reason.

##### Parameters

|  |  |
| --- | --- |
| name | A human readable name for the local endpoint, to appear on the remote device. Defined by client/application. |
| endpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `EndpointDiscoveryCallback.onEndpointFound(String, DiscoveredEndpointInfo)` |
| connectionLifecycleCallback | A callback notified when the remote endpoint sends a response to the connection request. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **requestConnection** ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) endpointId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) options)

Sends a request to connect to a remote endpoint.

Possible result status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if the connection request was sent.
* `ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT`
  if the app
  already has a connection to the specified endpoint.
* `ConnectionsStatusCodes.STATUS_RADIO_ERROR`
  if we failed to connect because
  of an issue with Bluetooth/WiFi.
* `ConnectionsStatusCodes.STATUS_ERROR`
  if we failed to connect for any other
  reason.

##### Parameters

|  |  |
| --- | --- |
| name | A human readable name for this endpoint, to appear on the remote device. Defined by client/application. |
| endpointId | The identifier for the remote endpoint to which a connection request will be sent. Should match the value provided in a call to `EndpointDiscoveryCallback.onEndpointFound(String, DiscoveredEndpointInfo)` |
| connectionLifecycleCallback | A callback notified when the remote endpoint sends a response to the connection request. |
| options | The options to set up a connection. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **sendPayload** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload)

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

##### Parameters

|  |  |
| --- | --- |
| endpointId | The identifier for the remote endpoint to which the payload should be sent. |
| payload | The `Payload` to be sent. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **sendPayload** ( [List](//developer.android.com/reference/java/util/List.html) < [String](//developer.android.com/reference/java/lang/String.html) > endpointIds, [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) payload)

Variant of
`sendPayload(String, Payload)`
that takes a list of remote endpoint IDs. If
none of the requested endpoints are connected,
`ConnectionsStatusCodes.STATUS_ENDPOINT_UNKNOWN`
will be returned.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **startAdvertising** (byte[] endpointInfo, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options)

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
  `stopAllEndpoints()`
  first.

##### Parameters

|  |  |
| --- | --- |
| endpointInfo | Identifing information about this endpoint (eg. name, device type), to appear on the remote device. Defined by client/application. |
| serviceId | An identifier to advertise your app to other endpoints. This can be an arbitrary string, so long as it uniquely identifies your service. A good default is to use your app's package name. |
| connectionLifecycleCallback | A callback notified when remote endpoints request a connection to this endpoint. |
| options | The options for advertising. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **startAdvertising** ( [String](//developer.android.com/reference/java/lang/String.html) name, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) connectionLifecycleCallback, [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) options)

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
  `stopAllEndpoints()`
  first.

##### Parameters

|  |  |
| --- | --- |
| name | A human readable name for this endpoint, to appear on the remote device. Defined by client/application. |
| serviceId | An identifier to advertise your app to other endpoints. This can be an arbitrary string, so long as it uniquely identifies your service. A good default is to use your app's package name. |
| connectionLifecycleCallback | A callback notified when remote endpoints request a connection to this endpoint. |
| options | The options for advertising. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **startDiscovery** ( [String](//developer.android.com/reference/java/lang/String.html) serviceId, [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback) endpointDiscoveryCallback, [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) options)

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
  `stopAllEndpoints()`
  first.

##### Parameters

|  |  |
| --- | --- |
| serviceId | The ID for the service to be discovered, as specified in the corresponding call to `startAdvertising(String, String, ConnectionLifecycleCallback, AdvertisingOptions)` . |
| endpointDiscoveryCallback | A callback notified when a remote endpoint is discovered. |
| options | The options for discovery. |

##### Returns

* `Task`
  to access
  the status of the operation when available.

#### public abstract void **stopAdvertising** ()

Stops advertising a local endpoint. Should be called after calling
`startAdvertising(String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
, as soon as the application no longer needs to advertise
itself or goes inactive.
`Payload`
s
can still be sent to connected endpoints after advertising ends.

#### public abstract void **stopAllEndpoints** ()

Disconnects from, and removes all traces of, all connected and/or discovered
endpoints. This call is expected to be preceded by a call to
`stopAdvertising()`
or
`startDiscovery(String, EndpointDiscoveryCallback, DiscoveryOptions)`
as
needed. After calling
`stopAllEndpoints()`
, no further operations with remote endpoints will be
possible until a new call to one of
`startAdvertising(String, String, ConnectionLifecycleCallback,
AdvertisingOptions)`
or
`startDiscovery(String, EndpointDiscoveryCallback, DiscoveryOptions)`
.

#### public abstract void **stopDiscovery** ()

Stops discovery for remote endpoints, after a previous call to
`startDiscovery(String, EndpointDiscoveryCallback, DiscoveryOptions)`
, when
the client no longer needs to discover endpoints or goes inactive.
`Payload`
s
can still be sent to connected endpoints after discovery ends.