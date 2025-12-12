package com.wifiguard.feature.scanner.domain.usecase

import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.core.domain.model.WifiScanStatus
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import com.wifiguard.feature.scanner.domain.repository.WifiScannerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class GetActiveWifiUseCaseTest {

    @MockK
    private lateinit var wifiRepository: WifiScannerRepository // Not used in the UseCase but injected
    
    private lateinit var wifiScannerService: WifiScannerService
    private lateinit var useCase: GetActiveWifiUseCase

    @Before
    fun setUp() {
        wifiRepository = mockk()
        wifiScannerService = mockk()
        useCase = GetActiveWifiUseCase(wifiRepository, wifiScannerService)
    }

    @Test
    fun `invoke should return empty list if scan fails`() = runTest {
        // Given
        coEvery { wifiScannerService.startScan() } returns WifiScanStatus.Failed("Error")

        // When
        val result = useCase()

        // Then
        assertTrue(result.isEmpty())
        coVerify(exactly = 1) { wifiScannerService.startScan() }
        coVerify(exactly = 0) { wifiScannerService.getScanResults() }
    }

    @Test
    fun `invoke should return results if scan succeeds`() = runTest {
        // Given
        val mockResults = listOf(mockk<WifiInfo>())
        coEvery { wifiScannerService.startScan() } returns WifiScanStatus.Success(1000L)
        coEvery { wifiScannerService.getScanResults() } returns mockResults

        // When
        val result = useCase()

        // Then
        assertEquals(mockResults, result)
        coVerify(exactly = 1) { wifiScannerService.startScan() }
        coVerify(exactly = 1) { wifiScannerService.getScanResults() }
    }
}











