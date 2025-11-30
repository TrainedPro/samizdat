<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo -->

# DiscoveredEndpointInfo

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* DiscoveredEndpointInfo is a class containing information about an endpoint when it is discovered.
* It has a public constructor that takes a serviceId and endpointName as arguments.
* Public methods include getEndpointInfo, getEndpointName, and getServiceId to retrieve information about the discovered endpoint.
* It inherits methods from the java.lang.Object class.



public final class
**DiscoveredEndpointInfo**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Information about an endpoint when it's discovered.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [DiscoveredEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo#DiscoveredEndpointInfo(java.lang.String,%20java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) serviceId, [String](//developer.android.com/reference/java/lang/String.html) endpointName) Creates a new `DiscoveredEndpointInfo` . |

### Public Method Summary

|  |  |
| --- | --- |
| byte[] | [getEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo#getEndpointInfo()) () Information advertised by the remote endpoint. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getEndpointName](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo#getEndpointName()) () The human readable name of the remote endpoint. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getServiceId](/android/reference/com/google/android/gms/nearby/connection/DiscoveredEndpointInfo#getServiceId()) () The ID of the service advertised by the remote endpoint. |

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

#### public **DiscoveredEndpointInfo** ( [String](//developer.android.com/reference/java/lang/String.html) serviceId, [String](//developer.android.com/reference/java/lang/String.html) endpointName)

Creates a new
`DiscoveredEndpointInfo`
.





## Public Methods

#### public byte[] **getEndpointInfo** ()

Information advertised by the remote endpoint.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getEndpointName** ()

The human readable name of the remote endpoint.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getServiceId** ()

The ID of the service advertised by the remote endpoint.