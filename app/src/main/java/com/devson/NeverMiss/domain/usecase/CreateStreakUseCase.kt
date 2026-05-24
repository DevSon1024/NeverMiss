package com.devson.NeverMiss.domain.usecase

import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.domain.model.Streak
import com.devson.NeverMiss.domain.repository.StreakRepository
import javax.inject.Inject

class CreateStreakUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler
) {
    suspend operator fun invoke(streak: Streak): Long {
        val id = repository.insertStreak(streak)
        reminderScheduler.scheduleNextAlarm(id)
        return id
    }
}
