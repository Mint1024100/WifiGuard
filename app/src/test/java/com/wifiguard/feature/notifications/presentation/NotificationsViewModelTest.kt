package com.wifiguard.feature.notifications.presentation

import com.wifiguard.core.data.local.dao.ThreatDao
import com.wifiguard.core.data.local.entity.ThreatEntity
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class NotificationsViewModelTest {

    @MockK
    private lateinit var threatDao: ThreatDao

    private lateinit var viewModel: NotificationsViewModel
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
    fun `init should load notifications`() = runTest {
        // Given
        val mockThreats = listOf<ThreatEntity>()
        coEvery { threatDao.getAllThreats() } returns flowOf(mockThreats)

        // When
        viewModel = NotificationsViewModel(threatDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
    }

    @Test
    fun `error loading notifications should update state with error`() = runTest {
        // Given
        coEvery { threatDao.getAllThreats() } throws RuntimeException("DB Error")

        // When
        viewModel = NotificationsViewModel(threatDao)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assert(state.error == "DB Error")
    }
}
