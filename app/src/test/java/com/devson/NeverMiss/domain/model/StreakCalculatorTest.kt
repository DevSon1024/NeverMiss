package com.devson.NeverMiss.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class StreakCalculatorTest {

    @Test
    fun testIsScheduled_DailyInterval() {
        val streak = Streak(
            title = "Test Daily",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            repeatIntervalDays = 1
        )

        val creation = LocalDate.of(2026, 5, 20)
        assertTrue(StreakCalculator.isScheduled(streak, creation))
        assertTrue(StreakCalculator.isScheduled(streak, creation.plusDays(1)))
        assertTrue(StreakCalculator.isScheduled(streak, creation.plusDays(5)))
    }

    @Test
    fun testIsScheduled_CustomInterval() {
        val streak = Streak(
            title = "Test Every 3 Days",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            repeatIntervalDays = 3
        )

        val creation = LocalDate.of(2026, 5, 20)
        assertTrue(StreakCalculator.isScheduled(streak, creation))
        assertFalse(StreakCalculator.isScheduled(streak, creation.plusDays(1)))
        assertFalse(StreakCalculator.isScheduled(streak, creation.plusDays(2)))
        assertTrue(StreakCalculator.isScheduled(streak, creation.plusDays(3)))
    }

    @Test
    fun testIsScheduled_DaysOfWeek() {
        val streak = Streak(
            title = "Mon-Wed-Fri",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-18", // Monday
            daysOfWeek = listOf(1, 3, 5) // Mon, Wed, Fri
        )

        val mon = LocalDate.of(2026, 5, 18)
        val tue = LocalDate.of(2026, 5, 19)
        val wed = LocalDate.of(2026, 5, 20)
        val thu = LocalDate.of(2026, 5, 21)
        val fri = LocalDate.of(2026, 5, 22)

        assertTrue(StreakCalculator.isScheduled(streak, mon))
        assertFalse(StreakCalculator.isScheduled(streak, tue))
        assertTrue(StreakCalculator.isScheduled(streak, wed))
        assertFalse(StreakCalculator.isScheduled(streak, thu))
        assertTrue(StreakCalculator.isScheduled(streak, fri))
    }

    @Test
    fun testCalculateStreaks_SimpleDailyCompleted() {
        val streak = Streak(
            title = "Daily Workout",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            repeatIntervalDays = 1
        )

        val history = listOf(
            CompletionHistory(streakId = 1, date = "2026-05-20", status = CompletionStatus.COMPLETED, timestamp = 0),
            CompletionHistory(streakId = 1, date = "2026-05-21", status = CompletionStatus.COMPLETED, timestamp = 0),
            CompletionHistory(streakId = 1, date = "2026-05-22", status = CompletionStatus.COMPLETED, timestamp = 0)
        )

        val stats = StreakCalculator.calculateStreaks(
            streak = streak,
            history = history,
            nowDate = LocalDate.of(2026, 5, 22),
            nowTime = LocalTime.NOON
        )

        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.longestStreak)
    }

    @Test
    fun testCalculateStreaks_WithFreezeAndSkip() {
        val streak = Streak(
            title = "Daily Reading",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            repeatIntervalDays = 1
        )

        val history = listOf(
            CompletionHistory(streakId = 1, date = "2026-05-20", status = CompletionStatus.COMPLETED, timestamp = 0),
            CompletionHistory(streakId = 1, date = "2026-05-21", status = CompletionStatus.FROZEN, timestamp = 0), // Keeps alive
            CompletionHistory(streakId = 1, date = "2026-05-22", status = CompletionStatus.COMPLETED, timestamp = 0)
        )

        val stats = StreakCalculator.calculateStreaks(
            streak = streak,
            history = history,
            nowDate = LocalDate.of(2026, 5, 22),
            nowTime = LocalTime.NOON
        )

        assertEquals(2, stats.currentStreak) // 2 completions
        assertEquals(2, stats.longestStreak)
    }

    @Test
    fun testCalculateStreaks_BrokenStreak() {
        val streak = Streak(
            title = "Daily Writing",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            repeatIntervalDays = 1
        )

        // Missed day on May 21st
        val history = listOf(
            CompletionHistory(streakId = 1, date = "2026-05-20", status = CompletionStatus.COMPLETED, timestamp = 0),
            CompletionHistory(streakId = 1, date = "2026-05-22", status = CompletionStatus.COMPLETED, timestamp = 0)
        )

        val stats = StreakCalculator.calculateStreaks(
            streak = streak,
            history = history,
            nowDate = LocalDate.of(2026, 5, 22),
            nowTime = LocalTime.NOON
        )

        assertEquals(1, stats.currentStreak) // Reset occurred, only today's is active
        assertEquals(1, stats.longestStreak)
    }

    @Test
    fun testCutoffGracePeriod() {
        val streak = Streak(
            title = "Grace Check",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            cutoffTime = "23:59",
            gracePeriodUntil3AM = true
        )

        // Today is May 22nd. 11 PM should NOT have passed cutoff if grace period is active (extends to 3 AM May 23rd)
        assertFalse(
            StreakCalculator.hasCutoffPassed(
                streak = streak,
                date = LocalDate.of(2026, 5, 22),
                nowTime = LocalTime.of(23, 0),
                nowDate = LocalDate.of(2026, 5, 22)
            )
        )

        // May 23rd at 4 AM should have passed the cutoff for May 22nd
        assertTrue(
            StreakCalculator.hasCutoffPassed(
                streak = streak,
                date = LocalDate.of(2026, 5, 22),
                nowTime = LocalTime.of(4, 0),
                nowDate = LocalDate.of(2026, 5, 23)
            )
        )
    }

    @Test
    fun testCutoffMidnight() {
        val streak = Streak(
            title = "Midnight Check",
            description = "",
            emoji = "",
            category = StreakCategory.CUSTOM,
            color = 0,
            creationDate = "2026-05-20",
            cutoffTime = "00:00",
            gracePeriodUntil3AM = false
        )

        // Today is May 22nd. 12:00 PM (noon) should NOT have passed the cutoff (ends midnight of the next day)
        assertFalse(
            StreakCalculator.hasCutoffPassed(
                streak = streak,
                date = LocalDate.of(2026, 5, 22),
                nowTime = LocalTime.of(12, 0),
                nowDate = LocalDate.of(2026, 5, 22)
            )
        )

        // May 22nd at 11:59 PM should NOT have passed the cutoff
        assertFalse(
            StreakCalculator.hasCutoffPassed(
                streak = streak,
                date = LocalDate.of(2026, 5, 22),
                nowTime = LocalTime.of(23, 59),
                nowDate = LocalDate.of(2026, 5, 22)
            )
        )

        // May 23rd at 00:01 AM should have passed the cutoff for May 22nd
        assertTrue(
            StreakCalculator.hasCutoffPassed(
                streak = streak,
                date = LocalDate.of(2026, 5, 22),
                nowTime = LocalTime.of(0, 1),
                nowDate = LocalDate.of(2026, 5, 23)
            )
        )
    }
}
