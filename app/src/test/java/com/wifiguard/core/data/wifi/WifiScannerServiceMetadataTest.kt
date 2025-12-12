package com.wifiguard.core.data.wifi

import android.app.ActivityManager
import android.content.Context
import android.net.wifi.WifiManager
import android.os.Build
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.ScanSource
import com.wifiguard.core.domain.model.WifiScanStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Тесты для проверки работы WifiScannerService с метаданными сканирования
 * 
 * Проверяет:
 * - Корректное определение свежести данных (FRESH, STALE, EXPIRED)
 * - Корректное определение источника данных (ACTIVE_SCAN, SYSTEM_CACHE)
 * - Throttling на Android 10+
 * - Fail-safe поведение при ограничениях
 * 
 * ИСПРАВЛЕНО:
 * - RunningAppProcessInfo.processName и importance - это ПОЛЯ, а не методы!
 * - MockK не может перехватить доступ к полям через every { field }
 * - Используем реальные объекты RunningAppProcessInfo с установкой полей напрямую
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q]) // Android 10
class WifiScannerServiceMetadataTest {
    
    private lateinit var context: Context
    private lateinit var wifiManager: WifiManager
    private lateinit var activityManager: ActivityManager
    private lateinit var wifiScannerService: WifiScannerService
    
    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        wifiManager = mockk(relaxed = true)
        activityManager = mockk(relaxed = true)
        
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { context.getSystemService(Context.ACTIVITY_SERVICE) } returns activityManager
        every { context.packageName } returns "com.wifiguard"
        every { wifiManager.isWifiEnabled } returns true
        
        wifiScannerService = WifiScannerService(context, wifiManager)
    }
    
    /**
     * Создаёт реальный объект RunningAppProcessInfo с заданными параметрами.
     * ВАЖНО: processName и importance - это публичные ПОЛЯ, не методы,
     * поэтому MockK не может их перехватить через every { }.
     */
    private fun createProcessInfo(
        name: String,
        importanceLevel: Int
    ): ActivityManager.RunningAppProcessInfo {
        return ActivityManager.RunningAppProcessInfo().apply {
            processName = name
            importance = importanceLevel
        }
    }
    
    @Test
    fun `getScanResultsWithMetadata should return FRESH for recent scan`() = runTest {
        // Given: Недавнее сканирование (< 5 минут)
        every { wifiManager.scanResults } returns emptyList()
        every { wifiManager.startScan() } returns true
        
        // Выполняем сканирование
        val scanStatus = wifiScannerService.startScan()
        assertTrue(scanStatus is WifiScanStatus.Success)
        
        // When: Получаем результаты сразу после сканирования
        val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then: Данные должны быть FRESH
        assertEquals("Recent scan should have FRESH data", Freshness.FRESH, metadata.freshness)
        assertEquals("Recent scan should be from ACTIVE_SCAN", ScanSource.ACTIVE_SCAN, metadata.source)
    }
    
    @Test
    fun `getScanResultsWithMetadata should return STALE for old scan`() = runTest {
        // Given: Старое сканирование (10 минут назад)
        // Используем reflection для установки lastSuccessfulScan
        val lastScanField = WifiScannerService::class.java.getDeclaredField("lastSuccessfulScan")
        lastScanField.isAccessible = true
        lastScanField.set(wifiScannerService, System.currentTimeMillis() - 600_000L) // 10 минут назад
        
        every { wifiManager.scanResults } returns emptyList()
        
        // When
        val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then: Данные должны быть STALE
        assertEquals("10-minute old scan should have STALE data", Freshness.STALE, metadata.freshness)
        assertEquals("Old scan should be from SYSTEM_CACHE", ScanSource.SYSTEM_CACHE, metadata.source)
    }
    
    @Test
    fun `getScanResultsWithMetadata should return EXPIRED for very old scan`() = runTest {
        // Given: Очень старое сканирование (35 минут назад)
        val lastScanField = WifiScannerService::class.java.getDeclaredField("lastSuccessfulScan")
        lastScanField.isAccessible = true
        lastScanField.set(wifiScannerService, System.currentTimeMillis() - 2_100_000L) // 35 минут назад
        
        every { wifiManager.scanResults } returns emptyList()
        
        // When
        val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then: Данные должны быть EXPIRED
        assertEquals("35-minute old scan should have EXPIRED data", Freshness.EXPIRED, metadata.freshness)
        assertEquals("Very old scan should be from SYSTEM_CACHE", ScanSource.SYSTEM_CACHE, metadata.source)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q]) // Android 10
    fun `startScan on Android 10+ should return Throttled when called too frequently in background`() = runTest {
        // Given: Приложение в background
        // ИСПРАВЛЕНО: используем реальный объект с установкой полей напрямую
        val backgroundProcessInfo = createProcessInfo(
            name = "com.wifiguard",
            importanceLevel = ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND
        )
        every { activityManager.runningAppProcesses } returns listOf(backgroundProcessInfo)
        
        // Первое сканирование успешно
        every { wifiManager.startScan() } returns true
        val firstScan = wifiScannerService.startScan()
        assertTrue(firstScan is WifiScanStatus.Success)
        
        // When: Пытаемся сканировать снова сразу (в течение 30 минут)
        val secondScan = wifiScannerService.startScan()
        
        // Then: Должно вернуть Throttled
        assertTrue("Android 10+ background scan should be throttled", secondScan is WifiScanStatus.Throttled)
        assertTrue("Next available time should be in the future", (secondScan as WifiScanStatus.Throttled).nextAvailableTime > System.currentTimeMillis())
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q]) // Android 10
    fun `startScan on Android 10+ should allow scan in foreground after 30 seconds`() = runTest {
        // Given: Приложение в foreground
        // ИСПРАВЛЕНО: используем реальный объект с установкой полей напрямую
        val foregroundProcessInfo = createProcessInfo(
            name = "com.wifiguard",
            importanceLevel = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        )
        every { activityManager.runningAppProcesses } returns listOf(foregroundProcessInfo)
        
        // Первое сканирование
        every { wifiManager.startScan() } returns true
        val firstScan = wifiScannerService.startScan()
        assertTrue(firstScan is WifiScanStatus.Success)
        
        // When: Пытаемся сканировать снова в течение 30 секунд
        val secondScan = wifiScannerService.startScan()
        
        // Then: Должно вернуть Throttled (foreground throttle = 30 секунд)
        assertTrue("Android 10+ foreground scan should be throttled within 30 seconds", secondScan is WifiScanStatus.Throttled)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.Q]) // Android 10
    fun `startScan on Android 10+ should return Restricted when startScan returns false`() = runTest {
        // Given: WifiManager.startScan() возвращает false (система ограничила)
        every { wifiManager.startScan() } returns false
        
        // ИСПРАВЛЕНО: используем реальный объект с установкой полей напрямую
        val foregroundProcessInfo = createProcessInfo(
            name = "com.wifiguard",
            importanceLevel = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        )
        every { activityManager.runningAppProcesses } returns listOf(foregroundProcessInfo)
        
        // When
        val scanStatus = wifiScannerService.startScan()
        
        // Then: Должно вернуть Restricted
        assertTrue("When startScan returns false, should return Restricted", scanStatus is WifiScanStatus.Restricted)
        assertTrue("Restricted status should contain reason", (scanStatus as WifiScanStatus.Restricted).reason.contains("restricted", ignoreCase = true))
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Android 9
    fun `startScan on Android 9 should not throttle`() = runTest {
        // Given: Android 9 (без throttling)
        every { wifiManager.startScan() } returns true
        
        // When: Выполняем несколько сканирований подряд
        val firstScan = wifiScannerService.startScan()
        val secondScan = wifiScannerService.startScan()
        val thirdScan = wifiScannerService.startScan()
        
        // Then: Все должны быть успешными (нет throttling на Android 9)
        assertTrue(firstScan is WifiScanStatus.Success)
        assertTrue(secondScan is WifiScanStatus.Success)
        assertTrue(thirdScan is WifiScanStatus.Success)
        
        verify(exactly = 3) { wifiManager.startScan() }
    }
    
    @Test
    fun `startScan should return Failed when WiFi is disabled`() = runTest {
        // Given: WiFi отключен
        every { wifiManager.isWifiEnabled } returns false
        
        // When
        val scanStatus = wifiScannerService.startScan()
        
        // Then: Должно вернуть Failed
        assertTrue("When WiFi is disabled, should return Failed", scanStatus is WifiScanStatus.Failed)
        assertTrue("Failed status should contain error message", (scanStatus as WifiScanStatus.Failed).error.contains("not enabled", ignoreCase = true))
    }
    
    @Test
    fun `getScanResultsWithMetadata should handle no previous scan gracefully`() = runTest {
        // Given: Нет предыдущих сканирований (lastSuccessfulScan = 0)
        every { wifiManager.scanResults } returns emptyList()
        
        // When
        val (networks, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then: Должно вернуть EXPIRED (fail-safe подход)
        assertEquals("No previous scan should return EXPIRED (fail-safe)", Freshness.EXPIRED, metadata.freshness)
        assertNotNull(metadata.timestamp)
    }
    
    @Test
    fun `metadata age calculation should be accurate`() = runTest {
        // Given: Сканирование 5 минут назад
        val fiveMinutesAgo = System.currentTimeMillis() - 300_000L
        val lastScanField = WifiScannerService::class.java.getDeclaredField("lastSuccessfulScan")
        lastScanField.isAccessible = true
        lastScanField.set(wifiScannerService, fiveMinutesAgo)
        
        every { wifiManager.scanResults } returns emptyList()
        
        // When
        val (_, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then
        val age = metadata.getAge()
        assertTrue("Age should be at least 5 minutes", age >= 300_000L)
        assertTrue("Age should be less than 5 minutes 10 seconds", age < 310_000L)
        
        val ageInMinutes = metadata.getAgeInMinutes()
        assertEquals("Age should be 5 minutes", 5L, ageInMinutes)
    }
    
    @Test
    fun `metadata should correctly identify ACTIVE_SCAN source`() = runTest {
        // Given: Только что выполненное сканирование
        every { wifiManager.startScan() } returns true
        every { wifiManager.scanResults } returns emptyList()
        
        val scanStatus = wifiScannerService.startScan()
        assertTrue(scanStatus is WifiScanStatus.Success)
        
        // When: Получаем результаты в течение 1 минуты
        val (_, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then
        assertEquals("Recent scan within 1 minute should be ACTIVE_SCAN", ScanSource.ACTIVE_SCAN, metadata.source)
    }
    
    @Test
    fun `metadata should correctly identify SYSTEM_CACHE source`() = runTest {
        // Given: Старое сканирование (2 минуты назад)
        val twoMinutesAgo = System.currentTimeMillis() - 120_000L
        val lastScanField = WifiScannerService::class.java.getDeclaredField("lastSuccessfulScan")
        lastScanField.isAccessible = true
        lastScanField.set(wifiScannerService, twoMinutesAgo)
        
        every { wifiManager.scanResults } returns emptyList()
        
        // When
        val (_, metadata) = wifiScannerService.getScanResultsWithMetadata()
        
        // Then
        assertEquals("Scan older than 1 minute should be SYSTEM_CACHE", ScanSource.SYSTEM_CACHE, metadata.source)
    }
}
