package com.wifiguard.testing

import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.notification.NotificationHelper
import com.wifiguard.core.data.preferences.PreferencesDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Singleton

/**
 * Фейковая реализация ThreatRepository для тестирования
 */
class FakeThreatRepository : ThreatRepository {
    private var criticalUnnotifiedThreats: List<SecurityThreat> = emptyList()
    var getCriticalUnnotifiedThreatsCalled = false
    var markThreatAsNotifiedCalled = false

    fun setCriticalUnnotifiedThreats(threats: List<SecurityThreat>) {
        this.criticalUnnotifiedThreats = threats
    }

    override fun getAllThreats(): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByScanId(scanId: Long): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByType(type: com.wifiguard.core.security.ThreatType): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsBySeverity(severity: com.wifiguard.core.domain.model.ThreatLevel): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByNetworkSsid(ssid: String): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getUnresolvedThreats(): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsFromTimestamp(fromTimestamp: Long): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override suspend fun getCriticalUnnotifiedThreats(): List<SecurityThreat> {
        getCriticalUnnotifiedThreatsCalled = true
        return criticalUnnotifiedThreats
    }

    override suspend fun insertThreat(threat: SecurityThreat): Long = 0L

    override suspend fun insertThreats(threats: List<SecurityThreat>): List<Long> = emptyList()

    override suspend fun updateThreat(threat: SecurityThreat) {}

    override suspend fun deleteThreat(threat: SecurityThreat) {}

    override suspend fun markThreatAsNotified(threatId: Long) {
        markThreatAsNotifiedCalled = true
    }

    override suspend fun markThreatsAsNotifiedForScan(scanId: Long) {}

    override suspend fun resolveThreat(threatId: Long, resolutionNote: String?) {}

    override suspend fun unresolveThreat(threatId: Long) {}
}

/**
 * Фейковая реализация PreferencesDataSource для тестирования
 */
class FakePreferencesDataSource {
    private var notificationsEnabled = false
    private var notificationSoundEnabled = false
    private var notificationVibrationEnabled = false

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    fun setNotificationSoundEnabled(enabled: Boolean) {
        notificationSoundEnabled = enabled
    }

    fun setNotificationVibrationEnabled(enabled: Boolean) {
        notificationVibrationEnabled = enabled
    }

    fun getNotificationsEnabled() = kotlinx.coroutines.flow.flowOf(notificationsEnabled)
    fun getNotificationSoundEnabled() = kotlinx.coroutines.flow.flowOf(notificationSoundEnabled)
    fun getNotificationVibrationEnabled() = kotlinx.coroutines.flow.flowOf(notificationVibrationEnabled)
}

/**
 * Фейковая реализация NotificationHelper для тестирования
 */
class FakeNotificationHelper : NotificationHelper {
    var showThreatNotificationCalled = false
    var lastNotificationTitle: String? = null
    var lastNotificationContent: String? = null

    override fun createNotificationChannel() {
        // Не требуется для теста
    }

    override fun showThreatNotification(
        title: String,
        content: String,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean
    ) {
        showThreatNotificationCalled = true
        lastNotificationTitle = title
        lastNotificationContent = content
    }
}

/**
 * Тестовый Hilt модуль для замены реальных реализаций ThreatRepository и других зависимостей
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.wifiguard.di.RepositoryModule::class]
)
@Module
abstract class TestThreatRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindThreatRepository(
        fakeThreatRepository: FakeThreatRepository
    ): ThreatRepository

    @Binds
    @Singleton
    abstract fun bindPreferencesDataSource(
        fakePreferencesDataSource: FakePreferencesDataSource
    ): PreferencesDataSource

    @Binds
    @Singleton
    abstract fun bindNotificationHelper(
        fakeNotificationHelper: FakeNotificationHelper
    ): NotificationHelper
}