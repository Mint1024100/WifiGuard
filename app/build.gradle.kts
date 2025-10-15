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
            // Для production используйте реальный keystore
            // Создайте keystore командой:
            // keytool -genkey -v -keystore wifiguard.keystore -alias wifiguard -keyalg RSA -keysize 2048 -validity 10000
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            val keystoreProperties = java.util.Properties()
            if (keystorePropertiesFile.exists()) {
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                
                // Validate required properties exist
                val storeFileValue = keystoreProperties["storeFile"] as String?
                val storePasswordValue = keystoreProperties["storePassword"] as String?
                val keyAliasValue = keystoreProperties["keyAlias"] as String?
                val keyPasswordValue = keystoreProperties["keyPassword"] as String?
                
                if (storeFileValue != null && storePasswordValue != null && 
                    keyAliasValue != null && keyPasswordValue != null) {
                    val actualStoreFile = file(storeFileValue)
                    if (actualStoreFile.exists()) {
                        storeFile = actualStoreFile
                        storePassword = storePasswordValue
                        keyAlias = keyAliasValue
                        keyPassword = keyPasswordValue
                    } else {
                        throw GradleException("Release build failed: Store file does not exist at path: $storeFileValue")
                    }
                } else {
                    throw GradleException("Release build failed: Missing required keystore properties. Required: storeFile, storePassword, keyAlias, keyPassword")
                }
            } else {
                throw GradleException("Release build failed: keystore.properties file not found. Create it with storeFile, storePassword, keyAlias, and keyPassword properties.")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            
            // Поля BuildConfig для релизной сборки
            buildConfigField("String", "API_BASE_URL", "\"https://api.wifiguard.com/\"")
            buildConfigField("String", "SECURE_API_URL", "\"https://secure.wifiguard.com/\"")
            buildConfigField("String", "ANALYTICS_API_URL", "\"https://analytics.wifiguard.com/\"")
            buildConfigField("boolean", "ENABLE_CRASHLYTICS", "true")
            buildConfigField("boolean", "ENABLE_ANALYTICS", "true")
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
            
            signingConfig = signingConfigs.getByName("debug")
            
            // Поля BuildConfig для отладочной сборки
            buildConfigField("String", "API_BASE_URL", "\"http://localhost:8080/api/\"")
            buildConfigField("String", "SECURE_API_URL", "\"http://localhost:8080/secure/\"")
            buildConfigField("String", "ANALYTICS_API_URL", "\"http://localhost:8080/analytics/\"")
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
