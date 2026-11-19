package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class RemoveTrackedAppUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long) {
        appStreakRepository.deleteTrackedAppById(appId)
    }
}

