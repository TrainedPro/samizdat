<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo -->

# ConnectionInfo

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionInfo provides information about a connection being initiated.
* It includes methods to retrieve details such as authentication digits, authentication status, endpoint information, and endpoint name.
* You can determine if the connection request was initiated by a remote device using the
  `isIncomingConnection()`
  method.
* Some methods and the constructor are deprecated, suggesting newer or alternative approaches should be used.



public final class
**ConnectionInfo**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Information about a connection that is being initiated.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [ConnectionInfo](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#ConnectionInfo(java.lang.String,%20java.lang.String,%20boolean)) ( [String](//developer.android.com/reference/java/lang/String.html) endpointName, [String](//developer.android.com/reference/java/lang/String.html) authenticationToken, boolean isIncomingConnection) *This constructor is deprecated. Creates a new `ConnectionInfo` .* |

### Public Method Summary

|  |  |
| --- | --- |
| [String](//developer.android.com/reference/java/lang/String.html) | [getAuthenticationDigits](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getAuthenticationDigits()) () A 4 digit authentication token that has been given to both devices. |
| int | [getAuthenticationStatus](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getAuthenticationStatus()) () An authentication status for Authentication handshaking result after uKey2 verification. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getAuthenticationToken](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getAuthenticationToken()) () *This method is deprecated. Use `getAuthenticationDigits()` instead.* |
| byte[] | [getEndpointInfo](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getEndpointInfo()) () Information that represents the remote device which is defined by the client or application. |
| [String](//developer.android.com/reference/java/lang/String.html) | [getEndpointName](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getEndpointName()) () The name of the remote device we're connecting to. |
| byte[] | [getRawAuthenticationToken](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#getRawAuthenticationToken()) () The raw (significantly longer) version of the authentication token from `getAuthenticationToken()` -- this is intended for headless authentication, typically on devices with no output capabilities, where the authentication is purely programmatic and does not have the luxury of human intervention. |
| boolean | [isConnectionVerified](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#isConnectionVerified()) () *This method is deprecated. This API has been added, but the implementation has never been completed. It always returns false.* |
| boolean | [isIncomingConnection](/android/reference/com/google/android/gms/nearby/connection/ConnectionInfo#isIncomingConnection()) () True if the connection request was initiated from a remote device. |

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

#### public **ConnectionInfo** ( [String](//developer.android.com/reference/java/lang/String.html) endpointName, [String](//developer.android.com/reference/java/lang/String.html) authenticationToken, boolean isIncomingConnection)

**This constructor is deprecated.**
  
Creates a new
`ConnectionInfo`
.





## Public Methods

#### public [String](//developer.android.com/reference/java/lang/String.html) **getAuthenticationDigits** ()

A 4 digit authentication token that has been given to both devices.

#### public int **getAuthenticationStatus** ()

An authentication status for Authentication handshaking result after uKey2
verification.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getAuthenticationToken** ()

**This method is deprecated.**
  
Use
`getAuthenticationDigits()`
instead.

A short human-readable authentication token that has been given to both devices.

#### public byte[] **getEndpointInfo** ()

Information that represents the remote device which is defined by the client or
application.

#### public [String](//developer.android.com/reference/java/lang/String.html) **getEndpointName** ()

The name of the remote device we're connecting to.

#### public byte[] **getRawAuthenticationToken** ()

The raw (significantly longer) version of the authentication token from
`getAuthenticationToken()`
-- this is intended for headless authentication,
typically on devices with no output capabilities, where the authentication is purely
programmatic and does not have the luxury of human intervention.

#### public boolean **isConnectionVerified** ()

**This method is deprecated.**
  
This API has been added, but the implementation has never been completed. It always
returns false.

True if the connection has been verified by internal verification mechanisms.

#### public boolean **isIncomingConnection** ()

True if the connection request was initiated from a remote device. False if this
device was the one to try and initiate the connection.