package com.wifiguard.feature.scanner.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "scan_history",
    foreignKeys = [
        ForeignKey(
            entity = WifiNetworkEntity::class,
            parentColumns = ["bssid"],
            childColumns = ["networkBssid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val networkBssid: String,
    val signalStrength: Int,
    val timestamp: Long,
    val scanSessionId: String
)