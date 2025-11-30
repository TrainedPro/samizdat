<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason -->

# UwbAvailabilityObserver.UwbStateChangeReason

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `UwbAvailabilityObserver.UwbStateChangeReason`
  is an annotation that indicates the reason why the UWB state changed.
* Possible reasons for UWB state change include country code errors, system policy, or unknown reasons.
* It inherits methods from the
  `java.lang.annotation.Annotation`
  interface.



public static abstract @interface
**UwbAvailabilityObserver.UwbStateChangeReason**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Reason why UWB state changed

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [REASON\_COUNTRY\_CODE\_ERROR](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason#REASON_COUNTRY_CODE_ERROR) |  |
| int | [REASON\_SYSTEM\_POLICY](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason#REASON_SYSTEM_POLICY) |  |
| int | [REASON\_UNKNOWN](/android/reference/com/google/android/gms/nearby/uwb/UwbAvailabilityObserver.UwbStateChangeReason#REASON_UNKNOWN) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **REASON\_COUNTRY\_CODE\_ERROR**

Constant Value:

2

#### public static final int **REASON\_SYSTEM\_POLICY**

Constant Value:

1

#### public static final int **REASON\_UNKNOWN**

Constant Value:

0