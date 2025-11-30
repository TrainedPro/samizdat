<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/Messages -->

# Messages

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* This interface is deprecated and
  `MessagesClient`
  should be used instead.
* The API allows publishing and subscribing to simple messages from nearby devices unauthenticated.
* Using the API requires enabling the Nearby Messages API and creating an API key in the Google Developers Console.
* Publish and subscribe operations should ideally be managed within the
  `onStart()`
  and
  `onStop()`
  lifecycle methods of a foreground Activity.



public interface
**Messages**

**This interface is deprecated.**
  
Use
`MessagesClient`
.

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
 protected void onCreate(Bundle savedInstanceState) {
   mGoogleApiClient = new GoogleApiClient.Builder(context)
       .addApi(Nearby.MESSAGES_API)
       .addConnectionCallbacks(this)
       .build();
 }

 @Override
 protected void onStart() {
   mGoogleApiClient.connect();
 }

 @Override
 public void onConnected(Bundle connectionHint) {
   PendingResult<Status> pendingResult = Nearby.Messages.publish(mGoogleApiClient,
       new Message(bytes));
 }
```

All of the Messages APIs should be used from a foreground Activity, with the exception of
the variants of
`subscribe`
that take a
`PendingIntent`
parameter. Your Activity should
`publish(GoogleApiClient, Message)`
or
`subscribe(GoogleApiClient, MessageListener)`
either in
`Activity.onStart()`
or in response to a user action in a visible Activity, and you should always symmetrically
`unpublish(GoogleApiClient, Message)`
or
`unsubscribe(GoogleApiClient, MessageListener)`
in
`Activity.onStop()`
.

### Public Method Summary

|  |  |
| --- | --- |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [getPermissionStatus](/android/reference/com/google/android/gms/nearby/messages/Messages#getPermissionStatus(com.google.android.gms.common.api.GoogleApiClient)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client) *This method is deprecated. This call will always succeed now that permission status is handled at connection time.* |
| abstract void | [handleIntent](/android/reference/com/google/android/gms/nearby/messages/Messages#handleIntent(android.content.Intent,%20com.google.android.gms.nearby.messages.MessageListener)) ( [Intent](//developer.android.com/reference/android/content/Intent.html) intent, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) messageListener) Extracts information from an Intent sent as a subscription callback, and calls the corresponding methods on the given MessageListener. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [publish](/android/reference/com/google/android/gms/nearby/messages/Messages#publish(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.Message)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Publishes a message so that it is visible to nearby devices, using the default options from `PublishOptions.DEFAULT` . |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [publish](/android/reference/com/google/android/gms/nearby/messages/Messages#publish(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.Message,%20com.google.android.gms.nearby.messages.PublishOptions)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) options) Publishes a message so that it is visible to nearby devices. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [registerStatusCallback](/android/reference/com/google/android/gms/nearby/messages/Messages#registerStatusCallback(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.StatusCallback)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback) Registers a status callback, which will be notified when significant events occur that affect Nearby for your app. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#subscribe(com.google.android.gms.common.api.GoogleApiClient,%20android.app.PendingIntent,%20com.google.android.gms.nearby.messages.SubscribeOptions)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options) Note: Currently, this method only finds messages attached to BLE beacons. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#subscribe(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.MessageListener,%20com.google.android.gms.nearby.messages.SubscribeOptions)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options) Subscribes for published messages from nearby devices. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#subscribe(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.MessageListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener) Subscribes for published messages from nearby devices, using the default options in `SubscribeOptions.DEFAULT` . |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [subscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#subscribe(com.google.android.gms.common.api.GoogleApiClient,%20android.app.PendingIntent)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent) Note: Currently, this method only finds messages attached to BLE beacons. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [unpublish](/android/reference/com/google/android/gms/nearby/messages/Messages#unpublish(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.Message)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message) Cancels an existing published message. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [unregisterStatusCallback](/android/reference/com/google/android/gms/nearby/messages/Messages#unregisterStatusCallback(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.StatusCallback)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback) Unregisters a status callback previously registered with `registerStatusCallback(GoogleApiClient, StatusCallback)` . |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [unsubscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#unsubscribe(com.google.android.gms.common.api.GoogleApiClient,%20android.app.PendingIntent)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent) Cancels an existing subscription. |
| abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > | [unsubscribe](/android/reference/com/google/android/gms/nearby/messages/Messages#unsubscribe(com.google.android.gms.common.api.GoogleApiClient,%20com.google.android.gms.nearby.messages.MessageListener)) ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener) Cancels an existing subscription. |












## Public Methods

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **getPermissionStatus** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client)

**This method is deprecated.**
  
This call will always succeed now that permission status is handled at connection
time.

Checks if the user has granted this app permission to publish and subscribe.

In particular, the status returned can be

* `NearbyMessagesStatusCodes.APP_NOT_OPTED_IN`
  - If the app asked for
  permission and was denied by user.
* `CommonStatusCodes.SUCCESS`
  - If the app has permission to publish and
  subscribe.

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the app has all the required permissions to
  publish/subscribe.

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

 Nearby.Messages.subscribe(googleApiClient, pendingIntent, ...);

 public class MyService extends IntentService {
   protected void onHandleIntent(Intent intent) {
     Nearby.Messages.handleIntent(intent, new MessageListener() {
       @Override
       public void onFound(Message message) {...}
     }
   });
 }
```

##### Parameters

|  |  |
| --- | --- |
| intent | An intent that was sent as a result of `subscribe(GoogleApiClient, PendingIntent, SubscribeOptions)` |
| messageListener | This will be called immediately with information present in the Intent |

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **publish** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Publishes a message so that it is visible to nearby devices, using the default
options from
`PublishOptions.DEFAULT`
.

##### See Also

* `publish(GoogleApiClient, Message, PublishOptions)`

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **publish** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message, [PublishOptions](/android/reference/com/google/android/gms/nearby/messages/PublishOptions) options)

Publishes a message so that it is visible to nearby devices.

The message is only delivered to apps that share the same project id in the
Developer Console and have an active subscription. Create project identifiers and turn
on the Nearby API in the
[Google Developers
Console](//console.developers.google.com)
.

Allowed Contexts
`GoogleApiClient`
can be bound to:

* `Activity`
* `FragmentActivity`

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| message | A `Message` to publish for nearby devices to see |
| options | A `PublishOptions` object for this operation |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **registerStatusCallback** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback)

Registers a status callback, which will be notified when significant events occur
that affect Nearby for your app.

When your app first calls this API, it may be immediately called back with current
status.

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` . |
| statusCallback | A callback to notify when events occur. |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

##### See Also

* `unregisterStatusCallback(GoogleApiClient, StatusCallback)`

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **subscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options)

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
`unsubscribe(GoogleApiClient, PendingIntent)`
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
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| pendingIntent | A `PendingIntent` to get callbacks about nearby messages |
| options | A `SubscribeOptions` object for this operation |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **subscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener, [SubscribeOptions](/android/reference/com/google/android/gms/nearby/messages/SubscribeOptions) options)

Subscribes for published messages from nearby devices.

Only messages published by apps sharing the same project id in the Developer Console
will be delivered.

Allowed Contexts
`GoogleApiClient`
can be bound to:

* `Activity`
* `FragmentActivity`

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| listener | A `MessageListener` implementation to get callbacks of received messages |
| options | A `SubscribeOptions` object for this operation |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **subscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener)

Subscribes for published messages from nearby devices, using the default options in
`SubscribeOptions.DEFAULT`
.

##### See Also

* `subscribe(GoogleApiClient, MessageListener, SubscribeOptions)`

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **subscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent)

Note: Currently, this method only finds messages attached to BLE beacons. See
[Beacons](//developers.google.com/beacons/)
.

Subscribes for published messages from nearby devices in a persistent and low-power
manner, using the default options in
`SubscribeOptions.DEFAULT`
.

##### See Also

* `subscribe(GoogleApiClient, PendingIntent, SubscribeOptions)`

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **unpublish** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [Message](/android/reference/com/google/android/gms/nearby/messages/Message) message)

Cancels an existing published message.

If this method is called and the message is not currently published, it will return
a
`Status`
of
`CommonStatusCodes.SUCCESS`
.

Allowed Contexts
`GoogleApiClient`
can be bound to:

* `Activity`
* `FragmentActivity`

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| message | A `Message` that is currently published |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **unregisterStatusCallback** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [StatusCallback](/android/reference/com/google/android/gms/nearby/messages/StatusCallback) statusCallback)

Unregisters a status callback previously registered with
`registerStatusCallback(GoogleApiClient, StatusCallback)`
.

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` . |
| statusCallback | A callback previously registered with `registerStatusCallback(GoogleApiClient, StatusCallback)` . |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

##### See Also

* `registerStatusCallback(GoogleApiClient, StatusCallback)`

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **unsubscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [PendingIntent](//developer.android.com/reference/android/app/PendingIntent.html) pendingIntent)

Cancels an existing subscription.

If this method is called and the
`pendingIntent`
is not currently
subscribed, it will return a
`Status`
of
`CommonStatusCodes.SUCCESS`
.

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| pendingIntent | A `PendingIntent` that is currently subscribed |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.

#### public abstract [PendingResult](/android/reference/com/google/android/gms/common/api/PendingResult) < [Status](/android/reference/com/google/android/gms/common/api/Status) > **unsubscribe** ( [GoogleApiClient](/android/reference/com/google/android/gms/common/api/GoogleApiClient) client, [MessageListener](/android/reference/com/google/android/gms/nearby/messages/MessageListener) listener)

Cancels an existing subscription.

If this method is called and the
`listener`
is not currently subscribed,
it will return a
`Status`
of
`CommonStatusCodes.SUCCESS`
.

Allowed Contexts
`GoogleApiClient`
can be bound to:

* `Activity`
* `FragmentActivity`

##### Parameters

|  |  |
| --- | --- |
| client | A connected `GoogleApiClient` for `Nearby.MESSAGES_API` |
| listener | A `MessageListener` implementation that is currently subscribed |

##### Returns

* The
  `PendingResult`
  which can be used to determine if the call succeeded.