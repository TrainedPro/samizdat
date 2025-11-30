<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement -->

# RangingMeasurement

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingMeasurement is a class that provides the value and confidence of ranging.
* The class has a public method
  `getValue()`
  which returns the value of the measurement as a float.
* RangingMeasurement inherits several methods from the
  `java.lang.Object`
  class, including
  `clone()`
  ,
  `equals()`
  , and
  `toString()`
  .



public class
**RangingMeasurement**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Measurement providing the value and confidence of the ranging.

### Public Method Summary

|  |  |
| --- | --- |
| float | [getValue](/android/reference/com/google/android/gms/nearby/uwb/RangingMeasurement#getValue()) () Gets value of this measurement. |

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












## Public Methods

#### public float **getValue** ()

Gets value of this measurement.