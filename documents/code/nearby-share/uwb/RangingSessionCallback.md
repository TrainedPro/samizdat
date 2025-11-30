<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback -->

# RangingSessionCallback

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingSessionCallback is an interface used for callbacks when starting a ranging session.
* It includes nested classes like RangingSuspendedReason to explain why ranging was stopped.
* Public methods include onRangingInitialized for session initiation, onRangingResult for receiving device position, and onRangingSuspended when a session is suspended.



public interface
**RangingSessionCallback**

Callbacks used by startRanging.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| @interface | [RangingSessionCallback.RangingSuspendedReason](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason) | | Reason why ranging was stopped. |

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onRangingInitialized](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback#onRangingInitialized(com.google.android.gms.nearby.uwb.UwbDevice)) ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device) Callback when a ranging session has been initiated. |
| abstract void | [onRangingResult](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback#onRangingResult(com.google.android.gms.nearby.uwb.UwbDevice,%20com.google.android.gms.nearby.uwb.RangingPosition)) ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device, [RangingPosition](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition) position) Callback when a ranging device's position is received. |
| abstract void | [onRangingSuspended](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback#onRangingSuspended(com.google.android.gms.nearby.uwb.UwbDevice,%20int)) ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device, int reason) Callback when a session has been suspended. |












## Public Methods

#### public abstract void **onRangingInitialized** ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device)

Callback when a ranging session has been initiated.

#### public abstract void **onRangingResult** ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device, [RangingPosition](/android/reference/com/google/android/gms/nearby/uwb/RangingPosition) position)

Callback when a ranging device's position is received.

#### public abstract void **onRangingSuspended** ( [UwbDevice](/android/reference/com/google/android/gms/nearby/uwb/UwbDevice) device, int reason)

Callback when a session has been suspended.