package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.AppStreakCompletion
import com.devson.NeverMiss.domain.model.apptracking.AppStreakStats
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

data class TrackedAccountWithStats(
    val account: TrackedAccount,
    val stats: AppStreakStats
)

data class AppStreakDetail(
    val app: TrackedApp,
    val stats: AppStreakStats,
    val completions: List<AppStreakCompletion>,
    val accountsWithStats: List<TrackedAccountWithStats>
)

class GetAppStreakDetailsUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long): AppStreakDetail? {
        val app = appStreakRepository.getTrackedAppById(appId) ?: return null
        val completions = appStreakRepository.getCompletionsForApp(appId)
        val stats = AppStreakCalculator.calculateAppStreak(app, completions)

        val accounts = appStreakRepository.getAccountsForApp(appId)
        val accountsWithStats = accounts.map { account ->
            val accountCompletions = appStreakRepository.getCompletionsForAccount(account.id)
            val accountStats = AppStreakCalculator.calculateAccountStreak(account, accountCompletions)
            TrackedAccountWithStats(
                account = account,
                stats = accountStats
            )
        }

        return AppStreakDetail(
            app = app,
            stats = stats,
            completions = completions,
            accountsWithStats = accountsWithStats
        )
    }
}

