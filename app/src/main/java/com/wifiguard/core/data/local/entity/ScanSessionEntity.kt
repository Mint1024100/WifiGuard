package com.wifiguard.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Entity для сессий сканирования
 */
@Entity(tableName = "scan_sessions")
@TypeConverters(DatabaseConverters::class)
data class ScanSessionEntity(
    @PrimaryKey
    val sessionId: String,
    val startTimestamp: Long,
    val endTimestamp: Long? = null,
    val totalNetworks: Int = 0,
    val safeNetworks: Int = 0,
    val lowRiskNetworks: Int = 0,
    val mediumRiskNetworks: Int = 0,
    val highRiskNetworks: Int = 0,
    val criticalRiskNetworks: Int = 0,
    val overallRiskLevel: ThreatLevel = ThreatLevel.UNKNOWN,
    val totalThreats: Int = 0,
    val locationLatitude: Double? = null,
    val locationLongitude: Double? = null,
    val locationAccuracy: Float? = null,
    val isBackgroundScan: Boolean = false
)
