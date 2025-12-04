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
    namespace = findProperty("APP_PACKAGE_NAME") as String? ?: "com.wifiguard" // Safe fallback if property not found
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = findProperty("APP_PACKAGE_NAME") as String? ?: "com.wifiguard" // Safe fallback if property not found
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()  // ДОЛЖНО БЫТЬ 34 минимум для новых приложений
        versionCode = findProperty("APP_VERSION_CODE")?.toString()?.toInt() ?: 1
        versionName = findProperty("APP_VERSION_NAME") as String? ?: "1.0.1" // Safe fallback if property not found
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // BuildConfig fields
        // NOTE: This app is designed to work OFFLINE - no external API connections
        // The following flags control optional future features
        buildConfigField("boolean", "ENABLE_CRASHLYTICS", "false")
        buildConfigField("boolean", "ENABLE_ANALYTICS", "false")
        
        // App configuration
        buildConfigField("int", "MIN_SDK_FOR_WIFI_6", "30")
        buildConfigField("long", "SCAN_THROTTLE_MS", "2000L")
    }
    
    signingConfigs {
        create("release") {
            if (keystorePropertiesFile.exists()) {
                val keystoreFileValue = keystoreProperties["storeFile"] as? String
                if (keystoreFileValue != null) {
                    val keystoreFile = file(keystoreFileValue)
                    if (keystoreFile.exists()) {
                        storeFile = keystoreFile
                        storePassword = keystoreProperties["storePassword"] as? String ?: ""
                        keyAlias = keystoreProperties["keyAlias"] as? String ?: ""
                        keyPassword = keystoreProperties["keyPassword"] as? String ?: ""
                    } else {
                        println("WARNING: Keystore file not found: ${keystoreFile.absolutePath}")
                    }
                } else {
                    println("WARNING: Store file path not found in keystore properties")
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
            val keystoreFileValue = keystoreProperties["storeFile"] as? String
            if (keystorePropertiesFile.exists() && keystoreFileValue != null && file(keystoreFileValue).exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else {
                println("WARNING: Release signing configuration not available, using debug signing for release build")
                signingConfig = signingConfigs.findByName("debug")
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.material)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.gson)
    implementation(libs.androidx.compose.material.icons.extended)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.androidx.room.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // WorkManager Testing
    androidTestImplementation(libs.androidx.work.testing)
}
