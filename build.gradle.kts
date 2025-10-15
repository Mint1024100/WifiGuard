// Файл сборки верхнего уровня, в котором можно добавить общие для всех подпроектов/модулей параметры конфигурации.

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

// Задача очистки для всех проектов
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}

// Конфигурация для всех проектов
allprojects {
    // Общие репозитории для всех модулей
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // JitPack для библиотек GitHub
        maven { url = uri("https://jitpack.io") }
    }
}

// Конфигурация только для подпроектов
subprojects {
    // Применить общие конфигурации
    afterEvaluate {
        if (hasProperty("android")) {
            configure<com.android.build.gradle.BaseExtension> {
                compileSdkVersion(35)
                
                defaultConfig {
                    minSdk = 26
                    targetSdk = 35
                    
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    
                    // Поддержка векторных рисунков
                    vectorDrawables {
                        useSupportLibrary = true
                    }
                }
                
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                
                // Конфигурация Compose для модулей приложения
                if (hasProperty("buildFeatures")) {
                    buildFeatures {
                        compose = true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
                    }
                }
                
                // Общие параметры упаковки
                packagingOptions {
                    resources {
                        excludes += "/META-INF/{AL2.0,LGPL2.1}"
                        excludes += "/META-INF/INDEX.LIST"
                        excludes += "/META-INF/DEPENDENCIES"
                    }
                }
            }
        }
        
        // Параметры компиляции Kotlin для всех модулей
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

// Конфигурация Gradle
gradle {
    beforeProject {
        // Настроить свойства проекта
        extra.apply {
            set("APP_NAME", "WifiGuard")
            set("APP_ID", "com.wifiguard")
            set("VERSION_NAME", "1.0.0")
            set("VERSION_CODE", 1)
        }
    }
}