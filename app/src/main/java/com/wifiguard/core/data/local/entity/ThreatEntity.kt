package com.wifiguard.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.ThreatType

/**
 * Entity для обнаруженных угроз безопасности
 */
@Entity(tableName = "threats")
@TypeConverters(DatabaseConverters::class)
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scanId: Long, // Ссылка на WifiScanEntity
    val threatType: ThreatType,
    val severity: ThreatLevel,
    val description: String,
    val networkSsid: String,
    val networkBssid: String,
    val additionalInfo: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isResolved: Boolean = false,
    val resolutionTimestamp: Long? = null,
    val resolutionNote: String? = null,
    val isNotified: Boolean = false // Поле для отслеживания уведомлений
)
