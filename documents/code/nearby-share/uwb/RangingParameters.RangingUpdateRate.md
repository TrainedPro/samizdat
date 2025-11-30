<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate -->

# RangingParameters.RangingUpdateRate

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* RangingUpdateRate defines settings for how often ranging data is reported, with intervals varying based on the UwbConfigId.
* There are three main update rates: AUTOMATIC, FREQUENT, and INFREQUENT, each with different reporting frequencies and corresponding ranging intervals.
* Developers should query ranging capabilities to check supported update rates before starting a new session.
* The
  `FREQUENT`
  update rate may become unavailable if multiple sessions are opened using it, potentially requiring the use of
  `AUTOMATIC`
  or
  `INFREQUENT`
  .



public static abstract @interface
**RangingParameters.RangingUpdateRate**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Update rate settings. The corresponding ranging interval (in milliseconds) for each
combination of update rate and
`UwbConfigId`
is shown in the table below.

```
   ___________________________________________________________
  |   CONFIG_ID     | AUTOMATIC  |   FREQUENT   |  INFREQUENT |
  |-----------------|------------|--------------|-------------|
  |  CONFIG_ID_1    |    240     |     120      |     600     |
  |  CONFIG_ID_2    |    200     |     120      |     600     |
  |  CONFIG_ID_3    |    200     |     120      |     600     |
  |  CONFIG_ID_4    |    240     |     120      |     600     |
  |  CONFIG_ID_5    |    200     |     120      |     600     |
  |  CONFIG_ID_6    |    200     |     120      |     600     |
  |  CONFIG_ID_7    |    200     |     120      |     600     |
  |___________________________________________________________|
```

It is recommended to query ranging capabilities to check supported ranging update rate for
each new session. If multiple sessions are opened with
`Frequent`
, it will no longer be
available and the app may have to use
`Automatic`
or
`Infrequent`
.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [AUTOMATIC](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate#AUTOMATIC) | Automatically changing the reporting rate when screen is on or off. |
| int | [FREQUENT](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate#FREQUENT) | Reports ranging data frequently. |
| int | [INFREQUENT](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate#INFREQUENT) | Reports ranging data infrequently. |
| int | [UNKNOWN](/android/reference/com/google/android/gms/nearby/uwb/RangingParameters.RangingUpdateRate#UNKNOWN) | Invalid Update Rate Setting |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **AUTOMATIC**

Automatically changing the reporting rate when screen is on or off. The ranging
interval is set to 240 milliseconds when using
`CONFIG_ID_1`
and
`CONFIG_ID_4`
. In other cases, the ranging interval is set to 200
milliseconds.

Constant Value:

1

#### public static final int **FREQUENT**

Reports ranging data frequently. The ranging interval is set to 120 milliseconds for
this update rate.

Constant Value:

3

#### public static final int **INFREQUENT**

Reports ranging data infrequently. The ranging interval is set to 600 milliseconds
for this update rate.

Constant Value:

2

#### public static final int **UNKNOWN**

Invalid Update Rate Setting

Constant Value:

0