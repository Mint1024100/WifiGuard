# Development Environment Setup - WifiGuard

## System Requirements

### Hardware Requirements
- **CPU**: 2 cores or more (4+ cores recommended)
- **RAM**: 8 GB or more (16+ GB recommended for emulator)
- **Storage**: 5+ GB of available space
- **Operating System**: Windows 7+, macOS 10.14+, or Linux

### Software Requirements
- **JDK**: 17 or higher
- **Android Studio**: Hedgehog (2023.1.1) or newer
- **Gradle**: 8.2+ (automatically managed via Gradle Wrapper)
- **Git**: Version control system

## Android Studio Setup

### Installation
1. Download Android Studio from [developer.android.com](https://developer.android.com/studio)
2. Install with default settings
3. Launch Android Studio and complete initial setup wizard

### SDK Components
During installation, ensure the following components are selected:
- **Android SDK Platform 35** (API Level 35)
- **Android SDK Platform 26** (API Level 26) - minimum supported
- **Android SDK Build-Tools** version 35.0.0 or higher
- **Android SDK Platform-Tools**
- **Android SDK Tools**
- **Android Emulator** (if you plan to use emulators)

### Recommended Settings

#### Performance Settings
```
File → Settings → Appearance & Behavior → System Settings → Android SDK → SDK Platforms
- Check "Show Package Details"
- Install API levels: 26, 28, 30, 33, 34, 35

File → Settings → Appearance & Behavior → System Settings → Android SDK → SDK Tools
- Check "Show Package Details"
- Update to latest versions of:
  - Android SDK Build-Tools
  - Android SDK Platform-Tools
  - Android SDK Tools
  - Android Emulator
```

## Project Setup

### 1. Clone the Repository
```bash
git clone https://github.com/Mint1024100/WifiGuard.git
cd WifiGuard
```

### 2. Open in Android Studio
- File → Open
- Navigate to the cloned directory
- Select the project root directory
- Click "OK"

### 3. Initial Gradle Sync
- Android Studio will automatically attempt Gradle sync
- If it fails, check your JDK version (must be 17+)
- Ensure internet connection for dependency downloads

## Development Environment Configuration

### Gradle Configuration

#### gradle.properties (Project-level)
Create or update the `gradle.properties` file in your project root:

```properties
# JVM settings for Gradle
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8

# Parallel builds (recommended for faster compilation)
org.gradle.parallel=true

# Configure daemon
org.gradle.daemon=true

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official

# Memory settings
org.gradle.configureondemand=true
```

### IDE Configuration

#### Android Studio Plugins
Ensure these plugins are installed/enabled:
- Kotlin
- Android Support
- Git Integration
- Gradle
- XML

#### Code Style
1. File → Settings → Editor → Code Style → Kotlin
2. Set scheme to "Default" or import custom formatting rules
3. Use the project's existing code style as reference

## Building the Project

### Command Line Build
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK (without signing)
./gradlew assembleRelease

# Run all checks
./gradlew check

# Install on connected device/emulator
./gradlew installDebug
```

### Android Studio Build
1. Select "app" in the "Build Variants" tab
2. Choose "debug" build variant
3. Build → Make Project (or Ctrl+F9)

## Testing Setup

### Unit Tests
```bash
# Run all unit tests
./gradlew test

# Run debug unit tests
./gradlew testDebugUnitTest
```

### Instrumented Tests
```bash
# Run instrumented tests (requires connected device/emulator)
./gradlew connectedAndroidTest
```

### Test Dependencies
The project uses:
- **JUnit 4** for basic unit testing
- **MockK** for Kotlin mocking
- **Mockito** and **Mockito-Kotlin** for Java/Kotlin mocking
- **Robolectric** for Android framework mocking
- **Espresso** for UI testing
- **Hilt Testing** for dependency injection tests

## Debugging Configuration

### Debugging Settings in Android Studio

#### Run Configuration
1. Run → Edit Configurations
2. Add "Android App" configuration
3. Name: "WifiGuard Debug"
4. Module: ":app"
5. Launch Options: "Default Activity"
6. Deployment Target: Choose your preferred device/emulator

#### Debugger Settings
- Enable "Enable "Update" actions when code is hot-swapped"
- Use "Mixed" debug type (Java and Kotlin)
- Enable "Allow running multiple instances" for testing

### Logging Configuration
The app uses Android's standard logging system:
- Tag: "WifiGuard" (defined in Constants.LOG_TAG)
- Level: V (verbose) for debug builds, W (warning) for release builds

## Emulator Setup

### Recommended Emulator Configuration
For testing purposes, create an AVD with:
- **Device**: Pixel 4, 5, 6, or similar
- **API Level**: 30+ for modern features testing
- **System Image**: Google Play or Google APIs
- **RAM**: 4GB or higher
- **VM Heap**: 512MB or higher

### Hardware Features
Ensure emulator has:
- Wi-Fi simulation enabled
- Location services enabled
- Proper permissions for testing

## Required Android Permissions for Development

Your development environment should allow these permissions for testing:
- Location (for Wi-Fi scanning)
- Wi-Fi access
- Notification access
- Background activity

## Code Quality Tools

### Lint Configuration
```bash
# Run Android Lint
./gradlew lint

# Generate HTML report
./gradlew lintDebug
# Report location: app/build/reports/lint-results.html
```

### Kotlin Static Analysis
The project uses:
- Kotlin compiler's built-in analysis
- Android Lint for Android-specific issues
- Detekt for additional Kotlin static analysis (if configured)

## Version Control

### Git Configuration
```bash
# Set up Git username and email
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"

# Recommended Git settings for this project
git config core.autocrlf false  # Use LF for line endings
```

### Branching Strategy
- `main` branch: Stable production-ready code
- `develop` branch: Integration branch for features
- `feature/*` branches: Individual feature development
- `release/*` branches: Release preparation
- `hotfix/*` branches: Urgent bug fixes

## Development Workflow

### 1. Feature Development
```bash
# Create feature branch
git checkout -b feature/amazing-feature

# Make changes
# (develop your feature)

# Run tests
./gradlew test

# Check code quality
./gradlew lint

# Commit changes
git add .
git commit -m "Add amazing feature"
```

### 2. Code Review Process
1. Push feature branch to remote repository
2. Create Pull Request to `develop` branch
3. Wait for code review and CI checks
4. Address review comments if needed
5. Merge after approval

### 3. Testing Checklist
Before submitting changes:
- [ ] Unit tests pass (`./gradlew test`)
- [ ] Lint checks pass (`./gradlew lint`)
- [ ] Code follows project style
- [ ] New functionality is documented
- [ ] No sensitive information in commits
- [ ] Dependencies are properly declared

## Troubleshooting Development Issues

### Common Build Problems

#### Out of Memory Errors
Solution: Increase Gradle's memory in `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

#### Gradle Sync Failures
1. Check JDK version (must be 17+)
2. Try "File → Invalidate Caches and Restart"
3. Delete `.gradle` directories and retry sync

#### Dependency Resolution Issues
1. Check internet connection
2. Try `./gradlew clean`
3. Verify `gradle/libs.versions.toml` has correct versions

### Debugging Common Issues

#### Wi-Fi Scanning Not Working
- Ensure device/emulator has Wi-Fi permissions enabled
- Check physical Wi-Fi is turned on (for real devices)
- Verify location permissions granted to the app

#### UI Not Updating
- Check if updates happen on correct thread (use `viewModelScope` for ViewModels)
- Verify State/Flow changes trigger recomposition
- Use Android Studio's Layout Inspector to debug UI

## Environment Variables (Optional)

### Build Customization
Set these environment variables to customize builds:

```bash
# Custom package name
export APP_PACKAGE_NAME=com.yourcompany.wifiguard

# Custom version
export APP_VERSION_NAME=2.0.0
export APP_VERSION_CODE=2
```

## Continuous Integration Setup

### Local CI Testing
Before pushing to remote, run:
```bash
# Full test suite
./gradlew clean build check

# Verify all tests pass
./gradlew test connectedAndroidTest

# Check code quality
./gradlew lint
```

### Pre-push Checklist
- [ ] All tests pass
- [ ] Code style compliance
- [ ] No debug logs in production code
- [ ] No hardcoded sensitive information
- [ ] README updated if necessary
- [ ] Documentation updated if necessary