package com.devson.NeverMiss.domain.usecase.common

import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import javax.inject.Inject

class ToggleStreakArchiveUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(type: StreakType, id: Long): Boolean {
        return when (type) {
            StreakType.NORMAL -> {
                val streak = streakRepository.getStreakById(id) ?: return false
                val newArchived = !streak.isArchived
                streakRepository.updateStreak(
                    streak.copy(
                        isArchived = newArchived,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
            StreakType.APP -> {
                val app = appStreakRepository.getTrackedAppById(id) ?: return false
                val newArchived = !app.isArchived
                appStreakRepository.updateTrackedApp(
                    app.copy(
                        isArchived = newArchived,
                        updatedAt = System.currentTimeMillis()
                    )
                )
                true
            }
        }
    }
}
