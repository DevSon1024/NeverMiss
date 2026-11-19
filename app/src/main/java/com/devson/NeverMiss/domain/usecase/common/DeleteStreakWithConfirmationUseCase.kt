package com.devson.NeverMiss.domain.usecase.common

import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import javax.inject.Inject

data class DeletionConfirmation(
    val id: Long,
    val type: StreakType,
    val name: String,
    val affectedRecordCount: Int
)

class DeleteStreakWithConfirmationUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository
) {
    suspend fun getConfirmationInfo(type: StreakType, id: Long): DeletionConfirmation? {
        return when (type) {
            StreakType.NORMAL -> {
                val streak = streakRepository.getStreakById(id) ?: return null
                val history = streakRepository.getHistoryForStreak(id)
                DeletionConfirmation(
                    id = id,
                    type = type,
                    name = streak.title,
                    affectedRecordCount = history.size
                )
            }
            StreakType.APP -> {
                val app = appStreakRepository.getTrackedAppById(id) ?: return null
                val completions = appStreakRepository.getCompletionsForApp(id)
                val accounts = appStreakRepository.getAccountsForApp(id)
                DeletionConfirmation(
                    id = id,
                    type = type,
                    name = app.appName,
                    affectedRecordCount = completions.size + accounts.size
                )
            }
        }
    }

    suspend fun executeDelete(type: StreakType, id: Long): Boolean {
        return when (type) {
            StreakType.NORMAL -> {
                val streak = streakRepository.getStreakById(id) ?: return false
                streakRepository.deleteStreak(streak)
                true
            }
            StreakType.APP -> {
                val app = appStreakRepository.getTrackedAppById(id) ?: return false
                appStreakRepository.deleteTrackedApp(app)
                true
            }
        }
    }
}
