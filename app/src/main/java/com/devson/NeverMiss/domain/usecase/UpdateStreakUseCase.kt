package com.devson.NeverMiss.domain.usecase

import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import java.time.LocalDate
import javax.inject.Inject

class UpdateStreakUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(streak: Streak, updatedStreakCount: Int? = null) {
        if (updatedStreakCount != null && updatedStreakCount > 0 && updatedStreakCount != streak.currentStreak) {
            val today = AppClock.now()
            val scheduledDates = mutableListOf<LocalDate>()
            var checkDate = today.minusDays(1)

            while (scheduledDates.size < updatedStreakCount) {
                val isScheduled = if (streak.daysOfWeek.isNotEmpty()) {
                    streak.daysOfWeek.contains(checkDate.dayOfWeek.value)
                } else {
                    true
                }
                if (isScheduled) {
                    scheduledDates.add(checkDate)
                }
                checkDate = checkDate.minusDays(1)
            }

            val earliestDate = scheduledDates.last()
            val latestCompletedDate = scheduledDates.first()

            val existingCreationDate = StreakCalculator.parseDate(streak.creationDate)
            val newCreationDate = if (earliestDate.isBefore(existingCreationDate)) {
                StreakCalculator.formatDate(earliestDate)
            } else {
                streak.creationDate
            }

            val updatedStreak = streak.copy(
                creationDate = newCreationDate,
                currentStreak = updatedStreakCount,
                longestStreak = maxOf(streak.longestStreak, updatedStreakCount),
                lastCompletedDate = StreakCalculator.formatDate(latestCompletedDate)
            )

            repository.updateStreak(updatedStreak)

            for (date in scheduledDates) {
                repository.logCompletionState(
                    streakId = streak.id,
                    date = StreakCalculator.formatDate(date),
                    status = CompletionStatus.COMPLETED.name,
                    timestamp = System.currentTimeMillis()
                )
            }
        } else {
            repository.updateStreak(streak)
        }
        reminderScheduler.scheduleNextAlarm(streak.id)
    }
}
