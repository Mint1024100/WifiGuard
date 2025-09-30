package com.wifiguard.feature.scanner.domain.model

import com.wifiguard.feature.analyzer.domain.model.SecurityLevel

/**
 * Модель данных для представления информации о WiFi сети.
 * Содержит основные характеристики сети и ее уровень безопасности.
 */
data class WifiInfo(
    /**
     * Имя сети (SSID - Service Set Identifier).
     * Может быть null для скрытых сетей.
     */
    val ssid: String?,
    
    /**
     * MAC-адрес точки доступа (BSSID - Basic Service Set Identifier).
     * Уникальный идентификатор точки доступа.
     */
    val bssid: String,
    
    /**
     * Сила сигнала в dBm (decibel-milliwatts).
     * Обычно в диапазоне от -100 до -30 dBm.
     * Чем ближе к 0, тем сильнее сигнал.
     */
    val signalStrength: Int,
    
    /**
     * Частота работы сети в МГц.
     * Обычно 2400-2500 МГц (2.4 ГГц) или 5000-6000 МГц (5 ГГц).
     */
    val frequency: Int,
    
    /**
     * Тип шифрования сети.
     * Определяет метод защиты сети (открытая, WEP, WPA, WPA2, WPA3).
     */
    val encryptionType: EncryptionType,
    
    /**
     * Уровень безопасности сети.
     * Определяется на основе типа шифрования и других факторов.
     * Null если анализ еще не выполнен.
     */
    val securityLevel: SecurityLevel? = null,
    
    /**
     * Временная метка последнего обнаружения сети.
     * В миллисекундах с 1 января 1970 г. (Unix timestamp).
     */
    val lastSeen: Long = System.currentTimeMillis()
) {
    /**
     * Проверяет, является ли сеть скрытой (без SSID).
     */
    val isHidden: Boolean
        get() = ssid.isNullOrBlank()
    
    /**
     * Получает отображаемое имя сети.
     * Для скрытых сетей возвращает плейсхолдер.
     */
    val displayName: String
        get() = if (isHidden) "Скрытая сеть" else ssid ?: "Неизвестная сеть"
    
    /**
     * Получает диапазон частот сети.
     */
    val frequencyBand: String
        get() = when {
            frequency < 2500 -> "2.4 ГГц"
            frequency < 6000 -> "5 ГГц"
            else -> "6 ГГц"
        }
    
    /**
     * Получает процент силы сигнала (0-100).
     * Преобразует dBm в проценты для удобного отображения.
     */
    val signalPercentage: Int
        get() = when {
            signalStrength >= -30 -> 100
            signalStrength >= -50 -> 75
            signalStrength >= -70 -> 50
            signalStrength >= -90 -> 25
            else -> 0
        }
}