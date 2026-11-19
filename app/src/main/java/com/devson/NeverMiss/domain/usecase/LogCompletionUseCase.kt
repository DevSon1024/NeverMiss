package com.devson.NeverMiss.domain.usecase

import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.core.widget.StreakWidgetProvider
import com.devson.NeverMiss.domain.model.CompletionStatus
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class LogCompletionUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(streakId: Long, date: String, status: CompletionStatus?): Boolean {
        val streak = repository.getStreakById(streakId) ?: return false
        
        val existingLog = repository.getHistoryForStreakOnDate(streakId, date)
        
        if (existingLog?.status == CompletionStatus.COMPLETED && status == CompletionStatus.COMPLETED) {
            // Already completed today, ignore
            return true
        }

        // Save status in DB
        repository.logCompletionState(
            streakId = streakId,
            date = date,
            status = status?.name ?: "",
            timestamp = System.currentTimeMillis()
        )

        // Fetch updated history and recalculate streaks
        val updatedHistory = repository.getHistoryForStreak(streakId)
        val stats = StreakCalculator.calculateStreaks(streak, updatedHistory)

        // Determine new last completed date
        var newLastCompletedDate = streak.lastCompletedDate
        if (status == CompletionStatus.COMPLETED) {
            newLastCompletedDate = date
        } else if (existingLog?.status == CompletionStatus.COMPLETED && status != CompletionStatus.COMPLETED) {
            // If we removed today's completion, get the previous completed date from history
            val lastCompleted = updatedHistory
                .filter { it.status == CompletionStatus.COMPLETED }
                .maxByOrNull { it.date }
            newLastCompletedDate = lastCompleted?.date ?: ""
        }

        // Update streak details
        val updatedStreak = streak.copy(
            currentStreak = stats.currentStreak,
            longestStreak = stats.longestStreak,
            lastCompletedDate = newLastCompletedDate,
            updatedAt = System.currentTimeMillis()
        )
        repository.updateStreak(updatedStreak)

        // Reschedule alarms
        reminderScheduler.scheduleNextAlarm(streakId)

        // Update Home Screen widgets
        triggerWidgetUpdate()

        return true
    }

    private fun triggerWidgetUpdate() {
        val intent = Intent(context, StreakWidgetProvider::class.java).apply {
            action = StreakWidgetProvider.ACTION_UPDATE_WIDGETS
        }
        context.sendBroadcast(intent)
    }
}
