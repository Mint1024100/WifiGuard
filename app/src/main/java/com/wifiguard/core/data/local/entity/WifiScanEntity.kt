package com.wifiguard.core.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiStandard

/**
 * Entity для истории сканирований Wi-Fi сетей
 */
@Entity(
    tableName = "wifi_scans",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["bssid"]),
        Index(value = ["ssid"]),
        Index(value = ["scanSessionId"]),
        Index(value = ["threatLevel"]),
        Index(value = ["securityType"]),
        Index(value = ["isConnected"])
    ]
)
@TypeConverters(DatabaseConverters::class)
data class WifiScanEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val ssid: String,
    val bssid: String,
    val capabilities: String,
    val frequency: Int,
    val level: Int, // RSSI в dBm
    val timestamp: Long,
    val securityType: SecurityType,
    val threatLevel: ThreatLevel,
    val isConnected: Boolean = false,
    val isHidden: Boolean = false,
    val vendor: String? = null,
    val channel: Int = 0,
    val standard: WifiStandard = WifiStandard.UNKNOWN,
    val scanType: com.wifiguard.core.domain.model.ScanType = com.wifiguard.core.domain.model.ScanType.MANUAL,
    val securityScore: Int = 0,
    val scanSessionId: String? = null // Для группировки сканирований
)
