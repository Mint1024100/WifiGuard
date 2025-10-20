package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Типы безопасности Wi-Fi сетей
 */
@Parcelize
@Serializable
enum class SecurityType : Parcelable {
    OPEN,           // Открытая сеть
    WEP,            // WEP (устаревший, небезопасный)
    WPA,            // WPA (устаревший)
    WPA2,           // WPA2 (рекомендуемый)
    WPA3,           // WPA3 (новейший)
    WPA2_WPA3,      // WPA2/WPA3 переходный режим
    EAP,            // EAP (корпоративные сети)
    UNKNOWN;        // Неизвестный тип
    
    /**
     * Получить описание типа безопасности
     */
    fun getDescription(): String {
        return when (this) {
            OPEN -> "Открытая сеть (без шифрования)"
            WEP -> "WEP (устаревший, небезопасный)"
            WPA -> "WPA (устаревший)"
            WPA2 -> "WPA2 (безопасный)"
            WPA3 -> "WPA3 (наиболее безопасный)"
            WPA2_WPA3 -> "WPA2/WPA3 (переходный режим)"
            EAP -> "EAP (корпоративная сеть)"
            UNKNOWN -> "Неизвестный тип"
        }
    }
    
    /**
     * Получить уровень безопасности (1-5, где 5 - самый безопасный)
     */
    fun getSecurityLevel(): Int {
        return when (this) {
            OPEN -> 1
            WEP -> 2
            WPA -> 3
            WPA2 -> 4
            WPA3 -> 5
            WPA2_WPA3 -> 4
            EAP -> 4
            UNKNOWN -> 1
        }
    }
    
    /**
     * Проверить, является ли тип безопасности устаревшим
     */
    fun isDeprecated(): Boolean {
        return this == WEP || this == WPA
    }
    
    /**
     * Проверить, является ли тип безопасности рекомендуемым
     */
    fun isRecommended(): Boolean {
        return this == WPA2 || this == WPA3 || this == WPA2_WPA3
    }
    
    /**
     * Проверить, является ли сеть небезопасной
     */
    fun isInsecure(): Boolean {
        return this == OPEN || this == WEP
    }
    
    companion object {
        /**
         * Определить тип безопасности по capabilities строке
         */
        fun fromCapabilities(capabilities: String): SecurityType {
            return when {
                capabilities.isEmpty() || capabilities.contains("OPEN") -> OPEN
                capabilities.contains("WEP") -> WEP
                capabilities.contains("WPA3") && capabilities.contains("WPA2") -> WPA2_WPA3
                capabilities.contains("WPA3") -> WPA3
                capabilities.contains("WPA2") -> WPA2
                capabilities.contains("WPA") -> WPA
                capabilities.contains("EAP") -> EAP
                else -> UNKNOWN
            }
        }
    }
}

/**
 * Проверить, является ли тип безопасности безопасным
 */
fun SecurityType.isSecure(): Boolean {
    return when (this) {
        SecurityType.WPA2, SecurityType.WPA3, SecurityType.WPA2_WPA3, SecurityType.EAP -> true
        else -> false
    }
}

/**
 * Проверить, является ли сеть открытой
 */
fun SecurityType.isOpen(): Boolean {
    return this == SecurityType.OPEN
}