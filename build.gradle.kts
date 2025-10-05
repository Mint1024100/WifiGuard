// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    extra.apply {
        set("compose_version", "1.5.8")
        set("compose_compiler_version", "1.5.8")
        set("kotlin_version", "1.9.22")
        set("hilt_version", "2.50")
        set("room_version", "2.6.1")
        set("coroutines_version", "1.7.3")
        set("lifecycle_version", "2.7.0")
        set("navigation_version", "2.7.6")
        set("okhttp_version", "4.12.0")
        set("retrofit_version", "2.9.0")
        set("gson_version", "2.10.1")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
}

// Clean task for all projects
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Configuration for all projects
allprojects {
    // Common repositories for all modules
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // JitPack for GitHub libraries
        maven { url = uri("https://jitpack.io") }
    }
}

// Configuration for subprojects only
subprojects {
    // Apply common configurations
    afterEvaluate {
        if (hasProperty("android")) {
            configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(35)
                
                defaultConfig {
                    minSdk = 26
                    targetSdk = 35
                    
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    
                    // Vector drawables support
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                
                // Compose configuration for app modules
                if (hasProperty("buildFeatures")) {
                    buildFeatures {
                        compose = true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion = rootProject.extra["compose_compiler_version"] as String
                    }
                }
                
                // Common packaging options
                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        excludes += "/META-INF/INDEX.LIST"
                        excludes += "/META-INF/DEPENDENCIES"
                    }
                }
            }
        }
        
        // Kotlin compile options for all modules
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions {
                jvmTarget = "17"
                freeCompilerArgs += listOf(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                    "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
                )
            }
        }
    }
}

// Gradle configuration
gradle {
    beforeProject {
        // Configure project properties
        extra.apply {
            set("APP_NAME", "WifiGuard")
            set("APP_ID", "com.wifiguard")
            set("VERSION_NAME", "1.0.0")
            set("VERSION_CODE", 1)
        }
    }
}