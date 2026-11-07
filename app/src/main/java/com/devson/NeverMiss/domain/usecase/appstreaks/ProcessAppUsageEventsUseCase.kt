package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.apptracking.engine.AppUsageTrackingEngine
import com.devson.NeverMiss.domain.calculator.AppStreakCalculator
import com.devson.NeverMiss.domain.model.apptracking.CompletionMethod
import com.devson.NeverMiss.domain.model.apptracking.PendingConfirmation
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import javax.inject.Inject

data class ProcessUsageSummary(
    val detectedAppsCount: Int,
    val newlyCompletedAppsCount: Int,
    val pendingConfirmationsQueued: Int
)

class ProcessAppUsageEventsUseCase @Inject constructor(
    private val appUsageTrackingEngine: AppUsageTrackingEngine,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(endTime: Long = System.currentTimeMillis()): ProcessUsageSummary {
        val report = appUsageTrackingEngine.scanAndDetectUsage(endTime)
        if (!report.isPermissionGranted || report.detectedAppUsages.isEmpty()) {
            return ProcessUsageSummary(0, 0, 0)
        }

        var newlyCompletedCount = 0
        var confirmationsQueued = 0

        for (detection in report.detectedAppUsages) {
            val app = detection.trackedApp
            val date = detection.sessionDate

            val wasLogged = appStreakRepository.logAppCompletion(
                appId = app.id,
                date = date,
                timestamp = detection.firstDetectedTimestamp,
                method = CompletionMethod.AUTOMATIC_USAGE
            )

            if (wasLogged) {
                newlyCompletedCount++
                // Recalculate streak metrics
                val completions = appStreakRepository.getCompletionsForApp(app.id)
                val stats = AppStreakCalculator.calculateAppStreak(app, completions)

                appStreakRepository.updateTrackedApp(
                    app.copy(
                        currentStreak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        lastCompletedDate = date
                    )
                )
            }

            // Check if app has accounts for sub-streak attribution
            val accounts = appStreakRepository.getAccountsForApp(app.id)
            if (accounts.isNotEmpty()) {
                val existingPending = appStreakRepository.getPendingConfirmationsForApp(app.id)
                val alreadyPendingForDate = existingPending.any { it.sessionDate == date }

                if (!alreadyPendingForDate) {
                    appStreakRepository.addPendingConfirmation(
                        PendingConfirmation(
                            trackedAppId = app.id,
                            sessionTimestamp = detection.firstDetectedTimestamp,
                            sessionDate = date
                        )
                    )
                    confirmationsQueued++
                }
            }
        }

        return ProcessUsageSummary(
            detectedAppsCount = report.detectedAppUsages.size,
            newlyCompletedAppsCount = newlyCompletedCount,
            pendingConfirmationsQueued = confirmationsQueued
        )
    }
}

