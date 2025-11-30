<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/PublishOptions -->

# PublishOptions

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* PublishOptions provides options for calls to Messages.publish.
* Instances of PublishOptions should be created using the PublishOptions.Builder.
* PublishOptions has a default instance available.
* You can retrieve the publishing callback and strategy using the getCallback and getStrategy methods.



public final class
**PublishOptions**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Options for calls to
`Messages.publish(GoogleApiClient, Message)`
.

Use the
`PublishOptions.Builder`
to create an instance of this class, e.g.:

```
PublishOptions options = new PublishOptions.Builder()
     .setStrategy(someStrategy)
     .setCallback(myCallback)
     .build();
```

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) | | Builder for instances of `PublishOptions` . |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) | [DEFAULT](/android/reference/com/google/android/gms/nearby/messages/PublishOptions#DEFAULT) |  |

### Public Method Summary

|  |  |
| --- | --- |
| [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback) | [getCallback](/android/reference/com/google/android/gms/nearby/messages/PublishOptions#getCallback()) () Gets the publishing callback. |
| [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) | [getStrategy](/android/reference/com/google/android/gms/nearby/messages/PublishOptions#getStrategy()) () Gets the publishing strategy. |

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

#### public static final PublishOptions **DEFAULT**






## Public Methods

#### public [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback) **getCallback** ()

Gets the publishing callback.

#### public [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) **getStrategy** ()

Gets the publishing strategy.