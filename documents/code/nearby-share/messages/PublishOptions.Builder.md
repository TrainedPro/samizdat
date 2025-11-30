<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder -->

# PublishOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `PublishOptions.Builder`
  is a builder for creating instances of
  `PublishOptions`
  .
* It has a constructor to create a new builder.
* It provides methods to build the
  `PublishOptions`
  instance, set a callback for publish events, and set the publishing strategy.



public static class
**PublishOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for instances of
`PublishOptions`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) | [build](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder#build()) () Builds an instance of `PublishOptions` . |
| [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) | [setCallback](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder#setCallback(com.google.android.gms.nearby.messages.PublishCallback)) ( [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback) callback) Sets a callback which will be notified when significant events occur that affect this publish. |
| [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) | [setStrategy](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder#setStrategy(com.google.android.gms.nearby.messages.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) strategy) Sets the strategy for publishing. |

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

#### public [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) **build** ()

Builds an instance of
`PublishOptions`
.

#### public [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) **setCallback** ( [PublishCallback](/android/reference/com/google/android/gms/nearby/messages/PublishCallback) callback)

Sets a callback which will be notified when significant events occur that affect
this publish.

#### public [PublishOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/PublishOptions.Builder) **setStrategy** ( [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) strategy)

Sets the strategy for publishing. The default if not explicitly set is
`Strategy.DEFAULT`
.