package com.wifiguard.core.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Результат сканирования Wi-Fi сети
 */
@Parcelize
data class WifiScanResult(
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val frequency: Int,
    val level: Int, // RSSI в dBm
    val timestamp: Long = System.currentTimeMillis(),
    val securityType: SecurityType = SecurityType.UNKNOWN,
    val threatLevel: ThreatLevel = ThreatLevel.UNKNOWN,
    val isConnected: Boolean = false,
    val isHidden: Boolean = false,
    val vendor: String? = null,
    val channel: Int = 0,
    val standard: WifiStandard = WifiStandard.UNKNOWN
) : Parcelable {
    
    /**
     * Получить уровень сигнала в процентах (0-100)
     */
    fun getSignalStrengthPercentage(): Int {
        return when {
            level >= -30 -> 100
            level >= -50 -> 80
            level >= -60 -> 60
            level >= -70 -> 40
            level >= -80 -> 20
            else -> 0
        }
    }
    
    /**
     * Получить описание уровня сигнала
     */
    fun getSignalStrengthDescription(): String {
        return when {
            level >= -30 -> "Отличный"
            level >= -50 -> "Хороший"
            level >= -60 -> "Средний"
            level >= -70 -> "Слабый"
            level >= -80 -> "Очень слабый"
            else -> "Критически слабый"
        }
    }
    
    /**
     * Проверить, является ли сеть открытой
     */
    fun isOpenNetwork(): Boolean {
        return capabilities.isEmpty() || 
               capabilities.contains("OPEN") || 
               !capabilities.contains("WPA") && 
               !capabilities.contains("WEP")
    }
    
    /**
     * Получить частоту в МГц
     */
    fun getFrequencyInMhz(): Int {
        return when {
            frequency in 2412..2484 -> frequency
            frequency in 5170..5825 -> frequency
            else -> frequency
        }
    }
    
    /**
     * Получить стандарт Wi-Fi на основе частоты
     */
    fun getWifiStandard(): WifiStandard {
        return when {
            frequency in 2412..2484 -> WifiStandard.WIFI_2_4_GHZ
            frequency in 5170..5825 -> WifiStandard.WIFI_5_GHZ
            frequency in 5925..7125 -> WifiStandard.WIFI_6E
            else -> WifiStandard.UNKNOWN
        }
    }
}

/**
 * Стандарты Wi-Fi
 */
enum class WifiStandard {
    WIFI_2_4_GHZ,   // 2.4 GHz (802.11b/g/n)
    WIFI_5_GHZ,     // 5 GHz (802.11a/n/ac)
    WIFI_6E,        // 6 GHz (802.11ax)
    UNKNOWN
}