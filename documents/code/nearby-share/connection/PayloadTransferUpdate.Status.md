<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status -->

# PayloadTransferUpdate.Status

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PayloadTransferUpdate.Status is an abstract annotation representing the status of a payload transfer.
* It includes constants to indicate the transfer status: CANCELED, FAILURE, IN\_PROGRESS, and SUCCESS.



public static abstract @interface
**PayloadTransferUpdate.Status**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

The status of the payload transfer at the time of this update.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [CANCELED](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status#CANCELED) |  |
| int | [FAILURE](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status#FAILURE) |  |
| int | [IN\_PROGRESS](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status#IN_PROGRESS) |  |
| int | [SUCCESS](/android/reference/com/google/android/gms/nearby/connection/PayloadTransferUpdate.Status#SUCCESS) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **CANCELED**

Constant Value:

4

#### public static final int **FAILURE**

Constant Value:

2

#### public static final int **IN\_PROGRESS**

Constant Value:

3

#### public static final int **SUCCESS**

Constant Value:

1