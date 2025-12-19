package com.wifiguard.feature.scanner.presentation

import androidx.lifecycle.SavedStateHandle
import com.wifiguard.core.common.PermissionHandler
import com.wifiguard.core.data.wifi.WifiScanner
import com.wifiguard.core.domain.model.Freshness
import com.wifiguard.core.domain.model.ScanMetadata
import com.wifiguard.core.domain.model.ScanSource
import com.wifiguard.core.domain.model.WifiScanResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.wifiguard.core.common.Result


/**
 * Unit tests для ScannerViewModel
 */
@ExperimentalCoroutinesApi
class ScannerViewModelTest {

    @MockK
    private lateinit var wifiScanner: WifiScanner

    @MockK
    private lateinit var permissionHandler: PermissionHandler

    private lateinit var savedStateHandle: SavedStateHandle
    private lateinit var viewModel: ScannerViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
        savedStateHandle = SavedStateHandle()

        // Default behavior
        every { permissionHandler.hasWifiScanPermissions() } returns true
        every { permissionHandler.isLocationEnabled() } returns true
        every { permissionHandler.isLocationRequiredForWifiScan() } returns true
        every { wifiScanner.isWifiEnabled() } returns true
        every { wifiScanner.observeWifiEnabled() } returns flowOf(true)
        every { wifiScanner.getLastScanMetadata() } returns ScanMetadata(
            timestamp = 0,
            source = ScanSource.ACTIVE_SCAN,
            freshness = Freshness.UNKNOWN
        )
        coEvery { wifiScanner.getCurrentNetwork() } returns null
        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should start scan if permissions granted`() = runTest(testDispatcher) {
        // Given
        every { permissionHandler.hasWifiScanPermissions() } returns true
        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(emptyList())

        // Create ViewModel first
        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)

        // Create collectors to observe StateFlows after ViewModel creation to ensure they're active
        val scanResultValues = mutableListOf<Result<List<WifiScanResult>>>()
        val uiStateValues = mutableListOf<ScannerUiState>()

        val scanResultCollector = backgroundScope.launch {
            viewModel.scanResult.collect { scanResultValues.add(it) }
        }
        val uiStateCollector = backgroundScope.launch {
            viewModel.uiState.collect { uiStateValues.add(it) }
        }

        // Wait for all initialization and scan to complete
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { wifiScanner.startScan() }

        // Wait for the scan initiated in the ViewModel constructor to complete and update state
        var retries = 0
        var finalResult: Result<List<WifiScanResult>>
        while (retries < 30) {
            finalResult = viewModel.scanResult.value
            if (finalResult !is com.wifiguard.core.common.Result.Loading) {
                break
            }
            delay(100) // Wait for the init-triggered scan to complete
            advanceUntilIdle()
            retries++
        }
        finalResult = viewModel.scanResult.value

        assertTrue("scanResult should be Success", finalResult is Result.Success)

        // Cancel collectors
        scanResultCollector.cancel()
        uiStateCollector.cancel()
    }

    @Test
    fun `init should NOT start scan if permissions NOT granted`() = runTest(testDispatcher) {
        // Given
        every { permissionHandler.hasWifiScanPermissions() } returns false

        // When
        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)

        // Then
        coVerify(exactly = 0) { wifiScanner.startScan() }
    }

    @Test
    fun `startScan success should update scanResult and uiState`() = runTest(testDispatcher) {
        // Given
        val mockResults = listOf(
            WifiScanResult(
                ssid = "TestWiFi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -65
            )
        )
        coEvery { wifiScanner.startScan() } returns kotlin.Result.success(mockResults)

        // ВАЖНО: отключаем auto-scan в init(), чтобы второй startScan() не попал под debounce.
        every { permissionHandler.hasWifiScanPermissions() } returns false

        // Create ViewModel first
        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)

        // When
        every { permissionHandler.hasWifiScanPermissions() } returns true
        viewModel.startScan()

        advanceUntilIdle()
        // Then
        coVerify(exactly = 1) { wifiScanner.startScan() }
        val finalResult = viewModel.scanResult.dropWhile { it is Result.Loading }.first()
        assertTrue("scanResult должен быть Success", finalResult is Result.Success)
        assertEquals("Должны вернуться те же сети", mockResults, (finalResult as Result.Success).data)
        assertEquals("networks должны совпадать", mockResults, viewModel.uiState.value.networks)
        assertEquals("isScanning должен быть false", false, viewModel.uiState.value.isScanning)
    }

    @Test
    fun `startScan failure should update scanResult and uiState with error`() = runTest(testDispatcher) {
        // Given
        val exception = Exception("Scan failed")
        coEvery { wifiScanner.startScan() } returns kotlin.Result.failure(exception)
        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)

        // Create collectors to observe StateFlows
        val scanResultValues = mutableListOf<Result<List<WifiScanResult>>>()
        val uiStateValues = mutableListOf<ScannerUiState>()

        val scanResultCollector = backgroundScope.launch {
            viewModel.scanResult.collect { scanResultValues.add(it) }
        }
        val uiStateCollector = backgroundScope.launch {
            viewModel.uiState.collect { uiStateValues.add(it) }
        }

        // Wait to ensure collectors are active
        advanceUntilIdle()
        
        // Clear any initial scan results from init
        scanResultValues.clear()
        uiStateValues.clear()

        // When
        viewModel.startScan()

        // Then
        // Wait more aggressively for all coroutines to complete, especially for SavedStateHandle operations
        advanceUntilIdle()
        
        var retries = 0
        var finalResult: Result<List<WifiScanResult>>
        while (retries < 50) {
            advanceUntilIdle()
            // Check collected values first (they might be more up-to-date)
            if (scanResultValues.isNotEmpty()) {
                finalResult = scanResultValues.last()
            } else {
                finalResult = viewModel.scanResult.value
            }
            if (finalResult !is com.wifiguard.core.common.Result.Loading) {
                break
            }
            delay(50)
            retries++
        }
        finalResult = if (scanResultValues.isNotEmpty()) {
            scanResultValues.last()
        } else {
            viewModel.scanResult.value
        }

        assertTrue("scanResult должен быть Error", finalResult is Result.Error)

        if (finalResult is Result.Error) {
            assertEquals("Сообщение об ошибке должно совпадать", "Scan failed", finalResult.message)
        }

        // Wait for UI state to update with error - check both collected values and current value
        var uiRetries = 0
        var finalUiState = viewModel.uiState.value
        while (uiRetries < 50 && finalUiState.error == null) {
            advanceUntilIdle()
            // Check collected values first
            if (uiStateValues.isNotEmpty()) {
                finalUiState = uiStateValues.last()
            } else {
                finalUiState = viewModel.uiState.value
            }
            if (finalUiState.error != null) {
                break
            }
            delay(50)
            uiRetries++
        }

        assertEquals("error в uiState должна быть установлена", "Scan failed", finalUiState.error)
        assertEquals("isScanning должен быть false", false, finalUiState.isScanning)

        // Cancel collectors
        scanResultCollector.cancel()
        uiStateCollector.cancel()
    }

    @Test
    fun `checkWifiStatus should update isWifiEnabled in uiState`() = runTest(testDispatcher) {
        // Given
        every { wifiScanner.isWifiEnabled() } returns false
        viewModel = ScannerViewModel(wifiScanner, permissionHandler, savedStateHandle)

        // Create collector to observe StateFlow
        val uiStateValues = mutableListOf<ScannerUiState>()

        val uiStateCollector = backgroundScope.launch {
            viewModel.uiState.collect { uiStateValues.add(it) }
        }

        // Wait to ensure collector is active
        advanceUntilIdle()

        // When
        viewModel.checkWifiStatus()

        // Then
        // Wait for all coroutines to complete
        advanceUntilIdle()
        assertEquals("isWifiEnabled должен быть false", false, viewModel.uiState.value.isWifiEnabled)

        // Cancel collector
        uiStateCollector.cancel()
    }
}
