package com.wifiguard.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wifiguard.core.domain.model.WifiNetwork
import com.wifiguard.core.domain.model.SecurityType

/**
 * Room Entity для хранения информации о Wi-Fi сетях.
 * Представляет структуру таблицы базы данных для сетей.
 */
@Entity(
    tableName = "wifi_networks"
)
data class WifiNetworkEntity(
    @PrimaryKey
    @ColumnInfo(name = "ssid")
    val ssid: String,
    
    @ColumnInfo(name = "bssid")
    val bssid: String,
    
    @ColumnInfo(name = "security_type")
    val securityType: String,
    
    @ColumnInfo(name = "signal_strength")
    val signalStrength: Int,
    
    @ColumnInfo(name = "frequency")
    val frequency: Int,
    
    @ColumnInfo(name = "channel")
    val channel: Int,
    
    @ColumnInfo(name = "vendor")
    val vendor: String?,
    
    @ColumnInfo(name = "first_seen")
    val firstSeen: Long,
    
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,
    
    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long,
    
    @ColumnInfo(name = "is_suspicious")
    val isSuspicious: Boolean = false,
    
    @ColumnInfo(name = "suspicious_reason")
    val suspiciousReason: String? = null,
    
    @ColumnInfo(name = "connection_count")
    val connectionCount: Int = 0,
    
    @ColumnInfo(name = "is_known")
    val isKnown: Boolean = false,
    
    @ColumnInfo(name = "trust_level")
    val trustLevel: String = "UNKNOWN"
)

/**
 * Преобразование Entity в доменную модель
 */
fun WifiNetworkEntity.toDomainModel(): WifiNetwork {
    return WifiNetwork(
        ssid = ssid,
        bssid = bssid,
        securityType = SecurityType.valueOf(securityType),
        signalStrength = signalStrength,
        frequency = frequency,
        channel = channel,
        vendor = vendor,
        firstSeen = firstSeen,
        lastSeen = lastSeen,
        lastUpdated = lastUpdated,
        isSuspicious = isSuspicious,
        suspiciousReason = suspiciousReason,
        connectionCount = connectionCount,
        isKnown = isKnown,
        trustLevel = WifiNetwork.TrustLevel.valueOf(trustLevel)
    )
}

/**
 * Преобразование доменной модели в Entity
 */
fun WifiNetwork.toEntity(): WifiNetworkEntity {
    return WifiNetworkEntity(
        ssid = ssid,
        bssid = bssid,
        securityType = securityType.name,
        signalStrength = signalStrength,
        frequency = frequency,
        channel = channel,
        vendor = vendor,
        firstSeen = firstSeen,
        lastSeen = lastSeen,
        lastUpdated = lastUpdated,
        isSuspicious = isSuspicious,
        suspiciousReason = suspiciousReason,
        connectionCount = connectionCount,
        isKnown = isKnown,
        trustLevel = trustLevel.name
    )
}