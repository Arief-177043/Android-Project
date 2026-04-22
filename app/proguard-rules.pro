# GrowwX ProGuard Rules

# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson (used by Retrofit converter)
-keep class com.google.gson.** { *; }
-keep class com.growwx.data.api.** { *; }
-keep class com.growwx.data.model.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# DataStore
-keep class androidx.datastore.** { *; }

# Keep data classes
-keepclassmembers class com.growwx.data.** {
    public <init>(...);
    public * get*();
    public void set*(...);
}
