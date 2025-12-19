import java.util.Properties
import java.io.FileInputStream
import org.gradle.api.GradleException

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Baseline Profile: ускорение старта/переходов без изменения функционала
    alias(libs.plugins.androidx.baselineprofile)
}

// Загрузка keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

// Форсируем версию androidx.tracing для всех конфигураций (решает конфликт 1.0.0 vs 1.3.0)
configurations.all {
    resolutionStrategy {
        force("androidx.tracing:tracing:1.3.0")
    }
}

android {
    namespace = findProperty("APP_PACKAGE_NAME") as String? ?: "com.wifiguard" // Safe fallback if property not found
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = findProperty("APP_PACKAGE_NAME") as String? ?: "com.wifiguard" // Safe fallback if property not found
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()  // Текущая версия: 36 (синхронизировано с libs.versions.toml)
        // ИСПРАВЛЕНО: Синхронизировано с gradle.properties для предотвращения проблем с обновлением
        val versionCodeValue = findProperty("APP_VERSION_CODE")?.toString()?.toInt() ?: 5
        val versionNameValue = findProperty("APP_VERSION_NAME") as String? ?: "1.0.3"
        versionCode = versionCodeValue
        versionName = versionNameValue
        println("INFO: Building app with versionCode=$versionCodeValue, versionName=$versionNameValue, applicationId=$applicationId")
        
        testInstrumentationRunner = "com.wifiguard.test.CustomTestRunner"
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

    // Проверка, запрошена ли release-сборка (вынесено за пределы блока конфигурации buildTypes)
    val isReleaseArtifactRequested = gradle.startParameter.taskNames.any { 
        it.contains("release", ignoreCase = true) 
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
            isDebuggable = true
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("debug")
        }

        /**
         * Специальный buildType для Macrobenchmark/Baseline Profile.
         * Не влияет на debug/release, используется только тестовыми модулями.
         */
        create("benchmark") {
            initWith(getByName("release"))
            matchingFallbacks += listOf("release")
            signingConfig = signingConfigs.findByName("debug")
            isDebuggable = false
        }
        
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // КРИТИЧЕСКИ ВАЖНО ДЛЯ GOOGLE PLAY:
            // release-сборка НЕ должна тихо подписываться debug-ключом, иначе обновления через Play будут невозможны.
            val keystoreFileValue = keystoreProperties["storeFile"] as? String
            val storePasswordValue = keystoreProperties["storePassword"] as? String
            val keyAliasValue = keystoreProperties["keyAlias"] as? String
            val keyPasswordValue = keystoreProperties["keyPassword"] as? String

            val releaseSigningAvailable =
                keystorePropertiesFile.exists() &&
                    !keystoreFileValue.isNullOrBlank() &&
                    file(keystoreFileValue).exists() &&
                    !storePasswordValue.isNullOrBlank() &&
                    !keyAliasValue.isNullOrBlank() &&
                    !keyPasswordValue.isNullOrBlank()

            if (releaseSigningAvailable) {
                signingConfig = signingConfigs.getByName("release")
                println("INFO: Release build будет подписан release keystore: $keystoreFileValue")
            } else if (isReleaseArtifactRequested) {
                throw GradleException(
                    "ОШИБКА: Release keystore не настроен. " +
                        "Для публикации в Google Play необходимо создать keystore.properties (см. keystore.properties.template) " +
                        "и указать существующий storeFile/пароли. " +
                        "Текущий storeFile=${keystoreFileValue ?: "<не задан>"}"
                )
            } else {
                // Не блокируем debug/unitTest задачи, где release-вариант не собирается.
                println("INFO: Release keystore не настроен (release артефакт не запрошен).")
            }
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
    
    // Для Kotlin 2.0+ с плагином kotlin-compose блок composeOptions не требуется
    // Компилятор Compose интегрирован в Kotlin компилятор
    
    testOptions {
        unitTests {
            isReturnDefaultValues = true
            isIncludeAndroidResources = true
        }
    }
    
    sourceSets {
        getByName("androidTest") {
            assets {
                srcDir("$projectDir/schemas")
            }
        }
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

// Обновленный синтаксис для jvmTarget (вместо deprecated kotlinOptions)
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
    ksp(libs.androidx.hilt.work.compiler)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)
    
    // Security - EncryptedSharedPreferences & Android Keystore
    implementation(libs.androidx.security.crypto)

    // Baseline Profile installer: устанавливает профили в рантайме (ускоряет запуск)
    implementation(libs.androidx.profileinstaller)

    // Google Play In-App Updates (опционально, безопасно деградирует вне Play Store)
    implementation(libs.play.app.update)
    implementation(libs.play.app.update.ktx)

    // Подключаем baseline profile, сгенерированный модулем :baselineprofile
    baselineProfile(project(":baselineprofile"))

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.hilt.android.testing)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.robolectric)
    kspTest(libs.hilt.compiler)
    
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
    kspAndroidTest(libs.androidx.hilt.work.compiler)
    
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Room Testing
    androidTestImplementation(libs.room.testing)

    // WorkManager Testing
    androidTestImplementation(libs.androidx.work.testing)

    // Tracing for AndroidX Test (решает NoSuchMethodError forceEnableAppTracing)
    // Явно форсируем версию 1.3.0 для всех транзитивных зависимостей
    androidTestImplementation("androidx.tracing:tracing:1.3.0") {
        because("AndroidX Test требует forceEnableAppTracing из 1.3.0+")
    }
}
