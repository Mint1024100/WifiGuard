# WifiGuard ProGuard Rules

# ===== ОСНОВНЫЕ ПРАВИЛА =====

# Keep application class
-keep class com.wifiguard.WifiGuardApplication { *; }

# Keep all classes with @Inject constructor (Hilt)
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}

# ===== KOTLIN =====

# Kotlin Metadata
-keep class kotlin.Metadata { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# Kotlin Serialization (если используется)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ===== JETPACK COMPOSE =====

# Compose Runtime - только необходимые классы
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class androidx.compose.runtime.** { *; }
-dontwarn androidx.compose.runtime.**

# Compose UI - только базовые классы
-keep class androidx.compose.ui.platform.** { *; }
-keep class androidx.compose.ui.graphics.** { *; }
-keep class androidx.compose.ui.text.** { *; }
-dontwarn androidx.compose.ui.**

# Material3 - только используемые компоненты
-keep class androidx.compose.material3.MaterialTheme { *; }
-keep class androidx.compose.material3.ColorScheme { *; }
-keep class androidx.compose.material3.Typography { *; }
-keep class androidx.compose.material3.Shapes { *; }
-dontwarn androidx.compose.material3.**

# ===== HILT / DAGGER =====

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Dagger
-dontwarn com.google.errorprone.annotations.**
-keep class dagger.** { *; }
-keep class * extends dagger.internal.Binding
-keep class * extends dagger.internal.ModuleAdapter
-keep class * extends dagger.internal.StaticInjection

# Generated Hilt classes
-keep class **_HiltModules { *; }
-keep class **_HiltComponents { *; }
-keep class **_Impl { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# ===== ROOM DATABASE =====

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *

-dontwarn androidx.room.paging.**

# Keep database classes
-keep class com.wifiguard.core.data.local.** { *; }

# ===== DATA CLASSES =====

# Keep data classes used by Room
-keep class com.wifiguard.core.data.local.entity.** { *; }

# Keep domain models
-keep class com.wifiguard.core.domain.model.** { *; }

# Keep data transfer objects
-keep class com.wifiguard.core.data.*.dto.** { *; }

# ===== ANDROID =====

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom views - только классы в пакете приложения
-keep public class com.wifiguard.** extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
    public void set*(...);
}

# Keep Android Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ===== WORKMANAGER =====

# WorkManager
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.InputMerger
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**

# Keep Worker classes
-keep class com.wifiguard.core.background.** { *; }

# ===== DATASTORE =====

# DataStore
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.** { *; }

# ===== SECURITY - КРИТИЧЕСКИЕ ПРАВИЛА =====

# Keep security classes (важно для шифрования)
-keep class com.wifiguard.core.security.** { *; }

# SecureKeyManager - КРИТИЧЕСКИ ВАЖНО для работы с Keystore
-keep class com.wifiguard.core.security.SecureKeyManager { *; }
-keep class com.wifiguard.core.security.SecureKeyException { *; }

# InputValidator - валидация входных данных
-keep class com.wifiguard.core.security.InputValidator { *; }
-keep class com.wifiguard.core.security.InputValidator$ValidationResult { *; }
-keep class com.wifiguard.core.security.InputValidator$ValidationResult$Valid { *; }
-keep class com.wifiguard.core.security.InputValidator$ValidationResult$Invalid { *; }

# Android KeyStore
-keep class javax.crypto.** { *; }
-keep class java.security.** { *; }
-keep class android.security.keystore.** { *; }
-dontwarn javax.crypto.**

# EncryptedSharedPreferences
-keep class androidx.security.crypto.** { *; }
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Обфускация имён классов безопасности (дополнительная защита)
-repackageclasses 'com.wifiguard.internal'
-allowaccessmodification

# ===== GSON (если используется) =====

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ===== RETROFIT (если используется) =====

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ===== NAVIGATION =====

# Navigation Component
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class * extends androidx.navigation.Navigator

# ===== VIEWMODEL =====

# ViewModel
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# ===== LOGGING =====

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# ===== WARNINGS =====

-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ===== OPTIMIZATIONS =====

# Optimization is turned on by default
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# Preserve line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Rename source file attribute to "SourceFile"
-renamesourcefileattribute SourceFile

# ===== CUSTOM APPLICATION RULES =====

# Keep WifiGuard specific classes that must not be obfuscated

# Security analyzer
-keep class com.wifiguard.core.security.SecurityAnalyzer { *; }
-keep class com.wifiguard.core.security.ThreatDetector { *; }

# Encryption
-keep class com.wifiguard.core.security.AesEncryption { *; }
-keep class com.wifiguard.core.security.EncryptionManager { *; }

# Wi-Fi Scanner (критично!)
-keep class com.wifiguard.core.data.wifi.WifiScanner { *; }
-keep class com.wifiguard.core.data.wifi.WifiScannerImpl { *; }
-keep class com.wifiguard.core.data.wifi.WifiScanResult { *; }
-keep class com.wifiguard.core.data.wifi.SecurityType { *; }

# Notification
-keep class com.wifiguard.feature.notifications.** { *; }

# ===== TESTING =====
-dontwarn org.junit.**
-dontwarn org.mockito.**
-dontwarn org.robolectric.**