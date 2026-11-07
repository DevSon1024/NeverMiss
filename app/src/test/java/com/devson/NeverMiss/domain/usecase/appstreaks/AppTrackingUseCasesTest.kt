package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.data.local.apps.InstalledAppInfo
import com.devson.NeverMiss.data.local.apps.InstalledAppScanner
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTrackingUseCasesTest {

    private val repository = mockk<AppStreakRepository>(relaxed = true)
    private val scanner = mockk<InstalledAppScanner>(relaxed = true)

    @Test
    fun testAddTrackedAppUseCase_newApp_addsToRepository() = runTest {
        coEvery { repository.getTrackedAppByPackage("com.reddit.frontpage") } returns null
        coEvery { repository.addTrackedApp(any()) } returns 101L

        val useCase = AddTrackedAppUseCase(repository)
        val id = useCase("com.reddit.frontpage", "Reddit")

        assertEquals(101L, id)
        coVerify {
            repository.addTrackedApp(match {
                it.packageName == "com.reddit.frontpage" && it.appName == "Reddit" && it.isEnabled
            })
        }
    }

    @Test
    fun testAddTrackedAppUseCase_existingDisabledApp_reenables() = runTest {
        val existing = TrackedApp(
            id = 50L,
            packageName = "com.reddit.frontpage",
            appName = "Reddit",
            isEnabled = false
        )
        coEvery { repository.getTrackedAppByPackage("com.reddit.frontpage") } returns existing

        val useCase = AddTrackedAppUseCase(repository)
        val id = useCase("com.reddit.frontpage", "Reddit")

        assertEquals(50L, id)
        coVerify {
            repository.updateTrackedApp(match {
                it.id == 50L && it.isEnabled
            })
        }
    }

    @Test
    fun testToggleTrackedAppUseCase_invertsEnabledState() = runTest {
        val app = TrackedApp(
            id = 10L,
            packageName = "com.duolingo",
            appName = "Duolingo",
            isEnabled = true
        )
        coEvery { repository.getTrackedAppById(10L) } returns app

        val useCase = ToggleTrackedAppUseCase(repository)
        val result = useCase(10L)

        assertFalse(result) // Toggled from true to false
        coVerify {
            repository.updateTrackedApp(match {
                it.id == 10L && !it.isEnabled
            })
        }
    }

    @Test
    fun testRemoveTrackedAppUseCase_callsDeleteById() = runTest {
        val useCase = RemoveTrackedAppUseCase(repository)
        useCase(42L)

        coVerify {
            repository.deleteTrackedAppById(42L)
        }
    }

    @Test
    fun testGetInstalledAppsUseCase_mergesTrackedStatus() = runTest {
        val installed = listOf(
            InstalledAppInfo(packageName = "com.reddit.frontpage", appName = "Reddit"),
            InstalledAppInfo(packageName = "com.twitter.android", appName = "X")
        )
        val tracked = listOf(
            TrackedApp(id = 1L, packageName = "com.reddit.frontpage", appName = "Reddit", isEnabled = true)
        )

        coEvery { scanner.getInstalledLaunchableApps() } returns installed
        coEvery { repository.getAllTrackedApps() } returns tracked

        val useCase = GetInstalledAppsUseCase(scanner, repository)
        val merged = useCase()

        assertEquals(2, merged.size)

        val reddit = merged.find { it.packageName == "com.reddit.frontpage" }
        assertTrue(reddit?.isTracked == true)
        assertEquals(1L, reddit?.trackedAppId)
        assertTrue(reddit?.isTrackingEnabled == true)

        val twitter = merged.find { it.packageName == "com.twitter.android" }
        assertFalse(twitter?.isTracked == true)
        assertEquals(null, twitter?.trackedAppId)
    }
}

