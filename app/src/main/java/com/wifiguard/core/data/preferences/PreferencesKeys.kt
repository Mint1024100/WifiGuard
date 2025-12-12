package com.wifiguard.core.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * Type-safe preference keys for DataStore
 */
object PreferencesKeys {
    
    // Auto-scan settings
    val AUTO_SCAN_ENABLED = booleanPreferencesKey("auto_scan_enabled")
    val SCAN_INTERVAL_MINUTES = intPreferencesKey("scan_interval_minutes")
    
    // Notification settings
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val NOTIFICATION_SOUND_ENABLED = booleanPreferencesKey("notification_sound_enabled")
    val NOTIFICATION_VIBRATION_ENABLED = booleanPreferencesKey("notification_vibration_enabled")
    
    // Data retention settings
    val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
    
    // Security settings
    val THREAT_ALERT_ENABLED = booleanPreferencesKey("threat_alert_enabled")
    val CRITICAL_THREAT_NOTIFICATIONS = booleanPreferencesKey("critical_threat_notifications")
    
    // UI settings
    val THEME_MODE = stringPreferencesKey("theme_mode") // "light", "dark", "system"
    val LANGUAGE = stringPreferencesKey("language")
    
    // App settings
    val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    val LAST_SCAN_TIMESTAMP = longPreferencesKey("last_scan_timestamp")
    val TOTAL_SCANS_COUNT = intPreferencesKey("total_scans_count")
    
    // Privacy settings
    val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
    val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
}
