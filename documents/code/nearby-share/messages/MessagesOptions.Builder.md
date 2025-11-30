<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder -->

# MessagesOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `MessagesOptions.Builder`
  is a builder class for creating
  `MessagesOptions`
  objects.
* It has a public constructor
  `Builder()`
  to create a new builder instance.
* The
  `build()`
  method creates the final
  `MessagesOptions`
  object from the builder.
* The
  `setPermissions(int permissions)`
  method is used to specify the required Nearby permissions.



public static class
**MessagesOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for
`MessagesOptions`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) | [build](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder#build()) () Builds the `MessagesOptions` . |
| [MessagesOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder) | [setPermissions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder#setPermissions(int)) (int permissions) Sets which `NearbyPermissions` are requested for Nearby. |

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









## Public Constructors

#### public **Builder** ()





## Public Methods

#### public [MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions) **build** ()

Builds the
`MessagesOptions`
.

#### public [MessagesOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions.Builder) **setPermissions** (int permissions)

Sets which
`NearbyPermissions`
are requested for Nearby.

By setting a more restrictive scope of permissions, Nearby will shrink its opt in
dialog appropriately. However, ensure that all the publish and subscribe calls are
limited to the provided scope. Otherwise, api calls will fail with error
`NearbyMessagesStatusCodes.MISSING_PERMISSIONS`
.