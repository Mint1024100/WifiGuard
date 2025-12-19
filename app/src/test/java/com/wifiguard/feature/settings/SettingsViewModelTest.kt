package com.wifiguard.feature.settings

import android.content.Context
import com.wifiguard.core.data.preferences.AppSettings
import com.wifiguard.core.domain.repository.WifiRepository
import com.wifiguard.feature.settings.domain.repository.SettingsRepository
import com.wifiguard.feature.settings.presentation.ClearDataResult
import com.wifiguard.feature.settings.presentation.SettingsViewModel
import io.mockk.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import kotlinx.coroutines.ExperimentalCoroutinesApi


/**
 * Unit tests for SettingsViewModel
 */
@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    private lateinit var settingsRepository: SettingsRepository
    private lateinit var wifiRepository: WifiRepository
    private lateinit var context: Context
    private lateinit var settingsViewModel: SettingsViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = mockk()
        wifiRepository = mockk()
        context = mockk(relaxed = true)

        // Мокаем все Flow-методы, которые вызываются в loadSettings()
        every { settingsRepository.getAutoScanEnabled() } returns flowOf(true)
        every { settingsRepository.getNotificationsEnabled() } returns flowOf(true)
        every { settingsRepository.getNotificationSoundEnabled() } returns flowOf(true)
        every { settingsRepository.getNotificationVibrationEnabled() } returns flowOf(true)
        every { settingsRepository.getScanIntervalMinutes() } returns flowOf(15)
        every { settingsRepository.getThemeMode() } returns flowOf("system")
        every { settingsRepository.getDataRetentionDays() } returns flowOf(30)
        every { settingsRepository.getAutoDisableWifiOnCritical() } returns flowOf(false)

        settingsViewModel = SettingsViewModel(
            settingsRepository = settingsRepository,
            wifiRepository = wifiRepository,
            context = context
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `setAutoScanEnabled calls repository`() = runTest(testDispatcher) {
        // Given
        val enabled = true
        coEvery { settingsRepository.setAutoScanEnabled(enabled) } returns Unit

        // When
        settingsViewModel.setAutoScanEnabled(enabled)

        // Then
        coVerify { settingsRepository.setAutoScanEnabled(enabled) }
    }

    @Test
    fun `setScanIntervalMinutes calls repository`() = runTest(testDispatcher) {
        // Given
        val minutes = 30
        coEvery { settingsRepository.setScanIntervalMinutes(minutes) } returns Unit

        // When
        settingsViewModel.setScanInterval(minutes)

        // Then
        coVerify { settingsRepository.setScanIntervalMinutes(minutes) }
    }

    @Test
    fun `setNotificationsEnabled calls repository`() = runTest(testDispatcher) {
        // Given
        val enabled = false
        coEvery { settingsRepository.setNotificationsEnabled(enabled) } returns Unit

        // When
        settingsViewModel.setNotificationsEnabled(enabled)

        // Then
        coVerify { settingsRepository.setNotificationsEnabled(enabled) }
    }

    @Test
    fun `setNotificationSoundEnabled calls repository`() = runTest(testDispatcher) {
        // Given
        val enabled = true
        coEvery { settingsRepository.setNotificationSoundEnabled(enabled) } returns Unit

        // When
        settingsViewModel.setNotificationSoundEnabled(enabled)

        // Then
        coVerify { settingsRepository.setNotificationSoundEnabled(enabled) }
    }

    @Test
    fun `setNotificationVibrationEnabled calls repository`() = runTest(testDispatcher) {
        // Given
        val enabled = false
        coEvery { settingsRepository.setNotificationVibrationEnabled(enabled) } returns Unit

        // When
        settingsViewModel.setNotificationVibrationEnabled(enabled)

        // Then
        coVerify { settingsRepository.setNotificationVibrationEnabled(enabled) }
    }

    @Test
    fun `showClearDataDialog sets dialog visible`() = runTest(testDispatcher) {
        // ИСПРАВЛЕНО: Запускаем collector через backgroundScope для активации StateFlow
        val job = backgroundScope.launch {
            settingsViewModel.clearDataDialogVisible.collect {}
        }

        // Wait to ensure collector is active
        advanceUntilIdle()

        // When
        settingsViewModel.showClearDataDialog()

        // Then
        advanceUntilIdle() // Wait for all coroutines to complete
        assertTrue("Dialog должен быть видимым", settingsViewModel.clearDataDialogVisible.value)

        job.cancel()
    }

    @Test
    fun `hideClearDataDialog sets dialog invisible`() = runTest(testDispatcher) {
        // ИСПРАВЛЕНО: Запускаем collector через backgroundScope для активации StateFlow
        val job = backgroundScope.launch {
            settingsViewModel.clearDataDialogVisible.collect {}
        }

        // Wait to ensure collector is active
        advanceUntilIdle()

        // Given
        settingsViewModel.showClearDataDialog()
        advanceUntilIdle() // Wait for the operation to complete
        assertTrue("Dialog должен быть видимым", settingsViewModel.clearDataDialogVisible.value)

        // When
        settingsViewModel.hideClearDataDialog()

        // Then
        advanceUntilIdle() // Wait for all coroutines to complete
        assertFalse("Dialog должен быть скрыт", settingsViewModel.clearDataDialogVisible.value)

        job.cancel()
    }

    @Test
    fun `clearAllData validates database and clears all data successfully`() = runTest(testDispatcher) {
        // Given
        coEvery { wifiRepository.validateDatabaseIntegrity() } returns true
        coEvery { wifiRepository.clearAllData() } returns Unit

        // ИСПРАВЛЕНО: Запускаем collectors через backgroundScope для активации StateFlow
        val results = mutableListOf<ClearDataResult?>()
        val clearDataResultCollector = backgroundScope.launch {
            settingsViewModel.clearDataResult.collect { result ->
                results.add(result)
            }
        }

        val dialogVisibleCollector = backgroundScope.launch {
            settingsViewModel.clearDataDialogVisible.collect {}
        }

        // Wait to ensure collectors are active
        advanceUntilIdle()

        // When
        settingsViewModel.clearAllData()

        // Then
        // Wait more aggressively for the operation to complete - withContext operations need extra time
        var retries = 0
        while (settingsViewModel.clearDataResult.value == null && retries < 20) {
            delay(50) // Small delay to allow context switching between Dispatchers
            advanceUntilIdle() // Ensure all pending coroutines complete
            retries++
        }

        coVerify(exactly = 1) { wifiRepository.validateDatabaseIntegrity() }
        coVerify(exactly = 1) { wifiRepository.clearAllData() }

        assertEquals("Result должен быть Success", ClearDataResult.Success, settingsViewModel.clearDataResult.value)
        assertFalse("Dialog должен быть закрыт", settingsViewModel.clearDataDialogVisible.value)

        clearDataResultCollector.cancel()
        dialogVisibleCollector.cancel()
    }

    @Test
    fun `clearAllData handles database validation failure gracefully`() = runTest(testDispatcher) {
        // Given
        coEvery { wifiRepository.validateDatabaseIntegrity() } returns false
        coEvery { wifiRepository.clearAllData() } returns Unit

        // ИСПРАВЛЕНО: Запускаем collectors через backgroundScope для активации StateFlow
        val results = mutableListOf<ClearDataResult?>()
        val clearDataResultCollector = backgroundScope.launch {
            settingsViewModel.clearDataResult.collect { result ->
                results.add(result)
            }
        }

        val dialogVisibleCollector = backgroundScope.launch {
            settingsViewModel.clearDataDialogVisible.collect {}
        }

        // Wait to ensure collectors are active
        advanceUntilIdle()

        // When
        settingsViewModel.clearAllData()

        // Then
        // Wait more aggressively for the operation to complete - withContext operations need extra time
        var retries = 0
        while (settingsViewModel.clearDataResult.value == null && retries < 20) {
            delay(50) // Small delay to allow context switching between Dispatchers
            advanceUntilIdle() // Ensure all pending coroutines complete
            retries++
        }

        coVerify(exactly = 1) { wifiRepository.validateDatabaseIntegrity() }
        coVerify(exactly = 1) { wifiRepository.clearAllData() }
        // Даже при неудачной валидации, очистка всё равно происходит (graceful degradation)

        assertEquals("Result должен быть Success", ClearDataResult.Success, settingsViewModel.clearDataResult.value)

        clearDataResultCollector.cancel()
        dialogVisibleCollector.cancel()
    }

    @Test
    fun `clearAllData handles errors and shows error message`() = runTest(testDispatcher) {
        // Given
        val errorMessage = "Database error"
        coEvery { wifiRepository.validateDatabaseIntegrity() } returns true
        coEvery { wifiRepository.clearAllData() } throws Exception(errorMessage)

        // ИСПРАВЛЕНО: Запускаем collectors через backgroundScope для активации StateFlow
        val results = mutableListOf<ClearDataResult?>()
        val clearDataResultCollector = backgroundScope.launch {
            settingsViewModel.clearDataResult.collect { result ->
                results.add(result)
            }
        }

        val dialogVisibleCollector = backgroundScope.launch {
            settingsViewModel.clearDataDialogVisible.collect {}
        }

        // Wait to ensure collectors are active
        advanceUntilIdle()

        // When
        settingsViewModel.clearAllData()

        // Then
        // Wait more aggressively for the operation to complete - withContext operations need extra time
        var retries = 0
        while (settingsViewModel.clearDataResult.value == null && retries < 20) {
            delay(50) // Small delay to allow context switching between Dispatchers
            advanceUntilIdle() // Ensure all pending coroutines complete
            retries++
        }

        coVerify(exactly = 1) { wifiRepository.validateDatabaseIntegrity() }
        coVerify(exactly = 1) { wifiRepository.clearAllData() }

        val result = settingsViewModel.clearDataResult.value
        assertTrue("Result должен быть Error", result is ClearDataResult.Error)
        if (result is ClearDataResult.Error) {
            assertEquals("Сообщение об ошибке должно совпадать", errorMessage, result.message)
        }
        assertFalse("Dialog должен быть закрыт", settingsViewModel.clearDataDialogVisible.value)

        clearDataResultCollector.cancel()
        dialogVisibleCollector.cancel()
    }

    @Test
    fun `resetClearDataResult clears the result`() = runTest(testDispatcher) {
        // Given
        coEvery { wifiRepository.validateDatabaseIntegrity() } returns true
        coEvery { wifiRepository.clearAllData() } returns Unit

        // ИСПРАВЛЕНО: Запускаем collector через backgroundScope для активации StateFlow
        val results = mutableListOf<ClearDataResult?>()
        val resultCollector = backgroundScope.launch {
            settingsViewModel.clearDataResult.collect { result ->
                results.add(result)
            }
        }

        // Wait to ensure collector is active
        advanceUntilIdle()

        settingsViewModel.showClearDataDialog()
        advanceUntilIdle() // Wait for dialog to show

        // Call clearAllData which should set the result
        settingsViewModel.clearAllData()

        // Wait more aggressively for the operation to complete and state to update
        var retries = 0
        while (settingsViewModel.clearDataResult.value == null && retries < 20) {
            delay(50) // Small delay to allow context switching between Dispatchers
            advanceUntilIdle() // Ensure all pending coroutines complete
            retries++
        }

        // Verify that a result was set (the original test expected this)
        val resultAfterClearAllData = settingsViewModel.clearDataResult.value
        assertNotNull("Result не должен быть null после clearAllData", resultAfterClearAllData)

        // When
        settingsViewModel.resetClearDataResult()

        // Then
        // Wait for the reset to complete with additional yield
        yield() // Allow any remaining coroutines to execute
        assertNull("Result должен быть null после reset", settingsViewModel.clearDataResult.value)

        resultCollector.cancel()
    }
}
