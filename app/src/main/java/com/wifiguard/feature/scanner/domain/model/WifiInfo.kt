package com.wifiguard.feature.scanner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Модель данных для представления информации о Wi-Fi сети
 */
@Parcelize
data class WifiInfo(
    /**
     * Название сети (SSID)
     */
    val ssid: String,
    
    /**
     * MAC-адрес точки доступа (BSSID)
     */
    val bssid: String,
    
    /**
     * Строка возможностей сети (определяет тип шифрования)
     */
    val capabilities: String,
    
    /**
     * Уровень сигнала в dBm (старое поле, совместимость)
     */
    val level: Int,
    
    /**
     * Частота сигнала в МГц
     */
    val frequency: Int,
    
    /**
     * Метка времени последнего обновления
     */
    val timestamp: Long,
    
    /**
     * Тип шифрования сети
     */
    val encryptionType: EncryptionType,
    
    /**
     * Уровень сигнала в dBm (основное поле)
     */
    val signalStrength: Int,
    
    /**
     * Номер канала
     */
    val channel: Int,
    
    /**
     * Полоса пропускания
     */
    val bandwidth: String?,
    
    /**
     * Индикатор скрытой сети
     */
    val isHidden: Boolean = false,
    
    /**
     * Индикатор подключения к данной сети
     */
    val isConnected: Boolean = false,
    
    /**
     * Индикатор сохраненной сети
     */
    val isSaved: Boolean = false
) : Parcelable {
    
    /**
     * Оценка качества сигнала на основе уровня сигнала
     */
    val signalQuality: SignalQuality
        get() = when {
            signalStrength >= -50 -> SignalQuality.EXCELLENT
            signalStrength >= -60 -> SignalQuality.GOOD
            signalStrength >= -70 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    
    /**
     * Проверяет, является ли сеть безопасной
     */
    val isSecure: Boolean
        get() = encryptionType.isSecure()
    
    /**
     * Возвращает уровень сигнала в процентах (0-100%)
     * Основано на стандартной шкале dBm для Wi-Fi
     */
    fun getSignalStrengthPercent(): Int {
        return when {
            signalStrength >= -30 -> 100
            signalStrength >= -50 -> 80
            signalStrength >= -60 -> 60
            signalStrength >= -70 -> 40
            signalStrength >= -80 -> 20
            else -> 0
        }
    }
    
    /**
     * Определяет диапазон частот по частоте
     */
    fun getFrequencyBand(): String {
        return when {
            frequency in 2400..2500 -> "2.4 ГГц"
            frequency in 5000..6000 -> "5 ГГц"
            frequency in 6000..7125 -> "6 ГГц"
            else -> "Неизвестно"
        }
    }
    
    companion object {
        /**
         * Определяет тип шифрования по строке возможностей
         */
        fun parseEncryptionType(capabilities: String): EncryptionType {
            return when {
                capabilities.contains("WPA3") -> EncryptionType.WPA3
                capabilities.contains("WPA2") -> EncryptionType.WPA2
                capabilities.contains("WPA") && !capabilities.contains("WPA2") -> EncryptionType.WPA
                capabilities.contains("WEP") -> EncryptionType.WEP
                capabilities.contains("WPS") -> EncryptionType.WPS
                capabilities.isEmpty() || capabilities.contains("[ESS]") -> EncryptionType.NONE
                else -> EncryptionType.UNKNOWN
            }
        }
    }
}

/**
 * Качество сигнала Wi-Fi сети
 */
enum class SignalQuality {
    EXCELLENT,  // Отлично (-50 dBm и выше)
    GOOD,       // Хорошо (-50 до -60 dBm)
    FAIR,       // Удовлетворительно (-60 до -70 dBm)
    POOR        // Плохо (ниже -70 dBm)
}