<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback -->

# EndpointDiscoveryCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `EndpointDiscoveryCallback`
  is an abstract class that acts as a listener during endpoint discovery.
* It includes an abstract method
  `onEndpointFound`
  which is called when a remote endpoint is discovered, providing the endpoint ID and additional information.
* It also includes an abstract method
  `onEndpointLost`
  which is called when a previously discovered remote endpoint is no longer discoverable, providing the endpoint ID.



public abstract class
**EndpointDiscoveryCallback**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Listener invoked during endpoint discovery.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [EndpointDiscoveryCallback](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback#EndpointDiscoveryCallback()) () |

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onEndpointFound](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback#onEndpointFound(java.lang.String,%20com.google.android.gms.nearby.connection.DiscoveredEndpointInfo)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [DiscoveredEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo) info) Called when a remote endpoint is discovered. |
| abstract void | [onEndpointLost](/android/reference/com/google/android/gms/nearby/connection/EndpointDiscoveryCallback#onEndpointLost(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) Called when a remote endpoint is no longer discoverable; only called for endpoints that previously had been passed to `onEndpointFound(String, DiscoveredEndpointInfo)` . |

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

#### public **EndpointDiscoveryCallback** ()





## Public Methods

#### public abstract void **onEndpointFound** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [DiscoveredEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo) info)

Called when a remote endpoint is discovered.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The ID of the remote endpoint that was discovered. |
| info | Additional information about the endpoint. |

#### public abstract void **onEndpointLost** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Called when a remote endpoint is no longer discoverable; only called for endpoints
that previously had been passed to
`onEndpointFound(String, DiscoveredEndpointInfo)`
.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The ID of the remote endpoint that was lost. |