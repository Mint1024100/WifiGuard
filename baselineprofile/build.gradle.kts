plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.wifiguard.baselineprofile"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        // Baseline Profile требует API 28+ (Android 9.0+)
        // Основной app модуль может иметь minSdk 26, но baselineprofile модуль требует 28+
        minSdk = 28
        targetSdk = libs.versions.targetSdk.get().toInt()

        // Для Macrobenchmark/BaselineProfile достаточно стандартного runner
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Целевой модуль приложения
    targetProjectPath = ":app"
}

// Форсируем версию androidx.tracing для всех конфигураций (решает конфликт 1.0.0 vs 1.3.0)
// Это необходимо для стабильной работы Macrobenchmark/BaselineProfile
configurations.all {
    resolutionStrategy {
        force("androidx.tracing:tracing:1.3.0")
    }
}

baselineProfile {
    // Генерация через подключённое устройство.
    //
    // ВАЖНО: Baseline Profile collection требует API 33+ (или root + adb root на API 28+).
    // Если оставить useConnectedDevices=true по умолчанию, обычные задачи сборки могут неожиданно
    // пытаться запускать connectedAndroidTest на реальном устройстве (и падать на API < 33).
    //
    // Поэтому по умолчанию отключаем и включаем только явным флагом:
    //   ./gradlew :baselineprofile:connectedNonMinifiedReleaseAndroidTest -Pbaselineprofile.connected=true
    useConnectedDevices = providers.gradleProperty("baselineprofile.connected")
        .map { it.equals("true", ignoreCase = true) }
        .getOrElse(false)
}

// Дополнительная защита: если флаг не задан/false, baselineprofile НЕ должен пытаться
// собирать профиль в рамках обычных сборочных задач (например, ./gradlew assemble).
// Это НЕ влияет на сборку APK/AAB и unit tests — только на задачи генерации/сбора baseline profile.
val baselineProfileEnabled = providers.gradleProperty("baselineprofile.connected")
    .map { it.equals("true", ignoreCase = true) }
    .getOrElse(false)

if (!baselineProfileEnabled) {
    // Отключаем запуск connected-тестов и tasks сборки baseline profile.
    tasks.matching { it.name.startsWith("connected", ignoreCase = true) }.configureEach {
        enabled = false
    }
    tasks.matching { it.name.contains("BaselineProfile", ignoreCase = true) }.configureEach {
        enabled = false
    }
}

dependencies {
    implementation(libs.androidx.benchmark.macro.junit4)
    implementation(libs.androidx.test.ext.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.test.uiautomator)
}












