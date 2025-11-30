<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult -->

# Connections.StartAdvertisingResult

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Connections.StartAdvertisingResult is a Result delivered when a local endpoint starts being advertised.
* Possible status codes include STATUS\_OK, STATUS\_ALREADY\_ADVERTISING, and STATUS\_ERROR.
* The getLocalEndpointName method retrieves the human readable name for the local endpoint being advertised.



public static interface
**Connections.StartAdvertisingResult**
implements
[Result](/android/reference/com/google/android/gms/common/api/Result)

Result delivered when a local endpoint starts being advertised.

Possible status codes include:

* `ConnectionsStatusCodes.STATUS_OK`
  if advertising started successfully.
* `ConnectionsStatusCodes.STATUS_ALREADY_ADVERTISING`
  if the app is already
  advertising itself.
* `ConnectionsStatusCodes.STATUS_ERROR`
  if an unknown error occurred while
  advertising the app.

### Public Method Summary

|  |  |
| --- | --- |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | [getLocalEndpointName](/android/reference/com/google/android/gms/nearby/connection/Connections.StartAdvertisingResult#getLocalEndpointName()) () Retrieves the human readable name for the local endpoint being advertised (possibly after resolving name collisions.) |

### Inherited Method Summary

From interface com.google.android.gms.common.api.Result

|  |  |
| --- | --- |
| abstract [Status](/android/reference/com/google/android/gms/common/api/Status) | getStatus () |












## Public Methods

#### public abstract [String](//developer.android.com/reference/java/lang/String.html) **getLocalEndpointName** ()

Retrieves the human readable name for the local endpoint being advertised (possibly
after resolving name collisions.)

##### Returns

* The name of the local endpoint.