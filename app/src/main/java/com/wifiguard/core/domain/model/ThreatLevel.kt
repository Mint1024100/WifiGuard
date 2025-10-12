package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Уровни угроз безопасности Wi-Fi сетей
 */
@Parcelize
enum class ThreatLevel : Parcelable {
    SAFE,           // Безопасно
    LOW,            // Низкий риск
    MEDIUM,         // Средний риск
    HIGH,           // Высокий риск
    CRITICAL,       // Критический риск
    UNKNOWN;        // Неизвестно
    
    /**
     * Получить описание уровня угрозы
     */
    fun getDescription(): String {
        return when (this) {
            SAFE -> "Безопасная сеть"
            LOW -> "Низкий риск"
            MEDIUM -> "Средний риск"
            HIGH -> "Высокий риск"
            CRITICAL -> "Критический риск"
            UNKNOWN -> "Неизвестный уровень"
        }
    }
    
    /**
     * Получить числовое значение уровня угрозы (1-5)
     */
    fun getNumericValue(): Int {
        return when (this) {
            SAFE -> 1
            LOW -> 2
            MEDIUM -> 3
            HIGH -> 4
            CRITICAL -> 5
            UNKNOWN -> 0
        }
    }
    
    /**
     * Получить цвет для отображения уровня угрозы
     */
    fun getColorHex(): String {
        return when (this) {
            SAFE -> "#4CAF50"      // Зеленый
            LOW -> "#8BC34A"       // Светло-зеленый
            MEDIUM -> "#FF9800"    // Оранжевый
            HIGH -> "#FF5722"      // Красно-оранжевый
            CRITICAL -> "#F44336"  // Красный
            UNKNOWN -> "#9E9E9E"   // Серый
        }
    }
    
    /**
     * Проверить, является ли уровень угрозы критическим
     */
    fun isCritical(): Boolean {
        return this == CRITICAL
    }
    
    /**
     * Проверить, является ли уровень угрозы высоким или критическим
     */
    fun isHighOrCritical(): Boolean {
        return this == HIGH || this == CRITICAL
    }
    
    /**
     * Проверить, является ли уровень угрозы безопасным
     */
    fun isSafe(): Boolean {
        return this == SAFE
    }
    
    /**
     * Получить рекомендацию по безопасности
     */
    fun getRecommendation(): String {
        return when (this) {
            SAFE -> "Сеть безопасна для использования"
            LOW -> "Сеть относительно безопасна, но рекомендуется осторожность"
            MEDIUM -> "Сеть имеет средний уровень риска, избегайте передачи чувствительных данных"
            HIGH -> "Сеть имеет высокий уровень риска, не рекомендуется для использования"
            CRITICAL -> "Сеть крайне опасна, категорически не рекомендуется подключаться"
            UNKNOWN -> "Не удалось определить уровень безопасности сети"
        }
    }
    
    companion object {
        /**
         * Определить уровень угрозы на основе типа безопасности
         */
        fun fromSecurityType(securityType: SecurityType): ThreatLevel {
            return when (securityType) {
                SecurityType.OPEN -> CRITICAL
                SecurityType.WEP -> HIGH
                SecurityType.WPA -> MEDIUM
                SecurityType.WPA2 -> LOW
                SecurityType.WPA3 -> SAFE
                SecurityType.WPA2_WPA3 -> LOW
                SecurityType.EAP -> LOW
                SecurityType.UNKNOWN -> UNKNOWN
            }
        }
        
        /**
         * Определить уровень угрозы на основе множественных факторов
         */
        fun fromMultipleFactors(
            securityType: SecurityType,
            isHidden: Boolean = false,
            hasDuplicateSsid: Boolean = false,
            signalStrength: Int = 0
        ): ThreatLevel {
            var baseLevel = fromSecurityType(securityType)
            
            // Повышаем уровень угрозы для скрытых сетей
            if (isHidden) {
                baseLevel = when (baseLevel) {
                    SAFE -> LOW
                    LOW -> MEDIUM
                    MEDIUM -> HIGH
                    HIGH -> CRITICAL
                    CRITICAL -> CRITICAL
                    UNKNOWN -> MEDIUM
                }
            }
            
            // Повышаем уровень угрозы для дублирующихся SSID
            if (hasDuplicateSsid) {
                baseLevel = when (baseLevel) {
                    SAFE -> LOW
                    LOW -> MEDIUM
                    MEDIUM -> HIGH
                    HIGH -> CRITICAL
                    CRITICAL -> CRITICAL
                    UNKNOWN -> HIGH
                }
            }
            
            // Повышаем уровень угрозы для очень слабого сигнала
            if (signalStrength < -80) {
                baseLevel = when (baseLevel) {
                    SAFE -> LOW
                    LOW -> MEDIUM
                    MEDIUM -> HIGH
                    HIGH -> CRITICAL
                    CRITICAL -> CRITICAL
                    UNKNOWN -> MEDIUM
                }
            }
            
            return baseLevel
        }
    }
}