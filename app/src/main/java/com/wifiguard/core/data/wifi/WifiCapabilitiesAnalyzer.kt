package com.wifiguard.core.data.wifi

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Анализатор возможностей Wi-Fi сетей
 */
@Singleton
class WifiCapabilitiesAnalyzer @Inject constructor() {
    
    /**
     * Получить канал по частоте
     */
    fun getChannelFromFrequency(frequency: Int): Int {
        return when {
            frequency in 2412..2484 -> (frequency - 2412) / 5 + 1
            frequency in 5170..5825 -> (frequency - 5000) / 5
            frequency in 5925..7125 -> (frequency - 5925) / 5 + 1
            else -> 0
        }
    }
    
    /**
     * Получить производителя по BSSID
     */
    fun getVendorFromBssid(bssid: String?): String? {
        if (bssid == null) return null
        
        val macPrefix = bssid.substring(0, 8).replace(":", "").uppercase()
        return VENDOR_DATABASE[macPrefix]
    }
    
    /**
     * Анализировать возможности сети
     */
    fun analyzeCapabilities(capabilities: String): NetworkCapabilities {
        return NetworkCapabilities(
            hasWpa3 = capabilities.contains("WPA3"),
            hasWpa2 = capabilities.contains("WPA2"),
            hasWpa = capabilities.contains("WPA"),
            hasWep = capabilities.contains("WEP"),
            hasEap = capabilities.contains("EAP"),
            hasPsk = capabilities.contains("PSK"),
            hasEnterprise = capabilities.contains("EAP"),
            isOpen = capabilities.isEmpty() || capabilities.contains("OPEN"),
            supports80211n = capabilities.contains("[WPA2-PSK-CCMP]") || capabilities.contains("[WPA2-PSK-TKIP]"),
            supports80211ac = capabilities.contains("RSN") && capabilities.contains("CCMP"),
            supports80211ax = capabilities.contains("WPA3")
        )
    }
    
    companion object {
        private val VENDOR_DATABASE = mapOf(
            "001122" to "Unknown",
            "001E58" to "Cisco",
            "001F5B" to "Apple",
            "0022FB" to "Google",
            "0026B9" to "TP-Link",
            "0026F2" to "Netgear",
            "0026F3" to "Netgear",
            "0026F4" to "Netgear",
            "0026F5" to "Netgear",
            "0026F6" to "Netgear",
            "0026F7" to "Netgear",
            "0026F8" to "Netgear",
            "0026F9" to "Netgear",
            "0026FA" to "Netgear",
            "0026FB" to "Netgear",
            "0026FC" to "Netgear",
            "0026FD" to "Netgear",
            "0026FE" to "Netgear",
            "0026FF" to "Netgear",
            "001A70" to "Cisco",
            "001A71" to "Cisco",
            "001A72" to "Cisco",
            "001A73" to "Cisco",
            "001A74" to "Cisco",
            "001A75" to "Cisco",
            "001A76" to "Cisco",
            "001A77" to "Cisco",
            "001A78" to "Cisco",
            "001A79" to "Cisco",
            "001A7A" to "Cisco",
            "001A7B" to "Cisco",
            "001A7C" to "Cisco",
            "001A7D" to "Cisco",
            "001A7E" to "Cisco",
            "001A7F" to "Cisco"
        )
    }
}

/**
 * Возможности сети
 */
data class NetworkCapabilities(
    val hasWpa3: Boolean = false,
    val hasWpa2: Boolean = false,
    val hasWpa: Boolean = false,
    val hasWep: Boolean = false,
    val hasEap: Boolean = false,
    val hasPsk: Boolean = false,
    val hasEnterprise: Boolean = false,
    val isOpen: Boolean = false,
    val supports80211n: Boolean = false,
    val supports80211ac: Boolean = false,
    val supports80211ax: Boolean = false
)
