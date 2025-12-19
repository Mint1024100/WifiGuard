# 🎓 Best Practices для работы с Room Database

## 📌 Предотвращение проблем с миграциями

### 1. Чек-лист перед изменением Entity

Перед изменением любого `@Entity` класса выполните:

- [ ] Создайте резервную копию базы данных (если production)
- [ ] Увеличьте версию базы данных на 1
- [ ] Создайте соответствующую миграцию
- [ ] Напишите тесты для миграции
- [ ] Протестируйте на реальных данных
- [ ] Обновите документацию

### 2. Правила изменения колонок

#### ✅ МОЖНО делать без миграции:
- Добавлять новые nullable колонки с DEFAULT значением
- Изменять имена классов/переменных (если используется `@ColumnInfo(name = "...")`)
- Изменять default значения параметров Kotlin

#### ❌ ТРЕБУЕТ миграции:
- Изменение типа данных колонки
- Изменение NOT NULL constraint
- Переименование колонки в базе данных
- Удаление колонки
- Изменение PRIMARY KEY
- Добавление NOT NULL колонки

### 3. Стратегии миграции по типу изменения

#### 📝 Добавление nullable колонки
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE table_name ADD COLUMN new_column TEXT"
        )
    }
}
```

#### 📝 Добавление NOT NULL колонки
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE table_name ADD COLUMN new_column TEXT NOT NULL DEFAULT 'default_value'"
        )
    }
}
```

#### 📝 Изменение nullable → NOT NULL
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.beginTransaction()
        try {
            // 1. Обработать NULL значения
            database.execSQL(
                "UPDATE table_name SET column_name = 'default' WHERE column_name IS NULL"
            )
            
            // 2. Пересоздать таблицу
            database.execSQL("CREATE TABLE table_new (...column_name TEXT NOT NULL...)")
            database.execSQL("INSERT INTO table_new SELECT * FROM table_name")
            database.execSQL("DROP TABLE table_name")
            database.execSQL("ALTER TABLE table_new RENAME TO table_name")
            
            // 3. Восстановить индексы
            database.execSQL("CREATE INDEX ...")
            
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
```

#### 📝 Переименование колонки
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE table_name RENAME COLUMN old_name TO new_name"
        )
        // Примечание: работает только с SQLite 3.25.0+
        // Для старых версий нужно пересоздавать таблицу
    }
}
```

#### 📝 Удаление колонки
```kotlin
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // SQLite не поддерживает DROP COLUMN до версии 3.35.0
        // Нужно пересоздать таблицу
        database.beginTransaction()
        try {
            database.execSQL("CREATE TABLE table_new (сохраняемые_колонки)")
            database.execSQL("INSERT INTO table_new SELECT сохраняемые_колонки FROM table_name")
            database.execSQL("DROP TABLE table_name")
            database.execSQL("ALTER TABLE table_new RENAME TO table_name")
            database.setTransactionSuccessful()
        } finally {
            database.endTransaction()
        }
    }
}
```

---

## 🔒 Безопасность данных

### 1. НИКОГДА не используйте `fallbackToDestructiveMigration()`

```kotlin
// ❌ ПЛОХО - уничтожает все данные пользователя
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()

// ✅ ХОРОШО - требует явных миграций
Room.databaseBuilder(...)
    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, ...)
    .build()
```

### 2. Используйте транзакции

```kotlin
database.beginTransaction()
try {
    // Все изменения схемы
    database.setTransactionSuccessful()
} catch (e: Exception) {
    Log.e(TAG, "Ошибка миграции", e)
    throw e
} finally {
    database.endTransaction()
}
```

### 3. Валидируйте данные после миграции

```kotlin
private fun validateDataIntegrity(database: SupportSQLiteDatabase) {
    // Проверка количества записей
    val cursor = database.query("SELECT COUNT(*) FROM table_name")
    cursor.use {
        if (it.moveToFirst()) {
            val count = it.getInt(0)
            Log.d(TAG, "Записей после миграции: $count")
        }
    }
    
    // Проверка отсутствия NULL в NOT NULL колонках
    val nullCheck = database.query(
        "SELECT COUNT(*) FROM table_name WHERE not_null_column IS NULL"
    )
    nullCheck.use {
        if (it.moveToFirst() && it.getInt(0) > 0) {
            throw IllegalStateException("Обнаружены NULL значения!")
        }
    }
}
```

---

## 🧪 Тестирование миграций

### 1. Настройка тестов

```kotlin
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        YourDatabase::class.java
    )
    
    @Test
    fun migrateX_Y() {
        // Given: создать базу версии X
        helper.createDatabase(TEST_DB, X).apply {
            execSQL("INSERT INTO ...")
            close()
        }
        
        // When: применить миграцию
        val db = helper.runMigrationsAndValidate(
            TEST_DB, Y, true, MIGRATION_X_Y
        )
        
        // Then: проверить результат
        db.query("SELECT * FROM ...").use { cursor ->
            // assertions
        }
    }
}
```

### 2. Что тестировать

- ✅ Сохранность существующих данных
- ✅ Обработку NULL значений
- ✅ Восстановление индексов
- ✅ Структуру таблицы после миграции
- ✅ Граничные случаи (пустая таблица, большой объём данных)

---

## 📊 Мониторинг и логирование

### 1. Структура логов

```kotlin
override fun migrate(database: SupportSQLiteDatabase) {
    Log.i(TAG, "🔄 Начало миграции $startVersion -> $endVersion")
    
    try {
        database.beginTransaction()
        
        Log.d(TAG, "📦 Шаг 1: Создание новой таблицы")
        // ...
        
        Log.d(TAG, "📋 Шаг 2: Копирование данных")
        // ...
        
        database.setTransactionSuccessful()
        Log.i(TAG, "✅ Миграция $startVersion -> $endVersion завершена")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Ошибка миграции: ${e.message}", e)
        throw e
    } finally {
        database.endTransaction()
    }
}
```

### 2. Callback для мониторинга

```kotlin
private val databaseCallback = object : Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        super.onCreate(db)
        Log.i(TAG, "📦 База данных создана")
    }
    
    override fun onOpen(db: SupportSQLiteDatabase) {
        super.onOpen(db)
        Log.d(TAG, "📂 База данных открыта")
    }
    
    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        super.onDestructiveMigration(db)
        Log.e(TAG, "🚨 КРИТИЧЕСКАЯ ОШИБКА: Деструктивная миграция!")
        // Отправить аналитику/crash report
    }
}
```

---

## 📝 Документация миграций

### Шаблон для комментария миграции

```kotlin
/**
 * Миграция с версии X на Y
 * 
 * ИЗМЕНЕНИЯ:
 * - Добавлена колонка new_column (тип TEXT, NOT NULL)
 * - Изменён constraint для old_column (nullable -> NOT NULL)
 * - Добавлен индекс на new_column
 * 
 * ПРОБЛЕМА:
 * [Описание проблемы, которую решает миграция]
 * 
 * РЕШЕНИЕ:
 * [Краткое описание стратегии миграции]
 * 
 * БЕЗОПАСНОСТЬ:
 * ✅ Используется транзакция
 * ✅ NULL значения обработаны
 * ✅ Индексы восстановлены
 * ✅ Валидация данных выполнена
 * 
 * ДАТА: YYYY-MM-DD
 * АВТОР: Mint1024
 */
val MIGRATION_X_Y = object : Migration(X, Y) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // ...
    }
}
```

---

## 🎯 Чек-лист перед релизом

### Перед выпуском новой версии с миграцией:

- [ ] Версия базы данных увеличена
- [ ] Миграция создана и протестирована
- [ ] Все тесты миграции проходят
- [ ] Миграция добавлена в `.addMigrations()`
- [ ] `exportSchema = true` установлен
- [ ] Схемы экспортированы в `schemas/` директорию
- [ ] Документация обновлена
- [ ] Логи миграции проверены
- [ ] Протестировано на реальных данных
- [ ] Протестировано на разных версиях Android
- [ ] Code review выполнен

---

## 🚫 Антипаттерны

### ❌ НЕ ДЕЛАЙТЕ ТАК:

#### 1. Изменение Entity без миграции
```kotlin
// Версия 1
@Entity
data class User(
    val id: Long,
    val name: String?  // nullable
)

// Версия 2 - БЕЗ миграции (ОШИБКА!)
@Entity
data class User(
    val id: Long,
    val name: String  // NOT NULL - несоответствие!
)
```

#### 2. Использование fallbackToDestructiveMigration
```kotlin
// ❌ ПЛОХО
Room.databaseBuilder(...)
    .fallbackToDestructiveMigration()
    .build()
```

#### 3. Игнорирование NULL значений
```kotlin
// ❌ ПЛОХО - может привести к потере данных
database.execSQL(
    "CREATE TABLE new_table (...column TEXT NOT NULL...)"
)
database.execSQL(
    "INSERT INTO new_table SELECT * FROM old_table"
    // Если в old_table есть NULL - ошибка!
)
```

#### 4. Отсутствие транзакций
```kotlin
// ❌ ПЛОХО - нет атомарности
override fun migrate(database: SupportSQLiteDatabase) {
    database.execSQL("CREATE TABLE ...")  // Может упасть
    database.execSQL("INSERT INTO ...")   // Данные будут частично изменены
}
```

### ✅ ДЕЛАЙТЕ ТАК:

```kotlin
// ✅ ХОРОШО
override fun migrate(database: SupportSQLiteDatabase) {
    database.beginTransaction()
    try {
        // 1. Обработать NULL
        database.execSQL("UPDATE ... SET column = 'default' WHERE column IS NULL")
        
        // 2. Изменить схему
        database.execSQL("CREATE TABLE ...")
        database.execSQL("INSERT INTO ... SELECT ... FROM ...")
        
        // 3. Валидировать
        validateDataIntegrity(database)
        
        database.setTransactionSuccessful()
    } finally {
        database.endTransaction()
    }
}
```

---

## 📚 Дополнительные ресурсы

### Официальная документация:
- [Room Migration Guide](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Testing Room Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions#test)
- [Room Database Inspector](https://developer.android.com/studio/inspect/database)

### Инструменты:
- **Database Inspector** в Android Studio для просмотра схемы
- **Logcat** для мониторинга миграций
- **MigrationTestHelper** для тестирования
- **Schema Export** для отслеживания изменений

### Полезные команды ADB:
```bash
# Просмотр списка баз данных
adb shell run-as com.wifiguard ls /data/data/com.wifiguard/databases/

# Экспорт базы данных
adb shell run-as com.wifiguard cat /data/data/com.wifiguard/databases/wifiguard_database > local_db.db

# Просмотр схемы в SQLite
adb shell run-as com.wifiguard sqlite3 /data/data/com.wifiguard/databases/wifiguard_database "PRAGMA table_info(table_name);"
```

---

## 🎓 Обучение команды

### Проведите code review с фокусом на:
1. Правильность определения Entity
2. Наличие миграций для всех изменений
3. Использование транзакций
4. Наличие тестов
5. Качество логирования

### Регулярные практики:
- Ревью всех изменений в Entity
- Peer review миграций
- Регрессионное тестирование базы данных
- Документирование всех изменений схемы

---

**Помните:** Потеря данных пользователя недопустима. Лучше потратить время на правильную миграцию, чем столкнуться с негативными отзывами и потерей доверия пользователей.

---

**Автор:** Mint1024  
**Последнее обновление:** 2025-12-07





















