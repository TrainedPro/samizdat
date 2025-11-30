<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters -->

# RangingControleeParameters

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `RangingControleeParameters`
  is a class used to pass parameters to the UWB controller when a Provisioned STS individual key is utilized.
* The class has two public constructors to create instances with different parameters.
* It provides public methods to retrieve the UWB address, sub-session ID, and sub-session key.
* The class inherits standard methods from the
  `java.lang.Object`
  class.



public class
**RangingControleeParameters**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Parameters passed to controller for
`UwbClient.addControleeWithSessionParams(RangingControleeParameters)`
when
Provisioned STS individual key is used.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [RangingControleeParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters#RangingControleeParameters(com.google.android.gms.nearby.uwb.UwbAddress,%20int,%20byte[])) ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address, int subSessionId, byte[] subSessionKey) |
|  | [RangingControleeParameters](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters#RangingControleeParameters(com.google.android.gms.nearby.uwb.UwbAddress)) ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address) |

### Public Method Summary

|  |  |
| --- | --- |
| [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) | [getAddress](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters#getAddress()) () Gets the UWB address |
| int | [getSubSessionId](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters#getSubSessionId()) () Gets sub-session id |
| byte[] | [getSubSessionKey](/android/reference/com/google/android/gms/nearby/uwb/RangingControleeParameters#getSubSessionKey()) () Gets sub-session key |

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

#### public **RangingControleeParameters** ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address, int subSessionId, byte[] subSessionKey)

#### public **RangingControleeParameters** ( [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) address)





## Public Methods

#### public [UwbAddress](/android/reference/com/google/android/gms/nearby/uwb/UwbAddress) **getAddress** ()

Gets the UWB address

#### public int **getSubSessionId** ()

Gets sub-session id

#### public byte[] **getSubSessionKey** ()

Gets sub-session key