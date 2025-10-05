package com.wifiguard.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index
import com.wifiguard.core.domain.model.WifiScanResult

/**
 * Room Entity для хранения результатов сканирования Wi-Fi сетей.
 * Используется для хранения истории сканирования и аналитики.
 */
@Entity(
    tableName = "wifi_scan_results",
    indices = [
        Index(value = ["ssid"]),
        Index(value = ["timestamp"]),
        Index(value = ["ssid", "timestamp"])
    ]
)
data class WifiScanResultEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    @ColumnInfo(name = "ssid")
    val ssid: String,
    
    @ColumnInfo(name = "bssid")
    val bssid: String,
    
    @ColumnInfo(name = "signal_strength")
    val signalStrength: Int,
    
    @ColumnInfo(name = "frequency")
    val frequency: Int,
    
    @ColumnInfo(name = "channel")
    val channel: Int,
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long,
    
    @ColumnInfo(name = "location_latitude")
    val locationLatitude: Double? = null,
    
    @ColumnInfo(name = "location_longitude")
    val locationLongitude: Double? = null,
    
    @ColumnInfo(name = "scan_type")
    val scanType: String = "MANUAL", // MANUAL, AUTOMATIC, BACKGROUND
    
    @ColumnInfo(name = "security_type")
    val securityType: String? = null,
    
    @ColumnInfo(name = "vendor")
    val vendor: String? = null
)

/**
 * Преобразование Entity в доменную модель
 */
fun WifiScanResultEntity.toDomainModel(): WifiScanResult {
    return WifiScanResult(
        id = id,
        ssid = ssid,
        bssid = bssid,
        signalStrength = signalStrength,
        frequency = frequency,
        channel = channel,
        timestamp = timestamp,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        scanType = WifiScanResult.ScanType.valueOf(scanType),
        securityType = securityType,
        vendor = vendor
    )
}

/**
 * Преобразование доменной модели в Entity
 */
fun WifiScanResult.toEntity(): WifiScanResultEntity {
    return WifiScanResultEntity(
        id = id,
        ssid = ssid,
        bssid = bssid,
        signalStrength = signalStrength,
        frequency = frequency,
        channel = channel,
        timestamp = timestamp,
        locationLatitude = locationLatitude,
        locationLongitude = locationLongitude,
        scanType = scanType.name,
        securityType = securityType,
        vendor = vendor
    )
}