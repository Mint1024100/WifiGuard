import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

// Загрузка keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.wifiguard.app"  // ПРОВЕРИТЬ ПРАВИЛЬНЫЙ PACKAGE NAME
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wifiguard.app"  // ПРОВЕРИТЬ УНИКАЛЬНОСТЬ
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()  // ДОЛЖНО БЫТЬ 34 минимум для новых приложений
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Поля BuildConfig для URL, чувствительных к безопасности
        buildConfigField("String", "API_BASE_URL", "\"http://localhost:8080/api/\"")
        buildConfigField("String", "SECURE_API_URL", "\"http://localhost:8080/secure/\"")
        buildConfigField("String", "ANALYTICS_API_URL", "\"http://localhost:8080/analytics/\"")
        buildConfigField("String", "API_VERSION", "\"v1\"")
        buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
    }
    
    signingConfigs {
        create("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        
        create("release") {
            if (keystorePropertiesFile.exists()) {
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // КРИТИЧНО: Использовать production signing
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
        arg("room.expandProjection", "true")
    }
}

dependencies {
    // Основные зависимости Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM для согласования версий
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    
    // Навигация
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel и LiveData
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Hilt DI
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    
    // База данных Room
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    
    // Сеть и сериализация
    implementation(libs.kotlinx.serialization.json)
    
    // Разрешения
    implementation(libs.accompanist.permissions)
    
    // Безопасность
    implementation(libs.androidx.security.crypto)
    
    // Тестирование
    testImplementation(libs.bundles.testing)
    
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.compose.test)
    
    debugImplementation(libs.bundles.compose.debug)
}
