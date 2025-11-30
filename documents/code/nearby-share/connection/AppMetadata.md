<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/AppMetadata -->

# AppMetadata

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The AppMetadata class is deprecated and no longer used.
* This class previously contained AppIdentifier objects for applications that could interact with advertised services.
* Google applications could use this metadata to suggest installing the relevant application.



public final class
**AppMetadata**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

**This class is deprecated.**
  
This class is no longer used.

Metadata about an application. Contains one or more
`AppIdentifier`
objects indicating identifiers that can be used to install or launch application(s) that can
discover and communicate with the advertised service. Google applications may use this data
to prompt the user to install the application.

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/AppMetadata#CREATOR) |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata#AppMetadata(java.util.List<com.google.android.gms.nearby.connection.AppIdentifier>)) ( [List](//developer.android.com/reference/java/util/List.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > appIdentifiers) |

### Public Method Summary

|  |  |
| --- | --- |
| [List](//developer.android.com/reference/java/util/List.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > | [getAppIdentifiers](/android/reference/com/google/android/gms/nearby/connection/AppMetadata#getAppIdentifiers()) () Returns a list of app identifiers that can discover and communicate with the advertised service. |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/AppMetadata#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags) |

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

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AppMetadata](/android/reference/com/google/android/gms/nearby/connection/AppMetadata) > **CREATOR**



## Public Constructors

#### public **AppMetadata** ( [List](//developer.android.com/reference/java/util/List.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > appIdentifiers)

##### Parameters

|  |  |
| --- | --- |
| appIdentifiers | One or more identifiers for application(s) that can discover and communicate with the advertised service. |





## Public Methods

#### public [List](//developer.android.com/reference/java/util/List.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > **getAppIdentifiers** ()

Returns a list of app identifiers that can discover and communicate with the
advertised service.

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags)