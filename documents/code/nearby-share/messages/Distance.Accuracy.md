<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Distance.Accuracy -->

# Distance.Accuracy

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Distance.Accuracy is an abstract interface defining possible values for the accuracy of a distance estimate.
* The constant
  `LOW`
  indicates that the distance was estimated from BLE signal strength.



public static abstract @interface
**Distance.Accuracy**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Possible values for the accuracy of a distance estimate.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [LOW](/android/reference/com/google/android/gms/nearby/messages/Distance.Accuracy#LOW) | Distance estimated from BLE signal strength has this accuracy. |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **LOW**

Distance estimated from BLE signal strength has this accuracy.

Constant Value:

1