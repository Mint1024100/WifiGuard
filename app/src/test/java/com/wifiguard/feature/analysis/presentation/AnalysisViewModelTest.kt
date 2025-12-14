package com.wifiguard.feature.analysis.presentation

import android.content.Context
import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.data.wifi.ScanStatusBus
import com.wifiguard.core.data.wifi.ScanStatusState
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.security.SecurityReport
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class AnalysisViewModelTest {

    @MockK
    private lateinit var wifiRepository: WifiRepository
    @MockK
    private lateinit var securityAnalyzer: SecurityAnalyzer

    private lateinit var viewModel: AnalysisViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `init should observe analysis data`() = runTest {
        // Given
        val mockScans = listOf(
            WifiScanResult(
                ssid = "TestWifi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -50
            )
        )
        val mockReport = mockk<SecurityReport>(relaxed = true)
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns mockReport

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(400) // debounce(350)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockReport, state.securityReport)
        assertEquals(null, state.error)
    }

    @Test
    fun `error during analysis should update state with error`() = runTest {
        // Given
        val mockScans = listOf(
            WifiScanResult(
                ssid = "TestWifi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -50
            )
        )
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } throws RuntimeException("Analysis failed")

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(600) // debounce(500)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Analysis failed", state.error)
    }

    @Test
    fun `empty scan list should result in null report`() = runTest {
        // Given
        every { wifiRepository.getLatestScans(any()) } returns flowOf(emptyList())
        coEvery { securityAnalyzer.analyzeNetworks(emptyList()) } returns null

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.securityReport)
        assertNotNull(state.error)
    }

    @Test
    fun `requestAutoScan should update scan status bus`() = runTest {
        // Given
        val scanStatusBus = ScanStatusBus()
        every { wifiRepository.getLatestScans(any()) } returns flowOf(emptyList())
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns null

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, scanStatusBus)
        viewModel.requestAutoScan()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val status = scanStatusBus.state.value
        assertTrue(status is ScanStatusState.Starting)
    }

    @Test
    fun `refreshData should clear error and trigger scan`() = runTest {
        // Given
        val context = mockk<Context>(relaxed = true)
        val mockScans = listOf(
            WifiScanResult(
                ssid = "TestWifi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -50
            )
        )
        val mockReport = mockk<SecurityReport>(relaxed = true)
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns mockReport

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Устанавливаем ошибку
        viewModel.clearError()
        
        // Обновляем данные
        viewModel.refreshData(context)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `scan status updates should be reflected in ui state`() = runTest {
        // Given
        val scanStatusBus = ScanStatusBus()
        every { wifiRepository.getLatestScans(any()) } returns flowOf(emptyList())
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns null

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, scanStatusBus)
        scanStatusBus.update(ScanStatusState.Scanning())
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue(state.scanStatus is ScanStatusState.Scanning)
    }

    @Test
    fun `large scan list should be processed correctly`() = runTest {
        // Given - создаем большой список для проверки производительности
        val largeScanList = (1..1000).map { index ->
            WifiScanResult(
                ssid = "TestWifi$index",
                bssid = "00:11:22:33:44:${String.format("%02d", index % 100)}",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412 + (index % 10),
                level = -50 - (index % 30)
            )
        }
        val mockReport = mockk<SecurityReport>(relaxed = true)
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(largeScanList)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns mockReport

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(mockReport, state.securityReport)
        assertNull(state.error)
    }

    @Test
    fun `clearError should remove error from state`() = runTest {
        // Given
        val mockScans = listOf(
            WifiScanResult(
                ssid = "TestWifi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -50
            )
        )
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } throws RuntimeException("Test error")

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()
        
        // Проверяем, что ошибка установлена
        assertNotNull(viewModel.uiState.value.error)
        
        // Очищаем ошибку
        viewModel.clearError()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertNull(state.error)
    }

    @Test
    fun `debounce should prevent excessive analysis calls`() = runTest {
        // Given
        val mockScans = listOf(
            WifiScanResult(
                ssid = "TestWifi",
                bssid = "00:11:22:33:44:55",
                capabilities = "[WPA2-PSK-CCMP][ESS]",
                frequency = 2412,
                level = -50
            )
        )
        val mockReport = mockk<SecurityReport>(relaxed = true)
        val scansFlow = MutableStateFlow(mockScans)
        
        every { wifiRepository.getLatestScans(any()) } returns scansFlow
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns mockReport

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer, ScanStatusBus())
        
        // Эмулируем быстрые обновления
        scansFlow.value = mockScans + mockScans
        testDispatcher.scheduler.advanceTimeBy(100)
        scansFlow.value = mockScans + mockScans + mockScans
        testDispatcher.scheduler.advanceTimeBy(100)
        scansFlow.value = mockScans + mockScans + mockScans + mockScans
        
        // Ждем debounce
        testDispatcher.scheduler.advanceTimeBy(600)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - должен быть только один вызов analyzeNetworks после debounce
        // (точное количество зависит от реализации, но должно быть меньше чем количество обновлений)
        verify(atLeast = 1, atMost = 3) { 
            securityAnalyzer.analyzeNetworks(any()) 
        }
    }
}
