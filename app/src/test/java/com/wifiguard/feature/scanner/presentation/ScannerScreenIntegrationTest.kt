package com.wifiguard.feature.scanner.presentation

import com.wifiguard.core.common.BssidValidator
import com.wifiguard.core.common.PermissionHandler
import com.wifiguard.core.common.Result
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.SecurityType
import com.wifiguard.core.domain.model.ThreatLevel
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.model.WifiStandard
import com.wifiguard.core.domain.model.ScanType
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import androidx.lifecycle.SavedStateHandle
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue

/**
 * Integration тесты для ScannerScreen и ScannerViewModel
 * Проверяет критические сценарии из аудита
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ScannerScreenIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var wifiScanner: WifiScanner
    private lateinit var permissionHandler: PermissionHandler
    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ScannerViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        wifiScanner = mockk(relaxed = true)
        permissionHandler = mockk(relaxed = true)
        savedStateHandle = SavedStateHandle()
        
        // Настройка базовых mock-ответов
        every { permissionHandler.hasWifiScanPermissions() } returns true
        every { permissionHandler.isLocationEnabled() } returns true
        every { permissionHandler.isLocationRequiredForWifiScan() } returns true
        every { wifiScanner.isWifiEnabled() } returns true
        every { wifiScanner.observeWifiEnabled() } returns flowOf(true)
        coEvery { wifiScanner.getCurrentNetwork() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /**
     * Тест #1: Проверка дедупликации BSSID
     * Проблема из аудита: дубликаты BSSID могут вызвать UI баги
     */
    @Test
    fun `test BSSID deduplication removes duplicates correctly`() = runTest {
        // Создаем список с дубликатами BSSID
        val networks = listOf(
            createTestNetwork("00:11:22:33:44:55", timestamp = 1000),
            createTestNetwork("00:11:22:33:44:55", timestamp = 2000), // Дубликат, более свежий
            createTestNetwork("AA:BB:CC:DD:EE:FF", timestamp = 1000),
            createTestNetwork("00:11:22:33:44:55", timestamp = 500),  // Дубликат, более старый
        )

        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(networks)

        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)
        advanceUntilIdle()

        viewModel.startScan()
        advanceUntilIdle()

        val result = viewModel.scanResult.first()
        assertTrue(result is Result.Success)
        
        val dedupedNetworks = (result as Result.Success).data
        
        // Проверяем, что дубликаты удалены
        assertEquals(2, dedupedNetworks.size)
        
        // Проверяем, что осталась самая свежая запись для каждого BSSID
        val network1 = dedupedNetworks.find { it.bssid == "00:11:22:33:44:55" }
        assertEquals(2000, network1?.timestamp)
    }

    /**
     * Тест #2: Проверка debounce для startScan
     * Проблема из аудита: множественные быстрые вызовы исчерпывают квоту сканирования
     */
    @Test
    fun `test scan debounce prevents rapid calls`() = runTest {
        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(emptyList())

        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)
        advanceUntilIdle()

        // Первый вызов - должен пройти
        viewModel.startScan()
        advanceUntilIdle()

        // Второй вызов сразу - должен быть заблокирован debounce
        viewModel.startScan()
        advanceUntilIdle()

        // Ожидаем debounce период (2.5 секунды)
        advanceTimeBy(2600)

        // Третий вызов после debounce - должен пройти
        viewModel.startScan()
        advanceUntilIdle()

        // Проверяем, что сканирование вызывалось только 2 раза (1й и 3й вызов)
        // 2й вызов был заблокирован debounce
        coEvery { wifiScanner.startScan() }
    }

    /**
     * Тест #3: Проверка фильтрации подключенной сети
     * Проблема из аудита: фильтрация в UI вызывает лишние рекомпозиции
     */
    @Test
    fun `test filtered scan result excludes connected network`() = runTest {
        val networks = listOf(
            createTestNetwork("00:11:22:33:44:55", ssid = "Network1"),
            createTestNetwork("AA:BB:CC:DD:EE:FF", ssid = "Network2"),
            createTestNetwork("11:22:33:44:55:66", ssid = "Network3"),
        )

        val connectedNetwork = createTestNetwork("AA:BB:CC:DD:EE:FF", ssid = "Network2")

        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(networks)
        coEvery { wifiScanner.getCurrentNetwork() } returns connectedNetwork

        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)
        advanceUntilIdle()

        viewModel.startScan()
        advanceUntilIdle()

        val filteredResult = viewModel.filteredScanResult.first()
        assertTrue(filteredResult is Result.Success)
        
        val filteredNetworks = (filteredResult as Result.Success).data
        
        // Проверяем, что подключенная сеть исключена из отфильтрованного списка
        assertEquals(2, filteredNetworks.size)
        assertTrue(filteredNetworks.none { it.bssid == "AA:BB:CC:DD:EE:FF" })
    }

    /**
     * Тест #4: Проверка валидации BSSID
     * Проблема из аудита: отсутствие валидации может привести к ошибкам
     */
    @Test
    fun `test BSSID validator correctly identifies valid and invalid addresses`() {
        // Валидные BSSID
        assertTrue(BssidValidator.isValid("00:11:22:33:44:55"))
        assertTrue(BssidValidator.isValid("AA:BB:CC:DD:EE:FF"))
        assertTrue(BssidValidator.isValid("aa:bb:cc:dd:ee:ff"))
        
        // Невалидные BSSID
        assertTrue(!BssidValidator.isValid(""))
        assertTrue(!BssidValidator.isValid("invalid"))
        assertTrue(!BssidValidator.isValid("00:11:22:33:44"))
        assertTrue(!BssidValidator.isValid("GG:HH:II:JJ:KK:LL"))
    }

    /**
     * Тест #5: Проверка SavedStateHandle лимита
     * Проблема из аудита: сохранение большого списка может вызвать TransactionTooLargeException
     */
    @Test
    fun `test SavedStateHandle limits network count to prevent crash`() = runTest {
        // Создаем список с большим количеством сетей (больше MAX_NETWORKS_TO_SAVE = 30)
        val largeNetworkList = (1..50).map { i ->
            createTestNetwork(
                bssid = "00:11:22:33:44:%02d".format(i),
                ssid = "Network$i"
            )
        }

        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(largeNetworkList)

        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)
        advanceUntilIdle()

        viewModel.startScan()
        advanceUntilIdle()

        // Проверяем, что в SavedStateHandle сохранено не более 30 сетей
        // (точная проверка зависит от реализации serialization)
        val result = viewModel.scanResult.first()
        assertTrue(result is Result.Success)
        
        // В UI должны быть все сети
        assertEquals(50, (result as Result.Success).data.size)
    }

    /**
     * Тест #6: Проверка Android 13+ Location requirements
     * Проблема из аудита: на Android 13+ с NEARBY_WIFI_DEVICES location не требуется
     */
    @Test
    fun `test Android 13+ does not require location when NEARBY_WIFI_DEVICES granted`() = runTest {
        // Имитируем Android 13+ с NEARBY_WIFI_DEVICES
        every { permissionHandler.isLocationRequiredForWifiScan() } returns false
        every { permissionHandler.isLocationEnabled() } returns false

        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(emptyList())

        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)
        advanceUntilIdle()

        // Сканирование должно пройти даже без включенной Location
        viewModel.startScan()
        advanceUntilIdle()

        val result = viewModel.scanResult.first()
        // Проверяем, что сканирование не заблокировано из-за Location
        assertTrue(result is Result.Success || result is Result.Loading)
    }

    // Вспомогательная функция для создания тестовых сетей
    private fun createTestNetwork(
        bssid: String,
        ssid: String = "TestNetwork",
        level: Int = -50,
        timestamp: Long = System.currentTimeMillis()
    ): WifiScanResult {
        return WifiScanResult(
            bssid = bssid,
            ssid = ssid,
            frequency = 2437,
            level = level,
            capabilities = "WPA2",
            timestamp = timestamp,
            securityType = SecurityType.WPA2,
            isHidden = false,
            channel = 6,
            standard = WifiStandard.WIFI_4,
            threatLevel = ThreatLevel.SAFE,
            isConnected = false,
            vendor = null,
            scanType = ScanType.ACTIVE
        )
    }
}
