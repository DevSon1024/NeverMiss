package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class ExpirePendingConfirmationsUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(nowMillis: Long = System.currentTimeMillis()): Int {
        return appStreakRepository.expireOldConfirmations(nowMillis)
    }
}

