package com.wifiguard.feature.scanner.domain.usecase

import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class ObserveWifiStateUseCaseTest {

    private lateinit var wifiRepository: WifiScannerRepository
    private lateinit var wifiScannerService: WifiScannerService
    private lateinit var useCase: ObserveWifiStateUseCase

    @Before
    fun setUp() {
        wifiRepository = mockk()
        wifiScannerService = mockk()
        useCase = ObserveWifiStateUseCase(wifiRepository, wifiScannerService)
    }

    @Test
    fun `invoke should return flow from service`() = runTest {
        // Given
        val mockResults = listOf(mockk<WifiInfo>())
        every { wifiScannerService.observeScanResults() } returns flowOf(mockResults)

        // When
        val result = useCase().first()

        // Then
        assertEquals(mockResults, result)
    }

    @Test
    fun `observeWifiEnabled should emit status periodically`() = runTest {
        // Given
        every { wifiScannerService.isWifiEnabled() } returns true

        // When
        val result = useCase.observeWifiEnabled().take(1).first()

        // Then
        assertEquals(true, result)
    }

    @Test
    fun `observeCurrentWifi should emit current network periodically`() = runTest {
        // Given
        val mockInfo = mockk<WifiInfo>()
        coEvery { wifiScannerService.getCurrentNetwork() } returns mockInfo

        // When
        val result = useCase.observeCurrentWifi().take(1).first()

        // Then
        assertEquals(mockInfo, result)
    }
}











