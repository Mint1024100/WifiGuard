package com.wifiguard.core.domain.model

/**
 * Доменная модель результата сканирования Wi-Fi.
 * Представляет единичный результат сканирования для хранения в истории.
 */
data class WifiScanResult(
    /**
     * Идентификатор сети (SSID)
     */
    val ssid: String,
    
    /**
     * MAC-адрес точки доступа (BSSID)
     */
    val bssid: String,
    
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
     * Время сканирования (timestamp в миллисекундах)
     */
    val timestamp: Long,
    
    /**
     * Координаты местоположения (широта)
     */
    val locationLatitude: Double? = null,
    
    /**
     * Координаты местоположения (долгота)
     */
    val locationLongitude: Double? = null,
    
    /**
     * Тип сканирования
     */
    val scanType: ScanType = ScanType.MANUAL,
    
    /**
     * Тип безопасности (опционально)
     */
    val securityType: SecurityType? = null,
    
    /**
     * Производитель устройства
     */
    val vendor: String? = null
) {
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
 * Тип сканирования Wi-Fi
 */
enum class ScanType {
    /**
     * Ручное сканирование (инициировано пользователем)
     */
    MANUAL,
    
    /**
     * Автоматическое сканирование (по расписанию)
     */
    AUTOMATIC,
    
    /**
     * Фоновое сканирование (WorkManager)
     */
    BACKGROUND
}

