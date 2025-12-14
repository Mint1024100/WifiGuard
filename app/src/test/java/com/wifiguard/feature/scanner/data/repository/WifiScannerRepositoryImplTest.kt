package com.wifiguard.feature.scanner.data.repository

import com.wifiguard.core.data.wifi.WifiScannerService
import com.wifiguard.feature.scanner.domain.model.WifiInfo
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WifiScannerRepositoryImplTest {

    private lateinit var wifiScannerService: WifiScannerService
    private lateinit var repository: WifiScannerRepositoryImpl

    @Before
    fun setUp() {
        wifiScannerService = mockk()
        repository = WifiScannerRepositoryImpl(wifiScannerService)
    }

    @Test
    fun `scanWifiNetworks should return results from service`() = runTest {
        // Given
        val mockResults = listOf(mockk<WifiInfo>())
        coEvery { wifiScannerService.getScanResults() } returns mockResults

        // When
        val result = repository.scanWifiNetworks()

        // Then
        assertEquals(mockResults, result)
        coVerify { wifiScannerService.getScanResults() }
    }

    @Test
    fun `observeWifiNetworks should return flow from service`() = runTest {
        // Given
        val mockResults = listOf(mockk<WifiInfo>())
        every { wifiScannerService.observeScanResults() } returns flowOf(mockResults)

        // When
        val result = repository.observeWifiNetworks()

        // Then
        result.collect {
             assertEquals(mockResults, it)
        }
    }
}














