<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbClient -->

# UwbClient

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbClient is an interface for getting UWB capabilities and interacting with nearby UWB devices for ranging.
* It provides methods to dynamically add or remove controlees from an active ranging session by a controller.
* The interface allows getting the local device's complex channel and UWB address, which are valid for a single ranging session.
* You can retrieve the device's supported ranging capabilities using getRangingCapabilities().
* It includes methods to check UWB availability and subscribe/unsubscribe to availability events.
* The interface provides functionalities to start and stop ranging sessions, requiring the UWB\_RANGING permission.
* Ranging session parameters and intervals can be dynamically reconfigured.



public interface
**UwbClient**
implements
[HasApiKey](/android/reference/com/google/android/gms/common/api/HasApiKey)
<
[UwbOptions](/android/reference/com/google/android/gms/nearby/uwb/UwbOptions)
>

Interface for getting UWB capabilities and interacting with nearby UWB devices to perform
ranging.

### Public Method Summary

|  |  |
| --- | --- |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [addControlee](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#addControlee(com.google.android.gms.nearby.uwb.UwbAddress)) ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address) Dynamically adds a controlee to an active ranging session. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [addControleeWithSessionParams](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#addControleeWithSessionParams(com.google.android.gms.nearby.uwb.RangingControleeParameters)) ( [RangingControleeParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters) params) Dynamically adds a controlee to an active ranging session. |
| abstract Task< [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) > | [getComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#getComplexChannel()) () Gets the local device's complex channel which can be used for ranging, if it exists. |
| abstract Task< [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) > | [getLocalAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#getLocalAddress()) () Gets the local device's UWB address, if it exists. |
| abstract Task< [RangingCapabilities](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities) > | [getRangingCapabilities](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#getRangingCapabilities()) () Returns the `RangingCapabilities` which the device supports. |
| abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > | [isAvailable](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#isAvailable()) () Returns whether UWB is currently usable. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [reconfigureRangeDataNtf](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#reconfigureRangeDataNtf(int,%20int,%20int)) (int configType, int proximityNear, int proximityFar) Dynamically reconfigures range data notification config to an active ranging session. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [reconfigureRangingInterval](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#reconfigureRangingInterval(int)) (int intervalSkipCount) Dynamically reconfigures ranging interval to an active ranging session. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [removeControlee](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#removeControlee(com.google.android.gms.nearby.uwb.UwbAddress)) ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address) Dynamically removes a controlee from an active ranging session. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [startRanging](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#startRanging(com.google.android.gms.nearby.uwb.RangingParameters,%20com.google.android.gms.nearby.uwb.RangingSessionCallback)) ( [RangingParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters) parameters, [RangingSessionCallback](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback) callback) Initiates a ranging session and start ranging. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [stopRanging](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#stopRanging(com.google.android.gms.nearby.uwb.RangingSessionCallback)) ( [RangingSessionCallback](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback) callback) Closes a ranging session. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [subscribeToUwbAvailability](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#subscribeToUwbAvailability(com.google.android.gms.nearby.uwb.UwbAvailabilityObserver)) ( [UwbAvailabilityObserver](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver) observer) Subscribes to UWB availability events. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [unsubscribeFromUwbAvailability](/android/reference/com/google/android/gms/nearby/uwb/UwbClient#unsubscribeFromUwbAvailability()) () Unsubscribes from UWB availability events. |












## Public Methods

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **addControlee** ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address)

Dynamically adds a controlee to an active ranging session. This method can only be
called by a controller. The controlee to be added must be configured with the a set of
parameters that can join the existing connection.

If the method is called by a controlee, or the profile is a unicast profile, or
`UWB_RANGING`
permission is not granted, the Task will return with
`ApiException`
.

Otherwise, this method will return successfully, then clients are expected to handle
either
`RangingSessionCallback.onRangingInitialized(UwbDevice)`
or
`RangingSessionCallback.onRangingSuspended(UwbDevice, int)`
to listen for
starts or failures. The controlee is used as the first parameter of these
callbacks.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **addControleeWithSessionParams** ( [RangingControleeParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters) params)

Dynamically adds a controlee to an active ranging session. This method can only be
called by a controller. This method is for Provisioned STS individual key case.
SubSessionId and subSessionKey should be provided by the controlees.

If the method is called by a controlee, or the profile is a unicast profile, or
`UWB_RANGING`
permission is not granted, the Task will return with
`ApiException`
.

Otherwise, this method will return successfully, then clients are expected to handle
either
`RangingSessionCallback.onRangingInitialized(UwbDevice)`
or
`RangingSessionCallback.onRangingSuspended(UwbDevice, int)`
to listen for
starts or failures. The controlee is used as the first parameter of these
callbacks.

#### public abstract Task< [UwbComplexChannel](/android/reference/com/google/android/gms/nearby/uwb/UwbComplexChannel) > **getComplexChannel** ()

Gets the local device's complex channel which can be used for ranging, if it exists.
If UWB is not supported, or the role is set to
`ROLE_CONTROLLEE`
in
UwbOptions, then the call will fail.

A complex channel can only be used for a single ranging session. After a ranging
session is ended through
`stopRanging(RangingSessionCallback)`
, a new channel will be allocated and
clients should get the new channel via this method before attempting to start another
ranging session.

Ranging session duration may also be limited to prevent channels from being used for
too long. In this case, your ranging session would be suspended and clients would need
to exchange the new channel with their peer before starting again.

#### public abstract Task< [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) > **getLocalAddress** ()

Gets the local device's UWB address, if it exists. If UWB is not supported, then the
call will fail.

A local address can only be used for a single ranging session. After a ranging
session is ended through
`stopRanging(RangingSessionCallback)`
), a new address will be allocated and
clients should get the new address via this method before attempting to start another
ranging session.

Ranging session duration may also be limited to prevent addresses from being used
for too long. In this case, your ranging session would be suspended and clients would
need to exchange the new address with their peer before starting again.

#### public abstract Task< [RangingCapabilities](/android/reference/com/google/android/gms/nearby/uwb/RangingCapabilities) > **getRangingCapabilities** ()

Returns the
`RangingCapabilities`
which the device supports.

#### public abstract Task< [Boolean](//developer.android.com/reference/java/lang/Boolean.html) > **isAvailable** ()

Returns whether UWB is currently usable. Possible reasons for which it isn’t usable
include airplane mode, or location disabled, or UWB hardware is busy.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **reconfigureRangeDataNtf** (int configType, int proximityNear, int proximityFar)

Dynamically reconfigures range data notification config to an active ranging
session.

If the method is called when
`UWB_RANGING`
permission is not granted, the
Task will return with
`ApiException`
.

Otherwise, this method will reconfigure range data notification configuration with
`UwbRangeDataNtfConfig.RangeDataNtfConfig`
. All the supported notification
configs are obtained by
`RangingCapabilities.getSupportedNtfConfigs()`
.

##### Parameters

|  |  |
| --- | --- |
| configType | the type of range data notification configuration to apply. Valid values are defined by the `UwbRangeDataNtfConfig.RangeDataNtfConfig` . If `UwbRangeDataNtfConfig.RangeDataNtfConfig.RANGE_DATA_NTF_DISABLE` or `UwbRangeDataNtfConfig.RangeDataNtfConfig.RANGE_DATA_NTF_ENABLE` is used, below parameters are ignored |
| proximityNear | the proximity near value in centimeters for range data notification config. This value must be within the range of 0 to 20000 (inclusive). Default is 0. |
| proximityFar | the proximity far value in centimeters for range data notification config. This value must be within the range of 0 to 20000 (inclusive). Default is 20000. |

##### Throws

|  |  |
| --- | --- |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if any of the parameters are outside the valid range. |

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **reconfigureRangingInterval** (int intervalSkipCount)

Dynamically reconfigures ranging interval to an active ranging session. This method
can only be called by a controller.

If the method is called by a controlee, or
`UWB_RANGING`
permission is
not granted, the Task will return with
`ApiException`
.

Otherwise, this method will return successfully with the ranging session
reconfigured to skip number of ranging intervals set in
`intervalSkipCount`
.
If
`intervalSkipCount`
is set to 0, the ranging interval will be set to the
interval used when
`startRanging(RangingParameters, RangingSessionCallback)`
was called.

Example: If ranging interval is 200ms,
`intervalSkipCount`
=3 would skip
3\*200=600ms, then the effective interval would be 800ms.

##### Parameters

|  |  |
| --- | --- |
| intervalSkipCount | the number of intervals to skip between range measurements. The value must be within the range of 0 to 255 (inclusive). |

##### Throws

|  |  |
| --- | --- |
| [IllegalArgumentException](//developer.android.com/reference/java/lang/IllegalArgumentException.html) | if the `intervalSkipCount` is outside the valid range. |

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **removeControlee** ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address)

Dynamically removes a controlee from an active ranging session. This method can only
be called by a controller.

If the method is called by a controlee, or the address doesn't belong to an active
controlee, or the profile is a unicast profile, or
`UWB_RANGING`
permission
is not granted, or the operation failed due to hardware or firmware issues, the Task
will return with
`ApiException`
.

Otherwise, this method will return successfully, then clients are expected to handle
`RangingSessionCallback.onRangingSuspended(UwbDevice, int)`
with the controlee
as parameter of the callback.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **startRanging** ( [RangingParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters) parameters, [RangingSessionCallback](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback) callback)

Initiates a ranging session and start ranging.

Ranging requires the
`android.permission.UWB_RANGING`
permission. Apps
must have been granted this permission before calling this method, otherwise the task
will return with
`ApiException`

If the client is ranging before this call, the task will return with
`ApiException`

If the client starts a controlee session without setting complex channel and peer
address, the task will return with
`ApiException`

Otherwise, this method will return successfully, then clients are expected to handle
both
`RangingSessionCallback.onRangingInitialized(UwbDevice)`
and
`RangingSessionCallback.onRangingSuspended(UwbDevice, int)`
to listen for
starts or failures.

##### Parameters

|  |  |
| --- | --- |
| parameters | uwbConfigId and peer devices must be set explicitly. For Controlee sessions, complex channel must be set. |
| callback |  |

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **stopRanging** ( [RangingSessionCallback](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback) callback)

Closes a ranging session. If a session was opened successfully, (startRanging
returns successfully and
`RangingSessionCallback.onRangingInitialized(UwbDevice)`
was called), then
make sure this function is called to close the session properly.

Ranging requires the
`android.permission.UWB_RANGING`
permission. Apps
must have been granted this permission before calling this method, otherwise the task
will return with
`ApiException`

If the client hasn't started ranging before this call, the task will return with
`ApiException`

If the ranging can't be stopped due to hardware or firmware issues, the task will
return with
`ApiException`

Otherwise, this method will return successfully and
`RangingSessionCallback.onRangingSuspended(UwbDevice, int)`
will be
called.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **subscribeToUwbAvailability** ( [UwbAvailabilityObserver](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver) observer)

Subscribes to UWB availability events. The event will be sent back through
`UwbAvailabilityObserver`
when UWB state is toggled.

One
`UwbClient`
can only have one subscription. If a new subscription is
added, the older one will be unsubscribed. The subscription will be cleared if the app
is closed or inactive for a few minutes.

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **unsubscribeFromUwbAvailability** ()

Unsubscribes from UWB availability events.
`UwbClient`
will not receive
callback events any more if successfully unsubscribed.