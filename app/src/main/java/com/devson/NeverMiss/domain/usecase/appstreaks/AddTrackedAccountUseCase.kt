package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class AddTrackedAccountUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long, accountName: String, initialStreak: Int = 0): Long? {
        val trimmed = accountName.trim()
        if (trimmed.isEmpty()) return null

        val newAccount = TrackedAccount(
            trackedAppId = appId,
            accountName = trimmed,
            currentStreak = initialStreak,
            longestStreak = initialStreak,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        val accountId = appStreakRepository.addAccount(newAccount)
        if (accountId > 0 && initialStreak > 0) {
            val today = AppClock.now()
            var checkDate = today.minusDays(1)
            for (i in 0 until initialStreak) {
                appStreakRepository.logAccountCompletion(
                    appId = appId,
                    accountId = accountId,
                    date = StreakCalculator.formatDate(checkDate),
                    timestamp = System.currentTimeMillis()
                )
                checkDate = checkDate.minusDays(1)
            }
        }
        return accountId
    }
}
