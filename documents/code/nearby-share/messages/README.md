<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/package-summary -->

# com.google.android.gms.nearby.messages

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Distance.Accuracy and NearbyPermissions are annotations providing possible values for distance accuracy and determining the scope of permissions, respectively.
* The Interfaces section includes representations for BLE signals, distance to messages, and the main interfaces for the API, Messages and MessagesClient, which are deprecated and should be replaced by ConnectionsClient.
* The Classes section defines various components for working with nearby messages, including representations for Eddystone UID, iBeacon ID, Message objects, filtering mechanisms, listeners, configuration options, status codes, and callbacks for publishing and subscribing messages.
* Builders are provided for configuring MessageFilter, MessagesOptions, PublishOptions, Strategy, and SubscribeOptions.
* The content outlines the key components for using the Nearby Messages API, including definitions of message types, filtering, listening, and options for publishing and subscribing, while noting the deprecation of core interfaces.

### Annotations

|  |  |
| --- | --- |
| [Distance.Accuracy](/android/reference/com/google/android/gms/nearby/messages/Distance.Accuracy) | Possible values for the accuracy of a distance estimate. |
| [NearbyPermissions](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions) | Determines the scope of permissions Nearby will ask for at connection time. |

### Interfaces

|  |  |
| --- | --- |
| [BleSignal](/android/reference/com/google/android/gms/nearby/messages/BleSignal) | Represents properties of the BLE signal associated with a `Message` . |
| [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) | Represents the distance to a `Message` . |
| [Messages](/android/reference/com/google/android/gms/nearby/messages/Messages) | *This interface is deprecated. Use `MessagesClient` .* |
| [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) | *This interface is deprecated. Nearby Messages will be removed by the end of 2023. Use `ConnectionsClient` instead.* |

### Classes

|  |  |
| --- | --- |
| [EddystoneUid](/android/reference/com/google/android/gms/nearby/messages/EddystoneUid) | An Eddystone UID, broadcast by BLE beacons. |
| [IBeaconId](/android/reference/com/google/android/gms/nearby/messages/IBeaconId) | An iBeacon ID, which can be broadcast by BLE beacons and iOS devices. |
| [Message](/android/reference/com/google/android/gms/nearby/messages/Message) | A message that will be shared with nearby devices. |
| [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) | Used to specify the set of messages to be received. |
| [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | Builder for `MessageFilter` . |
| [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) | A listener for receiving subscribed messages. |
| [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) | Configuration parameters for the `Messages` API. |
| [MessagesOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder) | Builder for `MessagesOptions` . |
| [NearbyMessagesStatusCodes](/android/reference/com/google/android/gms/nearby/messages/NearbyMessagesStatusCodes) | Nearby.Messages specific status codes, for use in `Status.getStatusCode()` . |
| [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback) | Callback for events which affect published messages. |
| [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) | Options for calls to `Messages.publish(GoogleApiClient, Message)` . |
| [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) | Builder for instances of `PublishOptions` . |
| [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) | Callbacks for global status changes that affect a client of Nearby Messages. |
| [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | Describes a set of strategies for publishing or subscribing for nearby messages. |
| [Strategy.Builder](/android/reference/com/google/android/gms/nearby/messages/Strategy.Builder) | Builder for `Strategy` . |
| [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback) | Callback for events which affect subscriptions. |
| [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) | Options for calls to `Messages.subscribe(GoogleApiClient, PendingIntent)` . |
| [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) | Builder for instances of `SubscribeOptions` . |