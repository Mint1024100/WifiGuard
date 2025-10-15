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
    version = 4,  // Обновленная версия с учетом миграций
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
         * Миграция с версии 3 на 4 (пример)
         * Создание новой таблицы
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
         * Фолбэк миграция - УДАЛЯЕТ ВСЕ ДАННЫЕ И СОЗДАЕТ НОВУЮ БАЗУ
         * ИСПОЛЬЗОВАТЬ ТОЛЬКО В КРАЙНЕМ СЛУЧАЕ!
         */
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
        
        @Volatile
        private var INSTANCE: WifiGuardDatabase? = null
        
        fun getDatabase(context: Context): WifiGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiGuardDatabase::class.java,
                    DATABASE_NAME
                )
                // Добавляем миграции в порядке возрастания версий
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4
                    // Добавляйте новые миграции по мере необходимости
                )
                // ВАЖНО: Не использовать fallbackToDestructiveMigration в production!
                // .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}