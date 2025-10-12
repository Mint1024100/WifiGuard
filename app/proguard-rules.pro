# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ===========================================
# WifiGuard Security Rules
# ===========================================

# Keep all classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }
-keep @kotlinx.parcelize.Parcelize class * { *; }

# ===========================================
# Hilt Dependency Injection
# ===========================================

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep @Inject annotated classes
-keep @javax.inject.Inject class * { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# Keep Hilt modules
-keep @dagger.Module class * { *; }
-keep @dagger.hilt.InstallIn class * { *; }

# ===========================================
# Room Database
# ===========================================

# Keep Room entities
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.Database class * { *; }

# Keep Room generated classes
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.RoomDatabase$Callback { *; }

# ===========================================
# Jetpack Compose
# ===========================================

# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class kotlin.Metadata { *; }

# Keep Compose functions
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ===========================================
# Security Classes - Obfuscate but keep functionality
# ===========================================

# Keep security classes but obfuscate internal methods
-keep class com.wifiguard.core.security.AesEncryption { 
    public <methods>;
    public <fields>;
}

-keep class com.wifiguard.core.security.SecurityManager { 
    public <methods>;
    public <fields>;
}

# Keep security enums
-keep enum com.wifiguard.core.security.SecurityThreat { *; }
-keep enum com.wifiguard.core.security.RiskLevel { *; }

# Keep EncryptedData class
-keep class com.wifiguard.core.security.EncryptedData { *; }

# ===========================================
# Domain Models - Keep for serialization
# ===========================================

# Keep WifiInfo and related models
-keep class com.wifiguard.feature.scanner.domain.model.WifiInfo { *; }
-keep class com.wifiguard.feature.scanner.domain.model.EncryptionType { *; }
-keep class com.wifiguard.feature.scanner.domain.model.SecurityLevel { *; }
-keep class com.wifiguard.feature.scanner.domain.model.SignalQuality { *; }

# ===========================================
# Constants - Obfuscate sensitive values
# ===========================================

# Obfuscate Constants class but keep structure
-keep class com.wifiguard.core.common.Constants {
    public static final java.lang.String DATABASE_NAME;
    public static final java.lang.String PREFERENCES_NAME;
    public static final long SCAN_INTERVAL_MS;
    public static final long MIN_SCAN_INTERVAL_MS;
    public static final java.lang.String LOG_TAG;
    public static final java.lang.String NOTIFICATION_CHANNEL_ID;
    public static final java.lang.String NOTIFICATION_CHANNEL_NAME;
    public static final java.lang.String WIFI_MONITOR_WORK_NAME;
    public static final int WEAK_SIGNAL_THRESHOLD;
    public static final int SUSPICIOUS_NETWORK_COUNT;
}

# Obfuscate security-related constants
-keep class com.wifiguard.core.common.Constants {
    public static final java.lang.String AES_KEY_ALIAS;
    public static final java.lang.String HMAC_KEY_ALIAS;
    public static final java.lang.String KEYSTORE_PROVIDER;
}

# ===========================================
# WorkManager
# ===========================================

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.Worker { *; }
-keep class * extends androidx.work.CoroutineWorker { *; }

# ===========================================
# DataStore
# ===========================================

# Keep DataStore classes
-keep class androidx.datastore.** { *; }

# ===========================================
# Navigation
# ===========================================

# Keep Navigation classes
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.NavType { *; }

# ===========================================
# ViewModels
# ===========================================

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }

# ===========================================
# Serialization
# ===========================================

# Keep serialization classes
-keep class kotlinx.serialization.** { *; }
-keep @kotlinx.serialization.Serializable class * { *; }

# ===========================================
# Android System Classes
# ===========================================

# Keep Android system classes
-keep class android.** { *; }
-keep class androidx.** { *; }

# Keep Parcelable classes
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ===========================================
# Logging - Remove in release
# ===========================================

# Remove logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# ===========================================
# Optimization Rules
# ===========================================

# Remove unused code
-dontwarn **
-ignorewarnings

# Optimize code
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep generic signatures
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ===========================================
# Debug Information
# ===========================================

# Keep source file names for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

