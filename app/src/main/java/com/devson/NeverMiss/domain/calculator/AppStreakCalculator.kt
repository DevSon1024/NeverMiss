package com.devson.NeverMiss.domain.calculator

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.AppStreakStats
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import java.time.LocalDate

object AppStreakCalculator {

    fun calculateAppStreak(
        app: TrackedApp,
        completions: List<AppStreakCompletion>,
        nowDate: LocalDate = AppClock.now()
    ): AppStreakStats {
        if (app.isPaused) {
            return AppStreakStats(
                currentStreak = app.currentStreak,
                longestStreak = app.longestStreak,
                totalCompletedDays = completions.map { it.completionDate }.distinct().size,
                isCompletedToday = app.lastCompletedDate == StreakCalculator.formatDate(nowDate)
            )
        }

        val completedDates = completions
            .map { it.completionDate }
            .distinct()
            .toSet()

        val stats = calculateStatsFromDates(completedDates, nowDate)
        return stats.copy(
            longestStreak = maxOf(app.longestStreak, stats.longestStreak)
        )
    }

    fun calculateAccountStreak(
        account: TrackedAccount,
        completions: List<AppStreakCompletion>,
        nowDate: LocalDate = AppClock.now()
    ): AppStreakStats {
        if (account.isPaused) {
            return AppStreakStats(
                currentStreak = account.currentStreak,
                longestStreak = account.longestStreak,
                totalCompletedDays = completions.map { it.completionDate }.distinct().size,
                isCompletedToday = account.lastCompletedDate == StreakCalculator.formatDate(nowDate)
            )
        }

        val completedDates = completions
            .map { it.completionDate }
            .distinct()
            .toSet()

        val stats = calculateStatsFromDates(completedDates, nowDate)
        return stats.copy(
            longestStreak = maxOf(account.longestStreak, stats.longestStreak)
        )
    }

    fun calculateStatsFromDates(
        completedDates: Set<String>,
        nowDate: LocalDate = AppClock.now()
    ): AppStreakStats {
        if (completedDates.isEmpty()) {
            return AppStreakStats(
                currentStreak = 0,
                longestStreak = 0,
                totalCompletedDays = 0,
                isCompletedToday = false
            )
        }

        val todayStr = StreakCalculator.formatDate(nowDate)
        val isCompletedToday = completedDates.contains(todayStr)

        // 1. Calculate Current Streak
        var currentStreak = 0
        val startCheckDate = if (isCompletedToday) nowDate else nowDate.minusDays(1)
        var checkDate = startCheckDate

        while (completedDates.contains(StreakCalculator.formatDate(checkDate))) {
            currentStreak++
            checkDate = checkDate.minusDays(1)
        }

        // 2. Calculate Longest Streak
        val sortedDates = completedDates
            .map { StreakCalculator.parseDate(it) }
            .sorted()

        var longestStreak = 0
        var runningStreak = 0
        var previousDate: LocalDate? = null

        for (date in sortedDates) {
            if (previousDate == null || date == previousDate.plusDays(1)) {
                runningStreak++
            } else if (date != previousDate) {
                runningStreak = 1
            }
            if (runningStreak > longestStreak) {
                longestStreak = runningStreak
            }
            previousDate = date
        }

        return AppStreakStats(
            currentStreak = currentStreak,
            longestStreak = maxOf(currentStreak, longestStreak),
            totalCompletedDays = completedDates.size,
            isCompletedToday = isCompletedToday
        )
    }
}
