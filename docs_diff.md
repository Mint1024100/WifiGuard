# Documentation Differences Report - WifiGuard

## Summary of Discrepancies

| Section | Status | Issue | Source |
|---------|--------|-------|---------|
| Architecture | Outdated | New features/components added (WifiConnectionObserver, WifiForegroundScanService) | WifiGuardApp.kt, AndroidManifest.xml |
| Permissions | Outdated | New permissions added (FOREGROUND_SERVICE_LOCATION, NEARBY_WIFI_DEVICES) | AndroidManifest.xml |
| Database version | Outdated | Documentation mentions "DATABASE_VERSION = 1" but schema may have changed | Constants.kt vs DATABASE_BEST_PRACTICES.md |
| Build configuration | Outdated | Target SDK updated to 35 (was 34) | build.gradle.kts vs README.md |
| Network security | Accurate | No discrepancies found | SECURITY.md |
| Data safety | Current | Information is up-to-date | DATA_SAFETY.md |

## Detailed Discrepancies

### 1. Architecture & Components

**Was:** README.md described basic architecture with core components
**Became:** New components added that aren't documented:
- `WifiConnectionObserver` for automatic threat notifications
- `WifiForegroundScanService` for full Wi-Fi scanning
- Enhanced background monitoring with `WifiMonitoringWorker`
- `WifiGuardNavigation` with new screens (SecurityReport, About, PrivacyPolicy, TermsOfService)

**Source:** MainActivity.kt, WifiGuardApp.kt, AndroidManifest.xml, Navigation files

### 2. Permissions

**Was:** Documentation listed standard Wi-Fi permissions
**Became:** Additional permissions added:
- `FOREGROUND_SERVICE_LOCATION` for Android 14+ (upside_down_cake)
- `NEARBY_WIFI_DEVICES` with `neverForLocation` flag for Android 13+

**Source:** AndroidManifest.xml

### 3. Build Configuration

**Was:** README.md mentioned Target SDK 34
**Became:** Target SDK updated to 35 in build configuration

**Source:** build.gradle.kts (targetSdk = 35)

### 4. New Features & Screens

**Was:** README.md mentioned basic feature set
**Became:** Additional screens and features implemented:
- Security Report screen
- About screen
- Privacy Policy screen
- Terms of Service screen
- Enhanced notification system with threat notifications
- Background monitoring with WifiConnectionObserver

**Source:** navigation/Screen.kt, feature/ directories, MainActivity.kt

### 5. Constants Update

**Was:** Documentation in various files mentioned certain constants
**Became:** Constants.kt has updated values:
- DATABASE_VERSION = 1
- NEW work manager constants for unique work names
- Updated notification channel IDs

**Source:** core/common/Constants.kt

### 6. Background Processing

**Was:** Basic WorkManager usage mentioned
**Became:** Enhanced background processing with:
- New Worker names to prevent duplicates
- Better handling of work cancellation
- Enhanced WifiMonitoringWorker with WifiConnectionObserver

**Source:** WifiGuardApp.kt, Constants.kt

### 7. Data Safety Documentation - No Issues

**Status:** Up-to-date as DATA_SAFETY.md accurately describes:
- Local data storage only
- No data transmission to servers
- Proper encryption with AES
- Android Keystore usage
- Detailed permission explanations

**Source:** DATA_SAFETY.md

### 8. Security Documentation - No Issues

**Status:** Up-to-date as SECURITY.md accurately describes:
- AES-256-GCM encryption
- Android Keystore usage
- Certificate pinning
- Network security configuration
- Proper permission usage

**Source:** SECURITY.md

### 9. Documentation Missing

**Issue:** No documentation for new components
**Missing:** Documentation for:
- WifiConnectionObserver functionality
- Foreground services
- Enhanced notification system
- New UI screens (SecurityReport, About, Privacy Policy, Terms of Service)
- Updated background monitoring system

**Source:** Various feature directories, core/monitoring/, core/service/

## Priority Issues (Need Immediate Documentation)

1. **WifiConnectionObserver**: Critical feature for automatic threat notifications
2. **Foreground Services**: Required for Android 14+ compliance
3. **Enhanced Permission Handling**: Updated for Android 13+ requirements
4. **New UI Screens**: Several new screens not documented
5. **Background Monitoring Enhancement**: Updated architecture not reflected in docs

## Recommendations

1. Update README.md with new architecture and features
2. Create comprehensive API/Architecture documentation
3. Update security documentation with new components
4. Add documentation for new UI screens and navigation
5. Document new permission requirements
6. Add troubleshooting section for new features