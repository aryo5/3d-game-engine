# ProGuard rules for Apex3D Engine
# Optimized for R8 code shrinking and obfuscation

# General Android Rules
-dontwarn java.lang.invoke.*
-dontwarn sun.misc.Unsafe
-dontwarn com.google.common.**
-dontwarn javax.annotation.**
-dontwarn com.sun.org.apache.**
-dontwarn org.w3c.dom.ls.**

# Keep Android Framework Classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.app.BroadcastReceiver
-keep public class * extends android.app.Dialog
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.appcompat.app.AppCompatActivity

# Keep Enum classes
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep View constructors for inflation
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Resource IDs
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Kotlin
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Compose
-keep class androidx.compose.** { *; }
-keep interface androidx.compose.** { *; }
-dontwarn androidx.compose.**

# Firebase
-keep class com.firebase.** { *; }
-keep class com.google.firebase.** { *; }
-keep interface com.google.firebase.** { *; }
-dontwarn com.firebase.**
-dontwarn com.google.firebase.**
-keepattributes SourceFile,LineNumberTable
-keep public class * extends com.google.firebase.auth.internal.zzb

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# Retrofit
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-keepattributes Signature
-keepattributes Exceptions

# Moshi JSON
-dontwarn com.squareup.moshi.**
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.FromJson <methods>;
    @com.squareup.moshi.ToJson <methods>;
}

# Room Database
-keep class androidx.room.** { *; }
-keep interface androidx.room.** { *; }
-dontwarn androidx.room.**
-keepclassmembers class * {
    @androidx.room.* <fields>;
}

# Project-specific classes - Keep your main game classes
-keep class com.example.engine3d.** { *; }
-keep interface com.example.engine3d.** { *; }
-keep class com.aistudio.apex3d.** { *; }
-keep interface com.aistudio.apex3d.** { *; }

# Keep classes with @Keep annotation
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * {*;}
-keepclassmembers class * {
    @androidx.annotation.Keep <fields>;
    @androidx.annotation.Keep <methods>;
}

# Preserve line numbers for crash reports
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Main Activity Entry Point
-keep class com.example.engine3d.MainActivity { *; }
-keep class com.aistudio.apex3d.MainActivity { *; }

# Remove logging in release builds (optional)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Optimization settings
-optimizationpasses 5
-dontobfuscate
-allowaccessmodification
-mergeinterfacesaggressively
-repackageclasses ''

# Verbose output for debugging
-verbose
