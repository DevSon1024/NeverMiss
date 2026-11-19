package com.devson.NeverMiss.domain.usecase.common

import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import javax.inject.Inject

class ToggleStreakPauseUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(type: StreakType, id: Long): Boolean {
        return when (type) {
            StreakType.NORMAL -> {
                val streak = streakRepository.getStreakById(id) ?: return false
                val newPaused = !streak.isPaused
                streakRepository.updateStreak(
                    streak.copy(
                        isPaused = newPaused,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
            StreakType.APP -> {
                val app = appStreakRepository.getTrackedAppById(id) ?: return false
                val newPaused = !app.isPaused
                appStreakRepository.updateTrackedApp(
                    app.copy(
                        isPaused = newPaused,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
    }
}
