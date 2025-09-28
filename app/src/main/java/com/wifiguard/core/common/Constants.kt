package com.wifiguard.core.common

object Constants {
    // Database
    const val DATABASE_NAME = "wifi_guard_database"
    const val DATABASE_VERSION = 1
    
    // DataStore
    const val PREFERENCES_NAME = "wifi_guard_preferences"
    
    // WiFi Scanning
    const val SCAN_INTERVAL_MS = 30_000L // 30 seconds
    const val MIN_SCAN_INTERVAL_MS = 5_000L // 5 seconds
    
    // Security
    const val AES_KEY_ALIAS = "WifiGuardSecretKey"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    
    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "wifi_security_alerts"
    const val NOTIFICATION_CHANNEL_NAME = "WiFi Security Alerts"
    
    // WorkManager
    const val WIFI_MONITOR_WORK_NAME = "wifi_monitor_work"
    
    // Security Thresholds
    const val WEAK_SIGNAL_THRESHOLD = -80 // dBm
    const val SUSPICIOUS_NETWORK_COUNT = 10
}