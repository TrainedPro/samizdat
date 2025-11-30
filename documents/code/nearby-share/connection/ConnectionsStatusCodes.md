<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes -->

# ConnectionsStatusCodes

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionsStatusCodes provides status codes for results from the Nearby Connections API.
* Many status codes indicate missing required permissions like location, Bluetooth, or Wi-Fi state access.
* Other status codes signal issues such as the API being in use, being already connected or advertising, or errors with endpoints or radio capabilities.
* The STATUS\_OK constant indicates a successful operation, while other status codes denote various types of errors or conditions.
* A method is available to get a debug string representation for a given status code.



public final class
**ConnectionsStatusCodes**
extends
[CommonStatusCodes](/android/reference/com/google/android/gms/common/api/CommonStatusCodes)

Status codes for nearby connections results.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [API\_CONNECTION\_FAILED\_ALREADY\_IN\_USE](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#API_CONNECTION_FAILED_ALREADY_IN_USE) | Error code upon trying to connect to the Nearby Connections API via Google Play Services. |
| int | [MISSING\_PERMISSION\_ACCESS\_COARSE\_LOCATION](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_ACCESS_COARSE_LOCATION) | The `Manifest.permission.ACCESS_COARSE_LOCATION` permission is required. |
| int | [MISSING\_PERMISSION\_ACCESS\_FINE\_LOCATION](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_ACCESS_FINE_LOCATION) | The `Manifest.permission.ACCESS_FINE_LOCATION` permission is required. |
| int | [MISSING\_PERMISSION\_ACCESS\_WIFI\_STATE](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_ACCESS_WIFI_STATE) | The `Manifest.permission.ACCESS_WIFI_STATE` permission is required. |
| int | [MISSING\_PERMISSION\_BLUETOOTH](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_BLUETOOTH) | The `Manifest.permission.BLUETOOTH` permission is required. |
| int | [MISSING\_PERMISSION\_BLUETOOTH\_ADMIN](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_BLUETOOTH_ADMIN) | The `Manifest.permission.BLUETOOTH_ADMIN` permission is required. |
| int | [MISSING\_PERMISSION\_BLUETOOTH\_ADVERTISE](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_BLUETOOTH_ADVERTISE) | The `Manifest.permission.BLUETOOTH_ADVERTISE` permission is required. |
| int | [MISSING\_PERMISSION\_BLUETOOTH\_CONNECT](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_BLUETOOTH_CONNECT) | The `Manifest.permission.BLUETOOTH_CONNECT` permission is required. |
| int | [MISSING\_PERMISSION\_BLUETOOTH\_SCAN](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_BLUETOOTH_SCAN) | The `Manifest.permission.BLUETOOTH_SCAN` permission is required. |
| int | [MISSING\_PERMISSION\_CHANGE\_WIFI\_STATE](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_CHANGE_WIFI_STATE) | The `Manifest.permission.CHANGE_WIFI_STATE` permission is required. |
| int | [MISSING\_PERMISSION\_NEARBY\_WIFI\_DEVICES](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_NEARBY_WIFI_DEVICES) | The `Manifest.permission.NEARBY_WIFI_DEVICES` permission is required. |
| int | [MISSING\_PERMISSION\_RECORD\_AUDIO](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_PERMISSION_RECORD_AUDIO) | The `Manifest.permission.RECORD_AUDIO` permission is required. |
| int | [MISSING\_SETTING\_LOCATION\_MUST\_BE\_ON](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#MISSING_SETTING_LOCATION_MUST_BE_ON) | *This constant is deprecated. This status code is no longer returned.* |
| int | [STATUS\_ALREADY\_ADVERTISING](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ALREADY_ADVERTISING) | The app is already advertising; call stopAdvertising() before trying to advertise again. |
| int | [STATUS\_ALREADY\_CONNECTED\_TO\_ENDPOINT](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ALREADY_CONNECTED_TO_ENDPOINT) | The app is already connected to the specified endpoint. |
| int | [STATUS\_ALREADY\_DISCOVERING](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ALREADY_DISCOVERING) | The app is already discovering the specified application ID; call stopDiscovery() before trying to advertise again. |
| int | [STATUS\_ALREADY\_HAVE\_ACTIVE\_STRATEGY](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ALREADY_HAVE_ACTIVE_STRATEGY) | The app already has active operations (advertising, discovering, or connected to other devices) with another Strategy. |
| int | [STATUS\_BLUETOOTH\_ERROR](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_BLUETOOTH_ERROR) | *This constant is deprecated. Use `STATUS_RADIO_ERROR` instead.* |
| int | [STATUS\_CONNECTION\_REJECTED](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_CONNECTION_REJECTED) | The remote endpoint rejected the connection request. |
| int | [STATUS\_ENDPOINT\_IO\_ERROR](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ENDPOINT_IO_ERROR) | An attempt to read from/write to a connected remote endpoint failed. |
| int | [STATUS\_ENDPOINT\_UNKNOWN](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ENDPOINT_UNKNOWN) | An attempt to interact with a remote endpoint failed because it's unknown to us -- it's either an endpoint that was never discovered, or an endpoint that never connected to us (both of which are indicative of bad input from the client app). |
| int | [STATUS\_ERROR](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_ERROR) | The operation failed, without any more information. |
| int | [STATUS\_NETWORK\_NOT\_CONNECTED](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_NETWORK_NOT_CONNECTED) | *This constant is deprecated. This status code is no longer returned.* |
| int | [STATUS\_NOT\_CONNECTED\_TO\_ENDPOINT](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_NOT_CONNECTED_TO_ENDPOINT) | The remote endpoint is not connected; messages cannot be sent to it. |
| int | [STATUS\_OK](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_OK) | The operation was successful. |
| int | [STATUS\_OUT\_OF\_ORDER\_API\_CALL](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_OUT_OF_ORDER_API_CALL) | The app called an API method out of order (i.e. |
| int | [STATUS\_PAYLOAD\_IO\_ERROR](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_PAYLOAD_IO_ERROR) | An attempt to read/write data for a Payload of type `Payload.Type.FILE` or `Payload.Type.STREAM` failed. |
| int | [STATUS\_RADIO\_ERROR](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#STATUS_RADIO_ERROR) | There was an error trying to use the phone's Bluetooth/WiFi/NFC capabilities. |

### Inherited Constant Summary

From class
com.google.android.gms.common.api.CommonStatusCodes

|  |  |  |
| --- | --- | --- |
| int | API\_NOT\_CONNECTED |  |
| int | CANCELED |  |
| int | CONNECTION\_SUSPENDED\_DURING\_CALL |  |
| int | DEVELOPER\_ERROR |  |
| int | ERROR |  |
| int | INTERNAL\_ERROR |  |
| int | INTERRUPTED |  |
| int | INVALID\_ACCOUNT |  |
| int | NETWORK\_ERROR |  |
| int | RECONNECTION\_TIMED\_OUT |  |
| int | RECONNECTION\_TIMED\_OUT\_DURING\_UPDATE |  |
| int | REMOTE\_EXCEPTION |  |
| int | RESOLUTION\_REQUIRED |  |
| int | SERVICE\_DISABLED |  |
| int | SERVICE\_VERSION\_UPDATE\_REQUIRED |  |
| int | SIGN\_IN\_REQUIRED |  |
| int | SUCCESS |  |
| int | SUCCESS\_CACHE |  |
| int | TIMEOUT |  |

### Public Method Summary

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | [getStatusCodeString](/android/reference/com/google/android/gms/nearby/connection/ConnectionsStatusCodes#getStatusCodeString(int)) (int statusCode) Returns an untranslated debug (not user-friendly!) string based on the current status code. |

### Inherited Method Summary

From class
com.google.android.gms.common.api.CommonStatusCodes

|  |  |
| --- | --- |
| static [String](//developer.android.com/reference/java/lang/String.html) | getStatusCodeString (int arg0) |

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







## Constants

#### public static final int **API\_CONNECTION\_FAILED\_ALREADY\_IN\_USE**

Error code upon trying to connect to the Nearby Connections API via Google Play
Services. This error indicates that Nearby Connections is already in use by some app,
and thus is currently unavailable to the caller. Delivered to
`GoogleApiClient.OnConnectionFailedListener`
.

Constant Value:

8050

#### public static final int **MISSING\_PERMISSION\_ACCESS\_COARSE\_LOCATION**

The
`Manifest.permission.ACCESS_COARSE_LOCATION`
permission is required.

Constant Value:

8034

#### public static final int **MISSING\_PERMISSION\_ACCESS\_FINE\_LOCATION**

The
`Manifest.permission.ACCESS_FINE_LOCATION`
permission is required.

Constant Value:

8036

#### public static final int **MISSING\_PERMISSION\_ACCESS\_WIFI\_STATE**

The
`Manifest.permission.ACCESS_WIFI_STATE`
permission is required.

Constant Value:

8032

#### public static final int **MISSING\_PERMISSION\_BLUETOOTH**

The
`Manifest.permission.BLUETOOTH`
permission is required.

Constant Value:

8030

#### public static final int **MISSING\_PERMISSION\_BLUETOOTH\_ADMIN**

The
`Manifest.permission.BLUETOOTH_ADMIN`
permission is required.

Constant Value:

8031

#### public static final int **MISSING\_PERMISSION\_BLUETOOTH\_ADVERTISE**

The
`Manifest.permission.BLUETOOTH_ADVERTISE`
permission is required.

Constant Value:

8038

#### public static final int **MISSING\_PERMISSION\_BLUETOOTH\_CONNECT**

The
`Manifest.permission.BLUETOOTH_CONNECT`
permission is required.

Constant Value:

8039

#### public static final int **MISSING\_PERMISSION\_BLUETOOTH\_SCAN**

The
`Manifest.permission.BLUETOOTH_SCAN`
permission is required.

Constant Value:

8037

#### public static final int **MISSING\_PERMISSION\_CHANGE\_WIFI\_STATE**

The
`Manifest.permission.CHANGE_WIFI_STATE`
permission is required.

Constant Value:

8033

#### public static final int **MISSING\_PERMISSION\_NEARBY\_WIFI\_DEVICES**

The
`Manifest.permission.NEARBY_WIFI_DEVICES`
permission is required.

Constant Value:

8029

#### public static final int **MISSING\_PERMISSION\_RECORD\_AUDIO**

The
`Manifest.permission.RECORD_AUDIO`
permission is required.

Constant Value:

8035

#### public static final int **MISSING\_SETTING\_LOCATION\_MUST\_BE\_ON**

**This constant is deprecated.**
  
This status code is no longer returned.

Location must be turned on (needed for Wifi scans starting from Android M),
preferably using /android/reference/com/google/android/gms/location/SettingsApi.

Constant Value:

8025

#### public static final int **STATUS\_ALREADY\_ADVERTISING**

The app is already advertising; call stopAdvertising() before trying to advertise
again.

Constant Value:

8001

#### public static final int **STATUS\_ALREADY\_CONNECTED\_TO\_ENDPOINT**

The app is already connected to the specified endpoint. Multiple connections to a
remote endpoint cannot be maintained simultaneously.

Constant Value:

8003

#### public static final int **STATUS\_ALREADY\_DISCOVERING**

The app is already discovering the specified application ID; call stopDiscovery()
before trying to advertise again.

Constant Value:

8002

#### public static final int **STATUS\_ALREADY\_HAVE\_ACTIVE\_STRATEGY**

The app already has active operations (advertising, discovering, or connected to
other devices) with another Strategy. Stop these operations on the current Strategy
before trying to advertise or discover with a new Strategy.

Constant Value:

8008

#### public static final int **STATUS\_BLUETOOTH\_ERROR**

**This constant is deprecated.**
  
Use
`STATUS_RADIO_ERROR`
instead.

There was an error trying to use the phone's Bluetooth capabilities.

Constant Value:

8007

#### public static final int **STATUS\_CONNECTION\_REJECTED**

The remote endpoint rejected the connection request.

Constant Value:

8004

#### public static final int **STATUS\_ENDPOINT\_IO\_ERROR**

An attempt to read from/write to a connected remote endpoint failed. If this occurs
repeatedly, consider invoking
`Connections.disconnectFromEndpoint(GoogleApiClient, String)`
.

Constant Value:

8012

#### public static final int **STATUS\_ENDPOINT\_UNKNOWN**

An attempt to interact with a remote endpoint failed because it's unknown to us --
it's either an endpoint that was never discovered, or an endpoint that never connected
to us (both of which are indicative of bad input from the client app).

Constant Value:

8011

#### public static final int **STATUS\_ERROR**

The operation failed, without any more information.

Constant Value:

13

#### public static final int **STATUS\_NETWORK\_NOT\_CONNECTED**

**This constant is deprecated.**
  
This status code is no longer returned.

The device is not connected to a network (over Wifi or Ethernet). Prompt the user to
connect their device when this status code is returned.

Constant Value:

8000

#### public static final int **STATUS\_NOT\_CONNECTED\_TO\_ENDPOINT**

The remote endpoint is not connected; messages cannot be sent to it.

Constant Value:

8005

#### public static final int **STATUS\_OK**

The operation was successful.

Constant Value:

0

#### public static final int **STATUS\_OUT\_OF\_ORDER\_API\_CALL**

The app called an API method out of order (i.e. another method is expected to be
called first).

Constant Value:

8009

#### public static final int **STATUS\_PAYLOAD\_IO\_ERROR**

An attempt to read/write data for a Payload of type
`Payload.Type.FILE`
or
`Payload.Type.STREAM`
failed.

Constant Value:

8013

#### public static final int **STATUS\_RADIO\_ERROR**

There was an error trying to use the phone's Bluetooth/WiFi/NFC capabilities.

Constant Value:

8007







## Public Methods

#### public static [String](//developer.android.com/reference/java/lang/String.html) **getStatusCodeString** (int statusCode)

Returns an untranslated debug (not user-friendly!) string based on the current
status code.