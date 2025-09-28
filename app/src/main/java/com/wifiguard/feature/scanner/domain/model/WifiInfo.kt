package com.wifiguard.feature.scanner.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class WifiInfo(
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val level: Int,
    val frequency: Int,
    val timestamp: Long,
    val securityType: SecurityType,
    val signalStrength: Int,
    val channel: Int,
    val bandwidth: String?,
    val isHidden: Boolean = false
) : Parcelable {
    
    val signalQuality: SignalQuality
        get() = when {
            signalStrength >= -50 -> SignalQuality.EXCELLENT
            signalStrength >= -60 -> SignalQuality.GOOD
            signalStrength >= -70 -> SignalQuality.FAIR
            else -> SignalQuality.POOR
        }
    
    val isSecure: Boolean
        get() = securityType != SecurityType.OPEN
}

enum class SecurityType {
    OPEN,
    WEP,
    WPA,
    WPA2,
    WPA3,
    UNKNOWN;
    
    companion object {
        fun fromCapabilities(capabilities: String): SecurityType {
            return when {
                capabilities.contains("WPA3") -> WPA3
                capabilities.contains("WPA2") -> WPA2
                capabilities.contains("WPA") && !capabilities.contains("WPA2") -> WPA
                capabilities.contains("WEP") -> WEP
                capabilities.isEmpty() || capabilities.contains("[ESS]") -> OPEN
                else -> UNKNOWN
            }
        }
    }
}

enum class SignalQuality {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR
}