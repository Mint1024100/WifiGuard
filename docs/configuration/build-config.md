# Конфигурация WifiGuard

## Build Configuration

### Gradle настройки

Проект использует Gradle с Kotlin DSL. Основные файлы конфигурации:

- `build.gradle.kts` (корневой) — глобальные настройки
- `app/build.gradle.kts` — настройки приложения
- `settings.gradle.kts` — настройки модулей
- `gradle/libs.versions.toml` — управление версиями зависимостей

### SDK версии

```kotlin
minSdk = 26  // Android 8.0
targetSdk = 35  // Android 14
compileSdk = 35
```

### JVM и Kotlin настройки

- **Java Version**: 17 (через `jvmToolchain(17)`)
- **Kotlin Version**: 2.1.0
- **Kotlin Compiler Args**:
  - `-opt-in=kotlin.RequiresOptIn`
  - `-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi`
  - `-opt-in=androidx.compose.material3.ExperimentalMaterial3Api`
  - и другие

## Environment Variables и BuildConfig

### BuildConfig поля

При сборке генерируются следующие поля в BuildConfig:

```kotlin
// API URLs (по умолчанию для безопасности)
API_BASE_URL = "https://api.example.com/api/"
SECURE_API_URL = "https://api.example.com/secure/"
ANALYTICS_API_URL = "https://api.example.com/analytics/"
API_VERSION = "v1"

// Флаги функций
ENABLE_CRASHLYTICS = false
ENABLE_ANALYTICS = false
```

### Свойства проекта

Возможные свойства командной строки:

- `APP_PACKAGE_NAME` — имя пакета приложения (по умолчанию: "com.wifiguard")
- `APP_VERSION_CODE` — код версии (по умолчанию: 1)
- `APP_VERSION_NAME` — имя версии (по умолчанию: "1.0.1")
- `API_BASE_URL`, `SECURE_API_URL`, `ANALYTICS_API_URL` — API endpoints

### Использование

Установка через Gradle:

```bash
./gradlew assembleRelease -PAPP_PACKAGE_NAME=com.mycompany.wifiguard -PAPP_VERSION_NAME=2.0.0
```

## Keystore Configuration

### Файл конфигурации

Создайте файл `keystore.properties` в корне проекта:

```properties
storeFile=path/to/wifiguard.keystore
storePassword=your_store_password
keyAlias=wifiguard
keyPassword=your_key_password
```

### Создание Keystore

```bash
keytool -genkey -v -keystore wifiguard.keystore \
  -alias wifiguard \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### Использование в сборке

Keystore автоматически используется для Release сборок, если файл существует:

```kotlin
// В app/build.gradle.kts
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

signingConfigs {
    create("release") {
        storeFile = file(keystoreProperties["storeFile"] as String)
        storePassword = keystoreProperties["storePassword"] as String
        keyAlias = keystoreProperties["keyAlias"] as String
        keyPassword = keystoreProperties["keyPassword"] as String
    }
}
```

## Конфигурация приложения (DataStore)

### Настройки пользователя

Приложение использует DataStore Preferences для хранения настроек:

```kotlin
// PreferencesDataSource.kt
- autoScanEnabled: Boolean (по умолчанию false)
- scanInterval: Long (по умолчанию 15 минут в миллисекундах)
- notificationsEnabled: Boolean (по умолчанию true)
- dataRetentionDays: Int (по умолчанию 30 дней)
- themeMode: String ("system", "light", "dark")
```

### Управление через UI

Настройки доступны через экран "Настройки" приложения:

- **Автоматическое сканирование**: Вкл/Выкл фоновое сканирование
- **Интервал сканирования**: 5 мин, 15 мин, 30 мин, 1 час
- **Уведомления об угрозах**: Вкл/Выкл уведомления
- **Звук уведомлений**: Вкл/Выкл звук
- **Вибрация**: Вкл/Выкл вибрация
- **Хранение данных**: 1 день, 1 неделя, 1 месяц, 3 месяца, навсегда
- **Тема**: Системная, светлая, темная

## Настройки фоновых задач (WorkManager)

### Конфигурация задач

| Задача | Имя | Интервал | Описание |
|--------|-----|----------|----------|
| Мониторинг Wi-Fi | `wifi_monitoring_periodic` | Настроенный пользователем | Периодическое сканирование и анализ |
| Уведомления об угрозах | `threat_notification_periodic` | 1 раз в 24 часа | Проверка и отправка уведомлений |
| Очистка данных | `data_cleanup_periodic` | 1 раз в 24 часа | Удаление устаревших данных |

### Настройка через код

```kotlin
// В WifiGuardApp.kt
// Автоматическое управление задачами в зависимости от настроек пользователя
// Если autoScanEnabled = true → задача мониторинга включена
// Если autoScanEnabled = false → задача мониторинга отключена
```

## Network Security Configuration

### Безопасность сети

В файле `AndroidManifest.xml`:

```xml
android:usesCleartextTraffic="false"  <!-- Запрет незашифрованного трафика -->
```

### Серверные URL (в BuildConfig)

Хотя приложение в основном работает локально, BuildConfig содержит заглушки для серверных URL:

```kotlin
// Эти URL НЕ используются в текущей реализации
// Добавлены для будущего развития и безопасности
API_BASE_URL
SECURE_API_URL
ANALYTICS_API_URL
```

## Notification Channels

### Конфигурация уведомлений

| Канал | ID | Назначение |
|-------|----|------------|
| Уведомления об угрозах | `threat_notifications` | Уведомления о небезопасных сетях |
| Сканирование Wi-Fi | `wifi_scan_channel` | Уведомления о сканировании |
| Мониторинг Wi-Fi | `wifi_monitoring_channel` | Уведомления о фоновом мониторинге |

## ProGuard/R8 Rules

### Файл конфигурации

`app/proguard-rules.pro` содержит правила для минификации:

```
# Сохранение классов Room
-keep class com.wifiguard.core.data.local.entities.** { *; }

# Сохранение WorkManager задач
-keep class com.wifiguard.core.background.**Worker { *; }

# Сохранение Hilt классов
-keep class * implements dagger.hilt.InstallIn
```

## Database Configuration

### Настройки Room базы данных

```kotlin
// Константы в Constants.kt
DATABASE_NAME = "wifiguard_database"
DATABASE_VERSION = 1
```

База данных:
- Хранится локально на устройстве
- Шифруется при необходимости с использованием Android Keystore
- Использует AES шифрование для чувствительных данных

## Permission Configuration

### Запрашиваемые разрешения

| Разрешение | Тип | Использование | Цель |
|------------|-----|---------------|------|
| ACCESS_FINE_LOCATION | Обязательное | Wi-Fi сканирование | Требование Android 6+ |
| ACCESS_COARSE_LOCATION | Дополнительное | Альтернатива точному местоположению | Резервное разрешение |
| ACCESS_WIFI_STATE | Обязательное | Получение информации о Wi-Fi | Основная функциональность |
| CHANGE_WIFI_STATE | Обязательное | Управление Wi-Fi | Возможные будущие функции |
| ACCESS_NETWORK_STATE | Обязательное | Проверка сетевого подключения | Мониторинг состояния |
| POST_NOTIFICATIONS | Обязательное | Уведомления | Android 13+ |
| NEARBY_WIFI_DEVICES | Обязательное | Wi-Fi сканирование | Android 13+, neverForLocation |
| WAKE_LOCK | Обязательное | Фоновый мониторинг | Поддержание активности |
| RECEIVE_BOOT_COMPLETED | Обязательное | Возобновление мониторинга | После перезагрузки |
| FOREGROUND_SERVICE | Обязательное | Фоновые службы | Android 9+ |
| FOREGROUND_SERVICE_LOCATION | Обязательное | Фоновая активность | Android 14+ |

## Build Variants

### Debug BuildType

- `applicationIdSuffix = ".debug"`
- `versionNameSuffix = "-DEBUG"`
- `isDebuggable = true`
- `isMinifyEnabled = false`
- Использует debug signing

### Release BuildType

- `isMinifyEnabled = true`
- `isShrinkResources = true`
- Использует release signing (если доступен keystore)
- Использует debug signing если keystore недоступен
- Применяет ProGuard rules

## Testing Configuration

### Unit Tests

- Используют Robolectric для эмуляции Android среды
- MockK и Mockito для создания заглушек
- Hilt для тестирования зависимостей

### Instrumented Tests

- Используют устройство/эмулятор для реального тестирования
- Espresso для UI тестирования
- Hilt Testing для внедрения зависимостей в тестах

## Development Configuration

### Локальная разработка

Для локальной разработки рекомендуется:

1. Использовать Debug сборку
2. Включить `debugImplementation` зависимости
3. Использовать Android Studio Profiler для отладки
4. Проверять работу на разных API уровнях (26, 28, 30, 33, 34, 35)

### CI/CD переменные

Для автоматической сборки:

```
ANDROID_COMPILE_SDK=35
ANDROID_BUILD_TOOLS=35.0.0
ANDROID_MIN_SDK=26
ANDROID_TARGET_SDK=35
KOTLIN_VERSION=2.1.0
AGP_VERSION=8.6.1
```