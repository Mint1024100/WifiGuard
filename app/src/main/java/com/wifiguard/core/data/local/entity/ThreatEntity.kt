package com.wifiguard.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.security.ThreatType

/**
 * Entity для обнаруженных угроз безопасности
 * 
 * КРИТИЧЕСКИЕ ОПТИМИЗАЦИИ:
 * ✅ Индекс на timestamp для быстрой сортировки по времени
 * ✅ Индекс на severity для фильтрации по критичности
 * ✅ Индекс на isResolved для фильтрации активных угроз
 * ✅ Индекс на scanId для связи с сканированиями
 * ✅ Составной индекс severity + isResolved для частых запросов
 * ✅ Индекс на isNotified для отслеживания уведомлений
 * 
 * @author WifiGuard Security Team
 */
@Entity(
    tableName = "threats",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["severity"]),
        Index(value = ["isResolved"]),
        Index(value = ["scanId"]),
        Index(value = ["severity", "isResolved"]),
        Index(value = ["isNotified"]),
        Index(value = ["networkBssid"]),
        Index(value = ["threatType"])
    ]
)
@TypeConverters(DatabaseConverters::class)
data class ThreatEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    /** Ссылка на WifiScanEntity */
    @ColumnInfo(name = "scanId")
    val scanId: Long,
    
    /** Тип угрозы */
    @ColumnInfo(name = "threatType")
    val threatType: ThreatType,
    
    /** Уровень серьёзности угрозы */
    @ColumnInfo(name = "severity")
    val severity: ThreatLevel,
    
    /** Описание угрозы на русском языке */
    @ColumnInfo(name = "description")
    val description: String,
    
    /** SSID сети, в которой обнаружена угроза */
    @ColumnInfo(name = "networkSsid")
    val networkSsid: String,
    
    /** BSSID (MAC-адрес) сети */
    @ColumnInfo(name = "networkBssid")
    val networkBssid: String,
    
    /** Дополнительная информация об угрозе */
    @ColumnInfo(name = "additionalInfo")
    val additionalInfo: String? = null,
    
    /** Временная метка обнаружения угрозы */
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    /** Флаг разрешения угрозы */
    @ColumnInfo(name = "isResolved")
    val isResolved: Boolean = false,
    
    /** Время разрешения угрозы */
    @ColumnInfo(name = "resolutionTimestamp")
    val resolutionTimestamp: Long? = null,
    
    /** Заметка о разрешении угрозы */
    @ColumnInfo(name = "resolutionNote")
    val resolutionNote: String? = null,
    
    /** Флаг отправки уведомления пользователю */
    @ColumnInfo(name = "isNotified")
    val isNotified: Boolean = false
)
