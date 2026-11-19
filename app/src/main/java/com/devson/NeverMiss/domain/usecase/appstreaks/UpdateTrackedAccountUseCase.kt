package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class UpdateTrackedAccountUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(account: TrackedAccount) {
        appStreakRepository.updateAccount(account.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun renameAccount(accountId: Long, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false

        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(
            existing.copy(
                accountName = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun toggleAccountEnabled(accountId: Long): Boolean {
        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(
            existing.copy(
                isEnabled = !existing.isEnabled,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun toggleAccountPause(accountId: Long): Boolean {
        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(
            existing.copy(
                isPaused = !existing.isPaused,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun toggleAccountArchive(accountId: Long): Boolean {
        val existing = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.updateAccount(
            existing.copy(
                isArchived = !existing.isArchived,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }
}
