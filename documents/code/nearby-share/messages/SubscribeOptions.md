<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions -->

# SubscribeOptions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* SubscribeOptions provides options for the Messages.subscribe method.
* SubscribeOptions instances are created using the SubscribeOptions.Builder.
* The class includes methods to retrieve the subscription callback, filter, and strategy.



public final class
**SubscribeOptions**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Options for calls to
`Messages.subscribe(GoogleApiClient, PendingIntent)`
.

Use the
`SubscribeOptions.Builder`
to create an instance of this class, e.g.:

```
SubscribeOptions options = new SubscribeOptions.Builder()
     .setStrategy(someStrategy)
     .setCallback(myCallback)
     .build();
```

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) | | Builder for instances of `SubscribeOptions` . |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) | [DEFAULT](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions#DEFAULT) |  |

### Public Method Summary

|  |  |
| --- | --- |
| [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback) | [getCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions#getCallback()) () Gets the subscription callback. |
| [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) | [getFilter](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions#getFilter()) () Gets the subscription filter. |
| [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | [getStrategy](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions#getStrategy()) () Gets the subscription strategy. |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions#toString()) () |

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








## Fields

#### public static final SubscribeOptions **DEFAULT**






## Public Methods

#### public [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback) **getCallback** ()

Gets the subscription callback.

#### public [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) **getFilter** ()

Gets the subscription filter.

#### public [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) **getStrategy** ()

Gets the subscription strategy.

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()