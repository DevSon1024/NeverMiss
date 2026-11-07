package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.TrackedAccount
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class DeleteTrackedAccountUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(account: TrackedAccount) {
        appStreakRepository.deleteAccount(account)
    }

    suspend fun deleteById(accountId: Long): Boolean {
        val account = appStreakRepository.getAccountById(accountId) ?: return false
        appStreakRepository.deleteAccount(account)
        return true
    }
}

