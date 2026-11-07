package com.devson.NeverMiss.presentation.appstreaks

import com.devson.NeverMiss.data.local.apps.InstalledAppInfo
import com.devson.NeverMiss.domain.usecase.appstreaks.AddTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.CheckUsagePermissionUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.GetInstalledAppsUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.RemoveTrackedAppUseCase
import com.devson.NeverMiss.domain.usecase.appstreaks.ToggleTrackedAppUseCase
import com.devson.NeverMiss.presentation.home.MainDispatcherRule
import com.devson.NeverMiss.viewmodel.appstreaks.InstalledAppPickerViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class InstalledAppPickerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getInstalledAppsUseCase = mockk<GetInstalledAppsUseCase>()
    private val addTrackedAppUseCase = mockk<AddTrackedAppUseCase>()
    private val toggleTrackedAppUseCase = mockk<ToggleTrackedAppUseCase>()
    private val removeTrackedAppUseCase = mockk<RemoveTrackedAppUseCase>()
    private val checkUsagePermissionUseCase = mockk<CheckUsagePermissionUseCase>()

    private val sampleApps = listOf(
        InstalledAppInfo(
            packageName = "com.reddit.frontpage",
            appName = "Reddit",
            isSystemApp = false,
            isTracked = true,
            trackedAppId = 1L,
            isTrackingEnabled = true
        ),
        InstalledAppInfo(
            packageName = "com.duolingo",
            appName = "Duolingo",
            isSystemApp = false,
            isTracked = false
        ),
        InstalledAppInfo(
            packageName = "com.android.settings",
            appName = "Settings",
            isSystemApp = true,
            isTracked = false
        )
    )

    private lateinit var viewModel: InstalledAppPickerViewModel

    @Before
    fun setUp() {
        every { checkUsagePermissionUseCase() } returns true
        coEvery { getInstalledAppsUseCase() } returns sampleApps

        viewModel = InstalledAppPickerViewModel(
            getInstalledAppsUseCase = getInstalledAppsUseCase,
            addTrackedAppUseCase = addTrackedAppUseCase,
            toggleTrackedAppUseCase = toggleTrackedAppUseCase,
            removeTrackedAppUseCase = removeTrackedAppUseCase,
            checkUsagePermissionUseCase = checkUsagePermissionUseCase
        )
    }

    @Test
    fun testInitialState_loadsAppsAndFiltersNonSystemByDefault() = runTest {
        val state = viewModel.state.first()
        assertTrue(state.hasUsageAccess)
        assertFalse(state.isLoading)
        assertEquals(3, state.apps.size)
        // System app "Settings" hidden by default
        assertEquals(2, state.filteredApps.size)
        assertEquals(1, state.trackedCount)
    }

    @Test
    fun testSearchQuery_filtersAppsByName() = runTest {
        viewModel.onSearchQueryChange("duo")
        val state = viewModel.state.first()

        assertEquals("duo", state.searchQuery)
        assertEquals(1, state.filteredApps.size)
        assertEquals("Duolingo", state.filteredApps[0].appName)
    }

    @Test
    fun testToggleShowSystemApps_revealsSystemApps() = runTest {
        viewModel.toggleShowSystemApps()
        val state = viewModel.state.first()

        assertTrue(state.showSystemApps)
        assertEquals(3, state.filteredApps.size)
    }

    @Test
    fun testToggleTracking_untrackedApp_callsAddTrackedAppUseCase() = runTest {
        coEvery { addTrackedAppUseCase("com.duolingo", "Duolingo") } returns 99L

        val duolingo = sampleApps[1]
        viewModel.toggleTracking(duolingo)

        val state = viewModel.state.first()
        coVerify { addTrackedAppUseCase("com.duolingo", "Duolingo") }

        val updatedDuo = state.apps.find { it.packageName == "com.duolingo" }
        assertTrue(updatedDuo?.isTracked == true)
        assertEquals(99L, updatedDuo?.trackedAppId)
        assertTrue(updatedDuo?.isTrackingEnabled == true)
    }

    @Test
    fun testToggleTracking_alreadyTrackedApp_callsToggleUseCase() = runTest {
        coEvery { toggleTrackedAppUseCase(1L) } returns false

        val reddit = sampleApps[0]
        viewModel.toggleTracking(reddit)

        val state = viewModel.state.first()
        coVerify { toggleTrackedAppUseCase(1L) }

        val updatedReddit = state.apps.find { it.packageName == "com.reddit.frontpage" }
        assertTrue(updatedReddit?.isTracked == true)
        assertFalse(updatedReddit?.isTrackingEnabled == true)
    }

    @Test
    fun testRemoveTracking_callsRemoveUseCase() = runTest {
        coEvery { removeTrackedAppUseCase(1L) } returns Unit

        val reddit = sampleApps[0]
        viewModel.removeTracking(reddit)

        val state = viewModel.state.first()
        coVerify { removeTrackedAppUseCase(1L) }

        val updatedReddit = state.apps.find { it.packageName == "com.reddit.frontpage" }
        assertFalse(updatedReddit?.isTracked == true)
        assertEquals(null, updatedReddit?.trackedAppId)
    }
}

