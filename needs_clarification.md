# Need to Clarify - WifiGuard Documentation

This section contains items that require additional clarification as they could not be determined solely from the codebase.

## Missing Information

### 1. Firebase Integration Configuration
- **Location**: `app/build.gradle.kts` (BuildConfig fields)
- **Issue**: Firebase fields exist in BuildConfig but no integration code found
- **Fields**:
  - `ENABLE_CRASHLYTICS`
  - `ENABLE_ANALYTICS`
- **Question**: Are Firebase services planned for future implementation?

### 2. Database Migration Strategy
- **Location**: `Constants.kt` and database configuration
- **Issue**: Current database version is 1, but no migration files exist
- **Question**: How should future schema changes be implemented for production users?

### 3. Server API Endpoints
- **Location**: `app/build.gradle.kts` (BuildConfig fields)
- **Issue**: API endpoint fields exist but no network calls found in current code
- **Fields**:
  - `API_BASE_URL`
  - `SECURE_API_URL` 
  - `ANALYTICS_API_URL`
- **Question**: Are remote API services planned for future implementation?

### 4. Advanced Scanning Features
- **Location**: `MainActivity.kt`, `WifiGuardApp.kt`
- **Issue**: Code references advanced scanning capabilities that are not fully implemented
- **Question**: Are there plans to implement active vulnerability testing or more detailed security analysis?

### 5. Theme Customization Options
- **Location**: `PreferencesDataSource.kt`
- **Issue**: Theme mode preferences exist but UI implementation details unclear
- **Options available**: "system", "light", "dark"
- **Question**: How is theme switching fully implemented across all screens?

### 6. Network Vendor Information
- **Location**: `strings.xml` (`network_vendor` string)
- **Issue**: UI string exists but no implementation to fetch vendor information found
- **Question**: Is MAC address vendor lookup planned for future implementation?

### 7. Data Export/Import Functionality
- **Location**: `strings.xml` (commented out resource strings)
- **Issue**: Export/import strings exist in commented form
- **Strings**: `settings_export_data`, `settings_import_data` 
- **Question**: Is data export/import functionality planned?

### 8. Biometric Authentication
- **Location**: `strings.xml` (biometric-related strings)
- **Issue**: References to biometric authentication in strings but no implementation found
- **Question**: Is secure app access via biometrics planned?

## Questions for Project Maintainers

### 1. Future Development Plans
- What are the planned features beyond current Wi-Fi scanning?
- Are there plans for cloud synchronization or account-based features?
- Will the Firebase integration be implemented?

### 2. Testing Strategy
- What is the coverage target for unit and instrumented tests?
- Are UI tests (Espresso/Jetpack Compose tests) planned?
- What device configurations should be prioritized for testing?

### 3. Release Process
- What is the release cadence and process?
- How are beta releases distributed (Google Play Beta, external APK)?
- What is the version numbering scheme?

### 4. Security Updates
- How are security threat definitions updated?
- Is there a mechanism for pushing security definition updates?
- How are security vulnerabilities reported and fixed?

### 5. Localization
- What languages are planned for localization?
- How are new language translations managed?
- Is automated translation used or manual translation preferred?

## Files to Check for Clarification

1. **`build.gradle.kts`** - Firebase configuration and API endpoints
2. **`AndroidManifest.xml`** - Future component declarations
3. **`res/values/strings.xml`** - Unused feature strings
4. **`core/network/`** - Potentially missing network modules
5. **`feature/settings/`** - Advanced settings functionality
6. **`gradle.properties`** - Build optimization flags
7. **`.github/`** - CI/CD workflow files that might be missing
8. **`keystore.properties.template`** - Complete keystore configuration

## Next Steps

1. Review with project maintainers to prioritize clarification needs
2. Update documentation once missing information is provided
3. Remove items from this list as they are clarified
4. Add implementation details to appropriate documentation sections