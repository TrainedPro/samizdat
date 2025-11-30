<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/fastpair/FastPairClient -->

# FastPairClient

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* FastPairClient is an interface for Fast Pair APIs, including Smart Audio Source Switching (SASS) functionalities.
* The
  `isSassDeviceAvailable`
  method checks if a SASS-supported Bluetooth device is available for a specific audio usage and requires certain permissions depending on the Android version.
* The
  `triggerSassForUsage`
  method initiates SASS for a specific audio usage, returning true on successful connection establishment, and also requires specific permissions based on the Android version.



public interface
**FastPairClient**
implements
[HasApiKey](/android/reference/com/google/android/gms/common/api/HasApiKey)
<
[Api.ApiOptions.NoOptions](/android/reference/com/google/android/gms/common/api/Api.ApiOptions.NoOptions)
>

Interface to Fast Pair APIs. It includes Smart Audio Source Switching (SASS) APIs through
which the client can implement
[Audio Switch](//developers.google.com/nearby/fast-pair/specifications/extensions/sass)
functionalities.

### Public Method Summary

|  |  |
| --- | --- |
| abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > | [isSassDeviceAvailable](/android/reference/com/google/android/gms/nearby/fastpair/FastPairClient#isSassDeviceAvailable(int)) (int audioUsage) Queries if any SASS-supported Bluetooth device is available for a specific `AudioUsage` or not. |
| abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > | [triggerSassForUsage](/android/reference/com/google/android/gms/nearby/fastpair/FastPairClient#triggerSassForUsage(int)) (int audioUsage) Triggers SASS with a specific `AudioUsage` . |












## Public Methods

#### public abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > **isSassDeviceAvailable** (int audioUsage)

Queries if any SASS-supported Bluetooth device is available for a specific
`AudioUsage`
or not.

Client must declare the following permissions depending on platform:

* `Manifest.permission.ACCESS_FINE_LOCATION`
  (Deprecated from Android S)
* `Manifest.permission.BLUETOOTH_SCAN`
  (Required for devices running S+)

Otherwise returns
`FastPairStatusCodes.FAILED_PERMISSION_DENIED`
error code. For other status
codes, check
`FastPairStatusCodes`
.

#### public abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > **triggerSassForUsage** (int audioUsage)

Triggers SASS with a specific
`AudioUsage`
.
Returns true if the trigger is accepted, and the connection established successfully,
otherwise false.

Client must declare the following permissions depending on platform:

* `Manifest.permission.BLUETOOTH`
  (Deprecated from Android S)
* `Manifest.permission.ACCESS_FINE_LOCATION`
  (Deprecated from Android S)
* `Manifest.permission.BLUETOOTH_SCAN`
  (Required for devices running S+)
* `Manifest.permission.BLUETOOTH_CONNECT`
  (Required for devices running
  S+)

Otherwise returns
`FastPairStatusCodes.FAILED_PERMISSION_DENIED`
error code. For other status
codes, check
`FastPairStatusCodes`
.