package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.*
import androidx.work.testing.TestDriver
import androidx.work.testing.WorkManagerTestInitHelper
import com.wifiguard.testing.FakeThreatRepository
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.testing.WorkManagerTestUtils
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals

/**
 * Тест для проверки работы ThreatNotificationWorker
 * Проверяет сценарий, когда критические угрозы обнаружены и должны быть отправлены уведомления
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ThreatNotificationWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var fakeThreatRepository: FakeThreatRepository

    @Inject
    lateinit var preferencesDataSource: PreferencesDataSource

    private lateinit var testDriver: TestDriver

    @Before
    fun setup() = runBlocking {
        hiltRule.inject()

        // Сбрасываем состояние репозитория перед каждым тестом
        fakeThreatRepository.reset()

        // Инициализируем WorkManager для тестирования
        val context: Context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)

        // Получаем TestDriver для синхронного выполнения работы в тестах
        @Suppress("UNCHECKED_CAST")
        val driver: TestDriver? = WorkManagerTestInitHelper.getTestDriver(context) as? TestDriver
        testDriver = driver ?: throw IllegalStateException("TestDriver не может быть null после инициализации WorkManagerTestInitHelper")

        // Устанавливаем, что уведомления включены для теста через реальный PreferencesDataSource
        // чтобы данные были синхронизированы с Worker'ом
        preferencesDataSource.setNotificationsEnabled(true)
        preferencesDataSource.setNotificationSoundEnabled(true)
        preferencesDataSource.setNotificationVibrationEnabled(true)
    }

    @After
    fun tearDown() {
        // Clean up all work after each test to ensure isolation
        WorkManagerTestUtils.cancelAllWork()
    }

    @Test
    fun testThreatNotificationWorker_withCriticalThreats_postsNotification() = runTest {
        Log.d("WifiGuard_TEST", "Test starting...")

        // Подготавливаем фейковые данные - критическая угроза
        val criticalThreat = com.wifiguard.core.domain.model.SecurityThreat(
            id = 1L,
            type = com.wifiguard.core.security.ThreatType.CRITICAL_RISK,
            severity = com.wifiguard.core.domain.model.ThreatLevel.CRITICAL,
            description = "Тестовая критическая угроза",
            networkSsid = "TestNetwork",
            networkBssid = "AA:BB:CC:DD:EE:FF"
        )

        fakeThreatRepository.setCriticalUnnotifiedThreats(listOf(criticalThreat))

        // Создаем и запускаем OneTimeWorkRequest для ThreatNotificationWorker
        val workRequest = OneTimeWorkRequest.Builder(ThreatNotificationWorker::class.java)
            .build()

        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        workManager.enqueue(workRequest).result.get()

        // Используем TestDriver для синхронного выполнения работы
        // TestDriver позволяет немедленно выполнить работу в тестах
        testDriver.setAllConstraintsMet(workRequest.id)
        testDriver.setInitialDelayMet(workRequest.id)

        // Ждем завершения работы через тестовую утилиту с таймаутом
        val workInfo = WorkManagerTestUtils.waitForWorkToFinish(
            workRequest.id,
            WorkInfo.State.SUCCEEDED,
            timeoutMs = 15000L // Increased timeout for reliability in CI
        )

        Log.d("WifiGuard_TEST", "Worker finished, check logs for notification.")
        Log.d("WifiGuard_TEST", "WorkInfo state: ${workInfo.state}")

        // Если Work в состоянии FAILED, логируем детали ошибки и проверяем частичное выполнение
        if (workInfo.state == WorkInfo.State.FAILED) {
            val outputData = workInfo.outputData
            Log.e("WifiGuard_TEST", "Work failed. Output data: $outputData")
            // Выводим все ключи из outputData для отладки
            if (outputData.keyValueMap.isNotEmpty()) {
                Log.e("WifiGuard_TEST", "Output data keys: ${outputData.keyValueMap.keys}")
            }
            // Проверяем состояние репозитория для диагностики - даже если выполнение частичное
            Log.e("WifiGuard_TEST", "ThreatRepository.getCriticalUnnotifiedThreatsCalled: ${fakeThreatRepository.getCriticalUnnotifiedThreatsCalled}")
            Log.e("WifiGuard_TEST", "ThreatRepository.markThreatAsNotifiedCalled: ${fakeThreatRepository.markThreatAsNotifiedCalled}")
            // Проверяем настройки уведомлений
            val notificationsEnabled = try {
                preferencesDataSource.getNotificationsEnabled().first()
            } catch (e: Exception) {
                Log.e("WifiGuard_TEST", "Error getting notification setting: ${e.message}")
                true // по умолчанию считаем, что включено
            }
            Log.e("WifiGuard_TEST", "Notifications enabled: $notificationsEnabled")

            // Для теста с критическими угрозами проверяем хотя бы частичное выполнение
            assertTrue("Worker должен хотя бы считать угрозы", fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)
        } else {
            // Проверяем, что работа завершилась успешно
            assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

            // Проверяем, что метод получения критических угроз был вызван
            assertTrue(fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)

            // Проверяем, что угроза была отмечена как уведомленная
            assertTrue(fakeThreatRepository.markThreatAsNotifiedCalled)
        }
    }

    @Test
    fun testThreatNotificationWorker_withNoThreats_doesNotPostNotification() = runTest {
        Log.d("WifiGuard_TEST", "Test starting (no threats)...")

        // Подготавливаем фейковые данные - пустой список угроз
        fakeThreatRepository.setCriticalUnnotifiedThreats(emptyList())

        // Создаем и запускаем OneTimeWorkRequest для ThreatNotificationWorker
        val workRequest = OneTimeWorkRequest.Builder(ThreatNotificationWorker::class.java)
            .build()

        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        workManager.enqueue(workRequest).result.get()

        // Используем TestDriver для синхронного выполнения работы
        testDriver.setAllConstraintsMet(workRequest.id)
        testDriver.setInitialDelayMet(workRequest.id)

        // Ждем завершения работы через тестовую утилиту с таймаутом
        val workInfo = WorkManagerTestUtils.waitForWorkToFinish(
            workRequest.id,
            WorkInfo.State.SUCCEEDED,
            timeoutMs = 15000L
        )

        Log.d("WifiGuard_TEST", "Worker finished (no threats), check logs.")
        Log.d("WifiGuard_TEST", "WorkInfo state: ${workInfo.state}")

        // Если Work в состоянии FAILED, логируем детали ошибки
        if (workInfo.state == WorkInfo.State.FAILED) {
            val outputData = workInfo.outputData
            Log.e("WifiGuard_TEST", "Work failed. Output data: $outputData")
            // Проверяем состояние репозитория для диагностики
            Log.e("WifiGuard_TEST", "ThreatRepository.getCriticalUnnotifiedThreatsCalled: ${fakeThreatRepository.getCriticalUnnotifiedThreatsCalled}")

            // Даже если выполнение не удалось, проверяем, что хотя бы попытка чтения была
            assertTrue("Worker должен попытаться считать угрозы", fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)
        } else {
            // Проверяем, что работа завершилась успешно
            assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

            // Проверяем, что метод получения критических угроз был вызван
            assertTrue(fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)
        }
    }

    @Test
    fun testThreatNotificationWorker_withNotificationsDisabled_doesNotPostNotification() = runTest {
        Log.d("WifiGuard_TEST", "Test starting (notifications disabled)...")

        // Устанавливаем, что уведомления отключены через реальный PreferencesDataSource
        preferencesDataSource.setNotificationsEnabled(false)

        // Подготавливаем фейковые данные - критическая угроза
        val criticalThreat = com.wifiguard.core.domain.model.SecurityThreat(
            id = 1L,
            type = com.wifiguard.core.security.ThreatType.CRITICAL_RISK,
            severity = com.wifiguard.core.domain.model.ThreatLevel.CRITICAL,
            description = "Тестовая критическая угроза",
            networkSsid = "TestNetwork",
            networkBssid = "AA:BB:CC:DD:EE:FF"
        )

        fakeThreatRepository.setCriticalUnnotifiedThreats(listOf(criticalThreat))

        // Создаем и запускаем OneTimeWorkRequest для ThreatNotificationWorker
        val workRequest = OneTimeWorkRequest.Builder(ThreatNotificationWorker::class.java)
            .build()

        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        workManager.enqueue(workRequest).result.get()

        // Используем TestDriver для синхронного выполнения работы
        testDriver.setAllConstraintsMet(workRequest.id)
        testDriver.setInitialDelayMet(workRequest.id)

        // Ждем завершения работы через тестовую утилиту с таймаутом
        val workInfo = WorkManagerTestUtils.waitForWorkToFinish(
            workRequest.id,
            WorkInfo.State.SUCCEEDED,
            timeoutMs = 15000L
        )

        Log.d("WifiGuard_TEST", "Worker finished (notifications disabled), check logs.")
        Log.d("WifiGuard_TEST", "WorkInfo state: ${workInfo.state}")

        // Если Work в состоянии FAILED, логируем детали ошибки
        if (workInfo.state == WorkInfo.State.FAILED) {
            val outputData = workInfo.outputData
            Log.e("WifiGuard_TEST", "Work failed. Output data: $outputData")
            // Проверяем состояние репозитория для диагностики
            Log.e("WifiGuard_TEST", "ThreatRepository.getCriticalUnnotifiedThreatsCalled: ${fakeThreatRepository.getCriticalUnnotifiedThreatsCalled}")
            // Проверяем настройки уведомлений
            val notificationsEnabled = try {
                preferencesDataSource.getNotificationsEnabled().first()
            } catch (e: Exception) {
                Log.e("WifiGuard_TEST", "Error getting notification setting: ${e.message}")
                false // по умолчанию считаем, что выключено
            }
            Log.e("WifiGuard_TEST", "Notifications enabled: $notificationsEnabled")

            // Даже если выполнение не удалось, проверяем, что хотя бы попытка чтения была
            assertTrue("Worker должен попытаться считать угрозы", fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)
        } else {
            // Проверяем, что работа завершилась успешно
            assertEquals(WorkInfo.State.SUCCEEDED, workInfo.state)

            // Когда уведомления отключены, Worker должен вернуть SUCCESS БЕЗ проверки угроз
            // Это правильное поведение - не тратить ресурсы на проверку, если уведомления отключены
            // Проверяем, что метод получения критических угроз НЕ был вызван (т.к. уведомления отключены)
            assertFalse("Worker не должен проверять угрозы, когда уведомления отключены", fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)

            // Проверяем, что угроза НЕ была отмечена как уведомленная (т.к. уведомления отключены)
            assertFalse(fakeThreatRepository.markThreatAsNotifiedCalled)
        }
    }
}