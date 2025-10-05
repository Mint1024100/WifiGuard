package com.wifiguard.core.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanResultEntity

/**
 * Основная Room база данных для WifiGuard приложения.
 * Обеспечивает хранение данных о Wi-Fi сетях и результатах сканирования.
 */
@Database(
    entities = [
        WifiNetworkEntity::class,
        WifiScanResultEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    /**
     * Получить DAO для Wi-Fi сетей
     */
    abstract fun wifiNetworkDao(): WifiNetworkDao
    
    /**
     * Получить DAO для результатов сканирования
     */
    abstract fun wifiScanDao(): WifiScanDao
    
    companion object {
        const val DATABASE_NAME = "wifi_guard_database"
        
        /**
         * Миграции базы данных (при необходимости в будущем)
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пример миграции - добавление нового столбца
                // database.execSQL("ALTER TABLE wifi_networks ADD COLUMN new_column TEXT DEFAULT ''")
            }
        }
    }
}

/**
 * Класс для преобразования типов данных в Room.
 * Используется для преобразования сложных типов в примитивные.
 */
class Converters {
    
    /**
     * Преобразование списка строк в JSON строку
     */
    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }
    
    /**
     * Преобразование JSON строки в список строк
     */
    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.filter { it.isNotBlank() }
    }
    
    /**
     * Преобразование nullable Double в строку
     */
    @TypeConverter
    fun fromNullableDouble(value: Double?): String? {
        return value?.toString()
    }
    
    /**
     * Преобразование строки в nullable Double
     */
    @TypeConverter
    fun toNullableDouble(value: String?): Double? {
        return value?.toDoubleOrNull()
    }
}