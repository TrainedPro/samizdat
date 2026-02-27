# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# --- Room entities, DAOs, and converters ---
-keep class com.fyp.resilientp2p.data.** { *; }

# --- Play Services (Nearby Connections uses reflection) ---
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# --- Kotlin metadata (for Room and other annotation processors) ---
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }

# --- Keep source file & line numbers for crash reports ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Coil (image loading) ---
-dontwarn coil.**
-keep class coil.** { *; }

# --- WorkManager ---
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}