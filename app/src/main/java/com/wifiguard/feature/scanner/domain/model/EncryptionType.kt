package com.wifiguard.feature.scanner.domain.model

/**
 * Перечисление типов шифрования Wi-Fi сетей
 */
enum class EncryptionType(
    val displayName: String,
    val securityLevel: SecurityLevel
) {
    
    /**
     * Открытая сеть без шифрования
     */
    NONE("Открытая", SecurityLevel.NONE),
    
    /**
     * WEP - устаревший стандарт с низкой безопасностью
     */
    WEP("WEP", SecurityLevel.LOW),
    
    /**
     * WPA - предыдущая версия со средним уровнем безопасности
     */
    WPA("WPA", SecurityLevel.MEDIUM),
    
    /**
     * WPA2 - современный стандарт с высоким уровнем безопасности
     */
    WPA2("WPA2", SecurityLevel.HIGH),
    
    /**
     * WPA3 - новейший стандарт с максимальным уровнем безопасности
     */
    WPA3("WPA3", SecurityLevel.MAXIMUM),
    
    /**
     * WPS - Wi-Fi Protected Setup, часто сочетается с другими типами
     */
    WPS("WPS", SecurityLevel.MEDIUM),
    
    /**
     * Неизвестный или неподдерживаемый тип
     */
    UNKNOWN("Неизвестно", SecurityLevel.UNKNOWN);
    
    /**
     * Проверяет, является ли сеть безопасной
     */
    fun isSecure(): Boolean = securityLevel != SecurityLevel.NONE
    
    /**
     * Проверяет, является ли сеть открытой
     */
    fun isOpen(): Boolean = this == NONE
}

/**
 * Уровни безопасности для оценки надежности
 */
enum class SecurityLevel {
    NONE,     // Нет защиты
    LOW,      // Низкий уровень защиты
    MEDIUM,   // Средний уровень защиты
    HIGH,     // Высокий уровень защиты
    MAXIMUM,  // Максимальный уровень защиты
    UNKNOWN   // Неизвестный уровень
}