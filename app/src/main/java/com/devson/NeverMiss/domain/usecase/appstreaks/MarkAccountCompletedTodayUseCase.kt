package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class MarkAccountCompletedTodayUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long, accountId: Long): Boolean {
        val account = appStreakRepository.getAccountById(accountId) ?: return false
        val todayStr = StreakCalculator.formatDate(AppClock.now())
        val nowMillis = System.currentTimeMillis()

        val wasLogged = appStreakRepository.logAccountCompletion(
            appId = appId,
            accountId = accountId,
            date = todayStr,
            timestamp = nowMillis,
            method = CompletionMethod.MANUAL_CONFIRMATION
        )

        val completions = appStreakRepository.getCompletionsForAccount(accountId)
        val stats = AppStreakCalculator.calculateAccountStreak(account, completions)

        if (wasLogged || account.lastCompletedDate != todayStr) {
            appStreakRepository.updateAccount(
                account.copy(
                    currentStreak = stats.currentStreak,
                    longestStreak = maxOf(account.longestStreak, stats.longestStreak),
                    lastCompletedDate = todayStr,
                    updatedAt = nowMillis
                )
            )
        }

        val app = appStreakRepository.getTrackedAppById(appId)
        if (app != null) {
            val allAccounts = appStreakRepository.getAccountsForApp(appId)
            val activeAccounts = allAccounts.filter { it.isEnabled && !it.isArchived }
            val allDone = activeAccounts.isNotEmpty() && activeAccounts.all {
                it.id == accountId || it.lastCompletedDate == todayStr
            }
            if (allDone && app.lastCompletedDate != todayStr) {
                val nextAppStreak = app.currentStreak + 1
                appStreakRepository.updateTrackedApp(
                    app.copy(
                        currentStreak = nextAppStreak,
                        longestStreak = maxOf(app.longestStreak, nextAppStreak),
                        lastCompletedDate = todayStr,
                        updatedAt = nowMillis
                    )
                )
            }
        }
        return true
    }
}
