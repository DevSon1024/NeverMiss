package com.devson.NeverMiss.presentation.home

import com.devson.NeverMiss.data.local.preferences.UserPreferences
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.usecase.*
import com.devson.NeverMiss.viewmodel.HomeViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getActiveStreaksUseCase = mockk<GetActiveStreaksUseCase>()
    private val getAllStreaksUseCase = mockk<GetAllStreaksUseCase>()
    private val getStreakByIdUseCase = mockk<GetStreakByIdUseCase>()
    private val createStreakUseCase = mockk<CreateStreakUseCase>()
    private val updateStreakUseCase = mockk<UpdateStreakUseCase>()
    private val deleteStreakUseCase = mockk<DeleteStreakUseCase>()
    private val logCompletionUseCase = mockk<LogCompletionUseCase>()
    private val checkAndResetMissedStreaksUseCase = mockk<CheckAndResetMissedStreaksUseCase>()

    private val streakUseCases = StreakUseCases(
        getActiveStreaks = getActiveStreaksUseCase,
        getAllStreaks = getAllStreaksUseCase,
        getStreakById = getStreakByIdUseCase,
        createStreak = createStreakUseCase,
        updateStreak = updateStreakUseCase,
        deleteStreak = deleteStreakUseCase,
        logCompletion = logCompletionUseCase,
        checkAndResetMissedStreaks = checkAndResetMissedStreaksUseCase
    )

    private val userPreferences = mockk<UserPreferences>(relaxed = true)
    private lateinit var viewModel: HomeViewModel

    private val sampleStreak = Streak(
        id = 1L,
        title = "Daily Coding",
        description = "Practice Kotlin",
        emoji = "💻",
        category = StreakCategory.STUDY,
        color = 0xFF6200EE.toInt(),
        creationDate = "2026-05-20",
        repeatIntervalDays = 1,
        reminderTime = "09:00"
    )

    @Before
    fun setUp() {
        coEvery { getActiveStreaksUseCase() } returns flowOf(listOf(sampleStreak))
        coEvery { getAllStreaksUseCase() } returns flowOf(listOf(sampleStreak))
        coEvery { userPreferences.streakFreezesFlow } returns flowOf(3)
        viewModel = HomeViewModel(streakUseCases, userPreferences)
    }

    @Test
    fun testInitialState_combinesFlowsCorrectly() = runTest {
        val state = viewModel.state.first()
        assertEquals(1, state.activeStreaks.size)
        assertEquals("Daily Coding", state.activeStreaks[0].title)
        assertEquals(3, state.freezesRemaining)
        assertFalse(state.isGridView)
    }

    @Test
    fun testSearchQueryChange_updatesFilteredState() = runTest {
        viewModel.onSearchQueryChanged("Gym")
        val state = viewModel.state.first()
        assertEquals("Gym", state.searchQuery)
        assertTrue(state.activeStreaks.isEmpty()) // No streak matches "Gym"
    }

    @Test
    fun testCategorySelection_updatesFilteredState() = runTest {
        viewModel.onCategorySelected(StreakCategory.HEALTH.name)
        val state = viewModel.state.first()
        assertEquals(StreakCategory.HEALTH.name, state.selectedCategory)
        assertTrue(state.activeStreaks.isEmpty()) // Category doesn't match STUDY
    }

    @Test
    fun testToggleLayout_updatesGridViewState() = runTest {
        viewModel.onToggleLayout()
        var state = viewModel.state.first()
        assertTrue(state.isGridView)

        viewModel.onToggleLayout()
        state = viewModel.state.first()
        assertFalse(state.isGridView)
    }

    @Test
    fun testCompleteStreak_callsLogCompletionUseCase() = runTest {
        coEvery { logCompletionUseCase(any(), any(), any()) } returns true
        viewModel.completeStreak(1L)
        coVerify { logCompletionUseCase(1L, any(), CompletionStatus.COMPLETED) }
    }

    @Test
    fun testSkipStreak_callsLogCompletionUseCase() = runTest {
        coEvery { logCompletionUseCase(any(), any(), any()) } returns true
        viewModel.skipStreak(1L)
        coVerify { logCompletionUseCase(1L, any(), CompletionStatus.SKIPPED) }
    }

    @Test
    fun testFreezeStreak_callsLogCompletionUseCase() = runTest {
        coEvery { logCompletionUseCase(any(), any(), any()) } returns true
        viewModel.freezeStreak(1L)
        coVerify { logCompletionUseCase(1L, any(), CompletionStatus.FROZEN) }
    }

    @Test
    fun testTogglePauseStreak_updatesStreakState() = runTest {
        coEvery { updateStreakUseCase(any()) } returns Unit
        viewModel.togglePauseStreak(sampleStreak)
        coVerify { updateStreakUseCase(match { it.id == 1L && it.isPaused }) }
    }

    @Test
    fun testDeleteStreak_callsDeleteUseCase() = runTest {
        coEvery { deleteStreakUseCase(any()) } returns Unit
        viewModel.deleteStreak(sampleStreak)
        coVerify { deleteStreakUseCase(sampleStreak) }
    }
}
