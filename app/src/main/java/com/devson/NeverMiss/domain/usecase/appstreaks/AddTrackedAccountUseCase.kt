package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class AddTrackedAccountUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long, accountName: String): Long? {
        val trimmed = accountName.trim()
        if (trimmed.isEmpty()) return null

        val newAccount = TrackedAccount(
            trackedAppId = appId,
            accountName = trimmed,
            isEnabled = true,
            createdAt = System.currentTimeMillis()
        )
        return appStreakRepository.addAccount(newAccount)
    }
}

