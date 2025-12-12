package com.wifiguard.feature.analysis.presentation

import com.wifiguard.core.domain.model.WifiScanResult
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.core.security.SecurityAnalyzer
import com.wifiguard.core.security.SecurityReport
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        val mockScans = listOf<WifiScanResult>()
        val mockReport = mockk<SecurityReport>(relaxed = true)
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } returns mockReport

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer)
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
        val mockScans = listOf<WifiScanResult>()
        
        every { wifiRepository.getLatestScans(any()) } returns flowOf(mockScans)
        coEvery { securityAnalyzer.analyzeNetworks(any()) } throws RuntimeException("Analysis failed")

        // When
        viewModel = AnalysisViewModel(wifiRepository, securityAnalyzer)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals("Analysis failed", state.error)
    }
}
