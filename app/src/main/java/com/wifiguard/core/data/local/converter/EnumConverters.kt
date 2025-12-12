package com.wifiguard.core.data.local.converter

import androidx.room.TypeConverter
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel

/**
 * Конвертеры для перечислений, чтобы Room мог сохранять их в базе данных.
 */
class EnumConverters {
    
    // ThreatLevel converters
    @TypeConverter
    fun fromThreatLevel(threatLevel: ThreatLevel): String {
        return threatLevel.name
    }
    
    @TypeConverter
    fun toThreatLevel(value: String): ThreatLevel {
        return ThreatLevel.valueOf(value)
    }
    
    // SecurityType converters
    @TypeConverter
    fun fromSecurityType(securityType: SecurityType): String {
        return securityType.name
    }
    
    @TypeConverter
    fun toSecurityType(value: String): SecurityType {
        return SecurityType.valueOf(value)
    }
}