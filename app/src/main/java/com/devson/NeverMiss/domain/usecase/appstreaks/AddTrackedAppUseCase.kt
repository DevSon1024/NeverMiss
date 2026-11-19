package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.domain.model.apptracking.TrackedApp
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class AddTrackedAppUseCase @Inject constructor(
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(packageName: String, appName: String): Long {
        val existing = appStreakRepository.getTrackedAppByPackage(packageName)
        return if (existing != null) {
            if (!existing.isEnabled) {
                appStreakRepository.updateTrackedApp(existing.copy(isEnabled = true))
            }
            existing.id
        } else {
            val newApp = TrackedApp(
                packageName = packageName,
                appName = appName,
                isEnabled = true,
                trackingCreatedAt = System.currentTimeMillis()
            )
            appStreakRepository.addTrackedApp(newApp)
        }
    }
}

