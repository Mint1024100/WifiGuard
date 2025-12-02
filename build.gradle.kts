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
        // Настройка Android модулей
        if (project.extensions.findByName("android") != null) {
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
        
        // Настройка Kotlin для всех модулей
        // Использование jvmToolchain - самый надежный способ исправить ошибки совместимости Java
        extensions.findByType<org.jetbrains.kotlin.gradle.dsl.KotlinBaseExtension>()?.apply {
            jvmToolchain(17)
        }
        
        // Дополнительные параметры компиляции
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                freeCompilerArgs.addAll(
                    listOf(
                        "-opt-in=kotlin.RequiresOptIn",
                        "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                        "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                        "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                        "-opt-in=androidx.compose.animation.ExperimentalAnimationApi"
                    )
                )
            }
        }
    }
}
