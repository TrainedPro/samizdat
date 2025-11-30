<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.SlotDuration -->

# RangingParameters.SlotDuration

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* SlotDuration defines settings for slot duration in ranging parameters.
* It provides constants for 1 millisecond and 2 millisecond slot durations.
* It is an abstract annotation interface.



public static abstract @interface
**RangingParameters.SlotDuration**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Slot duration settings.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [DURATION\_1\_MS](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.SlotDuration#DURATION_1_MS) | 1 millisecond slot duration |
| int | [DURATION\_2\_MS](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.SlotDuration#DURATION_2_MS) | 2 millisecond slot duration |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **DURATION\_1\_MS**

1 millisecond slot duration

Constant Value:

1

#### public static final int **DURATION\_2\_MS**

2 millisecond slot duration

Constant Value:

2