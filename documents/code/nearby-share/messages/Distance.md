<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Distance -->

# Distance

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The Distance interface represents the distance to a Message and implements Comparable.
* It includes a nested class Distance.Accuracy for possible values of distance estimate accuracy.
* A static final field UNKNOWN represents an unknown distance with low accuracy and NaN meters.
* Public methods include compareTo for comparing distances by meters, getAccuracy to get the distance estimate accuracy, and getMeters to get the distance estimate in meters.



public interface
**Distance**
implements
[Comparable](//developer.android.com/reference/java/lang/Comparable.html)
<
[Distance](/android/reference/com/google/android/gms/nearby/messages/Distance)
>

Represents the distance to a
`Message`
.

##### See Also

* `MessageListener.onDistanceChanged(Message, Distance)`

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| @interface | [Distance.Accuracy](/android/reference/com/google/android/gms/nearby/messages/Distance.Accuracy) | | Possible values for the accuracy of a distance estimate. |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) | [UNKNOWN](/android/reference/com/google/android/gms/nearby/messages/Distance#UNKNOWN) | Represents an unknown distance, with accuracy = `Distance.Accuracy.LOW` , and meters = `Double.NaN` . |

### Public Method Summary

|  |  |
| --- | --- |
| abstract int | [compareTo](/android/reference/com/google/android/gms/nearby/messages/Distance#compareTo(com.google.android.gms.nearby.messages.Distance)) ( [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) other) Note: This compares only `getMeters()` , not accuracy. |
| abstract int | [getAccuracy](/android/reference/com/google/android/gms/nearby/messages/Distance#getAccuracy()) () The accuracy of the distance estimate. |
| abstract double | [getMeters](/android/reference/com/google/android/gms/nearby/messages/Distance#getMeters()) () The distance estimate, in meters. |

### Inherited Method Summary

From interface java.lang.Comparable

|  |  |
| --- | --- |
| abstract int | compareTo ( [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) arg0) |








## Fields

#### public static final Distance **UNKNOWN**

Represents an unknown distance, with accuracy =
`Distance.Accuracy.LOW`
,
and meters =
`Double.NaN`
.






## Public Methods

#### public abstract int **compareTo** ( [Distance](/android/reference/com/google/android/gms/nearby/messages/Distance) other)

Note: This compares only
`getMeters()`
,
not accuracy. We also consider NaN == NaN, so that
`UNKNOWN`
.equals(
`UNKNOWN`
).

#### public abstract int **getAccuracy** ()

The accuracy of the distance estimate.

#### public abstract double **getMeters** ()

The distance estimate, in meters.