package com.devson.NeverMiss.core.apptracking.engine

import android.app.AppOpsManager
import android.content.Context
import com.devson.NeverMiss.core.apptracking.model.AppForegroundEvent
import com.devson.NeverMiss.core.apptracking.model.NormalizedEventType
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppUsageTrackingEngineTest {

    private val usageStatsManagerHelper = mockk<UsageStatsManagerHelper>(relaxed = true)
    private val appStreakRepository = mockk<AppStreakRepository>(relaxed = true)
    private val context = mockk<Context>(relaxed = true)
    private val appOps = mockk<AppOpsManager>(relaxed = true)

    private lateinit var engine: AppUsageTrackingEngine

    private val sampleTrackedApps = listOf(
        TrackedApp(
            id = 1L,
            packageName = "com.reddit.frontpage",
            appName = "Reddit",
            isEnabled = true
        ),
        TrackedApp(
            id = 2L,
            packageName = "com.duolingo",
            appName = "Duolingo",
            isEnabled = true
        )
    )

    @Before
    fun setUp() {
        every { context.getSystemService(Context.APP_OPS_SERVICE) } returns appOps
        every { context.packageName } returns "com.devson.NeverMiss"
        @Suppress("DEPRECATION")
        every {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_ALLOWED
        @Suppress("DEPRECATION")
        every {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_ALLOWED

        engine = AppUsageTrackingEngine(
            usageStatsManagerHelper = usageStatsManagerHelper,
            appStreakRepository = appStreakRepository,
            context = context
        )
    }

    @Test
    fun testScan_permissionDenied_returnsEarlyWithoutScan() = runTest {
        @Suppress("DEPRECATION")
        every {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_IGNORED
        @Suppress("DEPRECATION")
        every {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, any(), any())
        } returns AppOpsManager.MODE_IGNORED

        val report = engine.scanAndDetectUsage(endTime = 1000000L)

        assertFalse(report.isPermissionGranted)
        assertEquals(0, report.totalEventsExamined)
        assertTrue(report.detectedAppUsages.isEmpty())
        coVerify(exactly = 0) { usageStatsManagerHelper.queryUsageEvents(any(), any()) }
    }

    @Test
    fun testScan_noTrackedApps_updatesCheckpointAndReturnsEmpty() = runTest {
        coEvery { appStreakRepository.getEnabledTrackedApps() } returns emptyList()

        val report = engine.scanAndDetectUsage(endTime = 1000000L)

        assertTrue(report.isPermissionGranted)
        assertTrue(report.detectedAppUsages.isEmpty())
        coVerify { appStreakRepository.saveCheckpoint(UsageCheckpoint.DEFAULT_SOURCE, 1000000L) }
        coVerify(exactly = 0) { usageStatsManagerHelper.queryUsageEvents(any(), any()) }
    }

    @Test
    fun testScan_matchesTrackedAppsAndIgnoresUntrackedPackages() = runTest {
        coEvery { appStreakRepository.getEnabledTrackedApps() } returns sampleTrackedApps
        coEvery { appStreakRepository.getCheckpoint(UsageCheckpoint.DEFAULT_SOURCE) } returns UsageCheckpoint(
            lastProcessedTimestamp = 1000L
        )

        val events = listOf(
            AppForegroundEvent("com.reddit.frontpage", 2000L, NormalizedEventType.FOREGROUND),
            AppForegroundEvent("com.untracked.app", 3000L, NormalizedEventType.FOREGROUND),
            AppForegroundEvent("com.duolingo", 4000L, NormalizedEventType.FOREGROUND),
            AppForegroundEvent("com.reddit.frontpage", 5000L, NormalizedEventType.BACKGROUND)
        )
        coEvery { usageStatsManagerHelper.queryUsageEvents(1000L, 10000L) } returns events

        val report = engine.scanAndDetectUsage(endTime = 10000L)

        assertEquals(4, report.totalEventsExamined)
        assertEquals(2, report.detectedAppUsages.size)

        val redditDetection = report.detectedAppUsages.find { it.trackedApp.packageName == "com.reddit.frontpage" }
        assertEquals(1L, redditDetection?.trackedApp?.id)
        assertEquals(2000L, redditDetection?.firstDetectedTimestamp)

        val duoDetection = report.detectedAppUsages.find { it.trackedApp.packageName == "com.duolingo" }
        assertEquals(2L, duoDetection?.trackedApp?.id)
        assertEquals(4000L, duoDetection?.firstDetectedTimestamp)

        // Checkpoint updated to highest event timestamp + 1 = 5001
        assertEquals(5001L, report.updatedCheckpoint)
        coVerify { appStreakRepository.saveCheckpoint(UsageCheckpoint.DEFAULT_SOURCE, 5001L) }
    }

    @Test
    fun testScan_debouncesRapidConsecutiveForegroundEvents() = runTest {
        coEvery { appStreakRepository.getEnabledTrackedApps() } returns sampleTrackedApps
        coEvery { appStreakRepository.getCheckpoint(UsageCheckpoint.DEFAULT_SOURCE) } returns UsageCheckpoint(
            lastProcessedTimestamp = 1000L
        )

        val events = listOf(
            AppForegroundEvent("com.reddit.frontpage", 2000L, NormalizedEventType.FOREGROUND),
            AppForegroundEvent("com.reddit.frontpage", 2500L, NormalizedEventType.FOREGROUND), // debounced (<2000ms)
            AppForegroundEvent("com.reddit.frontpage", 5000L, NormalizedEventType.FOREGROUND)  // accepted (3000ms later)
        )
        coEvery { usageStatsManagerHelper.queryUsageEvents(1000L, 10000L) } returns events

        val report = engine.scanAndDetectUsage(endTime = 10000L)

        assertEquals(1, report.detectedAppUsages.size)
        val reddit = report.detectedAppUsages[0]
        assertEquals(2, reddit.eventCount)
        assertEquals(2000L, reddit.firstDetectedTimestamp)
        assertEquals(5000L, reddit.lastDetectedTimestamp)
    }
}

