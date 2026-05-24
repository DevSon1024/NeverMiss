package com.devson.NeverMiss.core.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.devson.NeverMiss.domain.model.StreakCalculator
import com.devson.NeverMiss.domain.repository.StreakRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var repository: StreakRepository

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var reminderScheduler: ReminderScheduler

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val streakId = intent.getLongExtra("STREAK_ID", -1L)
        if (streakId == -1L) return

        val pendingResult = goAsync()

        scope.launch {
            try {
                val streak = repository.getStreakById(streakId)
                if (streak != null && !streak.isArchived && !streak.isPaused) {
                    val today = com.devson.NeverMiss.core.util.AppClock.now()
                    val todayStr = StreakCalculator.formatDate(today)
                    
                    // Check if already completed today
                    val isCompleted = streak.lastCompletedDate == todayStr
                    
                    if (!isCompleted) {
                        // Check if it's danger time (1 hour before cutoff)
                        // Trigger appropriate message
                        val dangerMessage = "Danger! Your streak '${streak.title}' is about to break!"
                        val standardMessage = "Time to complete your streak: ${streak.title}!"

                        // If warning is configured and we are close to cutoff, show danger warning
                        val minutesToCutoff = 60 // Mock danger limit
                        
                        notificationHelper.showReminderNotification(streak, standardMessage)
                    }

                    // Reschedule next recurrence (repeating or next day)
                    reminderScheduler.scheduleNextAlarm(streakId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
