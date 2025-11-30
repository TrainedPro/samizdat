<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions -->

# NearbyPermissions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* NearbyPermissions is an annotation that determines the scope of permissions Nearby will ask for at connection time.
* Calls to Messages#Publish() and Messages#Subscribe() require a Strategy that matches the requested permissions.
* Failing to match the Strategy with the requested permissions will result in a failure.
* The available permission constants include BLE, BLUETOOTH, DEFAULT, MICROPHONE, and NONE.



public abstract @interface
**NearbyPermissions**
implements
[Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html)

Determines the scope of permissions Nearby will ask for at connection time.

Calls to
`Messages#Publish()`
and
`Messages#Subscribe()`
require a
`Strategy`
that matches the permissions you've requested. For example, attempting to subscribe using
BLE, but only requesting the
`MICROPHONE`
permission will result in a failure.

### Constant Summary

|  |  |  |
| --- | --- | --- |
| int | [BLE](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions#BLE) |  |
| int | [BLUETOOTH](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions#BLUETOOTH) |  |
| int | [DEFAULT](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions#DEFAULT) |  |
| int | [MICROPHONE](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions#MICROPHONE) |  |
| int | [NONE](/android/reference/com/google/android/gms/nearby/messages/NearbyPermissions#NONE) |  |

### Inherited Method Summary

From interface java.lang.annotation.Annotation

|  |  |
| --- | --- |
| abstract [Class](//developer.android.com/reference/java/lang/Class.html) <? extends [Annotation](//developer.android.com/reference/java/lang/annotation/Annotation.html) > | annotationType () |
| abstract boolean | equals ( [Object](//developer.android.com/reference/java/lang/Object.html) arg0) |
| abstract int | hashCode () |
| abstract [String](//developer.android.com/reference/java/lang/String.html) | toString () |







## Constants

#### public static final int **BLE**

Constant Value:

2

#### public static final int **BLUETOOTH**

Constant Value:

6

#### public static final int **DEFAULT**

Constant Value:

-1

#### public static final int **MICROPHONE**

Constant Value:

1

#### public static final int **NONE**

Constant Value:

0