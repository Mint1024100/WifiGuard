package com.wifiguard.core.domain.model

/**
 * Доменная модель Wi-Fi сети.
 * Представляет информацию о Wi-Fi сети для использования в бизнес-логике.
 */
data class WifiNetwork(
    /**
     * Идентификатор сети (SSID)
     */
    val ssid: String,
    
    /**
     * MAC-адрес точки доступа (BSSID)
     */
    val bssid: String,
    
    /**
     * Тип безопасности/шифрования
     */
    val securityType: SecurityType,
    
    /**
     * Уровень сигнала в dBm
     */
    val signalStrength: Int,
    
    /**
     * Частота в МГц
     */
    val frequency: Int,
    
    /**
     * Номер канала
     */
    val channel: Int,
    
    /**
     * Производитель устройства (определяется по MAC-адресу)
     */
    val vendor: String? = null,
    
    /**
     * Время первого обнаружения (timestamp в миллисекундах)
     */
    val firstSeen: Long,
    
    /**
     * Время последнего обнаружения (timestamp в миллисекундах)
     */
    val lastSeen: Long,
    
    /**
     * Время последнего обновления
     */
    val lastUpdated: Long = System.currentTimeMillis(),
    
    /**
     * Флаг подозрительной сети
     */
    val isSuspicious: Boolean = false,
    
    /**
     * Причина подозрения
     */
    val suspiciousReason: String? = null,
    
    /**
     * Количество подключений к этой сети
     */
    val connectionCount: Int = 0,
    
    /**
     * Флаг известной сети (пользователь подключался ранее)
     */
    val isKnown: Boolean = false,
    
    /**
     * Уровень доверия к сети
     */
    val trustLevel: TrustLevel = TrustLevel.UNKNOWN
) {
    /**
     * Проверяет, является ли сеть безопасной
     */
    val isSecure: Boolean
        get() = securityType.isSecure()
    
    /**
     * Проверяет, является ли сеть открытой
     */
    val isOpen: Boolean
        get() = securityType.isOpen()
    
    /**
     * Возвращает качество сигнала
     */
    val signalQuality: SignalQuality
        get() = when {
            signalStrength >= -50 -> SignalQuality.EXCELLENT
            signalStrength >= -60 -> SignalQuality.GOOD
            signalStrength >= -70 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    
    /**
     * Определяет диапазон частот
     */
    val frequencyBand: String
        get() = when {
            frequency in 2400..2500 -> "2.4 ГГц"
            frequency in 5000..6000 -> "5 ГГц"
            frequency in 6000..7125 -> "6 ГГц"
            else -> "Неизвестно"
        }
}

/**
 * Уровень доверия к сети
 */
enum class TrustLevel {
    TRUSTED,    // Доверенная сеть
    KNOWN,      // Известная сеть
    UNKNOWN,    // Неизвестная сеть
    SUSPICIOUS  // Подозрительная сеть
}

/**
 * Качество сигнала
 */
enum class SignalQuality {
    EXCELLENT,  // Отлично (-50 dBm и выше)
    GOOD,       // Хорошо (-50 до -60 dBm)
    FAIR,       // Удовлетворительно (-60 до -70 dBm)
    POOR        // Плохо (ниже -70 dBm)
}

