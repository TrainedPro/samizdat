<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver -->

# UwbAvailabilityObserver

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* UwbAvailabilityObserver is an observer for UWB availability change events.
* The onUwbStateChanged method is called when the UWB state changes, indicating whether UWB is available and the reason for the change.
* UwbAvailabilityObserver.UwbStateChangeReason is a nested interface providing reasons for UWB state changes.



public interface
**UwbAvailabilityObserver**

Observer for UWB availability change events.

When UWB state is changed,
`onUwbStateChanged(boolean, int)`
will be called, sending back the current UWB state
and the state change reason.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| @interface | [UwbAvailabilityObserver.UwbStateChangeReason](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason) | | Reason why UWB state changed |

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [onUwbStateChanged](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver#onUwbStateChanged(boolean,%20int)) (boolean isAvailable, int reason) Called when UWB state is changed. |












## Public Methods

#### public abstract void **onUwbStateChanged** (boolean isAvailable, int reason)

Called when UWB state is changed.

##### Parameters

|  |  |
| --- | --- |
| isAvailable | whether UWB is available or not |
| reason | the reason why UWB state changed |