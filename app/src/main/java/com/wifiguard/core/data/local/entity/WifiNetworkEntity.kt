package com.wifiguard.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Entity для хранения информации о Wi-Fi сетях в базе данных.
 * 
 * Основная сущность для сохранения данных о обнаруженных
 * Wi-Fi сетях, их характеристиках и уровне безопасности.
 */
@Entity(
    tableName = "wifi_networks",
    indices = [
        Index(value = ["bssid"], unique = true),
        Index(value = ["ssid"]),
        Index(value = ["first_seen"]),
        Index(value = ["last_seen"]),
        Index(value = ["threat_level"])
    ]
)
data class WifiNetworkEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,
    
    /**
     * BSSID (MAC-адрес) точки доступа - уникальный идентификатор
     */
    @ColumnInfo(name = "bssid")
    val bssid: String,
    
    /**
     * SSID (имя) сети
     */
    @ColumnInfo(name = "ssid")
    val ssid: String?,
    
    /**
     * Частота сигнала в МГц
     */
    @ColumnInfo(name = "frequency")
    val frequency: Int,
    
    /**
     * Мощность сигнала в dBm
     */
    @ColumnInfo(name = "signal_strength")
    val signalStrength: Int,
    
    /**
     * Тип шифрования/безопасности
     */
    @ColumnInfo(name = "security_type")
    val securityType: SecurityType,
    
    /**
     * Канал Wi-Fi
     */
    @ColumnInfo(name = "channel")
    val channel: Int,
    
    /**
     * Уровень угрозы сети
     */
    @ColumnInfo(name = "threat_level")
    val threatLevel: ThreatLevel,
    
    /**
     * Признак скрытости сети
     */
    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false,
    
    /**
     * Время первого обнаружения (в миллисекундах)
     */
    @ColumnInfo(name = "first_seen")
    val firstSeen: Long,
    
    /**
     * Время последнего обнаружения (в миллисекундах)
     */
    @ColumnInfo(name = "last_seen")
    val lastSeen: Long,
    
    /**
     * Количество обнаружений сети
     */
    @ColumnInfo(name = "detection_count")
    val detectionCount: Int = 1,
    
    /**
     * Признак подозрительности сети
     */
    @ColumnInfo(name = "is_suspicious")
    val isSuspicious: Boolean = false,
    
    /**
     * Производитель устройства (определяется по MAC)
     */
    @ColumnInfo(name = "vendor")
    val vendor: String? = null,
    
    /**
     * Описание обнаруженных угроз/проблем
     */
    @ColumnInfo(name = "notes")
    val notes: String? = null
)