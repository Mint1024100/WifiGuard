package com.wifiguard.core.common

/**
 * Константы приложения
 */
object Constants {
    const val LOG_TAG = "WifiGuard"
    
    // Database
    const val DATABASE_NAME = "wifiguard_database"
    const val DATABASE_VERSION = 1
    
    // WorkManager
    const val WORK_TAG_WIFI_MONITORING = "wifi_monitoring"
    const val WORK_TAG_THREAT_NOTIFICATION = "threat_notification"
    const val WORK_NAME_WIFI_MONITORING = "wifi_monitoring_work"
    const val WORK_NAME_THREAT_NOTIFICATION = "threat_notification_work"
    
    // Workers
    const val THREAT_NOTIFICATION_WORK_NAME = "threat_notification_periodic"
    
    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "threat_notifications"
    const val NOTIFICATION_ID = 1001
    
    // Scan intervals
    const val DEFAULT_SCAN_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes
    const val MIN_SCAN_INTERVAL_MS = 5 * 60 * 1000L // 5 minutes
    const val MAX_SCAN_INTERVAL_MS = 60 * 60 * 1000L // 1 hour
    
    // Data retention
    const val DEFAULT_DATA_RETENTION_DAYS = 30
    const val MAX_DATA_RETENTION_DAYS = 365
    
    // Security
    const val MIN_SIGNAL_STRENGTH = -100 // dBm
    const val MAX_SIGNAL_STRENGTH = -30 // dBm
    
    // Encryption
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    const val AES_KEY_ALIAS = "WifiGuardAESKey"
    const val HMAC_KEY_ALIAS = "WifiGuardHMACKey"
    const val WEAK_SIGNAL_THRESHOLD = -80 // dBm
    
    // Network
    const val SCAN_TIMEOUT_MS = 5000L
    const val MAX_NETWORKS_PER_SCAN = 100
    
    // WiFi Network Names
    const val HIDDEN_NETWORK_LABEL = "Скрытая сеть"
    const val UNKNOWN_SSID = "<unknown ssid>"
    const val HIDDEN_NETWORK = "Hidden Network"
    
    // Preferences
    const val PREFERENCES_NAME = "wifiguard_preferences"
}