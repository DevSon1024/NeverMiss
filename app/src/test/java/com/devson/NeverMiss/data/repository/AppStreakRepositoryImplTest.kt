package com.devson.NeverMiss.data.repository

import com.devson.NeverMiss.data.local.db.apptracking.AppStreakCompletionEntity
import com.devson.NeverMiss.data.local.db.apptracking.AppStreakDao
import com.devson.NeverMiss.data.local.db.apptracking.PendingConfirmationEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAccountEntity
import com.devson.NeverMiss.data.local.db.apptracking.TrackedAppEntity
import com.devson.NeverMiss.data.local.db.apptracking.UsageCheckpointEntity
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmationStatus
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.model.apptracking.UsageCheckpoint
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppStreakRepositoryImplTest {

    private val appStreakDao = mockk<AppStreakDao>(relaxed = true)
    private lateinit var repository: AppStreakRepositoryImpl

    private val sampleAppEntity = TrackedAppEntity(
        id = 1L,
        packageName = "com.reddit.frontpage",
        appName = "Reddit",
        isEnabled = true,
        currentStreak = 5,
        longestStreak = 10,
        lastCompletedDate = "2026-11-06"
    )

    private val sampleAccountEntity = TrackedAccountEntity(
        id = 10L,
        trackedAppId = 1L,
        accountName = "Personal Account",
        isEnabled = true,
        currentStreak = 4,
        longestStreak = 8,
        lastCompletedDate = "2026-11-06"
    )

    @Before
    fun setUp() {
        repository = AppStreakRepositoryImpl(appStreakDao)
    }

    @Test
    fun testGetAllTrackedAppsFlow_mapsEntitiesToDomain() = runTest {
        coEvery { appStreakDao.getAllTrackedAppsFlow() } returns flowOf(listOf(sampleAppEntity))

        val result = repository.getAllTrackedAppsFlow().first()

        assertEquals(1, result.size)
        val app = result[0]
        assertEquals(1L, app.id)
        assertEquals("com.reddit.frontpage", app.packageName)
        assertEquals("Reddit", app.appName)
        assertTrue(app.isEnabled)
        assertEquals(5, app.currentStreak)
    }

    @Test
    fun testAddTrackedApp_insertsEntityAndReturnsId() = runTest {
        coEvery { appStreakDao.insertTrackedApp(any()) } returns 42L

        val app = TrackedApp(
            packageName = "com.duolingo",
            appName = "Duolingo"
        )
        val id = repository.addTrackedApp(app)

        assertEquals(42L, id)
        coVerify {
            appStreakDao.insertTrackedApp(match {
                it.packageName == "com.duolingo" && it.appName == "Duolingo"
            })
        }
    }

    @Test
    fun testGetAccountsForApp_mapsToDomainList() = runTest {
        coEvery { appStreakDao.getAccountsForApp(1L) } returns listOf(sampleAccountEntity)

        val accounts = repository.getAccountsForApp(1L)

        assertEquals(1, accounts.size)
        assertEquals("Personal Account", accounts[0].accountName)
        assertEquals(1L, accounts[0].trackedAppId)
    }

    @Test
    fun testLogAppCompletion_delegatesToDaoTransaction() = runTest {
        coEvery {
            appStreakDao.recordAppUsageCompletion(1L, "2026-11-07", any(), "AUTOMATIC_USAGE")
        } returns true

        val success = repository.logAppCompletion(1L, "2026-11-07", 1000L, CompletionMethod.AUTOMATIC_USAGE)

        assertTrue(success)
        coVerify {
            appStreakDao.recordAppUsageCompletion(1L, "2026-11-07", 1000L, "AUTOMATIC_USAGE")
        }
    }

    @Test
    fun testLogAppCompletion_alreadyCompleted_returnsFalse() = runTest {
        coEvery {
            appStreakDao.recordAppUsageCompletion(1L, "2026-11-07", any(), any())
        } returns false

        val success = repository.logAppCompletion(1L, "2026-11-07", 1000L)

        assertFalse(success)
    }

    @Test
    fun testLogAccountCompletion_delegatesToDaoTransaction() = runTest {
        coEvery {
            appStreakDao.recordAccountCompletion(1L, 10L, "2026-11-07", any(), "MANUAL_CONFIRMATION")
        } returns true

        val success = repository.logAccountCompletion(1L, 10L, "2026-11-07", 1000L, CompletionMethod.MANUAL_CONFIRMATION)

        assertTrue(success)
        coVerify {
            appStreakDao.recordAccountCompletion(1L, 10L, "2026-11-07", 1000L, "MANUAL_CONFIRMATION")
        }
    }

    @Test
    fun testIsAppCompletedOnDate_returnsTrueWhenEntityExists() = runTest {
        coEvery { appStreakDao.getAppCompletionForDate(1L, "2026-11-07") } returns AppStreakCompletionEntity(
            trackedAppId = 1L,
            completionDate = "2026-11-07"
        )

        val completed = repository.isAppCompletedOnDate(1L, "2026-11-07")

        assertTrue(completed)
    }

    @Test
    fun testIsAppCompletedOnDate_returnsFalseWhenEntityNull() = runTest {
        coEvery { appStreakDao.getAppCompletionForDate(1L, "2026-11-07") } returns null

        val completed = repository.isAppCompletedOnDate(1L, "2026-11-07")

        assertFalse(completed)
    }

    @Test
    fun testCheckpointOperations_savesAndRetrieves() = runTest {
        val checkpointEntity = UsageCheckpointEntity(
            id = 5L,
            sourceIdentifier = UsageCheckpoint.DEFAULT_SOURCE,
            lastProcessedTimestamp = 500000L,
            updatedAt = 600000L
        )
        coEvery { appStreakDao.getCheckpoint(UsageCheckpoint.DEFAULT_SOURCE) } returns checkpointEntity

        val retrieved = repository.getCheckpoint(UsageCheckpoint.DEFAULT_SOURCE)

        assertNotNull(retrieved)
        assertEquals(500000L, retrieved?.lastProcessedTimestamp)

        // Test save checkpoint
        repository.saveCheckpoint(UsageCheckpoint.DEFAULT_SOURCE, 700000L)
        coVerify {
            appStreakDao.insertOrReplaceCheckpoint(match {
                it.sourceIdentifier == UsageCheckpoint.DEFAULT_SOURCE &&
                it.lastProcessedTimestamp == 700000L &&
                it.id == 5L
            })
        }
    }

    @Test
    fun testPendingConfirmation_addAndExpire() = runTest {
        val pending = PendingConfirmation(
            id = 100L,
            trackedAppId = 1L,
            sessionTimestamp = 123456L,
            sessionDate = "2026-11-07",
            status = PendingConfirmationStatus.PENDING
        )

        coEvery { appStreakDao.insertPendingConfirmation(any()) } returns 100L
        val id = repository.addPendingConfirmation(pending)
        assertEquals(100L, id)

        coEvery { appStreakDao.expireOldConfirmations(any()) } returns 3
        val expiredCount = repository.expireOldConfirmations(System.currentTimeMillis())
        assertEquals(3, expiredCount)
    }
}

