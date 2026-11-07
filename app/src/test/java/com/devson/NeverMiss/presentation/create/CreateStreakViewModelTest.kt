package com.devson.NeverMiss.presentation.create

import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCategory
import com.devson.NeverMiss.domain.usecase.*
import com.devson.NeverMiss.presentation.home.MainDispatcherRule
import com.devson.NeverMiss.viewmodel.CreateStreakViewModel
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CreateStreakViewModelTest {

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

    private lateinit var viewModel: CreateStreakViewModel

    private val sampleStreak = Streak(
        id = 1L,
        title = "Read Books",
        description = "Read 10 pages daily",
        emoji = "📖",
        category = StreakCategory.CUSTOM,
        color = 1234,
        creationDate = "2026-05-20",
        repeatIntervalDays = 1,
        reminderTime = "10:00"
    )

    @Before
    fun setUp() {
        viewModel = CreateStreakViewModel(streakUseCases)
    }

    @Test
    fun testInitialState_isEmpty() = runTest {
        val state = viewModel.state.first()
        assertEquals("", state.title)
        assertEquals("", state.description)
        assertNull(state.errorMessage)
        assertEquals("✨", state.emoji)
    }

    @Test
    fun testTitleChange_updatesState() = runTest {
        viewModel.onTitleChange("Go Running")
        val state = viewModel.state.first()
        assertEquals("Go Running", state.title)
        assertNull(state.errorMessage)
    }

    @Test
    fun testSaveStreak_emptyTitle_setsError() = runTest {
        viewModel.saveStreak()
        val state = viewModel.state.first()
        assertEquals("Please enter a title for your routine", state.errorMessage)
    }

    @Test
    fun testLoadStreak_populatesState() = runTest {
        coEvery { getStreakByIdUseCase(1L) } returns flowOf(sampleStreak)
        
        viewModel.loadStreak(1L)
        
        val state = viewModel.state.first()
        assertEquals(1L, state.id)
        assertEquals("Read Books", state.title)
        assertEquals("Read 10 pages daily", state.description)
        assertEquals("📖", state.emoji)
        assertEquals(StreakCategory.CUSTOM, state.category)
    }

    @Test
    fun testSaveStreak_newStreak_callsCreateStreakUseCase() = runTest {
        coEvery { createStreakUseCase(any()) } returns 2L

        viewModel.onTitleChange("Water Plants")
        viewModel.onDescriptionChange("Keep them green")
        viewModel.onCategoryChange(StreakCategory.HEALTH)
        viewModel.saveStreak()

        val state = viewModel.state.first()
        assertTrue(state.isSaved)
        assertNull(state.errorMessage)
        coVerify { createStreakUseCase(match { it.title == "Water Plants" && it.description == "Keep them green" }) }
    }

    @Test
    fun testSaveStreak_existingStreak_callsUpdateStreakUseCase() = runTest {
        coEvery { getStreakByIdUseCase(1L) } returns flowOf(sampleStreak)
        coEvery { updateStreakUseCase(any()) } returns Unit

        viewModel.loadStreak(1L)
        viewModel.onTitleChange("Read Advanced Books")
        viewModel.saveStreak()

        val state = viewModel.state.first()
        assertTrue(state.isSaved)
        coVerify { updateStreakUseCase(match { it.id == 1L && it.title == "Read Advanced Books" }) }
    }
}
