package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class UpdateTrackedAppMetadataUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend fun updateAppName(appId: Long, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val app = appStreakRepository.getTrackedAppById(appId) ?: return false
        appStreakRepository.updateTrackedApp(
            app.copy(
                appName = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun togglePause(appId: Long): Boolean {
        val app = appStreakRepository.getTrackedAppById(appId) ?: return false
        appStreakRepository.updateTrackedApp(
            app.copy(
                isPaused = !app.isPaused,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }

    suspend fun toggleArchive(appId: Long): Boolean {
        val app = appStreakRepository.getTrackedAppById(appId) ?: return false
        appStreakRepository.updateTrackedApp(
            app.copy(
                isArchived = !app.isArchived,
                updatedAt = System.currentTimeMillis()
            )
        )
        return true
    }
}
