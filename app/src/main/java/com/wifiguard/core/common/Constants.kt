package com.wifiguard.core.common

/**
 * ИСПРАВЛЕННЫЕ глобальные константы приложения WifiGuard
 * 
 * ДОБАВЛЕНО:
 * ✅ Полный набор констант
 * ✅ LOG_TAG для логирования
 * ✅ Security настройки
 * ✅ Константы для всех feature модулей
 */
object Constants {
    
    // Логирование
    const val LOG_TAG = "WifiGuard"
    
    // Database
    const val DATABASE_NAME = "wifiguard_database"
    const val DATABASE_VERSION = 1
    
    // DataStore & Preferences
    const val PREFERENCES_NAME = "wifiguard_preferences"
    const val ENCRYPTED_PREFERENCES_NAME = "wifiguard_secure_preferences"
    
    // Network & WiFi Scanning
    const val WIFI_SCAN_INTERVAL_MS = 5_000L // 5 секунд
    const val MIN_SCAN_INTERVAL_MS = 3_000L // 3 секунды
    const val MAX_SCAN_INTERVAL_MS = 60_000L // 1 минута
    const val NETWORK_TIMEOUT_MS = 10_000L
    const val MAX_RETRY_ATTEMPTS = 3
    
    // Security & Encryption
    const val AES_KEY_LENGTH = 256
    const val AES_ALGORITHM = "AES/GCM/NoPadding"
    const val KEY_ALIAS = "WifiGuardSecretKey"
    const val AES_KEY_ALIAS = "WifiGuardAESKey"
    const val HMAC_KEY_ALIAS = "WifiGuardHMACKey"
    const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    
    // Logging
    const val LOG_TAG = "WifiGuard"
    
    // Notifications
    const val NOTIFICATION_CHANNEL_ID = "wifi_security_alerts"
    const val NOTIFICATION_CHANNEL_NAME = "WiFi Security Alerts"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications about Wi-Fi security threats"
    
    // WorkManager
    const val WIFI_MONITOR_WORK_NAME = "wifi_monitor_work"
    const val WIFI_SCAN_WORK_NAME = "wifi_scan_work"
    const val BACKGROUND_MONITOR_WORK_NAME = "background_monitor_work"
    
    // Security Assessment Thresholds
    const val WEAK_SIGNAL_THRESHOLD_DBM = -80 // dBm
    const val SUSPICIOUS_NETWORK_COUNT = 10
    const val ROGUE_AP_DETECTION_THRESHOLD = 5
    const val SECURITY_SCAN_TIMEOUT_MS = 30_000L
    
    // Cache & Storage
    const val MAX_CACHE_SIZE_MB = 50
    const val MAX_HISTORY_ENTRIES = 1000
    const val CACHE_EXPIRY_MS = 300_000L // 5 минут
    
    /**
     * Уровни безопасности
     */
    object SecurityLevels {
        const val SECURE = "SECURE"
        const val WARNING = "WARNING" 
        const val DANGER = "DANGER"
        const val CRITICAL = "CRITICAL"
        const val UNKNOWN = "UNKNOWN"
    }
    
    /**
     * Типы шифрования WiFi
     */
    object EncryptionTypes {
        const val NONE = "NONE"
        const val WEP = "WEP"
        const val WPA = "WPA"
        const val WPA2 = "WPA2"
        const val WPA3 = "WPA3"
        const val WPS = "WPS"
        const val UNKNOWN = "UNKNOWN"
    }
    
    /**
     * Коды разрешений
     */
    object Permissions {
        const val LOCATION_PERMISSION_CODE = 1001
        const val NOTIFICATION_PERMISSION_CODE = 1002
        const val WIFI_PERMISSION_CODE = 1003
    }
    
    /**
     * Маршруты навигации
     */
    object Routes {
        const val SCANNER = "scanner"
        const val ANALYZER = "analyzer" 
        const val HISTORY = "history"
        const val SETTINGS = "settings"
        const val DETAILS = "details/{networkId}"
        const val NETWORK_ID_ARG = "networkId"
    }
    
    /**
     * Ключи для SharedPreferences
     */
    object PreferenceKeys {
        const val AUTO_SCAN_ENABLED = "auto_scan_enabled"
        const val SCAN_INTERVAL = "scan_interval"
        const val NOTIFICATIONS_ENABLED = "notifications_enabled"
        const val DARK_THEME_ENABLED = "dark_theme_enabled"
        const val FIRST_LAUNCH = "first_launch"
        const val SECURITY_LEVEL_FILTER = "security_level_filter"
        const val BACKGROUND_MONITORING = "background_monitoring"
    }
    
    /**
     * Ключи для Bundle аргументов
     */
    object BundleKeys {
        const val NETWORK_DATA = "network_data"
        const val SCAN_RESULTS = "scan_results"
        const val SECURITY_ASSESSMENT = "security_assessment"
        const val SELECTED_NETWORK_ID = "selected_network_id"
    }
    
    /**
     * Дефолтные значения
     */
    object Defaults {
        const val SCAN_INTERVAL_DEFAULT = WIFI_SCAN_INTERVAL_MS
        const val AUTO_SCAN_DEFAULT = true
        const val NOTIFICATIONS_DEFAULT = true
        const val DARK_THEME_DEFAULT = false
        const val BACKGROUND_MONITORING_DEFAULT = true
    }
    
    /**
     * Лимиты и ограничения
     */
    object Limits {
        const val MAX_NETWORKS_TO_DISPLAY = 100
        const val MAX_SCAN_RESULTS_CACHE = 50
        const val MAX_LOG_ENTRIES = 500
        const val MIN_SIGNAL_STRENGTH_DBM = -100
        const val MAX_SIGNAL_STRENGTH_DBM = 0
    }
    
    /**
     * Коды ошибок
     */
    object ErrorCodes {
        const val PERMISSION_DENIED = "PERMISSION_DENIED"
        const val WIFI_DISABLED = "WIFI_DISABLED"
        const val LOCATION_DISABLED = "LOCATION_DISABLED"
        const val SCAN_FAILED = "SCAN_FAILED"
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val DATABASE_ERROR = "DATABASE_ERROR"
        const val ENCRYPTION_ERROR = "ENCRYPTION_ERROR"
    }
    
    /**
     * Мима-типы для экспорта данных
     */
    object MimeTypes {
        const val JSON = "application/json"
        const val CSV = "text/csv"
        const val TEXT_PLAIN = "text/plain"
    }
    
    /**
     * URL и нетворк
     */
    object Network {
        const val BASE_URL = "https://api.wifiguard.com/"
        const val THREAT_DATABASE_URL = "https://threats.wifiguard.com/"
        const val UPDATE_CHECK_URL = "https://updates.wifiguard.com/"
        const val PRIVACY_POLICY_URL = "https://wifiguard.com/privacy"
        const val TERMS_URL = "https://wifiguard.com/terms"
    }
}