package com.wifiguard.core.common

/**
 * Константы приложения WifiGuard
 * Содержит все используемые в приложении константы
 */
object Constants {
    
    // Разрешения
    const val PERMISSION_LOCATION = android.Manifest.permission.ACCESS_FINE_LOCATION
    const val PERMISSION_WIFI_STATE = android.Manifest.permission.ACCESS_WIFI_STATE
    const val PERMISSION_CHANGE_WIFI_STATE = android.Manifest.permission.CHANGE_WIFI_STATE
    const val PERMISSION_NETWORK_STATE = android.Manifest.permission.ACCESS_NETWORK_STATE
    const val PERMISSION_POST_NOTIFICATIONS = android.Manifest.permission.POST_NOTIFICATIONS
    
    // Коды запросов разрешений
    const val REQUEST_CODE_LOCATION = 1001
    const val REQUEST_CODE_NOTIFICATIONS = 1002
    
    // Настройки сканирования
    const val WIFI_SCAN_INTERVAL_MS = 5000L // 5 секунд
    const val BACKGROUND_MONITORING_INTERVAL_MINUTES = 15L
    
    // Уровни безопасности
    const val SECURITY_LEVEL_HIGH_THRESHOLD = 80
    const val SECURITY_LEVEL_MEDIUM_THRESHOLD = 60
    const val SECURITY_LEVEL_LOW_THRESHOLD = 40
    
    // Настройки шифрования
    const val AES_KEY_SIZE = 256
    const val AES_TRANSFORMATION = "AES/GCM/NoPadding"
    const val AES_GCM_IV_LENGTH = 12
    const val AES_GCM_TAG_LENGTH = 16
    
    // Настройки базы данных
    const val DATABASE_NAME = "wifi_guard_database"
    const val DATABASE_VERSION = 1
    
    // Настройки уведомлений
    const val NOTIFICATION_CHANNEL_ID = "wifi_guard_alerts"
    const val NOTIFICATION_CHANNEL_NAME = "WiFi Security Alerts"
    const val NOTIFICATION_CHANNEL_DESCRIPTION = "Уведомления о безопасности WiFi сетей"
    const val NOTIFICATION_ID_SECURITY_ALERT = 1001
    const val NOTIFICATION_ID_MONITORING = 1002
    
    // Настройки WorkManager
    const val WORK_NAME_WIFI_MONITORING = "wifi_monitoring_work"
    
    // Таймауты и интервалы
    const val NETWORK_TIMEOUT_SECONDS = 30L
    const val WIFI_STATE_UPDATE_TIMEOUT_MS = 1000L
    
    // Настройки DataStore
    const val PREFERENCES_NAME = "wifi_guard_preferences"
    
    // Ключи для SharedPreferences/DataStore
    object PreferenceKeys {
        const val IS_MONITORING_ENABLED = "is_monitoring_enabled"
        const val NOTIFICATION_ENABLED = "notification_enabled"
        const val AUTO_SCAN_ENABLED = "auto_scan_enabled"
        const val SCAN_INTERVAL = "scan_interval"
        const val SECURITY_THRESHOLD = "security_threshold"
        const val DARK_THEME_ENABLED = "dark_theme_enabled"
        const val FIRST_LAUNCH = "first_launch"
    }
    
    // Типы шифрования WiFi
    object WifiSecurity {
        const val NONE = "NONE"
        const val WEP = "WEP"
        const val WPA = "WPA"
        const val WPA2 = "WPA2"
        const val WPA3 = "WPA3"
        const val WPA2_WPA3 = "WPA2/WPA3"
        const val UNKNOWN = "UNKNOWN"
    }
    
    // Уровни угроз
    object ThreatLevel {
        const val NONE = "NONE"
        const val LOW = "LOW"
        const val MEDIUM = "MEDIUM"
        const val HIGH = "HIGH"
        const val CRITICAL = "CRITICAL"
    }
    
    // Сообщения об ошибках
    object ErrorMessages {
        const val LOCATION_PERMISSION_REQUIRED = "Для сканирования WiFi сетей требуется разрешение на доступ к местоположению"
        const val WIFI_NOT_ENABLED = "WiFi не включен"
        const val SCAN_FAILED = "Не удалось выполнить сканирование WiFi сетей"
        const val DATABASE_ERROR = "Ошибка при работе с базой данных"
        const val ENCRYPTION_ERROR = "Ошибка шифрования данных"
        const val NETWORK_ERROR = "Ошибка сети"
        const val UNKNOWN_ERROR = "Неизвестная ошибка"
    }
    
    // Логи
    object LogTags {
        const val WIFI_SCANNER = "WifiScanner"
        const val SECURITY_ANALYZER = "SecurityAnalyzer"
        const val NOTIFICATION_SERVICE = "NotificationService"
        const val DATABASE = "Database"
        const val ENCRYPTION = "Encryption"
        const val MAIN = "WifiGuard"
    }
}