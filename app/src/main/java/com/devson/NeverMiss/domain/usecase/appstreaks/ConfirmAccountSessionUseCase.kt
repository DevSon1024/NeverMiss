package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmationStatus
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class ConfirmAccountSessionUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(
        pendingConfirmationId: Long,
        selectedAccountIds: List<Long>,
        dismissed: Boolean = false
    ): Boolean {
        val confirmation = appStreakRepository.getPendingConfirmationById(pendingConfirmationId) ?: return false
        if (confirmation.status != PendingConfirmationStatus.PENDING) {
            return false
        }

        if (dismissed || selectedAccountIds.isEmpty()) {
            appStreakRepository.updatePendingConfirmation(
                confirmation.copy(status = PendingConfirmationStatus.DISMISSED)
            )
            return true
        }

        val appAccounts = appStreakRepository.getAccountsForApp(confirmation.trackedAppId)
        val validAccountMap = appAccounts.associateBy { it.id }

        for (accountId in selectedAccountIds) {
            val account = validAccountMap[accountId] ?: continue

            val wasLogged = appStreakRepository.logAccountCompletion(
                appId = confirmation.trackedAppId,
                accountId = accountId,
                date = confirmation.sessionDate,
                timestamp = confirmation.sessionTimestamp,
                method = CompletionMethod.MANUAL_CONFIRMATION
            )

            if (wasLogged) {
                val completions = appStreakRepository.getCompletionsForAccount(accountId)
                val stats = AppStreakCalculator.calculateAccountStreak(account, completions)

                appStreakRepository.updateAccount(
                    account.copy(
                        currentStreak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        lastCompletedDate = confirmation.sessionDate
                    )
                )
            }
        }

        appStreakRepository.updatePendingConfirmation(
            confirmation.copy(status = PendingConfirmationStatus.CONFIRMED)
        )
        return true
    }
}

