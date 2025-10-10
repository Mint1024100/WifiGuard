package com.wifiguard.core.data.local.converter

import androidx.room.TypeConverter
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Конвертеры типов для Room базы данных.
 * 
 * Преобразует сложные типы данных (enums, объекты) в примитивы,
 * которые могут быть сохранены в SQLite.
 */
class DatabaseConverters {
    
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // SecurityType конвертеры
    @TypeConverter
    fun fromSecurityType(securityType: SecurityType): String {
        return securityType.name
    }
    
    @TypeConverter
    fun toSecurityType(value: String): SecurityType {
        return try {
            SecurityType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            SecurityType.UNKNOWN
        }
    }
    
    // ThreatLevel конвертеры
    @TypeConverter
    fun fromThreatLevel(threatLevel: ThreatLevel): String {
        return threatLevel.name
    }
    
    @TypeConverter
    fun toThreatLevel(value: String): ThreatLevel {
        return try {
            ThreatLevel.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ThreatLevel.UNKNOWN
        }
    }
    
    // String List конвертеры (для списков тегов, капабилитиес и т.д.)
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }
    
    // Map<String, String> конвертеры (для метаданных)
    @TypeConverter
    fun fromStringMap(value: Map<String, String>?): String? {
        return value?.let { json.encodeToString(it) }
    }
    
    @TypeConverter
    fun toStringMap(value: String?): Map<String, String>? {
        return value?.let {
            try {
                json.decodeFromString(it)
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }
    
    // Double конвертеры (для nullable валю)
    @TypeConverter
    fun fromNullableDouble(value: Double?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toNullableDouble(value: String?): Double? {
        return value?.toDoubleOrNull()
    }
    
    // Float конвертеры (для nullable валю)
    @TypeConverter
    fun fromNullableFloat(value: Float?): String? {
        return value?.toString()
    }
    
    @TypeConverter
    fun toNullableFloat(value: String?): Float? {
        return value?.toFloatOrNull()
    }
}