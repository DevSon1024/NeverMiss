package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmationWithDetails
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetActivePendingConfirmationsUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    operator fun invoke(): Flow<List<PendingConfirmationWithDetails>> {
        return appStreakRepository.getActivePendingConfirmationsFlow().map { pendingList ->
            pendingList.mapNotNull { confirmation ->
                val app = appStreakRepository.getTrackedAppById(confirmation.trackedAppId)
                if (app != null) {
                    val accounts = appStreakRepository.getEnabledAccountsForApp(app.id)
                    PendingConfirmationWithDetails(
                        confirmation = confirmation,
                        app = app,
                        availableAccounts = accounts
                    )
                } else {
                    null
                }
            }
        }
    }
}

