<!-- Source: https://developers.google.com/android/reference/com/google/android/gms/nearby/messages/MessageFilter -->

# MessageFilter

![Spark icon](/_static/images/icons/spark.svg)

## AI-generated Key Takeaways

outlined\_flag

* MessageFilter is used to specify the set of messages to be received, where the filter must match the message, and it is the logical OR of multiple single filters.
* The MessageFilter class includes a nested Builder class to create MessageFilter instances.
* There is a static final MessageFilter field called INCLUDE\_ALL\_MY\_TYPES that provides a convenient filter to return all message types published by the application's project.
* The MessageFilter class implements the Parcelable interface, allowing it to be written to and read from a Parcel.



public class
**MessageFilter**
extends
[Object](//developer.android.com/reference/java/lang/Object.html)
  
implements
[Parcelable](//developer.android.com/reference/android/os/Parcelable.html)

Used to specify the set of messages to be received. In order to receive a message, the
`MessageFilter`
must match the message. A
`MessageFilter`
is the logical OR of multiple single filters.

### Nested Class Summary

|  |  |  |  |
| --- | --- | --- | --- |
| class | [MessageFilter.Builder](/android/reference/com/google/android/gms/nearby/messages/MessageFilter.Builder) | | Builder for `MessageFilter` . |

### Inherited Constant Summary

From interface android.os.Parcelable

|  |  |  |
| --- | --- | --- |
| int | CONTENTS\_FILE\_DESCRIPTOR |  |
| int | PARCELABLE\_WRITE\_RETURN\_VALUE |  |

### Field Summary

|  |  |  |
| --- | --- | --- |
| public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) > | [CREATOR](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#CREATOR) |  |
| public static final [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) | [INCLUDE\_ALL\_MY\_TYPES](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#INCLUDE_ALL_MY_TYPES) | A convenient filter that returns all types of messages published by this application's project. |

### Public Method Summary

|  |  |
| --- | --- |
| boolean | [equals](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#equals(java.lang.Object)) ( [Object](//developer.android.com/reference/java/lang/Object.html) o) |
| int | [hashCode](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#hashCode()) () |
| [String](//developer.android.com/reference/java/lang/String.html) | [toString](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#toString()) () |
| void | [writeToParcel](/android/reference/com/google/android/gms/nearby/messages/MessageFilter#writeToParcel(android.os.Parcel,%20int)) ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags) |

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

#### public static final [Creator](//developer.android.com/reference/android/os/Parcelable.Creator.html) < [MessageFilter](/android/reference/com/google/android/gms/nearby/messages/MessageFilter) > **CREATOR**

#### public static final MessageFilter **INCLUDE\_ALL\_MY\_TYPES**

A convenient filter that returns all types of messages published by this
application's project.

##### See Also

* `MessageFilter.Builder.includeAllMyTypes()`






## Public Methods

#### public boolean **equals** ( [Object](//developer.android.com/reference/java/lang/Object.html) o)

#### public int **hashCode** ()

#### public [String](//developer.android.com/reference/java/lang/String.html) **toString** ()

#### public void **writeToParcel** ( [Parcel](//developer.android.com/reference/android/os/Parcel.html) dest, int flags)