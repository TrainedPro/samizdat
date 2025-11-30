<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/connection/AppIdentifier -->

# AppIdentifier

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* The AppIdentifier class is deprecated and no longer used.
* This class was used to identify an application, typically by its Android package name.
* Google applications could use this identifier to prompt the user to install the application.
* The class implements the Parcelable interface.
* The main methods were getIdentifier() to retrieve the package name and writeToParcel() for serialization.



public final class
**AppIdentifier**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

**This class is deprecated.**
  
This class is no longer used.

An identifier for an application; the value of the identifier should be the package name
for an Android application to be installed or launched to discover and communicate with the
advertised service (e.g. com.example.myapp). Google applications may use this data to prompt
the user to install the application.

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > | [CREATOR](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier#CREATOR) |  |

### Public Constructor Summary

|  |  |
| --- | --- |
|  | [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier#AppIdentifier(java.lang.String)) ( [String](//developer.android.com/reference/java/lang/String.html) identifier) |

### Public Method Summary

|  |  |
| --- | --- |
| [String](//developer.android.com/reference/java/lang/String.html) | [getIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier#getIdentifier()) () Retrieves the identifier string for this application (e.g. |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags) |

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

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [AppIdentifier](/android/reference/com/google/android/gms/nearby/connection/AppIdentifier) > **CREATOR**



## Public Constructors

#### public **AppIdentifier** ( [String](//developer.android.com/reference/java/lang/String.html) identifier)

##### Parameters

|  |  |
| --- | --- |
| identifier | The Android package name of an Android application to be installed or launched to discover and communicate with the advertised service (e.g. com.example.myapp). |





## Public Methods

#### public [String](//developer.android.com/reference/java/lang/String.html) **getIdentifier** ()

Retrieves the identifier string for this application (e.g. com.example.mygame).

##### Returns

* The identifier string.

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) out, int flags)