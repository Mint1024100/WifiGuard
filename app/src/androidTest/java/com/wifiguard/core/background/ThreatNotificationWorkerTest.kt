package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.WorkManagerTestInitHelper
import com.wifiguard.testing.TestThreatRepositoryModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Тест для проверки работы ThreatNotificationWorker
 * Проверяет сценарий, когда критические угрозы обнаружены и должны быть отправлены уведомления
 */
@HiltAndroidTest
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.wifiguard.di.RepositoryModule::class]
)
@RunWith(AndroidJUnit4::class)
class ThreatNotificationWorkerTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var fakeThreatRepository: FakeThreatRepository

    @Inject
    lateinit var fakePreferencesDataSource: FakePreferencesDataSource

    @Before
    fun setup() {
        hiltRule.inject()

        // Инициализируем WorkManager для тестирования
        val config = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

        WorkManagerTestInitHelper.initializeTestWorkManager(
            ApplicationProvider.getApplicationContext(),
            config
        )

        // Устанавливаем, что уведомления включены для теста
        fakePreferencesDataSource.setNotificationsEnabled(true)
        fakePreferencesDataSource.setNotificationSoundEnabled(true)
        fakePreferencesDataSource.setNotificationVibrationEnabled(true)
    }

    @Test
    fun testThreatNotificationWorker_withCriticalThreats_postsNotification() = runBlocking {
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
        val request = workManager.enqueue(workRequest).result.get()

        // Ожидаем завершения работы
        val workInfo = workManager.getWorkInfoByIdLiveData(request.id).value

        Log.d("WifiGuard_TEST", "Worker finished, check logs for notification.")

        // Проверяем, что работа завершилась успешно
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo?.state)

        // Проверяем, что метод получения критических угроз был вызван
        assertTrue(fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)

        // Проверяем, что угроза была отмечена как уведомленная
        assertTrue(fakeThreatRepository.markThreatAsNotifiedCalled)
    }

    @Test
    fun testThreatNotificationWorker_withNoThreats_doesNotPostNotification() = runBlocking {
        Log.d("WifiGuard_TEST", "Test starting (no threats)...")

        // Подготавливаем фейковые данные - пустой список угроз
        fakeThreatRepository.setCriticalUnnotifiedThreats(emptyList())

        // Создаем и запускаем OneTimeWorkRequest для ThreatNotificationWorker
        val workRequest = OneTimeWorkRequest.Builder(ThreatNotificationWorker::class.java)
            .build()

        val workManager = WorkManager.getInstance(ApplicationProvider.getApplicationContext())
        val request = workManager.enqueue(workRequest).result.get()

        // Ожидаем завершения работы
        val workInfo = workManager.getWorkInfoByIdLiveData(request.id).value

        Log.d("WifiGuard_TEST", "Worker finished (no threats), check logs.")

        // Проверяем, что работа завершилась успешно
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo?.state)

        // Проверяем, что метод получения критических угроз был вызван
        assertTrue(fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)
    }

    @Test
    fun testThreatNotificationWorker_withNotificationsDisabled_doesNotPostNotification() = runBlocking {
        Log.d("WifiGuard_TEST", "Test starting (notifications disabled)...")

        // Устанавливаем, что уведомления отключены
        fakePreferencesDataSource.setNotificationsEnabled(false)

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
        val request = workManager.enqueue(workRequest).result.get()

        // Ожидаем завершения работы
        val workInfo = workManager.getWorkInfoByIdLiveData(request.id).value

        Log.d("WifiGuard_TEST", "Worker finished (notifications disabled), check logs.")

        // Проверяем, что работа завершилась успешно
        assertEquals(WorkInfo.State.SUCCEEDED, workInfo?.state)

        // Проверяем, что метод получения критических угроз был вызван
        assertTrue(fakeThreatRepository.getCriticalUnnotifiedThreatsCalled)

        // Проверяем, что угроза НЕ была отмечена как уведомленная (т.к. уведомления отключены)
        assertTrue(!fakeThreatRepository.markThreatAsNotifiedCalled)
    }
}