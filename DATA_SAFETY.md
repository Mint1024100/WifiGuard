# Data Safety Section for Google Play Console

## Data Collection and Security Practices

### Data types collected
- **Wi-Fi network information**: SSID, BSSID, signal strength, encryption type, frequency and channel.
  - Collected: Yes (on device)
  - Shared: No
  - Required: Yes
  - Purposes: Security analysis, threat detection
  - Security practices: Stored locally on device, encrypted with AES

- **Location information**: Approximate location to enable Wi-Fi scanning.
  - Collected: Yes (on device)
  - Shared: No
  - Required: Yes
  - Purposes: Enable Wi-Fi network discovery
  - Security practices: Not stored permanently, used only during scanning

- **App usage data**: Information about when and how the app is used.
  - Collected: No
  - Shared: N/A

### Security practices
- All sensitive data is encrypted using AES encryption
- No data is transmitted to any server
- All analysis is performed locally on the device
- Data is stored locally and can be cleared by user at any time
- Android Keystore is used for secure key management
- No third-party analytics or tracking software is used

### Data sharing
- No data is shared with third parties
- No data is transmitted to external servers
- All processing happens locally on the device

## Permissions Explanation

### Required Permissions
- `ACCESS_WIFI_STATE`: Required to scan for Wi-Fi networks and analyze their security
- `CHANGE_WIFI_STATE`: Required to connect/disconnect from networks for testing purposes
- `ACCESS_FINE_LOCATION`: Required by Android for Wi-Fi scanning starting from Android 6+
- `ACCESS_COARSE_LOCATION`: Alternative location permission for Wi-Fi scanning
- `POST_NOTIFICATIONS`: Required to alert users of security threats (Android 13+)
- `WAKE_LOCK`: Required for background security monitoring
- `RECEIVE_BOOT_COMPLETED`: Required to resume background monitoring after device restart

### Hardware Features
- `android.hardware.wifi`: Required for Wi-Fi analysis functionality
- `android.hardware.location`: Optional, for enhanced location features

## Security Analysis Functionality

The WifiGuard app performs security analysis of Wi-Fi networks to help users identify potential security risks. This functionality:
- Analyzes encryption types (WEP, WPA, WPA2, WPA3)
- Identifies open and unsecured networks
- Detects potential threats like Evil Twin attacks
- Provides security recommendations
- All analysis is performed locally on the device
- No network data is transmitted to external servers

## Data Retention and Deletion

Users can:
- Clear all stored scan history from Settings
- Control data retention periods in Settings
- Export their data to local files
- Uninstall the app to remove all data

## Compliance Statement

WifiGuard is a security analysis tool designed to help users evaluate the security of Wi-Fi networks. All functionality is clearly disclosed to users in the app UI. The app does not perform any unauthorized network access or engage in malicious activity. The app is designed for legitimate security analysis and network safety purposes.