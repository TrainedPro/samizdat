<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/Nearby -->

# Nearby

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Nearby is an API for communication with nearby devices.
* Several fields related to Connections and Messages are deprecated and suggest using
  `getConnectionsClient`
  or
  `getMessagesClient`
  methods instead.
* The
  `getConnectionsClient`
  method creates a new instance of
  `ConnectionsClient`
  .
* The
  `getFastPairClient`
  method creates a new instance of
  `FastPairClient`
  .
* `MessagesClient`
  methods are deprecated and recommend using
  `ConnectionsClient`
  instead, as Nearby Messages will be removed by the end of 2023.
* `UwbClient`
  methods are available but direct use of the Nearby UWB SDK is not supported, recommending the Jetpack UWB SDK instead.



public final class
**Nearby**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

API for communication with nearby devices.

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Api](/android/reference/com/google/android/gms/common/api/Api) < [ConnectionsOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionsOptions) > | [CONNECTIONS\_API](/android/reference/com/google/android/gms/nearby/Nearby#CONNECTIONS_API) | *This field is deprecated. Use `getConnectionsClient(Context)` instead.* |
| public static final [Connections](/android/reference/com/google/android/gms/nearby/connection/Connections) | [Connections](/android/reference/com/google/android/gms/nearby/Nearby#Connections) | *This field is deprecated. Use `getConnectionsClient(Context)` instead.* |
| public static final [Api](/android/reference/com/google/android/gms/common/api/Api) < [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) > | [MESSAGES\_API](/android/reference/com/google/android/gms/nearby/Nearby#MESSAGES_API) | *This field is deprecated. Use `getMessagesClient(Context)` instead.* |
| public static final [Messages](/android/reference/com/google/android/gms/nearby/messages/Messages) | [Messages](/android/reference/com/google/android/gms/nearby/Nearby#Messages) | *This field is deprecated. Use `getMessagesClient(Context)` instead.* |

### Public Method Summary

|  |  |
| --- | --- |
| static [ConnectionsClient](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) | [getConnectionsClient](/android/reference/com/google/android/gms/nearby/Nearby#getConnectionsClient(android.content.Context)) ( [Context](//developer.android.com/reference/android/content/Context.html) context) Creates a new instance of `ConnectionsClient` . |
| static [ConnectionsClient](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) | [getConnectionsClient](/android/reference/com/google/android/gms/nearby/Nearby#getConnectionsClient(android.app.Activity)) ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity) Creates a new instance of `ConnectionsClient` . |
| static [FastPairClient](/android/reference/com/google/android/gms/nearby/fastpair/FastPairClient) | [getFastPairClient](/android/reference/com/google/android/gms/nearby/Nearby#getFastPairClient(android.content.Context)) ( [Context](//developer.android.com/reference/android/content/Context.html) context) Creates a new instance of `FastPairClient` . |
| static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) | [getMessagesClient](/android/reference/com/google/android/gms/nearby/Nearby#getMessagesClient(android.app.Activity,%20com.google.android.gms.nearby.messages.MessagesOptions)) ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity, [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) options) *This method is deprecated. Nearby Messages will be removed by the end of 2023. Use `ConnectionsClient` instead.* |
| static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) | [getMessagesClient](/android/reference/com/google/android/gms/nearby/Nearby#getMessagesClient(android.app.Activity)) ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity) *This method is deprecated. Nearby Messages will be removed by the end of 2023. Use `ConnectionsClient` instead.* |
| static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) | [getMessagesClient](/android/reference/com/google/android/gms/nearby/Nearby#getMessagesClient(android.content.Context,%20com.google.android.gms.nearby.messages.MessagesOptions)) ( [Context](//developer.android.com/reference/android/content/Context.html) context, [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) options) *This method is deprecated. Nearby Messages will be removed by the end of 2023. Use `ConnectionsClient` instead.* |
| static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) | [getMessagesClient](/android/reference/com/google/android/gms/nearby/Nearby#getMessagesClient(android.content.Context)) ( [Context](//developer.android.com/reference/android/content/Context.html) context) *This method is deprecated. Nearby Messages will be removed by the end of 2023. Use `ConnectionsClient` instead.* |
| static [UwbClient](/android/reference/com/google/android/gms/nearby/uwb/UwbClient) | [getUwbControleeClient](/android/reference/com/google/android/gms/nearby/Nearby#getUwbControleeClient(android.content.Context)) ( [Context](//developer.android.com/reference/android/content/Context.html) context) Creates a new instance of controlee-type `UwbClient` . |
| static [UwbClient](/android/reference/com/google/android/gms/nearby/uwb/UwbClient) | [getUwbControllerClient](/android/reference/com/google/android/gms/nearby/Nearby#getUwbControllerClient(android.content.Context)) ( [Context](//developer.android.com/reference/android/content/Context.html) context) Creates a new instance of controller-type `UwbClient` . |

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








## Fields

#### public static final [Api](/android/reference/com/google/android/gms/common/api/Api) < [ConnectionsOptions](/android/reference/com/google/android/gms/nearby/connection/ConnectionsOptions) > **CONNECTIONS\_API**

**This field is deprecated.**
  
Use
`getConnectionsClient(Context)`
instead.

#### public static final [Connections](/android/reference/com/google/android/gms/nearby/connection/Connections) **Connections**

**This field is deprecated.**
  
Use
`getConnectionsClient(Context)`
instead.

#### public static final [Api](/android/reference/com/google/android/gms/common/api/Api) < [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) > **MESSAGES\_API**

**This field is deprecated.**
  
Use
`getMessagesClient(Context)`
instead.

#### public static final [Messages](/android/reference/com/google/android/gms/nearby/messages/Messages) **Messages**

**This field is deprecated.**
  
Use
`getMessagesClient(Context)`
instead.






## Public Methods

#### public static [ConnectionsClient](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) **getConnectionsClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context)

Creates a new instance of
`ConnectionsClient`
.
Resolvable connections errors will create a system notification that the user can tap
in order to resolve the error.

#### public static [ConnectionsClient](/android/reference/com/google/android/gms/nearby/connection/ConnectionsClient) **getConnectionsClient** ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity)

Creates a new instance of
`ConnectionsClient`
.
The given
`Activity`
will
be used to automatically prompt for resolution of resolvable connection errors.

#### public static [FastPairClient](/android/reference/com/google/android/gms/nearby/fastpair/FastPairClient) **getFastPairClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context)

Creates a new instance of
`FastPairClient`
.
Resolvable connections errors will create a system notification that the user can tap
in order to resolve the error.

#### public static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) **getMessagesClient** ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity, [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) options)

**This method is deprecated.**
  
Nearby Messages will be removed by the end of 2023. Use
`ConnectionsClient`
instead.

Creates a new instance of
`MessagesClient`
.
The given Activity will be used to automatically prompt for resolution of resolvable
connection errors.

#### public static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) **getMessagesClient** ( [Activity](//developer.android.com/reference/android/app/Activity.html) activity)

**This method is deprecated.**
  
Nearby Messages will be removed by the end of 2023. Use
`ConnectionsClient`
instead.

Creates a new instance of
`MessagesClient`
.
The given Activity will be used to automatically prompt for resolution of resolvable
connection errors.

#### public static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) **getMessagesClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context, [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) options)

**This method is deprecated.**
  
Nearby Messages will be removed by the end of 2023. Use
`ConnectionsClient`
instead.

Creates a new instance of
`MessagesClient`
.
Resolvable connections errors will create a system notification that the user can tap
in order to resolve the error.

#### public static [MessagesClient](/android/reference/com/google/android/gms/nearby/messages/MessagesClient) **getMessagesClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context)

**This method is deprecated.**
  
Nearby Messages will be removed by the end of 2023. Use
`ConnectionsClient`
instead.

Creates a new instance of
`MessagesClient`
.
Resolvable connections errors will create a system notification that the user can tap
in order to resolve the error.

#### public static [UwbClient](/android/reference/com/google/android/gms/nearby/uwb/UwbClient) **getUwbControleeClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context)

Creates a new instance of controlee-type
`UwbClient`
.

Direct use of Nearby UWB SDK is not supported. You should use the
[Jetpack UWB SDK](//developer.android.com/jetpack/androidx/releases/core-uwb)
(androidx.core.uwb) instead.

#### public static [UwbClient](/android/reference/com/google/android/gms/nearby/uwb/UwbClient) **getUwbControllerClient** ( [Context](//developer.android.com/reference/android/content/Context.html) context)

Creates a new instance of controller-type
`UwbClient`
.

Direct use of Nearby UWB SDK is not supported. You should use the
[Jetpack UWB SDK](//developer.android.com/jetpack/androidx/releases/core-uwb)
(androidx.core.uwb) instead.