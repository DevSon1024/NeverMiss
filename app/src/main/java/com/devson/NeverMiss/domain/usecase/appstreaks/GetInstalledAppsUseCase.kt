package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.data.local.apps.InstalledAppInfo
import com.devson.NeverMiss.data.local.apps.InstalledAppScanner
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

class GetInstalledAppsUseCase @Inject constructor(
    private val installedAppScanner: InstalledAppScanner,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(): List<InstalledAppInfo> {
        val installedApps = installedAppScanner.getInstalledLaunchableApps()
        val trackedApps = appStreakRepository.getAllTrackedApps().associateBy { it.packageName }

        return installedApps.map { app ->
            val tracked = trackedApps[app.packageName]
            if (tracked != null) {
                app.copy(
                    isTracked = true,
                    trackedAppId = tracked.id,
                    isTrackingEnabled = tracked.isEnabled
                )
            } else {
                app.copy(
                    isTracked = false,
                    trackedAppId = null,
                    isTrackingEnabled = false
                )
            }
        }
    }
}

