package com.wifiguard.testing

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.mutablePreferencesOf
import com.wifiguard.core.domain.model.SecurityThreat
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.repository.ThreatRepository
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.notification.INotificationHelper
import com.wifiguard.core.data.preferences.PreferencesDataSource
import com.wifiguard.core.data.preferences.PreferencesDataSource as CorePreferencesDataSource
import com.wifiguard.core.data.preferences.PreferencesKeys
import com.wifiguard.core.data.preferences.AppSettings
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import javax.inject.Singleton

/**
 * Фейковая реализация ThreatRepository для тестирования
 */
class FakeThreatRepository @javax.inject.Inject constructor() : ThreatRepository {
    private var criticalUnnotifiedThreats: List<SecurityThreat> = emptyList()
    var getCriticalUnnotifiedThreatsCalled = false
        private set
    var markThreatAsNotifiedCalled = false
        private set

    fun setCriticalUnnotifiedThreats(threats: List<SecurityThreat>) {
        this.criticalUnnotifiedThreats = threats
    }

    /**
     * Сбросить состояние репозитория между тестами
     */
    fun reset() {
        criticalUnnotifiedThreats = emptyList()
        getCriticalUnnotifiedThreatsCalled = false
        markThreatAsNotifiedCalled = false
    }

    override fun getAllThreats(): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByScanId(scanId: Long): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByType(type: com.wifiguard.core.security.ThreatType): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsBySeverity(severity: com.wifiguard.core.domain.model.ThreatLevel): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByNetworkSsid(ssid: String): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getThreatsByNetworkBssid(bssid: String): Flow<List<SecurityThreat>> = flowOf(emptyList())

    override fun getUnresolvedThreatsByNetworkBssid(bssid: String): Flow<List<SecurityThreat>> = flowOf(emptyList())

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
 * Так как PreferencesDataSource - final класс, создаем отдельный класс с теми же методами
 */
class FakePreferencesDataSource {
    private var autoScanEnabled = true
    private var scanIntervalMinutes = 15
    private var notificationsEnabled = true
    private var notificationSoundEnabled = true
    private var notificationVibrationEnabled = true
    private var dataRetentionDays = 30
    private var themeMode = "system"

    fun getAutoScanEnabled(): Flow<Boolean> = flowOf(autoScanEnabled)
    
    suspend fun setAutoScanEnabled(enabled: Boolean) {
        autoScanEnabled = enabled
    }
    
    fun getScanIntervalMinutes(): Flow<Int> = flowOf(scanIntervalMinutes)
    
    suspend fun setScanIntervalMinutes(minutes: Int) {
        scanIntervalMinutes = minutes
    }

    fun getNotificationsEnabled(): Flow<Boolean> = flowOf(notificationsEnabled)
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }
    
    fun getNotificationSoundEnabled(): Flow<Boolean> = flowOf(notificationSoundEnabled)
    
    suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        notificationSoundEnabled = enabled
    }
    
    fun getNotificationVibrationEnabled(): Flow<Boolean> = flowOf(notificationVibrationEnabled)
    
    suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        notificationVibrationEnabled = enabled
    }
    
    fun getDataRetentionDays(): Flow<Int> = flowOf(dataRetentionDays)
    
    suspend fun setDataRetentionDays(days: Int) {
        dataRetentionDays = days
    }
    
    fun getThemeMode(): Flow<String> = flowOf(themeMode)
    
    suspend fun setThemeMode(mode: String) {
        themeMode = mode
    }
    
    fun getAllSettings(): Flow<AppSettings> = flowOf(
        AppSettings(
            autoScanEnabled = autoScanEnabled,
            scanIntervalMinutes = scanIntervalMinutes,
            notificationsEnabled = notificationsEnabled,
            notificationSoundEnabled = notificationSoundEnabled,
            notificationVibrationEnabled = notificationVibrationEnabled,
            dataRetentionDays = dataRetentionDays,
            themeMode = themeMode
        )
    )
    
    suspend fun updateSettings(settings: AppSettings) {
        autoScanEnabled = settings.autoScanEnabled
        scanIntervalMinutes = settings.scanIntervalMinutes
        notificationsEnabled = settings.notificationsEnabled
        notificationSoundEnabled = settings.notificationSoundEnabled
        notificationVibrationEnabled = settings.notificationVibrationEnabled
        dataRetentionDays = settings.dataRetentionDays
        themeMode = settings.themeMode
    }
    
    suspend fun clearAllSettings() {
        autoScanEnabled = true
        scanIntervalMinutes = 15
        notificationsEnabled = true
        notificationSoundEnabled = true
        notificationVibrationEnabled = true
        dataRetentionDays = 30
        themeMode = "system"
    }
}

/**
 * Фейковая реализация NotificationHelper для тестирования
 */
class FakeNotificationHelper @javax.inject.Inject constructor() : INotificationHelper {
    var showThreatNotificationCalled = false
    var showThreatNotificationWithBssidCalled = false
    var lastNotificationTitle: String? = null
    var lastNotificationContent: String? = null
    var lastNetworkBssid: String? = null
    var lastThreatLevel: ThreatLevel? = null

    override fun createNotificationChannel() {
        // Не требуется для теста
    }

    override suspend fun showThreatNotification(
        networkBssid: String,
        threatLevel: ThreatLevel,
        title: String,
        content: String,
        notificationId: Int?
    ): Boolean {
        showThreatNotificationCalled = true
        showThreatNotificationWithBssidCalled = true
        lastNetworkBssid = networkBssid
        lastThreatLevel = threatLevel
        lastNotificationTitle = title
        lastNotificationContent = content
        android.util.Log.d("WifiGuardDebug", "FakeNotificationHelper: Posting notification for threat: $content")
        return true
    }

    @Deprecated(
        message = "Используйте версию с параметрами networkBssid и threatLevel",
        replaceWith = ReplaceWith("showThreatNotification(\"\", ThreatLevel.LOW, title, content)")
    )
    override fun showThreatNotification(
        title: String,
        content: String,
        vibrationEnabled: Boolean,
        soundEnabled: Boolean
    ): Boolean {
        showThreatNotificationCalled = true
        lastNotificationTitle = title
        lastNotificationContent = content
        android.util.Log.d("WifiGuardDebug", "FakeNotificationHelper: Posting notification for threat: $content")
        return true
    }

    override fun cancelNotification() {
        // Не требуется для теста
    }

    override fun checkNotificationPermission(): Boolean = true

    override fun areNotificationsEnabled(): Boolean = true

    override fun testNotification(): Boolean = true

    override fun getNotificationStatus(): String = "Test notification status"
}

/**
 * Фейковая реализация SettingsRepository для тестирования
 */
class FakeSettingsRepository @javax.inject.Inject constructor() : SettingsRepository {
    private var autoScanEnabled = true
    private var scanIntervalMinutes = 15
    private var notificationsEnabled = true
    private var notificationSoundEnabled = true
    private var notificationVibrationEnabled = true
    private var dataRetentionDays = 30
    private var themeMode = "system"

    override fun getAutoScanEnabled(): Flow<Boolean> = flowOf(autoScanEnabled)

    override suspend fun setAutoScanEnabled(enabled: Boolean) {
        autoScanEnabled = enabled
    }

    override fun getScanIntervalMinutes(): Flow<Int> = flowOf(scanIntervalMinutes)

    override suspend fun setScanIntervalMinutes(minutes: Int) {
        scanIntervalMinutes = minutes
    }

    override fun getNotificationsEnabled(): Flow<Boolean> = flowOf(notificationsEnabled)

    override suspend fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
    }

    override fun getNotificationSoundEnabled(): Flow<Boolean> = flowOf(notificationSoundEnabled)

    override suspend fun setNotificationSoundEnabled(enabled: Boolean) {
        notificationSoundEnabled = enabled
    }

    override fun getNotificationVibrationEnabled(): Flow<Boolean> = flowOf(notificationVibrationEnabled)

    override suspend fun setNotificationVibrationEnabled(enabled: Boolean) {
        notificationVibrationEnabled = enabled
    }

    override fun getDataRetentionDays(): Flow<Int> = flowOf(dataRetentionDays)

    override suspend fun setDataRetentionDays(days: Int) {
        dataRetentionDays = days
    }

    override fun getAutoDisableWifiOnCritical(): Flow<Boolean> = flowOf(false)

    override suspend fun setAutoDisableWifiOnCritical(enabled: Boolean) {
        // No-op для тестов
    }

    override fun getThemeMode(): Flow<String> = flowOf(themeMode)

    override suspend fun setThemeMode(mode: String) {
        themeMode = mode
    }

    override fun getAllSettings(): Flow<AppSettings> = flowOf(
        AppSettings(
            autoScanEnabled = autoScanEnabled,
            scanIntervalMinutes = scanIntervalMinutes,
            notificationsEnabled = notificationsEnabled,
            notificationSoundEnabled = notificationSoundEnabled,
            notificationVibrationEnabled = notificationVibrationEnabled,
            dataRetentionDays = dataRetentionDays,
            themeMode = themeMode
        )
    )

    override suspend fun updateSettings(settings: AppSettings) {
        autoScanEnabled = settings.autoScanEnabled
        scanIntervalMinutes = settings.scanIntervalMinutes
        notificationsEnabled = settings.notificationsEnabled
        notificationSoundEnabled = settings.notificationSoundEnabled
        notificationVibrationEnabled = settings.notificationVibrationEnabled
        dataRetentionDays = settings.dataRetentionDays
        themeMode = settings.themeMode
    }

    override suspend fun clearAllSettings() {
        autoScanEnabled = true
        scanIntervalMinutes = 15
        notificationsEnabled = true
        notificationSoundEnabled = true
        notificationVibrationEnabled = true
        dataRetentionDays = 30
        themeMode = "system"
    }

    override suspend fun resetToDefaults() {
        clearAllSettings()
    }
}

/**
 * Фейковая реализация WifiRepository для тестирования
 */
class FakeWifiRepository @javax.inject.Inject constructor() : com.wifiguard.core.domain.repository.WifiRepository {
    private val networks = mutableListOf<com.wifiguard.core.domain.model.WifiNetwork>()
    private val scans = mutableListOf<com.wifiguard.core.domain.model.WifiScanResult>()
    private val suspiciousNetworks = mutableListOf<com.wifiguard.core.domain.model.WifiNetwork>()

    override fun getAllNetworks(): Flow<List<com.wifiguard.core.domain.model.WifiNetwork>> = flowOf(networks.toList())

    override suspend fun getNetworkBySSID(ssid: String): com.wifiguard.core.domain.model.WifiNetwork? {
        return networks.find { it.ssid == ssid }
    }

    override suspend fun getNetworkByBssid(bssid: String): com.wifiguard.core.domain.model.WifiNetwork? {
        return networks.find { it.bssid == bssid }
    }

    override suspend fun insertNetwork(network: com.wifiguard.core.domain.model.WifiNetwork) {
        networks.add(network)
    }

    override suspend fun updateNetwork(network: com.wifiguard.core.domain.model.WifiNetwork) {
        val index = networks.indexOfFirst { it.ssid == network.ssid }
        if (index != -1) {
            networks[index] = network
        }
    }

    override suspend fun deleteNetwork(network: com.wifiguard.core.domain.model.WifiNetwork) {
        networks.remove(network)
    }

    override fun getLatestScans(limit: Int): Flow<List<com.wifiguard.core.domain.model.WifiScanResult>> = flowOf(scans.take(limit))

    override suspend fun insertScanResult(scanResult: com.wifiguard.core.domain.model.WifiScanResult) {
        scans.add(scanResult)
    }

    override suspend fun insertScanResults(scanResults: List<com.wifiguard.core.domain.model.WifiScanResult>) {
        scans.addAll(scanResults)
    }

    override suspend fun upsertNetworksFromScanResults(scanResults: List<com.wifiguard.core.domain.model.WifiScanResult>) {
        // No-op for tests
    }

    override suspend fun persistScanResults(scanResults: List<com.wifiguard.core.domain.model.WifiScanResult>) {
        scans.addAll(scanResults)
    }

    override suspend fun clearOldScans(olderThanMillis: Long) {
        scans.removeAll { it.timestamp < olderThanMillis }
    }

    override suspend fun deleteScansOlderThan(timestampMillis: Long): Int {
        val initialSize = scans.size
        scans.removeAll { it.timestamp < timestampMillis }
        return initialSize - scans.size
    }

    override suspend fun getTotalScansCount(): Int = scans.size

    override suspend fun optimizeDatabase() {
        // No-op for tests
    }

    override fun getNetworkStatistics(ssid: String): Flow<List<com.wifiguard.core.domain.model.WifiScanResult>> {
        return flowOf(scans.filter { it.ssid == ssid })
    }

    override fun getNetworkStatisticsByBssid(bssid: String): Flow<List<com.wifiguard.core.domain.model.WifiScanResult>> {
        return flowOf(scans.filter { it.bssid == bssid })
    }

    override suspend fun markNetworkAsSuspicious(ssid: String, reason: String) {
        val network = networks.find { it.ssid == ssid }
        if (network != null) {
            val updatedNetwork = network.copy(
                isSuspicious = true,
                suspiciousReason = reason
            )
            updateNetwork(updatedNetwork)
            if (!suspiciousNetworks.any { it.ssid == ssid }) {
                suspiciousNetworks.add(updatedNetwork)
            }
        }
    }

    override fun getSuspiciousNetworks(): Flow<List<com.wifiguard.core.domain.model.WifiNetwork>> = flowOf(suspiciousNetworks.toList())

    override suspend fun clearAllData() {
        networks.clear()
        scans.clear()
        suspiciousNetworks.clear()
    }

    override suspend fun validateDatabaseIntegrity(): Boolean = true
}

/**
 * Тестовый Hilt модуль для замены реальных реализаций ThreatRepository и других зависимостей
 */
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [com.wifiguard.di.RepositoryModule::class, com.wifiguard.di.NotificationModule::class, com.wifiguard.di.DataModule::class]
)
@Module
object TestThreatRepositoryModule {

    // Храним singleton экземпляры на уровне модуля
    private val fakeThreatRepositoryInstance = FakeThreatRepository()
    private val fakeNotificationHelperInstance = FakeNotificationHelper()
    private val fakeWifiRepositoryInstance = FakeWifiRepository()
    private val fakeSettingsRepositoryInstance = FakeSettingsRepository()
    private val fakePreferencesDataSourceInstance = FakePreferencesDataSource()
    
    // Создаем singleton DataStore с общим MutableStateFlow
    private val preferencesFlow = kotlinx.coroutines.flow.MutableStateFlow(emptyPreferences())
    private val mockDataStoreInstance: DataStore<Preferences> = object : DataStore<Preferences> {
        override val data: Flow<Preferences> = preferencesFlow

        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
            val currentPreferences = preferencesFlow.value
            val updatedPreferences = transform(currentPreferences)
            preferencesFlow.value = updatedPreferences
            return updatedPreferences
        }
    }

    /**
     * Предоставляет FakeThreatRepository как singleton для всех зависимостей
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideFakeThreatRepository(): FakeThreatRepository {
        return fakeThreatRepositoryInstance
    }

    /**
     * Связывает FakeThreatRepository с ThreatRepository интерфейсом
     */
    @Provides
    @Singleton
    fun provideThreatRepository(
        fakeThreatRepository: FakeThreatRepository
    ): ThreatRepository = fakeThreatRepository

    /**
     * Предоставляет FakeNotificationHelper как singleton
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideFakeNotificationHelper(): FakeNotificationHelper {
        return fakeNotificationHelperInstance
    }

    /**
     * Связывает FakeNotificationHelper с INotificationHelper интерфейсом
     */
    @Provides
    @Singleton
    fun provideNotificationHelper(
        fakeNotificationHelper: FakeNotificationHelper
    ): INotificationHelper = fakeNotificationHelper

    /**
     * Предоставляет FakeWifiRepository как singleton
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideFakeWifiRepository(): FakeWifiRepository {
        return fakeWifiRepositoryInstance
    }

    /**
     * Связывает FakeWifiRepository с WifiRepository интерфейсом
     */
    @Provides
    @Singleton
    fun provideWifiRepository(
        fakeWifiRepository: FakeWifiRepository
    ): com.wifiguard.core.domain.repository.WifiRepository = fakeWifiRepository

    /**
     * Предоставляет FakeSettingsRepository как singleton
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideFakeSettingsRepository(): FakeSettingsRepository {
        return fakeSettingsRepositoryInstance
    }

    /**
     * Связывает FakeSettingsRepository с SettingsRepository интерфейсом
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(
        fakeSettingsRepository: FakeSettingsRepository
    ): SettingsRepository = fakeSettingsRepository

    /**
     * Предоставляет моковый DataStore для тестирования
     * Поддерживает чтение/запись настроек через MutableStateFlow
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideMockDataStore(): DataStore<Preferences> {
        return mockDataStoreInstance
    }

    /**
     * Предоставляет FakePreferencesDataSource для прямого инжектирования в тестах
     * Всегда возвращает один и тот же экземпляр
     */
    @Provides
    @Singleton
    fun provideFakePreferencesDataSource(): FakePreferencesDataSource {
        return fakePreferencesDataSourceInstance
    }

    /**
     * Предоставляет PreferencesDataSource для Worker'ов в тестах
     * Использует моковый DataStore для тестирования
     */
    @Provides
    @Singleton
    fun providePreferencesDataSource(
        mockDataStore: DataStore<Preferences>
    ): CorePreferencesDataSource {
        return CorePreferencesDataSource(mockDataStore)
    }
}