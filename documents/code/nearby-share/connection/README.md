<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/package-summary -->

# com.google.android.gms.nearby.connection

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Many interfaces and classes related to Nearby Connections, such as
  `Connections`
  ,
  `ConnectionResponseCallback`
  ,
  `MessageListener`
  ,
  `AppIdentifier`
  ,
  `AppMetadata`
  ,
  `ConnectionRequestListener`
  , and
  `EndpointDiscoveryListener`
  , are deprecated and replaced by newer alternatives like
  `ConnectionsClient`
  ,
  `ConnectionLifecycleCallback`
  , and
  `PayloadCallback`
  .
* The
  `ConnectionsClient`
  is the current entry point for advertising and discovering nearby apps and services and managing connections.
* Various classes provide information and options for managing advertising, discovery, connections, and data transfer, including
  `AdvertisingOptions`
  ,
  `DiscoveryOptions`
  ,
  `ConnectionInfo`
  ,
  `BandwidthInfo`
  ,
  `Payload`
  , and
  `PayloadTransferUpdate`
  .
* The
  `Payload`
  class represents data sent between devices and includes nested classes for file and stream types.
* Status codes and exceptions for Nearby Connections results are available through
  `ConnectionsStatusCodes`
  and
  `AuthenticationException`
  .

### Annotations

|  |  |
| --- | --- |
| [BandwidthInfo.Quality](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality) | Bandwidth quality. |
| [ConnectionType](/android/reference/com/google/android/gms/nearby/connection/ConnectionType) | The connection type which Nearby Connection used to establish a connection. |
| [Payload.Type](/android/reference/com/google/android/gms/nearby/connection/Payload.Type) | The type of this payload. |
| [PayloadTransferUpdate.Status](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status) | The status of the payload transfer at the time of this update. |

### Interfaces

|  |  |
| --- | --- |
| [Connections](/android/reference/com/google/android/gms/nearby/connection/Connections) | *This interface is deprecated. Use `ConnectionsClient` .* |
| [Connections.ConnectionResponseCallback](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionResponseCallback) | *This interface is deprecated. Use `ConnectionLifecycleCallback` instead.* |
| [Connections.MessageListener](/android/reference/com/google/android/gms/nearby/connection/Connections.MessageListener) | *This interface is deprecated. Use `PayloadCallback` instead.* |
| [Connections.StartAdvertisingResult](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult) | Result delivered when a local endpoint starts being advertised. |
| [ConnectionsClient](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) | Entry point for advertising and discovering nearby apps and services, and communicating with them over established connections. |

### Classes

|  |  |
| --- | --- |
| [AdvertisingOptions](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions) | Options for a call to `ConnectionsClient.startAdvertising(String, String, ConnectionLifecycleCallback, AdvertisingOptions)` . |
| [AdvertisingOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/AdvertisingOptions.Builder) | Builder class for AdvertisingOptions. |
| [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) | *This class is deprecated. This class is no longer used.* *An identifier for an application; the value of the identifier should be the package name for an Android application to be installed or launched to discover and communicate with the advertised service (e.g. com.example.myapp). Google applications may use this data to prompt the user to install the application.* |
| [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata) | *This class is deprecated. This class is no longer used.* *Metadata about an application. Contains one or more `AppIdentifier` objects indicating identifiers that can be used to install or launch application(s) that can discover and communicate with the advertised service. Google applications may use this data to prompt the user to install the application.* |
| [BandwidthInfo](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo) | Information about a connection's bandwidth. |
| [ConnectionInfo](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo) | Information about a connection that is being initiated. |
| [ConnectionLifecycleCallback](/android/reference/com/google/android/gms/nearby/connection/ConnectionLifecycleCallback) | Listener for lifecycle events associated with a connection to a remote endpoint. |
| [ConnectionOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions) | Options for a call to `ConnectionsClient.requestConnection(byte[], String, ConnectionLifecycleCallback)` . |
| [ConnectionOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionOptions.Builder) | Builder class for ConnectionOptions. |
| [ConnectionResolution](/android/reference/com/google/android/gms/nearby/connection/ConnectionResolution) | The result after `ConnectionLifecycleCallback.onConnectionInitiated(String, ConnectionInfo)` . |
| [Connections.ConnectionRequestListener](/android/reference/com/google/android/gms/nearby/connection/Connections.ConnectionRequestListener) | *This class is deprecated. Use `ConnectionLifecycleCallback` instead.* |
| [Connections.EndpointDiscoveryListener](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener) | *This class is deprecated. Use `EndpointDiscoveryCallback` instead.* |
| [ConnectionsOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionsOptions) | Configuration parameters for the `Connections` API. |
| [ConnectionsOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/ConnectionsOptions.Builder) | Builder for `ConnectionsOptions` . |
| [ConnectionsStatusCodes](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes) | Status codes for nearby connections results. |
| [DiscoveredEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo) | Information about an endpoint when it's discovered. |
| [DiscoveryOptions](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions) | Options for a call to `ConnectionsClient.startDiscovery(String, EndpointDiscoveryCallback, DiscoveryOptions)` . |
| [DiscoveryOptions.Builder](/android/reference/com/google/android/gms/nearby/connection/DiscoveryOptions.Builder) | Builder class for DiscoveryOptions. |
| [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback) | Listener invoked during endpoint discovery. |
| [Payload](/android/reference/com/google/android/gms/nearby/connection/Payload) | A Payload sent between devices. |
| [Payload.File](/android/reference/com/google/android/gms/nearby/connection/Payload.File) | Represents a file in local storage on the device. |
| [Payload.Stream](/android/reference/com/google/android/gms/nearby/connection/Payload.Stream) | Represents a stream of data. |
| [PayloadCallback](/android/reference/com/google/android/gms/nearby/connection/PayloadCallback) | Listener for incoming/outgoing `Payload` s between connected endpoints. |
| [PayloadTransferUpdate](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate) | Describes the status for an active `Payload` transfer, either incoming or outgoing. |
| [PayloadTransferUpdate.Builder](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Builder) | Builder class for PayloadTransferUpdate. |
| [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | The Strategy to be used when discovering or advertising to Nearby devices. |

### Exceptions

|  |  |
| --- | --- |
| [AuthenticationException](/android/reference/com/google/android/gms/nearby/connection/AuthenticationException) | `AuthenticationException` for DeviceProvider connection. |