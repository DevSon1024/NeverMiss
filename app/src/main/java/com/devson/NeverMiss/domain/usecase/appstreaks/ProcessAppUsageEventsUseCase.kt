package com.devson.NeverMiss.domain.usecase.appstreaks

import com.devson.NeverMiss.core.apptracking.engine.AppUsageTrackingEngine
import com.devson.NeverMiss.core.apptracking.notification.AccountConfirmationNotifier
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
    private val appStreakRepository: AppStreakRepository,
    private val accountConfirmationNotifier: AccountConfirmationNotifier
) {
    companion object {
        const val MIN_SINGLE_ACCOUNT_VERIFIED_MILLIS = 15_000L
    }

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

            // Paused and archived App Streaks must not receive new automatic completions
            if (app.isPaused || app.isArchived || !app.isEnabled) {
                continue
            }

            val accounts = appStreakRepository.getAccountsForApp(app.id)

            // Single-account 15-second verification rule:
            // When an App Streak contains exactly one tracked account, require at least 15 seconds
            // of verified foreground usage before considering the app session eligible.
            if (accounts.size == 1 && detection.maxSingleSessionDurationMillis < MIN_SINGLE_ACCOUNT_VERIFIED_MILLIS) {
                continue
            }

            val wasLogged = appStreakRepository.logAppCompletion(
                appId = app.id,
                date = date,
                timestamp = detection.firstDetectedTimestamp,
                method = CompletionMethod.AUTOMATIC_USAGE
            )

            if (wasLogged) {
                newlyCompletedCount++
                val completions = appStreakRepository.getCompletionsForApp(app.id)
                val stats = AppStreakCalculator.calculateAppStreak(app, completions)

                appStreakRepository.updateTrackedApp(
                    app.copy(
                        currentStreak = stats.currentStreak,
                        longestStreak = stats.longestStreak,
                        lastCompletedDate = date,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }

            // Queue confirmation flow when accounts are present
            if (accounts.isNotEmpty()) {
                val existingPending = appStreakRepository.getPendingConfirmationsForApp(app.id)
                val alreadyPendingForDate = existingPending.any { it.sessionDate == date }

                if (!alreadyPendingForDate) {
                    val pending = PendingConfirmation(
                        trackedAppId = app.id,
                        sessionTimestamp = detection.firstDetectedTimestamp,
                        sessionDate = date
                    )
                    val insertedId = appStreakRepository.addPendingConfirmation(pending)
                    confirmationsQueued++

                    accountConfirmationNotifier.notifyPendingConfirmation(
                        confirmation = pending.copy(id = insertedId),
                        app = app,
                        accounts = accounts
                    )
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
