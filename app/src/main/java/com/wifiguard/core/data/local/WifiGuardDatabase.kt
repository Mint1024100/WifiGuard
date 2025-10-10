package com.wifiguard.core.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanResultEntity
import com.wifiguard.core.data.local.converter.DatabaseConverters

/**
 * Основная база данных WifiGuard с использованием Room.
 * 
 * Содержит:
 * - Информацию о Wi-Fi сетях
 * - Результаты сканирования
 * - Настройки безопасности
 * 
 * @version 1 - начальная схема базы данных
 */
@Database(
    entities = [
        WifiNetworkEntity::class,
        WifiScanResultEntity::class
    ],
    version = 1,
    exportSchema = false // В продакшне установить true и создать schemas/
)
@TypeConverters(DatabaseConverters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    /**
     * DAO для работы с Wi-Fi сетями
     */
    abstract fun wifiNetworkDao(): WifiNetworkDao
    
    /**
     * DAO для работы с результатами сканирования
     */
    abstract fun wifiScanDao(): WifiScanDao
    
    companion object {
        /**
         * Имя файла базы данных
         */
        const val DATABASE_NAME = "wifi_guard_database"
        
        /**
         * Версия базы данных
         */
        const val DATABASE_VERSION = 1
        
        /**
         * Удобный метод для создания экземпляра базы данных
         * (для тестов и debug-сборок)
         */
        fun create(context: Context): WifiGuardDatabase {
            return Room.databaseBuilder(
                context = context,
                klass = WifiGuardDatabase::class.java,
                name = DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}