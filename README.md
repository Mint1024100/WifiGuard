# WifiGuard - Wi-Fi Security Analysis App

**WifiGuard** — это Android-приложение для анализа безопасности Wi-Fi сетей, обнаружения угроз и мониторинга сетевой активности.

## 📋 Требования

- **JDK**: 17 или выше
- **Android Studio**: Hedgehog (2023.1.1) или новее
- **Android SDK**: минимум API 26 (Android 8.0), рекомендуется API 34 (Android 14)
- **Gradle**: 8.2+ (используется через Gradle Wrapper)

## 🚀 Быстрый старт

### 1. Клонирование репозитория

```bash
git clone https://github.com/your-username/wifiguard.git
cd wifiguard
```

### 2. Сборка проекта

#### Debug сборка

```bash
./gradlew assembleDebug
```

APK будет создан в: `app/build/outputs/apk/debug/app-debug.apk`

#### Release сборка

```bash
./gradlew assembleRelease
```

APK будет создан в: `app/build/outputs/apk/release/app-release.apk`

**Примечание**: Для release сборки необходимо настроить signing configuration (см. раздел "Настройка Signing Config").

### 3. Установка на устройство

```bash
./gradlew installDebug
```

Или используйте Android Studio: `Run → Run 'app'`

## 🔧 Команды Gradle

### Сборка

- `./gradlew clean` — очистка проекта
- `./gradlew build` — полная сборка (debug + release)
- `./gradlew assembleDebug` — сборка debug APK
- `./gradlew assembleRelease` — сборка release APK

### Тестирование

- `./gradlew test` — запуск unit тестов
- `./gradlew testDebugUnitTest` — запуск debug unit тестов
- `./gradlew connectedAndroidTest` — запуск instrumented тестов (требуется подключенное устройство/эмулятор)

### Статический анализ

- `./gradlew lint` — запуск Android Lint
- `./gradlew lintDebug` — lint для debug варианта
- `./gradlew lintRelease` — lint для release варианта

### Зависимости

- `./gradlew dependencies` — просмотр дерева зависимостей
- `./gradlew app:dependencies` — зависимости модуля app

## 📦 Архитектура

Проект следует принципам **Clean Architecture** и **MVVM**:

```
app/
├── core/                      # Общие компоненты
│   ├── background/           # Фоновые задачи (WorkManager)
│   ├── common/               # Константы, утилиты
│   ├── data/                 # Репозитории, источники данных
│   │   ├── local/           # Room Database, DAO, Entity
│   │   └── wifi/            # Wi-Fi сканер
│   ├── domain/              # Бизнес-логика, модели
│   ├── security/            # Шифрование, анализ безопасности
│   └── ui/                  # Общие UI компоненты, темы
├── di/                       # Hilt модули зависимостей
├── feature/                  # Фичи приложения
│   ├── scanner/             # Сканирование Wi-Fi сетей
│   ├── analysis/            # Анализ безопасности
│   ├── settings/            # Настройки приложения
│   └── notifications/       # Уведомления об угрозах
└── navigation/              # Навигация между экранами
```

## 🛠 Технологии

- **UI**: Jetpack Compose + Material3
- **DI**: Hilt
- **Async**: Kotlin Coroutines + Flow
- **Database**: Room
- **Background**: WorkManager
- **Navigation**: Navigation Compose
- **Storage**: DataStore Preferences
- **Security**: Android KeyStore, AES encryption

## 📱 Основные функции

- ✅ Сканирование доступных Wi-Fi сетей
- ✅ Анализ типа шифрования (WEP, WPA, WPA2, WPA3)
- ✅ Обнаружение открытых и небезопасных сетей
- ✅ Детектирование потенциальных атак (Evil Twin, и др.)
- ✅ Фоновый мониторинг сетевой активности
- ✅ История сканирований
- ✅ Уведомления об угрозах
- ✅ Подробная статистика по сетям

## 🔐 Настройка Signing Config

Для создания подписанного release APK:

### 1. Создание keystore

```bash
keytool -genkey -v -keystore wifiguard.keystore \
  -alias wifiguard \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 2. Настройка переменных окружения

Создайте файл `keystore.properties` в корне проекта:

```properties
storeFile=path/to/wifiguard.keystore
storePassword=your_store_password
keyAlias=wifiguard
keyPassword=your_key_password
```

**⚠️ Важно**: Добавьте `keystore.properties` в `.gitignore`!

### 3. Обновление build.gradle.kts

Раскомментируйте строку в `app/build.gradle.kts`:

```kotlin
signingConfig = signingConfigs.getByName("release")
```

И обновите блок `signingConfigs`:

```kotlin
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

## 🧪 Тестирование

### Unit тесты

Расположены в `app/src/test/`:

- `AesEncryptionTest.kt` — тесты шифрования
- `SecurityManagerTest.kt` — тесты анализа безопасности
- `WifiScannerTest.kt` — тесты Wi-Fi сканера

Запуск:

```bash
./gradlew test
```

### Instrumented тесты

Расположены в `app/src/androidTest/`:

Запуск (требуется подключенное устройство):

```bash
./gradlew connectedAndroidTest
```

## 📝 Version Catalog

Проект использует Gradle Version Catalog (`gradle/libs.versions.toml`) для централизованного управления зависимостями:

```toml
[versions]
kotlin = "1.9.20"
compose-bom = "2024.02.00"
hilt = "2.50"
room = "2.6.1"
...

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "androidx-core" }
...

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
...
```

## 🔍 Lint и Качество кода

### Запуск Lint

```bash
./gradlew lint
```

Отчет будет сгенерирован в: `app/build/reports/lint-results.html`

### ProGuard/R8

Release сборка использует ProGuard rules (`app/proguard-rules.pro`) для:

- Минификации кода
- Обфускации
- Оптимизации
- Удаления неиспользуемого кода

## 📋 Разрешения

Приложение требует следующие разрешения:

- `ACCESS_WIFI_STATE` — доступ к Wi-Fi
- `CHANGE_WIFI_STATE` — изменение состояния Wi-Fi
- `ACCESS_FINE_LOCATION` — для сканирования Wi-Fi (требование Android 6+)
- `ACCESS_COARSE_LOCATION` — грубое местоположение
- `POST_NOTIFICATIONS` — уведомления (Android 13+)
- `INTERNET` — для обновлений базы угроз

## 🚧 Известные ограничения

1. **Keystore для release**: Не настроен по умолчанию (требуется создание)
2. **API endpoints**: Используются placeholder URL (настройте реальные в `build.gradle.kts`)
3. **Crashlytics/Analytics**: Не подключены (требуется настройка Firebase)
4. **Тесты**: Покрытие частичное (требуется расширение)

## 🤝 Вклад в проект

1. Fork репозитория
2. Создайте feature ветку (`git checkout -b feature/amazing-feature`)
3. Commit изменений (`git commit -m 'Add amazing feature'`)
4. Push в ветку (`git push origin feature/amazing-feature`)
5. Создайте Pull Request

## 📄 Лицензия

Этот проект лицензирован под MIT License - см. файл [LICENSE](LICENSE) для деталей.

## 📞 Контакты

- **GitHub**: [https://github.com/your-username/wifiguard](https://github.com/your-username/wifiguard)
- **Issues**: [https://github.com/your-username/wifiguard/issues](https://github.com/your-username/wifiguard/issues)

---

**Создано с ❤️ для безопасности Wi-Fi сетей**

