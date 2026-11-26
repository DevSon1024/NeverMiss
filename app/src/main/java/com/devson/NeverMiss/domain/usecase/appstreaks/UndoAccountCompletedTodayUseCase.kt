package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class UndoAccountCompletedTodayUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long, accountId: Long): Boolean {
        val account = appStreakRepository.getAccountById(accountId) ?: return false
        val todayStr = StreakCalculator.formatDate(AppClock.now())
        val nowMillis = System.currentTimeMillis()

        appStreakRepository.undoAccountCompletion(appId, accountId, todayStr)

        val remainingCompletions = appStreakRepository.getCompletionsForAccount(accountId)
        val stats = AppStreakCalculator.calculateAccountStreak(account, remainingCompletions)
        val prevLastCompleted = remainingCompletions.maxByOrNull { it.completionDate }?.completionDate ?: ""

        appStreakRepository.updateAccount(
            account.copy(
                currentStreak = stats.currentStreak,
                longestStreak = stats.longestStreak,
                lastCompletedDate = prevLastCompleted,
                updatedAt = nowMillis
            )
        )

        val app = appStreakRepository.getTrackedAppById(appId)
        if (app != null && app.lastCompletedDate == todayStr) {
            val allAppCompletions = appStreakRepository.getCompletionsForApp(appId)
            val prevAppLastCompleted = allAppCompletions.maxByOrNull { it.completionDate }?.completionDate ?: ""
            appStreakRepository.updateTrackedApp(
                app.copy(
                    currentStreak = (app.currentStreak - 1).coerceAtLeast(0),
                    lastCompletedDate = prevAppLastCompleted,
                    updatedAt = nowMillis
                )
            )
        }
        return true
    }
}
