package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import java.time.LocalDate
import javax.inject.Inject

class CheckAndResetMissedAppStreaksUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(nowDate: LocalDate = AppClock.now()): Int {
        val enabledApps = appStreakRepository.getEnabledTrackedApps()
        var updatedCount = 0

        for (app in enabledApps) {
            if (app.isPaused || app.isArchived) continue

            val completions = appStreakRepository.getCompletionsForApp(app.id)
            val stats = AppStreakCalculator.calculateAppStreak(app, completions, nowDate)

            if (stats.currentStreak != app.currentStreak || stats.longestStreak != app.longestStreak) {
                appStreakRepository.updateTrackedApp(
                    app.copy(
                        currentStreak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                updatedCount++
            }
        }

        return updatedCount
    }
}
