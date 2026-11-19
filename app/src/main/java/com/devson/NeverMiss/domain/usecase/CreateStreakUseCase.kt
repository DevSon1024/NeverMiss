package com.devson.NeverMiss.domain.usecase

import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.core.util.AppClock
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import java.time.LocalDate
import javax.inject.Inject

class CreateStreakUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(streak: Streak, initialStreakCount: Int = 0): Long {
        if (initialStreakCount <= 0) {
            val id = repository.insertStreak(streak)
            reminderScheduler.scheduleNextAlarm(id)
            return id
        }

        // Backfill initial completion history for already maintained streaks
        val today = AppClock.now()
        val scheduledDates = mutableListOf<LocalDate>()
        var checkDate = today.minusDays(1)

        while (scheduledDates.size < initialStreakCount) {
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
        val earliestDateStr = StreakCalculator.formatDate(earliestDate)

        val preparedStreak = streak.copy(
            creationDate = earliestDateStr,
            startDate = earliestDateStr,
            currentStreak = initialStreakCount,
            longestStreak = initialStreakCount,
            lastCompletedDate = StreakCalculator.formatDate(latestCompletedDate)
        )

        val id = repository.insertStreak(preparedStreak)

        for (date in scheduledDates) {
            repository.logCompletionState(
                streakId = id,
                date = StreakCalculator.formatDate(date),
                status = CompletionStatus.COMPLETED.name,
                timestamp = System.currentTimeMillis()
            )
        }

        reminderScheduler.scheduleNextAlarm(id)
        return id
    }
}
