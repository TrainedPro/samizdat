<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/AuthenticationException -->

# AuthenticationException

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* AuthenticationException is used for DeviceProvider connection errors.
* It is a public final class that extends Exception.
* There are four public constructors available for creating an AuthenticationException with varying levels of detail.
* AuthenticationException inherits methods from java.lang.Throwable and java.lang.Object.



public final class
**AuthenticationException**
extends
[Exception](//developer.android.com/reference/java/lang/Exception.html)

`AuthenticationException`
for DeviceProvider connection.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [AuthenticationException](/android/reference/com/google/android/gms/nearby/connection/AuthenticationException#AuthenticationException()) () Constructs an `AuthenticationException` with no detail message. |
|  | [AuthenticationException](/android/reference/com/google/android/gms/nearby/connection/AuthenticationException#AuthenticationException(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) msg) Constructs an `AuthenticationException` with the specified detail message. |
|  | [AuthenticationException](/android/reference/com/google/android/gms/nearby/connection/AuthenticationException#AuthenticationException(java.lang.String,%20java.lang.Throwable)) ( [String](//developer.android.com/reference/java/lang/String.html) msg, [Throwable](//developer.android.com/reference/java/lang/Throwable.html) cause) Creates an `AuthenticationException` with the specified detail message and cause. |
|  | [AuthenticationException](/android/reference/com/google/android/gms/nearby/connection/AuthenticationException#AuthenticationException(java.lang.Throwable)) ( [Throwable](//developer.android.com/reference/java/lang/Throwable.html) cause) Creates an `AuthenticationException` with the specified cause and a detail message of `(cause==null ? null : cause.toString())` (which typically contains the class and detail message of `cause` ). |

### Inherited Method Summary

From class java.lang.Throwable

|  |  |
| --- | --- |
| synchronized final void | addSuppressed ( [Throwable](//developer.android.com/reference/java/lang/Throwable.html) arg0) |
| synchronized [Throwable](//developer.android.com/reference/java/lang/Throwable.html) | fillInStackTrace () |
| synchronized [Throwable](//developer.android.com/reference/java/lang/Throwable.html) | getCause () |
| [String](//developer.android.com/reference/java/lang/String.html) | getLocalizedMessage () |
| [String](//developer.android.com/reference/java/lang/String.html) | getMessage () |
| [StackTraceElement[]](//developer.android.com/reference/java/lang/StackTraceElement.html) | getStackTrace () |
| synchronized final [Throwable[]](//developer.android.com/reference/java/lang/Throwable.html) | getSuppressed () |
| synchronized [Throwable](//developer.android.com/reference/java/lang/Throwable.html) | initCause ( [Throwable](//developer.android.com/reference/java/lang/Throwable.html) arg0) |
| void | printStackTrace () |
| void | printStackTrace ( [PrintWriter](//developer.android.com/reference/java/io/PrintWriter.html) arg0) |
| void | printStackTrace ( [PrintStream](//developer.android.com/reference/java/io/PrintStream.html) arg0) |
| void | setStackTrace ( [StackTraceElement[]](//developer.android.com/reference/java/lang/StackTraceElement.html) arg0) |
| [String](//developer.android.com/reference/java/lang/String.html) | toString () |

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









## Public Constructors

#### public **AuthenticationException** ()

Constructs an
`AuthenticationException`
with no detail message. A detail
message is a String that describes this particular exception.

#### public **AuthenticationException** ( [String](//developer.android.com/reference/java/lang/String.html) msg)

Constructs an
`AuthenticationException`
with the specified detail
message. A detail message is a String that describes this particular exception.

##### Parameters

|  |  |
| --- | --- |
| msg | the detail message. |

#### public **AuthenticationException** ( [String](//developer.android.com/reference/java/lang/String.html) msg, [Throwable](//developer.android.com/reference/java/lang/Throwable.html) cause)

Creates an
`AuthenticationException`
with the specified detail message
and cause.

##### Parameters

|  |  |
| --- | --- |
| msg | the detail message (which is saved for later retrieval by the `Throwable.getMessage()` method). |
| cause | the cause (which is saved for later retrieval by the `Throwable.getCause()` method). A `null` value is permitted, and indicates that the cause is nonexistent or unknown. |

#### public **AuthenticationException** ( [Throwable](//developer.android.com/reference/java/lang/Throwable.html) cause)

Creates an
`AuthenticationException`
with the specified cause and a
detail message of
`(cause==null ? null : cause.toString())`
(which typically
contains the class and detail message of
`cause`
).

##### Parameters

|  |  |
| --- | --- |
| cause | the cause (which is saved for later retrieval by the `Throwable.getCause()` method). A `null` value is permitted, and indicates that the cause is nonexistent or unknown. |