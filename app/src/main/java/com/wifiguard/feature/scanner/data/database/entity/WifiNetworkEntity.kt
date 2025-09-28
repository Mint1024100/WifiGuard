package com.wifiguard.feature.scanner.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.wifiguard.feature.scanner.domain.model.SecurityType

@Entity(tableName = "wifi_networks")
data class WifiNetworkEntity(
    @PrimaryKey
    val bssid: String,
    val ssid: String,
    val capabilities: String,
    val securityType: SecurityType,
    val frequency: Int,
    val channel: Int,
    val bandwidth: String?,
    val isHidden: Boolean,
    val firstSeenTimestamp: Long,
    val lastSeenTimestamp: Long,
    val seenCount: Int = 1,
    val maxSignalStrength: Int,
    val minSignalStrength: Int,
    val avgSignalStrength: Double
)