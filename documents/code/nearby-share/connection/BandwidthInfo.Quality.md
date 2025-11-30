<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality -->

# BandwidthInfo.Quality

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* BandwidthInfo.Quality is an annotation defining different levels of bandwidth quality.
* The different quality levels are HIGH, LOW, MEDIUM, and UNKNOWN, representing varying connection speeds and suitability for sending files.
* Each quality level has a constant integer value associated with it.
* The LOW quality is not suitable for sending files, MEDIUM is suitable for small files, and HIGH is good for readily sending files.



public static abstract @interface
**BandwidthInfo.Quality**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Bandwidth quality.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [HIGH](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality#HIGH) | The connection quality is good or great (6MBps~60MBps) and files can readily be sent. |
| int | [LOW](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality#LOW) | The connection quality is poor (5KBps) and is not suitable for sending files. |
| int | [MEDIUM](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality#MEDIUM) | The connection quality is ok (60~200KBps) and is suitable for sending small files. |
| int | [UNKNOWN](/android/reference/com/google/android/gms/nearby/connection/BandwidthInfo.Quality#UNKNOWN) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **HIGH**

The connection quality is good or great (6MBps~60MBps) and files can readily be
sent. The connection quality cannot improve further but may still be impacted by
environment or hardware limitations.

Constant Value:

3

#### public static final int **LOW**

The connection quality is poor (5KBps) and is not suitable for sending files. It's
recommended you wait until the connection quality improves.

Constant Value:

1

#### public static final int **MEDIUM**

The connection quality is ok (60~200KBps) and is suitable for sending small files.
For large files, it's recommended you wait until the connection quality improves.

Constant Value:

2

#### public static final int **UNKNOWN**

Constant Value:

0