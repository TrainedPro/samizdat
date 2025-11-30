<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo -->

# BandwidthInfo

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* BandwidthInfo provides information about a connection's bandwidth.
* It includes a nested class, BandwidthInfo.Quality, to indicate bandwidth quality.
* The getQuality() method returns the current connection quality as an integer.



public final class
**BandwidthInfo**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Information about a connection's bandwidth.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| @interface | [BandwidthInfo.Quality](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality) | | Bandwidth quality. |

### Public Method Summary

|  |  |
| --- | --- |
| int | [getQuality](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo#getQuality()) () The connection's current quality. |

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

#### public int **getQuality** ()

The connection's current quality. With
`BandwidthInfo.Quality.LOW`
, large payloads may be slow to send.