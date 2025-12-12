package com.wifiguard.core.background

import android.content.Context
import android.util.Log
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class DataCleanupWorkerTest {
    
    private lateinit var context: Context
    private lateinit var workerParams: WorkerParameters
    private lateinit var wifiRepository: WifiRepository
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var worker: DataCleanupWorker

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        workerParams = mockk(relaxed = true)
        wifiRepository = mockk(relaxed = true)
        settingsRepository = mockk(relaxed = true)
        
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        
        worker = DataCleanupWorker(context, workerParams, wifiRepository, settingsRepository)
    }

    @Test
    fun `doWork should clean up old data`() = runTest {
        // Given
        coEvery { settingsRepository.getDataRetentionDays() } returns flowOf(30)
        coEvery { wifiRepository.deleteScansOlderThan(any()) } returns 10
        coEvery { wifiRepository.optimizeDatabase() } returns Unit
        coEvery { wifiRepository.getTotalScansCount() } returns 100
        
        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.success(), result)
        coVerify { wifiRepository.deleteScansOlderThan(any()) }
        coVerify { wifiRepository.optimizeDatabase() }
    }
    
    @Test
    fun `doWork should retry on error`() = runTest {
        // Given
        coEvery { settingsRepository.getDataRetentionDays() } throws RuntimeException("Error")
        
        // When
        val result = worker.doWork()

        // Then
        assertEquals(ListenableWorker.Result.retry(), result)
    }
}












