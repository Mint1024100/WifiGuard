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
    delete(rootProject.layout.buildDirectory.get())
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

