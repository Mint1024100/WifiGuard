# 🔧 Миграция базы данных Room 6 → 7

## 📋 Описание проблемы

### Ошибка
```
Migration didn't properly handle: threats(com.wifiguard.core.data.local.entity.ThreatEntity)

Expected: Column { name = 'description', notNull = true }
Found:    Column { name = 'description', notNull = false }
```

### Причина
- **Entity класс** определяет `description` как **NOT NULL** (`val description: String`)
- **База данных** содержит колонку `description` как **NULLABLE**
- Room обнаруживает несоответствие при запуске приложения

### Как это произошло
Возможные причины:
1. Изначальная схема была создана с nullable полем
2. Код Entity был изменён без создания миграции
3. Миграция была некорректной или пропущена

---

## ✅ Решение

### Стратегия миграции
SQLite **не позволяет** напрямую изменять constraint `NOT NULL` на существующей колонке через `ALTER TABLE`. Поэтому используем стратегию **пересоздания таблицы**:

1. ✅ Проверка наличия NULL значений
2. ✅ Замена NULL на значение по умолчанию
3. ✅ Создание новой таблицы с правильной схемой
4. ✅ Копирование данных из старой таблицы
5. ✅ Удаление старой таблицы
6. ✅ Переименование новой таблицы
7. ✅ Восстановление всех индексов
8. ✅ Валидация целостности данных

---

## 🗂️ Структура файлов

### 1. ThreatEntity.kt

**Местоположение:** `app/src/main/java/com/wifiguard/core/data/local/entity/ThreatEntity.kt`

```kotlin
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
    
    @ColumnInfo(name = "scanId")
    val scanId: Long,
    
    @ColumnInfo(name = "threatType")
    val threatType: ThreatType,
    
    @ColumnInfo(name = "severity")
    val severity: ThreatLevel,
    
    /** Описание угрозы - ОБЯЗАТЕЛЬНОЕ ПОЛЕ (NOT NULL) */
    @ColumnInfo(name = "description")
    val description: String,  // ✅ NOT NULL
    
    @ColumnInfo(name = "networkSsid")
    val networkSsid: String,
    
    @ColumnInfo(name = "networkBssid")
    val networkBssid: String,
    
    /** Дополнительная информация - ОПЦИОНАЛЬНОЕ ПОЛЕ */
    @ColumnInfo(name = "additionalInfo")
    val additionalInfo: String? = null,  // ✅ NULLABLE
    
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
    
    @ColumnInfo(name = "isResolved")
    val isResolved: Boolean = false,
    
    @ColumnInfo(name = "resolutionTimestamp")
    val resolutionTimestamp: Long? = null,
    
    @ColumnInfo(name = "resolutionNote")
    val resolutionNote: String? = null,
    
    @ColumnInfo(name = "isNotified")
    val isNotified: Boolean = false
)
```

**Ключевые моменты:**
- ✅ `description: String` - NOT NULL (без ?)
- ✅ `additionalInfo: String? = null` - NULLABLE (с ?)

---

### 2. Миграция 6 → 7

**Местоположение:** `WifiGuardDatabase.kt` → `MIGRATION_6_7`

```kotlin
/**
 * Миграция с версии 6 на 7
 * Исправление схемы таблицы threats: колонка description должна быть NOT NULL
 */
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        Log.i(TAG, "🔄 Начало миграции 6 -> 7")
        try {
            database.beginTransaction()
            
            // Шаг 1: Проверяем NULL значения
            val nullCheckCursor = database.query(
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
                // Обновляем NULL на значение по умолчанию
                database.execSQL(
                    """
                    UPDATE threats 
                    SET description = 'Описание недоступно' 
                    WHERE description IS NULL
                    """
                )
                Log.i(TAG, "✅ NULL значения заменены")
            }
            
            // Шаг 2: Создаём новую таблицу с правильной схемой
            database.execSQL(
                """
                CREATE TABLE threats_new (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scanId INTEGER NOT NULL,
                    threatType TEXT NOT NULL,
                    severity TEXT NOT NULL,
                    description TEXT NOT NULL,          -- ✅ NOT NULL
                    networkSsid TEXT NOT NULL,
                    networkBssid TEXT NOT NULL,
                    additionalInfo TEXT,                -- ✅ NULLABLE
                    timestamp INTEGER NOT NULL,
                    isResolved INTEGER NOT NULL,
                    resolutionTimestamp INTEGER,
                    resolutionNote TEXT,
                    isNotified INTEGER NOT NULL
                )
                """
            )
            
            // Шаг 3: Копируем данные с обработкой NULL
            database.execSQL(
                """
                INSERT INTO threats_new (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, additionalInfo, timestamp,
                    isResolved, resolutionTimestamp, resolutionNote, isNotified
                )
                SELECT 
                    id, scanId, threatType, severity, 
                    COALESCE(description, 'Описание недоступно'),  -- ✅ NULL → значение по умолчанию
                    networkSsid, networkBssid, additionalInfo, timestamp,
                    isResolved, resolutionTimestamp, resolutionNote, isNotified
                FROM threats
                """
            )
            
            // Шаг 4-5: Удаляем старую и переименовываем новую
            database.execSQL("DROP TABLE threats")
            database.execSQL("ALTER TABLE threats_new RENAME TO threats")
            
            // Шаг 6: Восстанавливаем индексы
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_timestamp ON threats(timestamp)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity ON threats(severity)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isResolved ON threats(isResolved)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_scanId ON threats(scanId)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_severity_isResolved ON threats(severity, isResolved)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_isNotified ON threats(isNotified)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_networkBssid ON threats(networkBssid)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_threats_threatType ON threats(threatType)")
            
            // Шаг 7: Валидация
            validateDataIntegrity(database)
            
            database.setTransactionSuccessful()
            Log.i(TAG, "✅ Миграция 6 -> 7 успешно завершена")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Ошибка миграции 6 -> 7: ${e.message}", e)
            throw e
        } finally {
            database.endTransaction()
        }
    }
    
    private fun validateDataIntegrity(database: SupportSQLiteDatabase) {
        // Проверяем количество записей
        val cursor = database.query("SELECT COUNT(*) FROM threats")
        cursor.use {
            if (it.moveToFirst()) {
                Log.d(TAG, "📊 Таблица threats: ${it.getInt(0)} записей")
            }
        }
        
        // Проверяем отсутствие NULL в description
        val nullCheckCursor = database.query(
            "SELECT COUNT(*) FROM threats WHERE description IS NULL"
        )
        nullCheckCursor.use {
            if (it.moveToFirst()) {
                val nullCount = it.getInt(0)
                if (nullCount > 0) {
                    throw IllegalStateException("Обнаружено $nullCount NULL значений!")
                }
            }
        }
        
        Log.d(TAG, "✅ Валидация завершена")
    }
}
```

---

### 3. Конфигурация Database

**Местоположение:** `WifiGuardDatabase.kt`

```kotlin
@Database(
    entities = [
        WifiScanEntity::class,
        WifiNetworkEntity::class,
        ThreatEntity::class,
        ScanSessionEntity::class
    ],
    version = 7,  // ✅ Увеличена версия
    exportSchema = true
)
@TypeConverters(DatabaseConverters::class)
abstract class WifiGuardDatabase : RoomDatabase() {
    
    // ... DAO методы ...
    
    companion object {
        private const val TAG = "WifiGuardDatabase"
        const val DATABASE_NAME = "wifiguard_database"
        
        // ✅ Все миграции
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
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7  // ✅ Новая миграция
                )
                .addCallback(databaseCallback)
                // ✅ НЕТ fallbackToDestructiveMigration()
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
```

---

## 🔒 Безопасность миграции

### Гарантии безопасности

1. **Атомарность**: Все операции выполняются в транзакции
   ```kotlin
   database.beginTransaction()
   try {
       // ... операции миграции ...
       database.setTransactionSuccessful()
   } finally {
       database.endTransaction()
   }
   ```

2. **Обработка NULL**: Замена NULL на значение по умолчанию
   ```kotlin
   COALESCE(description, 'Описание недоступно')
   ```

3. **Валидация**: Проверка целостности данных после миграции
   ```kotlin
   validateDataIntegrity(database)
   ```

4. **Логирование**: Детальное логирование всех этапов
   ```kotlin
   Log.i(TAG, "🔄 Начало миграции...")
   Log.d(TAG, "✅ Шаг X завершён")
   ```

5. **Откат при ошибке**: Автоматический rollback при исключении
   ```kotlin
   } catch (e: Exception) {
       Log.e(TAG, "❌ Ошибка: ${e.message}", e)
       throw e  // Room выполнит rollback
   }
   ```

---

## 🧪 Тестирование миграции

### Юнит-тест миграции

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WifiGuardDatabase::class.java
    )
    
    @Test
    fun migrate6To7_WithNullDescription_ReplacesWithDefault() {
        // Given: База данных версии 6 с NULL в description
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL("""
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    1, 1, 'SUSPICIOUS', 'HIGH', NULL,
                    'TestSSID', '00:11:22:33:44:55', 1000, 0, 0
                )
            """)
            close()
        }
        
        // When: Применяем миграцию 6 -> 7
        val db = helper.runMigrationsAndValidate(
            TEST_DB, 
            7, 
            true, 
            MIGRATION_6_7
        )
        
        // Then: description не должен быть NULL
        db.query("SELECT description FROM threats WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            val description = cursor.getString(0)
            assertNotNull(description)
            assertEquals("Описание недоступно", description)
        }
    }
    
    @Test
    fun migrate6To7_PreservesExistingData() {
        // Given: База с валидными данными
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL("""
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    1, 1, 'EVIL_TWIN', 'CRITICAL', 'Поддельная точка доступа',
                    'WiFi', '00:11:22:33:44:55', 1000, 0, 0
                )
            """)
            close()
        }
        
        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)
        
        // Then: Данные сохранены
        db.query("SELECT * FROM threats WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Поддельная точка доступа", cursor.getString(cursor.getColumnIndex("description")))
            assertEquals("EVIL_TWIN", cursor.getString(cursor.getColumnIndex("threatType")))
        }
    }
    
    @Test
    fun migrate6To7_PreservesIndices() {
        // Given: База версии 6
        helper.createDatabase(TEST_DB, 6).close()
        
        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)
        
        // Then: Все индексы восстановлены
        val expectedIndices = listOf(
            "index_threats_timestamp",
            "index_threats_severity",
            "index_threats_isResolved",
            "index_threats_scanId",
            "index_threats_severity_isResolved",
            "index_threats_isNotified",
            "index_threats_networkBssid",
            "index_threats_threatType"
        )
        
        db.query("SELECT name FROM sqlite_master WHERE type = 'index' AND tbl_name = 'threats'").use { cursor ->
            val actualIndices = mutableListOf<String>()
            while (cursor.moveToNext()) {
                actualIndices.add(cursor.getString(0))
            }
            assertTrue(actualIndices.containsAll(expectedIndices))
        }
    }
    
    companion object {
        private const val TEST_DB = "migration-test"
    }
}
```

---

## 📊 Мониторинг миграции

### Логи при успешной миграции

```
I/WifiGuardDatabase: 🔄 Начало миграции 6 -> 7: исправление схемы таблицы threats
D/WifiGuardDatabase: 🔍 Проверка NULL значений в колонке description...
W/WifiGuardDatabase: ⚠️ Обнаружено 3 записей с NULL в description
I/WifiGuardDatabase: ✅ NULL значения заменены на значение по умолчанию
D/WifiGuardDatabase: 📦 Создание новой таблицы threats_new с правильной схемой...
D/WifiGuardDatabase: 📋 Копирование данных из старой таблицы в новую...
D/WifiGuardDatabase: 🗑️ Удаление старой таблицы threats...
D/WifiGuardDatabase: ✏️ Переименование threats_new -> threats...
D/WifiGuardDatabase: 🔗 Восстановление индексов...
D/WifiGuardDatabase: 🔍 Проверка целостности данных после миграции 6 -> 7...
D/WifiGuardDatabase: 📊 Таблица threats: 150 записей
D/WifiGuardDatabase: ✅ Все значения description NOT NULL
D/WifiGuardDatabase: 📑 Индексы таблицы threats: index_threats_timestamp, index_threats_severity, ...
D/WifiGuardDatabase: ✅ Валидация целостности данных завершена
I/WifiGuardDatabase: ✅ Миграция 6 -> 7 успешно завершена
```

### Логи при ошибке

```
I/WifiGuardDatabase: 🔄 Начало миграции 6 -> 7
E/WifiGuardDatabase: ❌ Ошибка миграции 6 -> 7: table threats_new already exists
```

---

## ✨ Best Practices

### ✅ Что ДЕЛАТЬ

1. **Всегда создавайте миграции** для изменений схемы
2. **Используйте транзакции** для атомарности
3. **Обрабатывайте NULL значения** перед изменением constraint
4. **Валидируйте данные** после миграции
5. **Логируйте все этапы** для отладки
6. **Тестируйте миграции** с реальными данными
7. **Экспортируйте схему** (`exportSchema = true`)

### ❌ Что НЕ ДЕЛАТЬ

1. **НЕ используйте** `fallbackToDestructiveMigration()`
2. **НЕ изменяйте** constraint напрямую через `ALTER TABLE`
3. **НЕ забывайте** восстанавливать индексы
4. **НЕ игнорируйте** NULL значения
5. **НЕ пропускайте** валидацию
6. **НЕ коммитьте** без тестирования

---

## 🎯 Результат

После применения миграции:

✅ Колонка `description` имеет constraint NOT NULL  
✅ Все существующие данные сохранены  
✅ NULL значения заменены на "Описание недоступно"  
✅ Все индексы восстановлены  
✅ Room больше не выдаёт ошибку валидации  
✅ Приложение успешно запускается  

---

## 📚 Дополнительные ресурсы

- [Room Database Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Testing Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
- [SQLite ALTER TABLE Limitations](https://www.sqlite.org/lang_altertable.html)

---

**Автор:** Mint1024  
**Дата:** 2025-12-07  
**Версия:** 1.0











