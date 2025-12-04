package com.wifiguard.core.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.wifiguard.core.data.local.converter.DatabaseConverters
import com.wifiguard.core.data.local.dao.ScanSessionDao
import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.dao.WifiNetworkDao
import com.wifiguard.core.data.local.dao.WifiScanDao
import com.wifiguard.core.data.local.entity.ScanSessionEntity
import com.wifiguard.core.data.local.entity.ThreatEntity
import com.wifiguard.core.data.local.entity.WifiNetworkEntity
import com.wifiguard.core.data.local.entity.WifiScanEntity

/**
 * Основная база данных WifiGuard
 */
@Database(
    entities = [
        WifiScanEntity::class,
        WifiNetworkEntity::class,
        ThreatEntity::class,
        ScanSessionEntity::class
    ],
    version = 5,  // Обновленная версия с учетом миграций
    exportSchema = true  // ЭКСПОРТИРОВАТЬ СХЕМУ ДЛЯ ОТСЛЕЖИВАНИЯ ИЗМЕНЕНИЙ
)
@TypeConverters(DatabaseConverters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    abstract fun wifiScanDao(): WifiScanDao
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun threatDao(): ThreatDao
    abstract fun scanSessionDao(): ScanSessionDao
    
    companion object {
        const val DATABASE_NAME = "wifiguard_database"
        
        // ===== МИГРАЦИИ =====
        
        /**
         * Миграция с версии 1 на 2 (пример)
         * Добавление нового поля в таблицу WifiNetworkEntity
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пример: добавление поля vendor
                database.execSQL(
                    "ALTER TABLE wifi_networks ADD COLUMN vendor TEXT"
                )
                
                // Пример: добавление поля channel
                database.execSQL(
                    "ALTER TABLE wifi_networks ADD COLUMN channel INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
        
        /**
         * Миграция с версии 2 на 3 (пример)
         * Добавление нового поля в таблицу ThreatEntity
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Пример: добавление поля resolved_timestamp
                database.execSQL(
                    "ALTER TABLE threats ADD COLUMN resolved_timestamp INTEGER"
                )
            }
        }
        
        /**
         * Миграция с версии 3 на 4
         * Добавление поля isNotified в таблицу threats и создание новой таблицы
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Добавление поля isNotified в таблицу threats
                database.execSQL(
                    "ALTER TABLE threats ADD COLUMN isNotified INTEGER NOT NULL DEFAULT 0"
                )
                
                // Пример: создание таблицы для настроек
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS settings (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        key TEXT NOT NULL UNIQUE,
                        value TEXT,
                        type TEXT NOT NULL DEFAULT 'STRING'
                    )
                    """.trimIndent()
                )
                
                // Создание индекса
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(key)"
                )
            }
        }
        
        /**
         * Migration from version 4 to 5
         * This is a no-op migration for schema verification purposes
         * Version 5 is the current stable schema version
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // No schema changes in this version
                // This migration exists to ensure smooth upgrade path
            }
        }
        
        /**
         * Fallback migration - DELETES ALL DATA AND CREATES NEW DATABASE
         * USE ONLY IN EXTREME CASES!
         * @deprecated This should never be used in production
         */
        @Deprecated("This causes data loss and should not be used in production")
        val DESTRUCTIVE_MIGRATION_FALLBACK = object : Migration(1, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Удаление всех таблиц
                database.execSQL("DROP TABLE IF EXISTS wifi_scans")
                database.execSQL("DROP TABLE IF EXISTS wifi_networks")
                database.execSQL("DROP TABLE IF EXISTS threats")
                database.execSQL("DROP TABLE IF EXISTS scan_sessions")
                database.execSQL("DROP TABLE IF EXISTS settings")
                
                // Таблицы будут пересозданы Room автоматически
            }
        }
        
        /**
         * DEPRECATED: Use Hilt DI to get WifiGuardDatabase instance instead.
         * This companion object method is kept only for migration purposes.
         * 
         * @see com.wifiguard.di.DatabaseModule for the proper DI configuration
         */
        @Deprecated(
            message = "Use Hilt DI injection instead of this method",
            replaceWith = ReplaceWith("Inject WifiGuardDatabase via Hilt")
        )
        @Volatile
        private var INSTANCE: WifiGuardDatabase? = null
        
        /**
         * @deprecated Use Hilt injection instead
         */
        @Deprecated(
            message = "Use Hilt DI injection instead",
            level = DeprecationLevel.WARNING
        )
        fun getDatabase(context: Context): WifiGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiGuardDatabase::class.java,
                    DATABASE_NAME
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4
                    // Add new migrations as needed
                )
                // CRITICAL: DO NOT use fallbackToDestructiveMigration in production!
                // This would cause data loss on database schema changes
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}