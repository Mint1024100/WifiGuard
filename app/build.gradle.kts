import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
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
    namespace = findProperty("APP_PACKAGE_NAME") as String // ПРОВЕРИТЬ ПРАВИЛЬНЫЙ PACKAGE NAME
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = findProperty("APP_PACKAGE_NAME") as String // ПРОВЕРИТЬ УНИКАЛЬНОСТЬ
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()  // ДОЛЖНО БЫТЬ 34 минимум для новых приложений
        versionCode = findProperty("APP_VERSION_CODE")?.toString()?.toInt() ?: 1
        versionName = findProperty("APP_VERSION_NAME") as String
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Поля BuildConfig для URL, чувствительных к безопасности
        buildConfigField("String", "API_BASE_URL",
            "\"${project.findProperty("API_BASE_URL") ?: "https://api.example.com/api/"}\"")
        buildConfigField("String", "SECURE_API_URL",
            "\"${project.findProperty("SECURE_API_URL") ?: "https://api.example.com/secure/"}\"")
        buildConfigField("String", "ANALYTICS_API_URL",
            "\"${project.findProperty("ANALYTICS_API_URL") ?: "https://api.example.com/analytics/"}\"")
        buildConfigField("String", "API_VERSION", "\"v1\"")
        buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
    }
    
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val keystoreFile = file(keystoreProperties["storeFile"] as String)
                if (keystoreFile.exists()) {
                    storeFile = keystoreFile
                    storePassword = keystoreProperties["storePassword"] as String
                    keyAlias = keystoreProperties["keyAlias"] as String
                    keyPassword = keystoreProperties["keyPassword"] as String
                } else {
                    println("WARNING: Keystore file not found: ${keystoreFile.absolutePath}")
                }
            } else {
                println("WARNING: Keystore properties file not found: ${keystorePropertiesFile.absolutePath}")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Использовать release signing, если keystore доступен
            if (keystorePropertiesFile.exists() && file(keystoreProperties["storeFile"] as String).exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("WARNING: Release signing configuration not available, using debug signing for release build")
                signingConfig = signingConfigs.findByName("debug")
            }
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
        // For Kotlin 2.0+, the compiler extension is integrated into the Kotlin compiler
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/gradle/incremental.annotation.processors"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "false")
        arg("room.expandProjection", "true")
        arg("ksp.incremental", "false")
        arg("ksp.incremental.log", "true")
        arg("ksp.allow.all.target.source.sets", "true")
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
    
    // Material Design 3 (for XML themes)
    implementation(libs.material)
    
    // Gson
    implementation(libs.gson)
    

    
    // Тестирование
    testImplementation(libs.bundles.testing)
    
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.bundles.compose.test)
    
    debugImplementation(libs.bundles.compose.debug)
}
