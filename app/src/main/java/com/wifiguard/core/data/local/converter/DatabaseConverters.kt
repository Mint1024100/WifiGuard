package com.wifiguard.core.data.local.converter

import androidx.room.TypeConverter
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiStandard
import com.wifiguard.core.security.ThreatType

/**
 * Конвертеры для Room Database
 */
class DatabaseConverters {
    
    @TypeConverter
    fun fromSecurityType(securityType: SecurityType): String {
        return securityType.name
    }
    
    @TypeConverter
    fun toSecurityType(securityType: String): SecurityType {
        return SecurityType.valueOf(securityType)
    }
    
    @TypeConverter
    fun fromThreatLevel(threatLevel: ThreatLevel): String {
        return threatLevel.name
    }
    
    @TypeConverter
    fun toThreatLevel(threatLevel: String): ThreatLevel {
        return ThreatLevel.valueOf(threatLevel)
    }
    
    @TypeConverter
    fun fromWifiStandard(standard: WifiStandard): String {
        return standard.name
    }
    
    @TypeConverter
    fun toWifiStandard(standard: String): WifiStandard {
        return WifiStandard.valueOf(standard)
    }
    
    @TypeConverter
    fun fromThreatType(threatType: ThreatType): String {
        return threatType.name
    }
    
    @TypeConverter
    fun toThreatType(threatType: String): ThreatType {
        return ThreatType.valueOf(threatType)
    }
    
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotEmpty() }
    }
    
    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }
    
    @TypeConverter
    fun toLongList(value: String?): List<Long>? {
        return value?.split(",")?.mapNotNull { it.toLongOrNull() }
    }
}