package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class UpdateTrackedAccountUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(account: TrackedAccount) {
        appStreakRepository.updateAccount(account)
    }

    suspend fun renameAccount(accountId: Long, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(existing.copy(accountName = trimmed))
        return true
    }

    suspend fun toggleAccountEnabled(accountId: Long): Boolean {
        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(existing.copy(isEnabled = !existing.isEnabled))
        return true
    }
}

