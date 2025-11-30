<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener -->

# Connections.EndpointDiscoveryListener

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Connections.EndpointDiscoveryListener class is deprecated and should be replaced with EndpointDiscoveryCallback.
* This class serves as a listener that is invoked during the process of discovering endpoints.
* The onEndpointFound method is triggered when a remote endpoint is successfully discovered.
* The onEndpointLost method is called when a previously found remote endpoint is no longer discoverable.



public static abstract class
**Connections.EndpointDiscoveryListener**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

**This class is deprecated.**
  
Use
`EndpointDiscoveryCallback`
instead.

Listener invoked during endpoint discovery.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [EndpointDiscoveryListener](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener#EndpointDiscoveryListener()) () |

### Public Method Summary

|  |  |
| --- | --- |
| void | [onEndpointFound](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener#onEndpointFound(java.lang.String,%20java.lang.String,%20java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [String](//developer.android.com/reference/java/lang/String.html) name) Called when a remote endpoint is discovered. |
| abstract void | [onEndpointLost](/android/reference/com/google/android/gms/nearby/connection/Connections.EndpointDiscoveryListener#onEndpointLost(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointId) Called when a remote endpoint is no longer discoverable; only called for endpoints that previously had been passed to `onEndpointFound(String, String, String)` . |

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

#### public **EndpointDiscoveryListener** ()





## Public Methods

#### public void **onEndpointFound** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId, [String](//developer.android.com/reference/java/lang/String.html) serviceId, [String](//developer.android.com/reference/java/lang/String.html) name)

Called when a remote endpoint is discovered.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The ID of the remote endpoint that was discovered. |
| serviceId | The ID of the service of the remote endpoint. |
| name | The human readable name of the remote endpoint. |

#### public abstract void **onEndpointLost** ( [String](//developer.android.com/reference/java/lang/String.html) endpointId)

Called when a remote endpoint is no longer discoverable; only called for endpoints
that previously had been passed to
`onEndpointFound(String, String, String)`
.

##### Parameters

|  |  |
| --- | --- |
| endpointId | The ID of the remote endpoint that was lost. |