# CodeWave ProGuard / R8 Optimization & Obfuscation Rules

# Preserve domain models and Room database entities
-keep class com.codewave.player.core.model.** { *; }
-keep class com.codewave.player.core.database.entity.** { *; }
-keep class com.codewave.player.core.database.dao.** { *; }
-keep class com.codewave.player.core.database.CodeWaveDatabase { *; }

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Media3 ExoPlayer & MediaSession
-keep class androidx.media3.** { *; }
-keep interface androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# AndroidX Navigation & Compose
-keep class androidx.navigation.** { *; }
