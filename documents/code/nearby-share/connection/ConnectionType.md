<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/ConnectionType -->

# ConnectionType

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* ConnectionType is an annotation used by Nearby Connection to establish a connection.
* There are three connection types: BALANCED, DISRUPTIVE, and NON\_DISRUPTIVE.
* BALANCED changes Wi-Fi or Bluetooth status only if necessary.
* DISRUPTIVE changes Wi-Fi or Bluetooth status to enhance throughput, potentially losing internet connection.
* NON\_DISRUPTIVE does not change Wi-Fi or Bluetooth status.



public abstract @interface
**ConnectionType**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

The connection type which Nearby Connection used to establish a connection.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [BALANCED](/android/reference/com/google/android/gms/nearby/connection/ConnectionType#BALANCED) | Nearby Connections will change the device's Wi-Fi or Bluetooth status only if necessary. |
| int | [DISRUPTIVE](/android/reference/com/google/android/gms/nearby/connection/ConnectionType#DISRUPTIVE) | Nearby Connections will change the device's Wi-Fi or Bluetooth status to enhance throughput. |
| int | [NON\_DISRUPTIVE](/android/reference/com/google/android/gms/nearby/connection/ConnectionType#NON_DISRUPTIVE) | Nearby Connections should not change the device's Wi-Fi or Bluetooth status. |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **BALANCED**

Nearby Connections will change the device's Wi-Fi or Bluetooth status only if
necessary.

Constant Value:

0

#### public static final int **DISRUPTIVE**

Nearby Connections will change the device's Wi-Fi or Bluetooth status to enhance
throughput. This may cause the device to lose its internet connection.

Constant Value:

1

#### public static final int **NON\_DISRUPTIVE**

Nearby Connections should not change the device's Wi-Fi or Bluetooth status.

Constant Value:

2