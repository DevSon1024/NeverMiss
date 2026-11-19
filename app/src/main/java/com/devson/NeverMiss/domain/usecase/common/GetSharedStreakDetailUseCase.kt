package com.devson.NeverMiss.domain.usecase.common

import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.SharedStreakDetail
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.model.StreakType
import com.devson.NeverMiss.domain.repository.AppStreakRepository
import com.devson.NeverMiss.domain.repository.StreakRepository
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

class GetSharedStreakDetailUseCase @Inject constructor(
    private val streakRepository: StreakRepository,
    private val appStreakRepository: AppStreakRepository
) {
    suspend operator fun invoke(type: StreakType, id: Long): SharedStreakDetail? {
        return when (type) {
            StreakType.NORMAL -> {
                val streak = streakRepository.getStreakById(id) ?: return null
                val history = streakRepository.getHistoryForStreak(id)
                val completedCount = history.count { it.status == CompletionStatus.COMPLETED }
                SharedStreakDetail(
                    id = streak.id,
                    type = StreakType.NORMAL,
                    name = streak.title,
                    description = streak.description.ifEmpty { null },
                    icon = streak.icon,
                    emoji = streak.emoji,
                    color = streak.color,
                    currentStreak = streak.currentStreak,
                    longestStreak = streak.longestStreak,
                    lastCompletedDate = streak.lastCompletedDate,
                    isPaused = streak.isPaused,
                    isArchived = streak.isArchived,
                    status = streak.status,
                    creationDate = streak.creationDate,
                    startDate = streak.startDate,
                    totalCompletedDays = completedCount,
                    updatedAt = streak.updatedAt
                )
            }
            StreakType.APP -> {
                val app = appStreakRepository.getTrackedAppById(id) ?: return null
                val completions = appStreakRepository.getCompletionsForApp(id)
                val creationDateStr = StreakCalculator.formatDate(
                    Instant.ofEpochMilli(app.trackingCreatedAt)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                )
                val completedDaysCount = completions.map { it.completionDate }.distinct().size
                SharedStreakDetail(
                    id = app.id,
                    type = StreakType.APP,
                    name = app.appName,
                    description = app.packageName,
                    icon = null,
                    emoji = null,
                    color = null,
                    currentStreak = app.currentStreak,
                    longestStreak = app.longestStreak,
                    lastCompletedDate = app.lastCompletedDate,
                    isPaused = app.isPaused,
                    isArchived = app.isArchived,
                    status = app.status,
                    creationDate = creationDateStr,
                    startDate = creationDateStr,
                    totalCompletedDays = completedDaysCount,
                    updatedAt = app.updatedAt
                )
            }
        }
    }
}
