# Discovery Report - WifiGuard Android Application

## Project Overview
**Project Name:** WifiGuard  
**Type:** Android application for Wi-Fi security analysis  
**Purpose:** Analyzes Wi-Fi networks for security threats and monitors network activity

## Application Architecture
- **Architecture Pattern:** Clean Architecture + MVVM
- **UI Framework:** Jetpack Compose with Material3
- **Language:** Kotlin
- **Build System:** Gradle with Kotlin DSL and Version Catalog

## Tech Stack
- Dependency Injection: Hilt
- Async Processing: Kotlin Coroutines + Flow
- Database: Room
- Background Processing: WorkManager
- Navigation: Navigation Compose
- Storage: DataStore Preferences
- Security: Android KeyStore, AES encryption

## Configuration Files
- `build.gradle.kts` (root): Global build configuration
- `app/build.gradle.kts`: Application module configuration
- `settings.gradle.kts`: Module inclusion
- `gradle/libs.versions.toml`: Dependency version management
- `keystore.properties.template`: Keystore configuration template

## Entry Points
- `WifiGuardApp.kt`: Application class with WorkManager initialization
- `MainActivity.kt`: Main activity with permission handling and Compose UI
- `WifiGuardNavigation.kt`: Navigation graph definition
- AndroidManifest.xml: Component declarations and permissions

## Key Features
- Wi-Fi network scanning
- Security analysis (encryption types, threat detection)
- Background monitoring
- Threat notifications
- Network history
- Security recommendations

## Source Directories
- `core/`: Common components (background, data, domain, security, UI)
- `di/`: Dependency injection modules
- `feature/`: Feature modules (scanner, analysis, settings, notifications)
- `navigation/`: Navigation components

## Permissions
- ACCESS_WIFI_STATE, CHANGE_WIFI_STATE
- ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION
- POST_NOTIFICATIONS (Android 13+)
- NEARBY_WIFI_DEVICES (Android 13+)
- FOREGROUND_SERVICE, WAKE_LOCK, RECEIVE_BOOT_COMPLETED

## Build Configuration
- Min SDK: 26 (Android 8.0)
- Target SDK: 35 (Android 14)
- Compile SDK: 35
- Java/Kotlin: 17
- Release builds with minification enabled