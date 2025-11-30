<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder -->

# SubscribeOptions.Builder

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* `SubscribeOptions.Builder`
  is used to build instances of
  `SubscribeOptions`
  .
* The builder includes methods to set a callback for subscription events, a filter for messages, and a strategy for subscribing.
* The
  `build()`
  method creates the final
  `SubscribeOptions`
  instance.



public static class
**SubscribeOptions.Builder**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)

Builder for instances of
`SubscribeOptions`
.

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder#Builder()) () |

### Public Method Summary

|  |  |
| --- | --- |
| [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) | [build](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder#build()) () Builds an instance of `SubscribeOptions` . |
| [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) | [setCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder#setCallback(com.google.android.gms.nearby.messages.SubscribeCallback)) ( [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback) callback) Sets a callback which will be notified when significant events occur that affect this subscription. |
| [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) | [setFilter](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder#setFilter(com.google.android.gms.nearby.messages.MessageFilter)) ( [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) filter) Sets a filter to specify which messages to receive. |
| [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) | [setStrategy](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder#setStrategy(com.google.android.gms.nearby.messages.Strategy)) ( [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) strategy) Sets a strategy for subscribing. |

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

#### public [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) **build** ()

Builds an instance of
`SubscribeOptions`
.

#### public [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) **setCallback** ( [SubscribeCallback](/android/reference/com/google/android/gms/nearby/messages/SubscribeCallback) callback)

Sets a callback which will be notified when significant events occur that affect
this subscription.

#### public [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) **setFilter** ( [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) filter)

Sets a filter to specify which messages to receive. If not specified, the default is
`MessageFilter.INCLUDE_ALL_MY_TYPES`
.

#### public [SubscribeOptions.Builder](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions.Builder) **setStrategy** ( [Strategy](/android/reference/com/google/android/gms/nearby/messages/Strategy) strategy)

Sets a strategy for subscribing. If not specified, the default is
`Strategy.DEFAULT`
.