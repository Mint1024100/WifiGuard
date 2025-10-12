plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.wifiguard"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.wifiguard"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // BuildConfig fields for security-sensitive URLs
        buildConfigField("String", "API_BASE_URL", "\"https://api.wifiguard.com/\"")
        buildConfigField("String", "SECURE_API_URL", "\"https://secure.wifiguard.com/\"")
        buildConfigField("String", "ANALYTICS_API_URL", "\"https://analytics.wifiguard.com/\"")
        buildConfigField("String", "API_VERSION", "\"v1\"")
        buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
    }
    
    signingConfigs {
        // Для production используйте реальный keystore
        // Создайте keystore командой:
        // keytool -genkey -v -keystore wifiguard.keystore -alias wifiguard -keyalg RSA -keysize 2048 -validity 10000
        create("release") {
            // В production эти значения должны браться из gradle.properties или environment variables
            // storeFile = file(System.getenv("KEYSTORE_FILE") ?: "release.keystore")
            // storePassword = System.getenv("KEYSTORE_PASSWORD")
            // keyAlias = System.getenv("KEY_ALIAS")
            // keyPassword = System.getenv("KEY_PASSWORD")
            
            // Временная конфигурация для тестирования (НЕ ИСПОЛЬЗУЙТЕ В PRODUCTION!)
            // TODO: Настроить реальный keystore для production
        }
    }

    buildTypes {
        release {
            // signingConfig = signingConfigs.getByName("release") // Раскомментируйте после настройки keystore
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Release-specific BuildConfig fields
            buildConfigField("String", "API_BASE_URL", "\"https://api.wifiguard.com/\"")
            buildConfigField("String", "SECURE_API_URL", "\"https://secure.wifiguard.com/\"")
            buildConfigField("String", "ANALYTICS_API_URL", "\"https://analytics.wifiguard.com/\"")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            
            // Debug-specific BuildConfig fields
            buildConfigField("String", "API_BASE_URL", "\"https://debug-api.wifiguard.com/\"")
            buildConfigField("String", "SECURE_API_URL", "\"https://debug-secure.wifiguard.com/\"")
            buildConfigField("String", "ANALYTICS_API_URL", "\"https://debug-analytics.wifiguard.com/\"")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
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
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose BOM for version alignment
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    
    // Navigation
    implementation(libs.androidx.navigation.compose)
    
    // ViewModel & LiveData
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    
    // Hilt DI
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.compiler)
    
    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    
    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    
    // Network & Serialization
    implementation(libs.kotlinx.serialization.json)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // Security
    implementation(libs.androidx.security.crypto)
    
    // Testing
    testImplementation(libs.bundles.testing)
    
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.compose.test)
    
    debugImplementation(libs.bundles.compose.debug)
}
