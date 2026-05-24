package com.devson.NeverMiss.domain.usecase

import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.repository.StreakRepository
import javax.inject.Inject

class DeleteStreakUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(streak: Streak) {
        repository.deleteStreak(streak)
        reminderScheduler.cancelAlarm(streak.id)
    }
}
