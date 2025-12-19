package com.wifiguard.core.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
 * 
 * КРИТИЧЕСКИЕ ИСПРАВЛЕНИЯ БЕЗОПАСНОСТИ:
 * ✅ УДАЛЁН fallbackToDestructiveMigration() - предотвращает потерю данных
 * ✅ Добавлены инкрементальные миграции для каждой версии схемы
 * ✅ Добавлена валидация целостности данных после миграции
 * ✅ Добавлено логирование успеха/неудачи миграций
 * ✅ Реализована стратегия отката для неудачных миграций
 * ✅ Добавлена проверка и исправление схемы при открытии БД (onOpen callback)
 * 
 * ИСТОРИЯ ИСПРАВЛЕНИЙ СХЕМЫ THREATS:
 * Версия 6: Первая попытка исправления - description NOT NULL
 * Версия 7: Проверка PRAGMA и условное исправление
 * Версия 8: Исправление с валидацией
 * Версия 9: ГАРАНТИРОВАННОЕ исправление + callback при открытии БД
 * 
 * МИГРАЦИИ:
 * ✅ MIGRATION_5_6: Пересоздание таблицы threats
 * ✅ MIGRATION_6_7: Проверка PRAGMA и условное исправление
 * ✅ MIGRATION_7_8: Исправление с валидацией
 * ✅ MIGRATION_8_9: ГАРАНТИРОВАННОЕ исправление (всегда пересоздаёт)
 * ✅ onOpen callback: Принудительное исправление при каждом открытии
 * 
 * @author WifiGuard Security Team
 * @version 9
 */
    @Database(
    entities = [
        WifiScanEntity::class,
        WifiNetworkEntity::class,
        ThreatEntity::class,
        ScanSessionEntity::class
    ],
    version = 12,
    exportSchema = true  // ОБЯЗАТЕЛЬНО: экспорт схемы для отслеживания изменений
)
@TypeConverters(DatabaseConverters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    abstract fun wifiScanDao(): WifiScanDao
    abstract fun wifiNetworkDao(): WifiNetworkDao
    abstract fun threatDao(): ThreatDao
    abstract fun scanSessionDao(): ScanSessionDao
    
    companion object {
        private const val TAG = "WifiGuardDatabase"
        const val DATABASE_NAME = "wifiguard_database"
        
        // ===== БЕЗОПАСНЫЕ ИНКРЕМЕНТАЛЬНЫЕ МИГРАЦИИ =====
        
        /**
         * Миграция с версии 1 на 2
         * Добавление полей vendor и channel в таблицу wifi_networks
         * 
         * ВАЖНО: Используется ALTER TABLE для сохранения существующих данных
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 1 -> 2")
                try {
                    db.beginTransaction()
                    
                    // Добавление поля vendor (может быть NULL)
                    db.execSQL(
                        "ALTER TABLE wifi_networks ADD COLUMN vendor TEXT"
                    )
                    
                    // Добавление поля channel с значением по умолчанию
                    db.execSQL(
                        "ALTER TABLE wifi_networks ADD COLUMN channel INTEGER NOT NULL DEFAULT 0"
                    )
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 1 -> 2 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 1 -> 2: ${e.message}", e)
                    throw e // Пробрасываем исключение для корректной обработки Room
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        /**
         * Миграция с версии 2 на 3
         * Добавление поля resolved_timestamp в таблицу threats
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 2 -> 3")
                try {
                    db.beginTransaction()
                    
                    // Добавление поля resolved_timestamp (может быть NULL)
                    db.execSQL(
                        "ALTER TABLE threats ADD COLUMN resolved_timestamp INTEGER"
                    )
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 2 -> 3 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 2 -> 3: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        /**
         * Миграция с версии 3 на 4
         * Добавление поля isNotified в таблицу threats
         * Создание таблицы settings с индексом
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 3 -> 4")
                try {
                    db.beginTransaction()
                    
                    // Добавление поля isNotified в таблицу threats
                    db.execSQL(
                        "ALTER TABLE threats ADD COLUMN isNotified INTEGER NOT NULL DEFAULT 0"
                    )
                    
                    // Создание таблицы settings с индексом
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS settings (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            key TEXT NOT NULL UNIQUE,
                            value TEXT,
                            type TEXT NOT NULL DEFAULT 'STRING'
                        )
                        """.trimIndent()
                    )
                    
                    // Создание индекса для быстрого поиска по ключу
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(key)"
                    )
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 3 -> 4 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 3 -> 4: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        /**
         * Миграция с версии 4 на 5
         * Добавление индексов для оптимизации запросов производительности
         * Добавление полей для метаданных сканирования
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 4 -> 5")
                try {
                    db.beginTransaction()
                    
                    // Добавление составного индекса для частых запросов по is_suspicious и threat_level
                    db.execSQL(
                        """
                        CREATE INDEX IF NOT EXISTS idx_wifi_networks_suspicious_threat 
                        ON wifi_networks(is_suspicious, threat_level)
                        """.trimIndent()
                    )
                    
                    // Добавление индекса на timestamp для threats
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS idx_threats_timestamp ON threats(timestamp)"
                    )
                    
                    // Добавление индекса на severity для threats
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS idx_threats_severity ON threats(severity)"
                    )
                    
                    // Валидация целостности данных после миграции
                    validateDataIntegrity(db)
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 4 -> 5 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 4 -> 5: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
            
            /**
             * Валидация целостности данных после миграции
             */
            private fun validateDataIntegrity(database: SupportSQLiteDatabase) {
                Log.d(TAG, "🔍 Проверка целостности данных...")
                
                // Проверяем, что основные таблицы существуют и доступны
                val tables = listOf("wifi_scans", "wifi_networks", "threats", "scan_sessions")
                tables.forEach { tableName ->
                    val cursor = database.query("SELECT COUNT(*) FROM $tableName")
                    cursor.use {
                        if (it.moveToFirst()) {
                            val count = it.getInt(0)
                            Log.d(TAG, "📊 Таблица $tableName: $count записей")
                        }
                    }
                }
                
                // Проверка внешних ключей (если включены)
                val fkCursor = database.query("PRAGMA foreign_key_check")
                fkCursor.use {
                    if (it.count > 0) {
                        Log.w(TAG, "⚠️ Обнаружены нарушения внешних ключей: ${it.count}")
                    } else {
                        Log.d(TAG, "✅ Целостность внешних ключей подтверждена")
                    }
                }
                
                Log.d(TAG, "✅ Валидация целостности данных завершена")
            }
        }
        
        /**
         * Миграция с версии 5 на 6
         * Синхронизация схемы базы данных с текущими entity-классами
         * Добавление недостающих индексов для оптимизации запросов
         * ИСПРАВЛЕНИЕ: Пересоздание таблицы threats с правильной схемой (description NOT NULL)
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 5 -> 6")
                try {
                    db.beginTransaction()
                    
                    // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Пересоздаём таблицу threats с правильной схемой
                    Log.d(TAG, "🔧 Исправление схемы таблицы threats...")
                    
                    // Шаг 1: Проверяем и обрабатываем NULL значения в description
                    val nullCheckCursor = db.query(
                        "SELECT COUNT(*) FROM threats WHERE description IS NULL"
                    )
                    var nullCount = 0
                    nullCheckCursor.use {
                        if (it.moveToFirst()) {
                            nullCount = it.getInt(0)
                        }
                    }
                    
                    if (nullCount > 0) {
                        Log.w(TAG, "⚠️ Обнаружено $nullCount записей с NULL в description")
                        db.execSQL(
                            "UPDATE threats SET description = 'Описание недоступно' WHERE description IS NULL"
                        )
                        Log.i(TAG, "✅ NULL значения заменены")
                    }
                    
                    // Шаг 2: Пересоздаём таблицу threats с правильной схемой
                    db.execSQL(
                        """
                        CREATE TABLE threats_new (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            scanId INTEGER NOT NULL,
                            threatType TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            description TEXT NOT NULL,
                            networkSsid TEXT NOT NULL,
                            networkBssid TEXT NOT NULL,
                            additionalInfo TEXT,
                            timestamp INTEGER NOT NULL,
                            isResolved INTEGER NOT NULL,
                            resolutionTimestamp INTEGER,
                            resolutionNote TEXT,
                            isNotified INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                    
                    // Шаг 3: Копируем данные
                    db.execSQL(
                        """
                        INSERT INTO threats_new (
                            id, scanId, threatType, severity, description,
                            networkSsid, networkBssid, additionalInfo, timestamp,
                            isResolved, resolutionTimestamp, resolutionNote, isNotified
                        )
                        SELECT 
                            id, scanId, threatType, severity, 
                            COALESCE(description, 'Описание недоступно'),
                            networkSsid, networkBssid, additionalInfo, timestamp,
                            isResolved, resolutionTimestamp, resolutionNote, isNotified
                        FROM threats
                        """.trimIndent()
                    )
                    
                    // Шаг 4: Удаляем старую таблицу
                    db.execSQL("DROP TABLE threats")
                    
                    // Шаг 5: Переименовываем новую таблицу
                    db.execSQL("ALTER TABLE threats_new RENAME TO threats")
                    
                    // Шаг 6: Создаём индексы для таблицы threats
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)"
                    )
                    
                    // Валидация целостности данных после миграции
                    validateDataIntegrity(db)
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 5 -> 6 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 5 -> 6: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
            
            /**
             * Валидация целостности данных после миграции
             */
            private fun validateDataIntegrity(database: SupportSQLiteDatabase) {
                Log.d(TAG, "🔍 Проверка целостности данных...")
                
                // Проверяем, что основные таблицы существуют и доступны
                val tables = listOf("wifi_scans", "wifi_networks", "threats", "scan_sessions")
                tables.forEach { tableName ->
                    val cursor = database.query("SELECT COUNT(*) FROM $tableName")
                    cursor.use {
                        if (it.moveToFirst()) {
                            val count = it.getInt(0)
                            Log.d(TAG, "📊 Таблица $tableName: $count записей")
                        }
                    }
                }
                
                // Проверка внешних ключей (если включены)
                val fkCursor = database.query("PRAGMA foreign_key_check")
                fkCursor.use {
                    if (it.count > 0) {
                        Log.w(TAG, "⚠️ Обнаружены нарушения внешних ключей: ${it.count}")
                    } else {
                        Log.d(TAG, "✅ Целостность внешних ключей подтверждена")
                    }
                }
                
                Log.d(TAG, "✅ Валидация целостности данных завершена")
            }
        }
        
        /**
         * Миграция с версии 6 на 7
         * Исправление схемы таблицы threats: колонка description должна быть NOT NULL
         * 
         * ПРОБЛЕМА: В базе данных description имеет notNull = false, но Entity требует notNull = true
         * РЕШЕНИЕ: Пересоздаём таблицу с правильной схемой и переносим данные
         * 
         * БЕЗОПАСНОСТЬ:
         * ✅ Проверяем NULL значения перед миграцией
         * ✅ Заменяем NULL на значение по умолчанию
         * ✅ Используем транзакцию для атомарности операции
         * ✅ Сохраняем все индексы
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 6 -> 7: исправление схемы таблицы threats")
                try {
                    db.beginTransaction()
                    
                    // Шаг 0: Проверяем реальную схему таблицы threats
                    Log.d(TAG, "🔍 Проверка реальной схемы таблицы threats...")
                    var needsRecreation = false
                    val pragmaCursor = db.query("PRAGMA table_info(threats)")
                    pragmaCursor.use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        val notNullIndex = cursor.getColumnIndex("notnull")
                        if (nameIndex < 0 || notNullIndex < 0) {
                            Log.e(TAG, "❌ Не найдены колонки в PRAGMA table_info")
                            return@use
                        }
                        while (cursor.moveToNext()) {
                            val columnName = cursor.getString(nameIndex)
                            val notNull = cursor.getInt(notNullIndex) == 1
                            
                            if (columnName == "description") {
                                if (!notNull) {
                                    Log.w(TAG, "⚠️ Колонка description имеет NULLABLE схему! Требуется пересоздание таблицы.")
                                    needsRecreation = true
                                } else {
                                    Log.d(TAG, "✅ Колонка description уже NOT NULL")
                                }
                                break
                            }
                        }
                    }
                    
                    // Если схема правильная - только добавляем недостающие индексы
                    if (!needsRecreation) {
                        Log.i(TAG, "✅ Схема таблицы threats корректна. Пропускаем пересоздание.")
                        
                        // Просто убеждаемся что все индексы на месте
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)")
                        
                        db.setTransactionSuccessful()
                        Log.i(TAG, "✅ Миграция 6 -> 7 успешно завершена (без изменений)")
                    } else {
                        // Шаг 1: Проверяем наличие NULL значений в колонке description
                        Log.d(TAG, "🔍 Проверка NULL значений в колонке description...")
                        val nullCheckCursor = db.query(
                            "SELECT COUNT(*) FROM threats WHERE description IS NULL"
                        )
                        var nullCount = 0
                        nullCheckCursor.use {
                            if (it.moveToFirst()) {
                                nullCount = it.getInt(0)
                            }
                        }
                        
                        if (nullCount > 0) {
                            Log.w(TAG, "⚠️ Обнаружено $nullCount записей с NULL в description")
                            // Обновляем NULL значения на значение по умолчанию
                            db.execSQL(
                                """
                                UPDATE threats 
                                SET description = 'Описание недоступно' 
                                WHERE description IS NULL
                                """.trimIndent()
                            )
                            Log.i(TAG, "✅ NULL значения заменены на значение по умолчанию")
                        } else {
                            Log.d(TAG, "✅ NULL значения не обнаружены")
                        }
                        
                        // Шаг 2: Создаём новую таблицу с правильной схемой
                        Log.d(TAG, "📦 Создание новой таблицы threats_new с правильной схемой...")
                        db.execSQL(
                            """
                            CREATE TABLE threats_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                scanId INTEGER NOT NULL,
                                threatType TEXT NOT NULL,
                                severity TEXT NOT NULL,
                                description TEXT NOT NULL,
                                networkSsid TEXT NOT NULL,
                                networkBssid TEXT NOT NULL,
                                additionalInfo TEXT,
                                timestamp INTEGER NOT NULL,
                                isResolved INTEGER NOT NULL,
                                resolutionTimestamp INTEGER,
                                resolutionNote TEXT,
                                isNotified INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        
                        // Шаг 3: Копируем данные из старой таблицы в новую
                        Log.d(TAG, "📋 Копирование данных из старой таблицы в новую...")
                        db.execSQL(
                            """
                            INSERT INTO threats_new (
                                id, scanId, threatType, severity, description,
                                networkSsid, networkBssid, additionalInfo, timestamp,
                                isResolved, resolutionTimestamp, resolutionNote, isNotified
                            )
                            SELECT 
                                id, scanId, threatType, severity, 
                                COALESCE(description, 'Описание недоступно'),
                                networkSsid, networkBssid, additionalInfo, timestamp,
                                isResolved, resolutionTimestamp, resolutionNote, isNotified
                            FROM threats
                            """.trimIndent()
                        )
                        
                        // Шаг 4: Удаляем старую таблицу
                        Log.d(TAG, "🗑️ Удаление старой таблицы threats...")
                        db.execSQL("DROP TABLE threats")
                        
                        // Шаг 5: Переименовываем новую таблицу
                        Log.d(TAG, "✏️ Переименование threats_new -> threats...")
                        db.execSQL("ALTER TABLE threats_new RENAME TO threats")
                        
                        // Шаг 6: Восстанавливаем все индексы
                        Log.d(TAG, "🔗 Восстановление индексов...")
                        
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)"
                        )
                        db.execSQL(
                            "CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)"
                        )
                        
                        // Шаг 7: Валидация целостности данных
                        validateDataIntegrity(db)
                        
                        db.setTransactionSuccessful()
                        Log.i(TAG, "✅ Миграция 6 -> 7 успешно завершена")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 6 -> 7: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
            
            /**
             * Валидация целостности данных после миграции
             */
            private fun validateDataIntegrity(db: SupportSQLiteDatabase) {
                Log.d(TAG, "🔍 Проверка целостности данных после миграции 6 -> 7...")
                
                // Проверяем количество записей в таблице threats
                val cursor = db.query("SELECT COUNT(*) FROM threats")
                cursor.use {
                    if (it.moveToFirst()) {
                        val count = it.getInt(0)
                        Log.d(TAG, "📊 Таблица threats: $count записей")
                    }
                }
                
                // Проверяем, что нет NULL значений в description
                val nullCheckCursor = db.query(
                    "SELECT COUNT(*) FROM threats WHERE description IS NULL"
                )
                nullCheckCursor.use {
                    if (it.moveToFirst()) {
                        val nullCount = it.getInt(0)
                        if (nullCount > 0) {
                            Log.e(TAG, "❌ КРИТИЧЕСКАЯ ОШИБКА: Обнаружено $nullCount NULL значений в description!")
                            throw IllegalStateException("Миграция 6->7 не удалила все NULL значения")
                        } else {
                            Log.d(TAG, "✅ Все значения description NOT NULL")
                        }
                    }
                }
                
                // Проверяем наличие всех индексов
                val indexCursor = db.query(
                    """
                    SELECT name FROM sqlite_master 
                    WHERE type = 'index' AND tbl_name = 'threats'
                    """.trimIndent()
                )
                val indexes = mutableListOf<String>()
                indexCursor.use {
                    while (it.moveToNext()) {
                        indexes.add(it.getString(0))
                    }
                }
                Log.d(TAG, "📑 Индексы таблицы threats: ${indexes.joinToString(", ")}")
                
                Log.d(TAG, "✅ Валидация целостности данных завершена")
            }
        }
        
        /**
         * Миграция с версии 7 на 8
         * Исправление схемы таблицы threats для баз данных, которые уже были на версии 7
         * но имеют неправильную схему (description NULLABLE вместо NOT NULL)
         * 
         * ПРОБЛЕМА: Некоторые базы данных версии 7 были созданы с неправильной схемой
         * РЕШЕНИЕ: Проверяем реальную схему и исправляем при необходимости
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 7 -> 8: финальная проверка и исправление схемы threats")
                try {
                    db.beginTransaction()
                    
                    // Проверяем реальную схему таблицы threats
                    Log.d(TAG, "🔍 Проверка реальной схемы таблицы threats...")
                    var needsRecreation = false
                    val pragmaCursor = db.query("PRAGMA table_info(threats)")
                    pragmaCursor.use { cursor ->
                        val nameIndex = cursor.getColumnIndex("name")
                        val notNullIndex = cursor.getColumnIndex("notnull")
                        if (nameIndex < 0 || notNullIndex < 0) {
                            Log.e(TAG, "❌ Не найдены колонки в PRAGMA table_info")
                            return@use
                        }
                        while (cursor.moveToNext()) {
                            val columnName = cursor.getString(nameIndex)
                            val notNull = cursor.getInt(notNullIndex) == 1
                            
                            if (columnName == "description") {
                                if (!notNull) {
                                    Log.w(TAG, "⚠️ Колонка description имеет NULLABLE схему! Исправляем...")
                                    needsRecreation = true
                                } else {
                                    Log.d(TAG, "✅ Колонка description уже NOT NULL - миграция не требуется")
                                }
                                break
                            }
                        }
                    }
                    
                    // Если схема правильная - ничего не делаем
                    if (!needsRecreation) {
                        Log.i(TAG, "✅ Схема таблицы threats корректна. Миграция 7 -> 8 завершена без изменений.")
                        db.setTransactionSuccessful()
                    } else {
                        // Исправляем схему: обрабатываем NULL значения
                        Log.d(TAG, "🔧 Исправление схемы: обработка NULL значений...")
                        val nullCheckCursor = db.query(
                            "SELECT COUNT(*) FROM threats WHERE description IS NULL"
                        )
                        var nullCount = 0
                        nullCheckCursor.use {
                            if (it.moveToFirst()) {
                                nullCount = it.getInt(0)
                            }
                        }
                        
                        if (nullCount > 0) {
                            Log.w(TAG, "⚠️ Обнаружено $nullCount записей с NULL в description")
                            db.execSQL(
                                "UPDATE threats SET description = 'Описание недоступно' WHERE description IS NULL"
                            )
                            Log.i(TAG, "✅ NULL значения заменены")
                        }
                        
                        // Пересоздаём таблицу с правильной схемой
                        Log.d(TAG, "📦 Пересоздание таблицы threats...")
                        db.execSQL(
                            """
                            CREATE TABLE threats_new (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                scanId INTEGER NOT NULL,
                                threatType TEXT NOT NULL,
                                severity TEXT NOT NULL,
                                description TEXT NOT NULL,
                                networkSsid TEXT NOT NULL,
                                networkBssid TEXT NOT NULL,
                                additionalInfo TEXT,
                                timestamp INTEGER NOT NULL,
                                isResolved INTEGER NOT NULL,
                                resolutionTimestamp INTEGER,
                                resolutionNote TEXT,
                                isNotified INTEGER NOT NULL
                            )
                            """.trimIndent()
                        )
                        
                        // Копируем данные
                        db.execSQL(
                            """
                            INSERT INTO threats_new (
                                id, scanId, threatType, severity, description,
                                networkSsid, networkBssid, additionalInfo, timestamp,
                                isResolved, resolutionTimestamp, resolutionNote, isNotified
                            )
                            SELECT 
                                id, scanId, threatType, severity, 
                                COALESCE(description, 'Описание недоступно'),
                                networkSsid, networkBssid, additionalInfo, timestamp,
                                isResolved, resolutionTimestamp, resolutionNote, isNotified
                            FROM threats
                            """.trimIndent()
                        )
                        
                        db.execSQL("DROP TABLE threats")
                        db.execSQL("ALTER TABLE threats_new RENAME TO threats")
                        
                        // Восстанавливаем индексы
                        Log.d(TAG, "🔗 Восстановление индексов...")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)")
                        
                        // Валидация
                        val validateCursor = db.query("PRAGMA table_info(threats)")
                        validateCursor.use { cursor ->
                            val nameIndex = cursor.getColumnIndex("name")
                            val notNullIndex = cursor.getColumnIndex("notnull")
                            if (nameIndex < 0 || notNullIndex < 0) {
                                Log.e(TAG, "❌ Не найдены колонки в PRAGMA table_info")
                                return@use
                            }
                            while (cursor.moveToNext()) {
                                val columnName = cursor.getString(nameIndex)
                                val notNull = cursor.getInt(notNullIndex) == 1
                                
                                if (columnName == "description") {
                                    if (!notNull) {
                                        throw IllegalStateException("❌ ОШИБКА: description всё ещё NULLABLE после миграции!")
                                    } else {
                                        Log.i(TAG, "✅ Валидация: description теперь NOT NULL")
                                    }
                                    break
                                }
                            }
                        }
                        
                        db.setTransactionSuccessful()
                        Log.i(TAG, "✅ Миграция 7 -> 8 успешно завершена")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 7 -> 8: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        /**
         * Миграция с версии 8 на 9
         * ГАРАНТИРОВАННОЕ исправление схемы threats
         * Эта миграция ВСЕГДА пересоздаёт таблицу независимо от текущей схемы
         */
        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 8 -> 9: ГАРАНТИРОВАННОЕ исправление схемы threats")
                try {
                    db.beginTransaction()
                    
                    // Обновляем NULL значения БЕЗ ПРОВЕРКИ
                    Log.d(TAG, "🔧 Обработка NULL значений...")
                    db.execSQL(
                        "UPDATE threats SET description = 'Описание недоступно' WHERE description IS NULL OR description = ''"
                    )
                    
                    // ВСЕГДА пересоздаём таблицу для гарантии правильной схемы
                    Log.d(TAG, "📦 Пересоздание таблицы threats...")
                    db.execSQL("""
                        CREATE TABLE threats_v9 (
                            id INTEGER PRIMARY KEY AUTOINCREMENT,
                            scanId INTEGER NOT NULL,
                            threatType TEXT NOT NULL,
                            severity TEXT NOT NULL,
                            description TEXT NOT NULL,
                            networkSsid TEXT NOT NULL,
                            networkBssid TEXT NOT NULL,
                            additionalInfo TEXT,
                            timestamp INTEGER NOT NULL,
                            isResolved INTEGER NOT NULL,
                            resolutionTimestamp INTEGER,
                            resolutionNote TEXT,
                            isNotified INTEGER NOT NULL
                        )
                    """.trimIndent())
                    
                    // Копируем данные с гарантией NOT NULL
                    db.execSQL("""
                        INSERT INTO threats_v9 (
                            id, scanId, threatType, severity, description,
                            networkSsid, networkBssid, additionalInfo, timestamp,
                            isResolved, resolutionTimestamp, resolutionNote, isNotified
                        )
                        SELECT 
                            id, scanId, threatType, severity,
                            COALESCE(NULLIF(description, ''), 'Описание недоступно'),
                            networkSsid, networkBssid, additionalInfo, timestamp,
                            isResolved, resolutionTimestamp, resolutionNote, isNotified
                        FROM threats
                    """.trimIndent())
                    
                    db.execSQL("DROP TABLE threats")
                    db.execSQL("ALTER TABLE threats_v9 RENAME TO threats")
                    
                    // Восстанавливаем индексы
                    Log.d(TAG, "🔗 Восстановление индексов...")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)")
                    
                    // Проверяем результат
                    val cursor = db.query("PRAGMA table_info(threats)")
                    cursor.use {
                        val nameIndex = it.getColumnIndex("name")
                        val notNullIndex = it.getColumnIndex("notnull")
                        if (nameIndex < 0 || notNullIndex < 0) {
                            Log.e(TAG, "❌ Не найдены колонки в PRAGMA table_info")
                            return@use
                        }
                        while (it.moveToNext()) {
                            val columnName = it.getString(nameIndex)
                            val notNull = it.getInt(notNullIndex) == 1
                            
                            if (columnName == "description") {
                                if (!notNull) {
                                    throw IllegalStateException("❌ description ВСЁ ЕЩЁ NULLABLE после миграции 8->9!")
                                }
                                Log.i(TAG, "✅ Валидация: description корректно установлен как NOT NULL")
                                break
                            }
                        }
                    }
                    
                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 8 -> 9 успешно завершена! Схема ГАРАНТИРОВАННО исправлена!")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 8 -> 9: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        /**
         * Callback для мониторинга операций базы данных
         */
        private val databaseCallback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                Log.i(TAG, "📦 База данных создана впервые")
            }
            
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                Log.d(TAG, "📂 База данных открыта")
                
                // Включаем проверку внешних ключей
                db.execSQL("PRAGMA foreign_keys = ON")
                
                // КРИТИЧЕСКОЕ ИСПРАВЛЕНИЕ: Проверяем и исправляем схему таблицы threats при каждом открытии
                fixThreatsTableSchema(db)
            }
            
            /**
             * Принудительное исправление схемы таблицы threats
             * Вызывается при каждом открытии базы данных
             */
            private fun fixThreatsTableSchema(db: SupportSQLiteDatabase) {
                try {
                    // Проверяем схему колонки description
                    var needsFix = false
                    val cursor = db.query("PRAGMA table_info(threats)")
                    cursor.use {
                        val nameIndex = it.getColumnIndex("name")
                        val notNullIndex = it.getColumnIndex("notnull")
                        if (nameIndex < 0 || notNullIndex < 0) {
                            Log.e(TAG, "❌ Не найдены колонки в PRAGMA table_info")
                            return@use
                        }
                        while (it.moveToNext()) {
                            val columnName = it.getString(nameIndex)
                            val notNull = it.getInt(notNullIndex) == 1
                            
                            if (columnName == "description" && !notNull) {
                                Log.w(TAG, "🚨 КРИТИЧЕСКАЯ ПРОБЛЕМА: description имеет NULLABLE схему!")
                                needsFix = true
                                break
                            }
                        }
                    }
                    
                    if (!needsFix) {
                        Log.d(TAG, "✅ Схема threats корректна")
                        return
                    }
                    
                    // ПРИНУДИТЕЛЬНОЕ ИСПРАВЛЕНИЕ СХЕМЫ
                    Log.w(TAG, "🔧 ПРИНУДИТЕЛЬНОЕ исправление схемы threats...")
                    
                    db.beginTransaction()
                    try {
                        // Обновляем NULL значения
                        db.execSQL("UPDATE threats SET description = 'Описание недоступно' WHERE description IS NULL")
                        
                        // Пересоздаём таблицу
                        db.execSQL("""
                            CREATE TABLE threats_fixed (
                                id INTEGER PRIMARY KEY AUTOINCREMENT,
                                scanId INTEGER NOT NULL,
                                threatType TEXT NOT NULL,
                                severity TEXT NOT NULL,
                                description TEXT NOT NULL,
                                networkSsid TEXT NOT NULL,
                                networkBssid TEXT NOT NULL,
                                additionalInfo TEXT,
                                timestamp INTEGER NOT NULL,
                                isResolved INTEGER NOT NULL,
                                resolutionTimestamp INTEGER,
                                resolutionNote TEXT,
                                isNotified INTEGER NOT NULL
                            )
                        """.trimIndent())
                        
                        // Копируем данные
                        db.execSQL("""
                            INSERT INTO threats_fixed 
                            SELECT 
                                id, scanId, threatType, severity,
                                COALESCE(description, 'Описание недоступно'),
                                networkSsid, networkBssid, additionalInfo, timestamp,
                                isResolved, resolutionTimestamp, resolutionNote, isNotified
                            FROM threats
                        """.trimIndent())
                        
                        db.execSQL("DROP TABLE threats")
                        db.execSQL("ALTER TABLE threats_fixed RENAME TO threats")
                        
                        // Восстанавливаем индексы
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)")
                        db.execSQL("CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)")
                        
                        db.setTransactionSuccessful()
                        Log.i(TAG, "✅ Схема threats ПРИНУДИТЕЛЬНО исправлена!")
                    } finally {
                        db.endTransaction()
                    }
                    
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка при исправлении схемы threats: ${e.message}", e)
                    // Не пробрасываем исключение, чтобы не крашить приложение
                }
            }
            
            override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
                super.onDestructiveMigration(db)
                // КРИТИЧЕСКОЕ ПРЕДУПРЕЖДЕНИЕ: этот метод не должен вызываться в production
                Log.e(TAG, "🚨 КРИТИЧЕСКАЯ ОШИБКА: Произошла деструктивная миграция! Данные потеряны!")
            }
        }
        
        /**
         * Миграция с версии 9 на 10
         * Финальное исправление несоответствий схемы таблицы threats
         * Использует точный синтаксис Room для создания таблицы
         */
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 9 -> 10: Синхронизация схемы threats")
                
                db.beginTransaction()
                try {
                    // Создаем временную таблицу с ТОЧНОЙ схемой (обратные кавычки, NOT NULL где нужно)
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS `threats_v10` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `scanId` INTEGER NOT NULL,
                            `threatType` TEXT NOT NULL,
                            `severity` TEXT NOT NULL,
                            `description` TEXT NOT NULL,
                            `networkSsid` TEXT NOT NULL,
                            `networkBssid` TEXT NOT NULL,
                            `additionalInfo` TEXT,
                            `timestamp` INTEGER NOT NULL,
                            `isResolved` INTEGER NOT NULL,
                            `resolutionTimestamp` INTEGER,
                            `resolutionNote` TEXT,
                            `isNotified` INTEGER NOT NULL
                        )
                    """)

                    // Копируем данные
                    db.execSQL("""
                        INSERT INTO `threats_v10` (
                            `id`, `scanId`, `threatType`, `severity`, `description`,
                            `networkSsid`, `networkBssid`, `additionalInfo`, `timestamp`,
                            `isResolved`, `resolutionTimestamp`, `resolutionNote`, `isNotified`
                        )
                        SELECT 
                            `id`, `scanId`, `threatType`, `severity`, `description`,
                            `networkSsid`, `networkBssid`, `additionalInfo`, `timestamp`,
                            `isResolved`, `resolutionTimestamp`, `resolutionNote`, `isNotified`
                        FROM `threats`
                    """)

                    // Удаляем старую таблицу
                    db.execSQL("DROP TABLE `threats`")

                    // Переименовываем новую
                    db.execSQL("ALTER TABLE `threats_v10` RENAME TO `threats`")

                    // Создаем индексы
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_timestamp` ON `threats` (`timestamp`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_severity` ON `threats` (`severity`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_isResolved` ON `threats` (`isResolved`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_scanId` ON `threats` (`scanId`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_severity_isResolved` ON `threats` (`severity`, `isResolved`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_isNotified` ON `threats` (`isNotified`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_networkBssid` ON `threats` (`networkBssid`)")
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_threats_threatType` ON `threats` (`threatType`)")

                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 9 -> 10 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 9 -> 10: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }

        /**
         * Миграция с версии 10 на 11
         * Оптимизация производительности: добавление индексов для wifi_scans.
         *
         * ВАЖНО: индексы ускоряют:
         * - получение последних сканов (ORDER BY timestamp)
         * - выборки по bssid/ssid/scanSessionId
         * - фильтры по threatLevel/securityType/isConnected
         */
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 10 -> 11: индексы wifi_scans")
                db.beginTransaction()
                try {
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_timestamp` ON `wifi_scans`(`timestamp`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_bssid` ON `wifi_scans`(`bssid`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_ssid` ON `wifi_scans`(`ssid`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_scanSessionId` ON `wifi_scans`(`scanSessionId`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_threatLevel` ON `wifi_scans`(`threatLevel`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_securityType` ON `wifi_scans`(`securityType`)"
                    )
                    db.execSQL(
                        "CREATE INDEX IF NOT EXISTS `index_wifi_scans_isConnected` ON `wifi_scans`(`isConnected`)"
                    )

                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 10 -> 11 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 10 -> 11: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }

        /**
         * Миграция с версии 11 на 12
         * Исправление рассинхрона "Статистики":
         *
         * Исторически в БД могли попадать timestamp'ы из ScanResult.timestamp (uptime),
         * которые НЕ являются unix-epoch. Это ломает:
         * - фильтры "за 24 часа/неделю"
         * - дневную статистику (DATE(timestamp/1000, 'unixepoch'))
         * - автоочистку по cutoffTime (epoch)
         *
         * Решение:
         * - удаляем явно некорректные wifi_scans со слишком маленьким timestamp
         * - нормализуем wifi_networks.first_seen/last_seen, если они некорректны
         */
        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                Log.i(TAG, "🔄 Начало миграции 11 -> 12: нормализация timestamp для статистики")
                db.beginTransaction()
                try {
                    // Всё, что раньше 2000-01-01, считаем некорректным (uptime).
                    val minValidEpochMillis = 946684800000L

                    // Удаляем некорректную историю сканов
                    db.execSQL("DELETE FROM `wifi_scans` WHERE `timestamp` < $minValidEpochMillis")

                    // Нормализуем времена в wifi_networks (для сортировки/экранов)
                    val nowMillis = System.currentTimeMillis()
                    db.execSQL(
                        "UPDATE `wifi_networks` SET `first_seen` = $nowMillis WHERE `first_seen` < $minValidEpochMillis"
                    )
                    db.execSQL(
                        "UPDATE `wifi_networks` SET `last_seen` = $nowMillis WHERE `last_seen` < $minValidEpochMillis"
                    )

                    db.setTransactionSuccessful()
                    Log.i(TAG, "✅ Миграция 11 -> 12 успешно завершена")
                } catch (e: Exception) {
                    Log.e(TAG, "❌ Ошибка миграции 11 -> 12: ${e.message}", e)
                    throw e
                } finally {
                    db.endTransaction()
                }
            }
        }
        
        @Volatile
        private var INSTANCE: WifiGuardDatabase? = null
        
        /**
         * Получить экземпляр базы данных (Singleton)
         * 
         * ВАЖНО: НЕ используется fallbackToDestructiveMigration()
         * Все миграции должны быть явно определены для предотвращения потери данных
         */
        fun getDatabase(context: Context): WifiGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                Log.i(TAG, "🏗️ Инициализация базы данных...")
                
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WifiGuardDatabase::class.java,
                    DATABASE_NAME
                )
                // Добавляем ВСЕ миграции в порядке возрастания версий
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7,
                    MIGRATION_7_8,
                    MIGRATION_8_9,
                    MIGRATION_9_10,
                    MIGRATION_10_11,
                    MIGRATION_11_12
                )
                // Добавляем callback для мониторинга
                .addCallback(databaseCallback)
                // КРИТИЧЕСКИ ВАЖНО: НЕ вызываем fallbackToDestructiveMigration()
                // Если миграция не найдена - Room выбросит IllegalStateException
                // Это безопаснее, чем молчаливая потеря данных пользователя
                .build()
                
                INSTANCE = instance
                Log.i(TAG, "✅ База данных инициализирована успешно")
                instance
            }
        }
        
        /**
         * Закрыть базу данных и освободить ресурсы
         * Вызывается при завершении приложения для корректной очистки
         * 
         * @Suppress("unused") - функция может использоваться в будущем для явного закрытия БД
         */
        @Suppress("unused")
        fun closeDatabase() {
            synchronized(this) {
                INSTANCE?.close()
                INSTANCE = null
                Log.i(TAG, "🔒 База данных закрыта")
            }
        }
    }
}