<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason -->

# RangingSessionCallback.RangingSuspendedReason

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingSessionCallback.RangingSuspendedReason is an annotation that indicates why a ranging session was stopped.
* The class defines several constant integer values representing different reasons for suspension, such as FAILED\_TO\_START, STOPPED\_BY\_PEER, and SYSTEM\_POLICY.
* It inherits standard annotation methods like annotationType, equals, hashCode, and toString from the java.lang.annotation.Annotation interface.



public static abstract @interface
**RangingSessionCallback.RangingSuspendedReason**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Reason why ranging was stopped.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [FAILED\_TO\_START](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#FAILED_TO_START) |  |
| int | [MAX\_RANGING\_ROUND\_RETRY\_REACHED](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#MAX_RANGING_ROUND_RETRY_REACHED) |  |
| int | [STOPPED\_BY\_PEER](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#STOPPED_BY_PEER) |  |
| int | [STOP\_RANGING\_CALLED](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#STOP_RANGING_CALLED) |  |
| int | [SYSTEM\_POLICY](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#SYSTEM_POLICY) |  |
| int | [UNKNOWN](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#UNKNOWN) |  |
| int | [WRONG\_PARAMETERS](/android/reference/com/google/android/gms/nearby/uwb/RangingSessionCallback.RangingSuspendedReason#WRONG_PARAMETERS) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **FAILED\_TO\_START**

Constant Value:

2

#### public static final int **MAX\_RANGING\_ROUND\_RETRY\_REACHED**

Constant Value:

5

#### public static final int **STOPPED\_BY\_PEER**

Constant Value:

3

#### public static final int **STOP\_RANGING\_CALLED**

Constant Value:

4

#### public static final int **SYSTEM\_POLICY**

Constant Value:

6

#### public static final int **UNKNOWN**

Constant Value:

0

#### public static final int **WRONG\_PARAMETERS**

Constant Value:

1