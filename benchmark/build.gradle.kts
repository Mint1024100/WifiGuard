plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wifiguard.benchmark"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // Для Macrobenchmark достаточно стандартного runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Целевой модуль приложения
    targetProjectPath = ":app"

    // Macrobenchmark запускается как отдельный test APK и измеряет целевое приложение.
    // Важно: создаём buildType `benchmark` в тестовом модуле, чтобы он совпадал с buildType приложения.
    buildTypes {
        create("benchmark") {
            initWith(getByName("debug"))
        }
    }

    // Требуется для Macrobenchmark на AGP 8.x
    experimentalProperties["android.experimental.self-instrumenting"] = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}



