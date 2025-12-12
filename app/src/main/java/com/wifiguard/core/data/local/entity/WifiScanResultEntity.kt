package com.wifiguard.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity для хранения результатов сканирования Wi-Fi.
 * 
 * Хранит исторические данные о каждом сканировании,
 * что позволяет анализировать тренды и изменения.
 */
@Entity(
    tableName = "wifi_scan_results",
    foreignKeys = [
        ForeignKey(
            entity = WifiNetworkEntity::class,
            parentColumns = ["id"],
            childColumns = ["network_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["network_id"]),
        Index(value = ["scan_timestamp"]),
        Index(value = ["location_latitude", "location_longitude"]),
        Index(value = ["signal_strength"])
    ]
)
data class WifiScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    /**
     * ID связанной сети (Foreign Key)
     */
    @ColumnInfo(name = "network_id")
    val networkId: Long,
    
    /**
     * Временная метка сканирования
     */
    @ColumnInfo(name = "scan_timestamp")
    val scanTimestamp: Long,
    
    /**
     * Мощность сигнала во время сканирования
     */
    @ColumnInfo(name = "signal_strength")
    val signalStrength: Int,
    
    /**
     * Широта местоположения обнаружения
     */
    @ColumnInfo(name = "location_latitude")
    val locationLatitude: Double? = null,
    
    /**
     * Долгота местоположения обнаружения
     */
    @ColumnInfo(name = "location_longitude")
    val locationLongitude: Double? = null,
    
    /**
     * Точность местоположения в метрах
     */
    @ColumnInfo(name = "location_accuracy")
    val locationAccuracy: Float? = null,
    
    /**
     * Дополнительные данные сканирования
     */
    @ColumnInfo(name = "additional_data")
    val additionalData: String? = null,
    
    /**
     * ID сессии сканирования (для группировки)
     */
    @ColumnInfo(name = "scan_session_id")
    val scanSessionId: String? = null,
    
    /**
     * Признак ручного сканирования
     */
    @ColumnInfo(name = "scan_type")
    val scanType: com.wifiguard.core.domain.model.ScanType = com.wifiguard.core.domain.model.ScanType.MANUAL,
    
    /**
     * Источник данных сканирования (ACTIVE_SCAN, SYSTEM_CACHE, UNKNOWN)
     */
    @ColumnInfo(name = "scan_source")
    val scanSource: com.wifiguard.core.domain.model.ScanSource = com.wifiguard.core.domain.model.ScanSource.UNKNOWN,
    
    /**
     * Свежесть данных (FRESH, STALE, EXPIRED)
     */
    @ColumnInfo(name = "data_freshness")
    val dataFreshness: com.wifiguard.core.domain.model.Freshness = com.wifiguard.core.domain.model.Freshness.UNKNOWN
)