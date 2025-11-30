<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/Strategy -->

# Strategy

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* Strategy is used when discovering or advertising to Nearby devices, defining connectivity requirements and topology constraints.
* There are three peer-to-peer strategies available: P2P\_CLUSTER (M-to-N connections), P2P\_POINT\_TO\_POINT (1-to-1 connections with highest bandwidth), and P2P\_STAR (1-to-N connections in a star shape).
* Implementing Strategy requires specific permissions depending on the platform for advertising, discovering, and connecting.



public final class
**Strategy**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

The Strategy to be used when discovering or advertising to Nearby devices. The Strategy
defines

1. the connectivity requirements for the device, and
2. the topology constraints of the connection.

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | [P2P\_CLUSTER](/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_CLUSTER) | Peer-to-peer strategy that supports an M-to-N, or cluster-shaped, connection topology. |
| public static final [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | [P2P\_POINT\_TO\_POINT](/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_POINT_TO_POINT) | Peer-to-peer strategy that supports a 1-to-1 connection topology. |
| public static final [Strategy](/android/reference/com/google/android/gms/nearby/connection/Strategy) | [P2P\_STAR](/android/reference/com/google/android/gms/nearby/connection/Strategy#P2P_STAR) | Peer-to-peer strategy that supports a 1-to-N, or star-shaped, connection topology. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/connection/Strategy#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) object) |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/connection/Strategy#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/connection/Strategy#toString()) () |

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

From interface android.os.Parcelable

|  |  |
| --- | --- |
| abstract int | describeContents () |
| abstract void | writeToParcel ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) arg0, int arg1) |








## Fields

#### public static final Strategy **P2P\_CLUSTER**

Peer-to-peer strategy that supports an M-to-N, or cluster-shaped, connection
topology. In other words, this enables connecting amorphous clusters of devices within
radio range (~100m), where each device can both initiate outgoing connections to M
other devices and accept incoming connections from N other devices.

This is the default strategy, equivalent to calling the deprecated
`Connections`
API methods with no Strategy parameter.

In order to advertise with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH (Deprecated from Android S)
* BLUETOOTH\_ADMIN (Deprecated from Android S)
* BLUETOOTH\_ADVERTISE (Required for devices running S+)
* ACCESS\_WIFI\_STATE
* CHANGE\_WIFI\_STATE

In order to discover with this Strategy, your app must declare one of the following
permissions depending on your platform:

* ACCESS\_COARSE\_LOCATION (Required for devices running between M and S)
* ACCESS\_FINE\_LOCATION (Required for devices running between Q and S)
* BLUETOOTH\_SCAN (Required for devices running S+)
* NEARBY\_WIFI\_DEVICES (Required for devices running T+)

In order to connect with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH\_CONNECT (Required for devices running S+)

#### public static final Strategy **P2P\_POINT\_TO\_POINT**

Peer-to-peer strategy that supports a 1-to-1 connection topology. In other words,
this enables connecting to a single device within radio range (~100m). This strategy
will give the absolute highest bandwidth, but will not allow multiple connections at a
time.

In order to advertise with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH (Deprecated from Android S)
* BLUETOOTH\_ADMIN (Deprecated from Android S)
* BLUETOOTH\_ADVERTISE (Required for devices running S+)
* ACCESS\_WIFI\_STATE
* CHANGE\_WIFI\_STATE

In order to discover with this Strategy, your app must declare one of the following
permissions depending on your platform:

* ACCESS\_COARSE\_LOCATION (Required for devices running between M and S)
* ACCESS\_FINE\_LOCATION (Required for devices running between Q and S)
* BLUETOOTH\_SCAN (Required for devices running S+)
* NEARBY\_WIFI\_DEVICES (Required for devices running T+)

In order to connect with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH\_CONNECT (Required for devices running S+)

#### public static final Strategy **P2P\_STAR**

Peer-to-peer strategy that supports a 1-to-N, or star-shaped, connection topology.
In other words, this enables connecting devices within radio range (~100m) in a star
shape, where each device can, at any given time, play the role of either a hub (where
it can accept incoming connections from N other devices), or a spoke (where it can
initiate an outgoing connection to a single hub), but not both.

This strategy lends itself best to one device who advertises itself, and N devices
who discover that advertisement, though you may still advertise and discover
simultaneously if required.

In order to advertise with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH (Deprecated from Android S)
* BLUETOOTH\_ADMIN (Deprecated from Android S)
* BLUETOOTH\_ADVERTISE (Required for devices running S+)
* ACCESS\_WIFI\_STATE
* CHANGE\_WIFI\_STATE

In order to discover with this Strategy, your app must declare one of the following
permissions depending on your platform:

* ACCESS\_COARSE\_LOCATION (Required for devices running between M and S)
* ACCESS\_FINE\_LOCATION (Required for devices running between Q and S)
* BLUETOOTH\_SCAN (Required for devices running S+)
* NEARBY\_WIFI\_DEVICES (Required for devices running T+)

In order to connect with this Strategy, your app must declare the following
permissions depending on your platform:

* BLUETOOTH\_CONNECT (Required for devices running S+)






## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) object)

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()