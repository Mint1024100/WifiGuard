plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.wifiguard.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()

        // Для Macrobenchmark/BaselineProfile достаточно стандартного runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Целевой модуль приложения
    targetProjectPath = ":app"
}

baselineProfile {
    // Генерация через подключённое устройство
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}


