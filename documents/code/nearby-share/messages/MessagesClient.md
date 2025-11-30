<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/MessagesClient -->

# MessagesClient

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The
  `MessagesClient`
  interface is deprecated and will be removed by the end of 2023;
  `ConnectionsClient`
  should be used instead.
* This API allows apps to publish and subscribe to simple messages with nearby devices without requiring a Google account.
* Using the API requires enabling the Nearby Messages API in the Google Developers Console and adding an API key to the application's manifest.
* Messages API operations should generally be performed from a foreground Activity, except for subscribe variants using
  `PendingIntent`
  .



public interface
**MessagesClient**
implements
[HasApiKey](/android/reference/com/google/android/gms/common/api/HasApiKey)
<
[MessagesOptions](/android/reference/com/google/android/gms/nearby/messages/MessagesOptions)
>

**This interface is deprecated.**
  
Nearby Messages will be removed by the end of 2023. Use
`ConnectionsClient`
instead.

API which allows your app to publish simple messages and subscribe to receive those
messages from nearby devices.

The API performs its operations in an unauthenticated manner, so it does not require a
Google account. However, it requires that the developer has a project in the
[Google Developers Console](//console.developers.google.com)
with the following
prerequisites:

1. Nearby Messages API turned on.
     
   Follow these
   [instructions](//support.google.com/cloud/answer/6158841)
   to
   enable the "Nearby Messages API".
2. An API key for the Android application using its package name and SHA1 fingerprint.
     
   Follow these
   [instructions](//support.google.com/cloud/answer/6158862)
   to
   create the "Public API access" API key specific for your app.
3. Add the API key generated above to your application's manifest:

   ```
    <manifest ...>
      <application ...>
        <meta-data
            android:name="com.google.android.nearby.messages.API_KEY"
            android:value="SPECIFY_APPLICATION_API_KEY_HERE" />
        <activity>
        ...
        </activity>
      </application>
    </manifest>
   ```

The Messages API should be accessed from the
`Nearby`
entry point.
For example:

```
 @Override
 protected void onStart() {
   Task<Void> task = Nearby.getMessagesClient(context).publish(new Message(bytes));
 }
```

All of the Messages APIs should be used from a foreground Activity, with the exception of
the variants of
`subscribe`
that take a
`PendingIntent`
parameter. Your Activity should
`publish(Message)`
or
`subscribe(MessageListener)`
either in
`Activity.onStart()`
or in response to a user action in a visible Activity, and you should always symmetrically
`unpublish(Message)`
or
`unsubscribe(MessageListener)`
in
`Activity.onStop()`
.

### Public Method Summary

|  |  |
| --- | --- |
| abstract void | [handleIntent](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#handleIntent(android.content.Intent,%20com.google.android.gms.nearby.messages.MessageListener)) ( [Intent](//developer.android.com/reference/android/content/Intent.html) intent, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) messageListener) Extracts information from an Intent sent as a subscription callback, and calls the corresponding methods on the given MessageListener. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [publish](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#publish(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Publishes a message so that it is visible to nearby devices, using the default options from `PublishOptions.DEFAULT` . |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [publish](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#publish(com.google.android.gms.nearby.messages.Message,%20com.google.android.gms.nearby.messages.PublishOptions)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) options) Publishes a message so that it is visible to nearby devices. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [registerStatusCallback](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#registerStatusCallback(com.google.android.gms.nearby.messages.StatusCallback)) ( [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback) Registers a status callback, which will be notified when significant events occur that affect Nearby for your app. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#subscribe(com.google.android.gms.nearby.messages.MessageListener,%20com.google.android.gms.nearby.messages.SubscribeOptions)) ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options) Subscribes for published messages from nearby devices. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#subscribe(com.google.android.gms.nearby.messages.MessageListener)) ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener) Subscribes for published messages from nearby devices, using the default options in `SubscribeOptions.DEFAULT` . |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#subscribe(android.app.PendingIntent,%20com.google.android.gms.nearby.messages.SubscribeOptions)) ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options) Note: Currently, this method only finds messages attached to BLE beacons. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#subscribe(android.app.PendingIntent)) ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent) Note: Currently, this method only finds messages attached to BLE beacons. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [unpublish](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#unpublish(com.google.android.gms.nearby.messages.Message)) ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Cancels an existing published message. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [unregisterStatusCallback](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#unregisterStatusCallback(com.google.android.gms.nearby.messages.StatusCallback)) ( [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback) Unregisters a status callback previously registered with `registerStatusCallback(StatusCallback)` . |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [unsubscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#unsubscribe(com.google.android.gms.nearby.messages.MessageListener)) ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener) Cancels an existing subscription. |
| abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > | [unsubscribe](/android/reference/com/google/android/gms/nearby/messages/MessagesClient#unsubscribe(android.app.PendingIntent)) ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent) Cancels an existing subscription. |












## Public Methods

#### public abstract void **handleIntent** ( [Intent](//developer.android.com/reference/android/content/Intent.html) intent, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) messageListener)

Extracts information from an Intent sent as a subscription callback, and calls the
corresponding methods on the given MessageListener.

**Note:**
Only
`MessageListener.onFound(Message)`
and
`MessageListener.onLost(Message)`
are supported.

For example:

```
 PendingIntent pendingIntent = PendingIntent.getService(context, 0,
     new Intent(context, MyService.class), PendingIntent.FLAG_UPDATE_CURRENT);

 messagesClient.subscribe(pendingIntent, ...);

 public class MyService extends IntentService {
   protected void onHandleIntent(Intent intent) {
     messagesClient.handleIntent(intent, new MessageListener() {
       @Override
       public void onFound(Message message) {...}
     }
   });
 }
```

##### Parameters

|  |  |
| --- | --- |
| intent | An intent that was sent as a result of `subscribe(PendingIntent, SubscribeOptions)` |
| messageListener | This will be called immediately with information present in the Intent |

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **publish** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Publishes a message so that it is visible to nearby devices, using the default
options from
`PublishOptions.DEFAULT`
.

##### Parameters

|  |  |
| --- | --- |
| message | A `Message` to publish for nearby devices to see |

##### See Also

* `publish(Message, PublishOptions)`

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **publish** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) options)

Publishes a message so that it is visible to nearby devices.

The message is only delivered to apps that share the same project id in the
Developer Console and have an active subscription. Create project identifiers and turn
on the Nearby API in the
[Google Developers
Console](//console.developers.google.com)
.

##### Parameters

|  |  |
| --- | --- |
| message | A `Message` to publish for nearby devices to see |
| options | A `PublishOptions` object for this operation |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **registerStatusCallback** ( [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback)

Registers a status callback, which will be notified when significant events occur
that affect Nearby for your app.

When your app first calls this API, it may be immediately called back with current
status.

##### Parameters

|  |  |
| --- | --- |
| statusCallback | A callback to notify when events occur |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

##### See Also

* `unregisterStatusCallback(StatusCallback)`

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **subscribe** ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options)

Subscribes for published messages from nearby devices.

Only messages published by apps sharing the same project id in the Developer Console
will be delivered.

##### Parameters

|  |  |
| --- | --- |
| listener | A `MessageListener` implementation to get callbacks of received messages |
| options | A `SubscribeOptions` object for this operation |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **subscribe** ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener)

Subscribes for published messages from nearby devices, using the default options in
`SubscribeOptions.DEFAULT`
.

##### Parameters

|  |  |
| --- | --- |
| listener | A `MessageListener` implementation to get callbacks of received messages |

##### See Also

* `subscribe(MessageListener, SubscribeOptions)`

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **subscribe** ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options)

Note: Currently, this method only finds messages attached to BLE beacons. See
[Beacons](//developers.google.com/beacons/)
.

Subscribes for published messages from nearby devices in a persistent and low-power
manner. This uses less battery, but will have higher latency and lower reliability.
Notably, updates may only be delivered when the device's screen turns on (or when
Bluetooth turns on). The subscription will be retained after the client
disconnects.

Call
`handleIntent(Intent, MessageListener)`
to handle the Intents received from
Nearby while subscribed. Call
`unsubscribe(PendingIntent)`
to cancel the subscription.

Each application can pass up to 5
`PendingIntent`
s.
When the limit is reached, this returns
`NearbyMessagesStatusCodes.TOO_MANY_PENDING_INTENTS`
, and the subscription is
not created.

##### Parameters

|  |  |
| --- | --- |
| pendingIntent | A `PendingIntent` to get callbacks about nearby messages |
| options | A `SubscribeOptions` object for this operation |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **subscribe** ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent)

Note: Currently, this method only finds messages attached to BLE beacons. See
[Beacons](//developers.google.com/beacons/)
.

Subscribes for published messages from nearby devices in a persistent and low-power
manner, using the default options in
`SubscribeOptions.DEFAULT`
.

##### Parameters

|  |  |
| --- | --- |
| pendingIntent | A `PendingIntent` to get callbacks about nearby messages |

##### See Also

* `subscribe(PendingIntent, SubscribeOptions)`

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **unpublish** ( [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Cancels an existing published message.

##### Parameters

|  |  |
| --- | --- |
| message | A `Message` that is currently published |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **unregisterStatusCallback** ( [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback)

Unregisters a status callback previously registered with
`registerStatusCallback(StatusCallback)`
.

##### Parameters

|  |  |
| --- | --- |
| statusCallback | A callback previously registered with `registerStatusCallback(StatusCallback)` |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

##### See Also

* `registerStatusCallback(StatusCallback)`

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **unsubscribe** ( [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener)

Cancels an existing subscription.

##### Parameters

|  |  |
| --- | --- |
| listener | A `MessageListener` implementation that is currently subscribed |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded

#### public abstract Task< [Void](//developer.android.com/reference/java/lang/Void.html) > **unsubscribe** ( [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent)

Cancels an existing subscription.

##### Parameters

|  |  |
| --- | --- |
| pendingIntent | A `PendingIntent` that is currently subscribed |

##### Returns

* The
  `Task`
  which can
  be used to determine if the call succeeded