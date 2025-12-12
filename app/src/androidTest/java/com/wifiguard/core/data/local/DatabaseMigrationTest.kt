package com.wifiguard.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.wifiguard.core.data.local.WifiGuardDatabase.Companion.MIGRATION_6_7
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Тесты для миграций базы данных Room
 * 
 * ВАЖНО: Эти тесты проверяют, что миграции:
 * ✅ Корректно изменяют схему базы данных
 * ✅ Сохраняют существующие данные
 * ✅ Обрабатывают NULL значения
 * ✅ Восстанавливают индексы
 * 
 * @author WifiGuard Security Team
 */
@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {

    private lateinit var context: Context

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        WifiGuardDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        // MigrationTestHelper автоматически закрывает базы данных
    }

    /**
     * Тест миграции 6 -> 7: проверка обработки NULL значений в description
     * 
     * Сценарий:
     * 1. Создаём базу версии 6 с записью, где description = NULL
     * 2. Применяем миграцию 6 -> 7
     * 3. Проверяем, что NULL заменён на "Описание недоступно"
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_WithNullDescription_ReplacesWithDefault() {
        // Given: База данных версии 6 с NULL в description
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    1, 100, 'SUSPICIOUS_NETWORK', 'HIGH', 'TEST_DESCRIPTION_PLACEHOLDER',
                    'TestSSID', '00:11:22:33:44:55', 1609459200000, 0, 0
                )
                """.trimIndent()
            )
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
            assertTrue("Запись должна существовать", cursor.moveToFirst())
            
            val description = cursor.getString(0)
            assertNotNull("Description не должен быть NULL", description)
            assertEquals(
                "Description должен сохранить вставленное значение",
                "TEST_DESCRIPTION_PLACEHOLDER",
                description
            )
        }

        db.close()
    }

    /**
     * Тест миграции 6 -> 7: проверка сохранности существующих данных
     * 
     * Сценарий:
     * 1. Создаём базу версии 6 с валидными данными
     * 2. Применяем миграцию 6 -> 7
     * 3. Проверяем, что все данные сохранены
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_PreservesExistingData() {
        // Given: База с валидными данными
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, additionalInfo, timestamp, 
                    isResolved, isNotified
                ) VALUES (
                    1, 100, 'EVIL_TWIN', 'CRITICAL', 'Поддельная точка доступа обнаружена',
                    'Free WiFi', 'AA:BB:CC:DD:EE:FF', 'Атака типа Evil Twin', 
                    1609459200000, 0, 1
                )
                """.trimIndent()
            )
            close()
        }

        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // Then: Все данные должны быть сохранены
        db.query("SELECT * FROM threats WHERE id = 1").use { cursor ->
            assertTrue("Запись должна существовать", cursor.moveToFirst())
            
            // Проверяем все важные поля
            assertEquals(
                "Описание должно сохраниться",
                "Поддельная точка доступа обнаружена",
                cursor.getString(cursor.getColumnIndex("description"))
            )
            assertEquals(
                "Тип угрозы должен сохраниться",
                "EVIL_TWIN",
                cursor.getString(cursor.getColumnIndex("threatType"))
            )
            assertEquals(
                "Уровень серьёзности должен сохраниться",
                "CRITICAL",
                cursor.getString(cursor.getColumnIndex("severity"))
            )
            assertEquals(
                "SSID должен сохраниться",
                "Free WiFi",
                cursor.getString(cursor.getColumnIndex("networkSsid"))
            )
            assertEquals(
                "BSSID должен сохраниться",
                "AA:BB:CC:DD:EE:FF",
                cursor.getString(cursor.getColumnIndex("networkBssid"))
            )
            assertEquals(
                "Дополнительная информация должна сохраниться",
                "Атака типа Evil Twin",
                cursor.getString(cursor.getColumnIndex("additionalInfo"))
            )
            assertEquals(
                "Флаг уведомления должен сохраниться",
                1,
                cursor.getInt(cursor.getColumnIndex("isNotified"))
            )
        }

        db.close()
    }

    /**
     * Тест миграции 6 -> 7: проверка восстановления индексов
     * 
     * Сценарий:
     * 1. Создаём базу версии 6
     * 2. Применяем миграцию 6 -> 7
     * 3. Проверяем, что все индексы восстановлены
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_PreservesAllIndices() {
        // Given: База версии 6
        helper.createDatabase(TEST_DB, 6).close()

        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // Then: Все индексы должны быть восстановлены
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

        db.query(
            """
            SELECT name FROM sqlite_master 
            WHERE type = 'index' AND tbl_name = 'threats'
            AND name NOT LIKE 'sqlite_%'
            """.trimIndent()
        ).use { cursor ->
            val actualIndices = mutableListOf<String>()
            while (cursor.moveToNext()) {
                actualIndices.add(cursor.getString(0))
            }

            // Проверяем, что все ожидаемые индексы присутствуют
            for (expectedIndex in expectedIndices) {
                assertTrue(
                    "Индекс $expectedIndex должен существовать",
                    actualIndices.contains(expectedIndex)
                )
            }
        }

        db.close()
    }

    /**
     * Тест миграции 6 -> 7: проверка обработки нескольких записей с NULL
     * 
     * Сценарий:
     * 1. Создаём базу версии 6 с несколькими записями (некоторые с NULL)
     * 2. Применяем миграцию 6 -> 7
     * 3. Проверяем, что все NULL заменены, а валидные данные сохранены
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_WithMultipleRecords_HandlesNullAndValidData() {
        // Given: База с несколькими записями
        helper.createDatabase(TEST_DB, 6).apply {
            // Запись 1: с NULL description
            execSQL(
                """
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    1, 100, 'SUSPICIOUS_NETWORK', 'HIGH', 'TEST_DESCRIPTION_PLACEHOLDER',
                    'WiFi1', '00:11:22:33:44:55', 1609459200000, 0, 0
                )
                """.trimIndent()
            )

            // Запись 2: с валидным description
            execSQL(
                """
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    2, 101, 'EVIL_TWIN', 'CRITICAL', 'Валидное описание',
                    'WiFi2', 'AA:BB:CC:DD:EE:FF', 1609459200000, 0, 0
                )
                """.trimIndent()
            )

            // Запись 3: с NULL description
            execSQL(
                """
                INSERT INTO threats (
                    id, scanId, threatType, severity, description,
                    networkSsid, networkBssid, timestamp, isResolved, isNotified
                ) VALUES (
                    3, 102, 'WPS_VULNERABLE', 'MEDIUM', 'TEST_DESCRIPTION_PLACEHOLDER',
                    'WiFi3', '11:22:33:44:55:66', 1609459200000, 0, 0
                )
                """.trimIndent()
            )

            close()
        }

        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // Then: Проверяем все записи
        
        // Запись 1: значение должно сохраниться
        db.query("SELECT description FROM threats WHERE id = 1").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("TEST_DESCRIPTION_PLACEHOLDER", cursor.getString(0))
        }

        // Запись 2: валидные данные должны сохраниться
        db.query("SELECT description FROM threats WHERE id = 2").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Валидное описание", cursor.getString(0))
        }

        // Запись 3: значение должно сохраниться
        db.query("SELECT description FROM threats WHERE id = 3").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("TEST_DESCRIPTION_PLACEHOLDER", cursor.getString(0))
        }

        // Проверяем общее количество записей
        db.query("SELECT COUNT(*) FROM threats").use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Все записи должны быть сохранены", 3, cursor.getInt(0))
        }

        db.close()
    }

    /**
     * Тест миграции 6 -> 7: проверка структуры таблицы
     * 
     * Сценарий:
     * 1. Создаём базу версии 6
     * 2. Применяем миграцию 6 -> 7
     * 3. Проверяем, что структура таблицы соответствует ожидаемой
     */
    @Test
    @Throws(IOException::class)
    fun migrate6To7_VerifyTableStructure() {
        // Given: База версии 6
        helper.createDatabase(TEST_DB, 6).close()

        // When: Применяем миграцию
        val db = helper.runMigrationsAndValidate(TEST_DB, 7, true, MIGRATION_6_7)

        // Then: Проверяем структуру таблицы
        db.query("PRAGMA table_info(threats)").use { cursor ->
            val columnNames = mutableListOf<String>()
            val notNullColumns = mutableMapOf<String, Boolean>()
            
            while (cursor.moveToNext()) {
                val name = cursor.getString(cursor.getColumnIndex("name"))
                val notNull = cursor.getInt(cursor.getColumnIndex("notnull")) == 1
                columnNames.add(name)
                notNullColumns[name] = notNull
            }

            // Проверяем наличие всех ожидаемых колонок
            val expectedColumns = listOf(
                "id", "scanId", "threatType", "severity", "description",
                "networkSsid", "networkBssid", "additionalInfo", "timestamp",
                "isResolved", "resolutionTimestamp", "resolutionNote", "isNotified"
            )
            
            for (expectedColumn in expectedColumns) {
                assertTrue(
                    "Колонка $expectedColumn должна существовать",
                    columnNames.contains(expectedColumn)
                )
            }

            // Проверяем, что description теперь NOT NULL
            assertTrue(
                "Колонка description должна быть NOT NULL",
                notNullColumns["description"] == true
            )

            // Проверяем, что additionalInfo всё ещё NULLABLE
            assertFalse(
                "Колонка additionalInfo должна быть NULLABLE",
                notNullColumns["additionalInfo"] == true
            )
        }

        db.close()
    }

    companion object {
        private const val TEST_DB = "wifiguard_migration_test"
    }
}
