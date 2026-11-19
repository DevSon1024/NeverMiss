package com.devson.NeverMiss.domain.usecase

import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.core.reminder.ReminderScheduler
import com.devson.NeverMiss.core.widget.StreakWidgetProvider
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CheckAndResetMissedStreaksUseCase @Inject constructor(
    private val repository: StreakRepository,
    private val reminderScheduler: ReminderScheduler,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke() {
        val activeStreaks = repository.getActiveStreaks()
        val nowDate = com.devson.NeverMiss.core.util.AppClock.now()
        val nowTime = com.devson.NeverMiss.core.util.AppClock.nowTime()

        var hasChanges = false
        for (streak in activeStreaks) {
            if (streak.isPaused || streak.isArchived) continue

            val history = repository.getHistoryForStreak(streak.id)
            val stats = StreakCalculator.calculateStreaks(streak, history, nowDate, nowTime)
            
            if (stats.currentStreak != streak.currentStreak) {
                val updatedStreak = streak.copy(
                    currentStreak = stats.currentStreak,
                    longestStreak = stats.longestStreak,
                    updatedAt = System.currentTimeMillis()
                )
                repository.updateStreak(updatedStreak)
                hasChanges = true
            }

            // Always reschedule reminder to ensure it never gets stuck on date change
            reminderScheduler.scheduleNextAlarm(streak.id)
        }

        if (hasChanges) {
            val intent = Intent(context, StreakWidgetProvider::class.java).apply {
                action = StreakWidgetProvider.ACTION_UPDATE_WIDGETS
            }
            context.sendBroadcast(intent)
        }
    }
}
