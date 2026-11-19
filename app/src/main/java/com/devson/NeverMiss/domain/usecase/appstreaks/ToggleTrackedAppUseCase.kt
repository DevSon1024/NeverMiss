package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class ToggleTrackedAppUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(appId: Long): Boolean {
        val app = appStreakRepository.getTrackedAppById(appId) ?: return false
        val newEnabled = !app.isEnabled
        appStreakRepository.updateTrackedApp(app.copy(isEnabled = newEnabled))
        return newEnabled
    }
}

